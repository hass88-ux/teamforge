package com.teamforge;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.*;
import org.springframework.web.bind.annotation.*;

@RestController
class AccountExportController {
 private final CollaborationController collaboration;
 private final AccountRepository accounts;
 private final ProfileRepository profiles;
 private final JdbcTemplate sql;
 private final ObjectMapper json;
 AccountExportController(CollaborationController collaboration,AccountRepository accounts,ProfileRepository profiles,JdbcTemplate sql,ObjectMapper json) {
  this.collaboration=collaboration; this.accounts=accounts; this.profiles=profiles; this.sql=sql; this.json=json;
 }
 @GetMapping("/api/account/export")
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 ResponseEntity<Map<String,Object>> export(Authentication auth) throws Exception {
  UUID actor=collaboration.owner(auth); var account=accounts.findById(actor).orElseThrow();
  var result=new LinkedHashMap<String,Object>();
  result.put("formatVersion",1); result.put("exportedAt",Instant.now());
  result.put("account",Map.of("id",actor,"email",account.email,"accountType",account.accountType,"createdAt",account.createdAt));
  var profile=profiles.findById(actor);
  result.put("profile",profile.isEmpty()?null:Map.of("data",json.readTree(profile.get().profileJson),"discoverable",profile.get().discoverable,"updatedAt",profile.get().updatedAt));
  result.put("decisions",sql.queryForList("SELECT target_id,decision,created_at FROM profile_decisions WHERE actor_id=? ORDER BY created_at,target_id",actor));
  result.put("matches",sql.queryForList("SELECT id,compatibility,created_at,closed_at FROM collaboration_matches WHERE member_a=? OR member_b=? ORDER BY created_at,id",actor,actor));
  result.put("sentMessages",sql.queryForList("SELECT sequence_id,match_id,body,created_at FROM collaboration_messages WHERE sender_id=? ORDER BY sequence_id",actor));
  result.put("proposedInvitations",sql.queryForList("SELECT id,match_id,kind,starts_at,timezone,note,status,created_at FROM coffee_proposals WHERE proposer_id=? ORDER BY created_at,id",actor));
  result.put("ownedProjects",sql.queryForList("SELECT id,name,description,stage,revision,created_at FROM projects WHERE owner_id=? ORDER BY created_at,id",actor));
  result.put("teamMemberships",sql.queryForList("SELECT project_id,status,ended_reason FROM project_members WHERE account_id=? ORDER BY project_id",actor));
  result.put("blocks",sql.queryForList("SELECT target_id,created_at FROM account_blocks WHERE actor_id=? ORDER BY created_at,target_id",actor));
  result.put("reports",sql.queryForList("SELECT id,match_id,reason,details,created_at FROM safety_reports WHERE reporter_id=? ORDER BY created_at,id",actor));
  result.put("publicProfileDetails",sql.queryForList("SELECT github_url,project_history FROM profile_details WHERE account_id=?",actor));
  result.put("authoredTasks",sql.queryForList("SELECT id,project_id,title,kind,due_date,done,revision FROM project_tasks WHERE creator_id=? ORDER BY project_id,id",actor));
  result.put("scope","Your account, saved profile, choices, match metadata, authored messages/invitations, owned projects, and membership history. Other members' profiles, messages, and rosters are excluded. Retained history includes ended matches and memberships.");
  return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).cacheControl(CacheControl.noStore())
   .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=teamforge-account-data.json")
   .header("X-Content-Type-Options","nosniff").body(result);
 }
}
