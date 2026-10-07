package com.teamforge;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

@RestController
class WorkspaceController {
 private final CollaborationController collaboration;
 private final ProjectController projects;
 private final ProfileRepository profiles;
 private final JdbcTemplate sql;
 private final ObjectMapper json;
 private final DecisionLookup decisions;
 WorkspaceController(CollaborationController collaboration,ProjectController projects,ProfileRepository profiles,JdbcTemplate sql,ObjectMapper json,DecisionLookup decisions) { this.collaboration=collaboration; this.projects=projects; this.profiles=profiles; this.sql=sql; this.json=json; this.decisions=decisions; }
 record Details(@NotNull @Size(max=100) @Pattern(regexp="|https://github\\.com/[A-Za-z0-9](?:[A-Za-z0-9-]{0,37}[A-Za-z0-9])?/?") String githubUrl,@NotNull @Size(max=1000) String projectHistory) {}
 @GetMapping("/api/dashboard") Map<String,Object> dashboard(Authentication auth) {
  UUID actor=collaboration.owner(auth);
  var projectSummaries=sql.query("SELECT p.id,p.name,p.stage,CASE WHEN p.owner_id=? THEN 'OWNER' ELSE pm.status END AS your_status FROM projects p LEFT JOIN project_members pm ON pm.project_id=p.id AND pm.account_id=? WHERE p.owner_id=? OR pm.status IN ('INVITED','ACCEPTED') ORDER BY p.created_at DESC,p.id LIMIT 100",(r,n)->Map.of("id",r.getObject("id"),"name",r.getString("name"),"stage",r.getString("stage"),"yourStatus",r.getString("your_status")),actor,actor,actor);
  return Map.of("matches",collaboration.matches(auth),"projects",projectSummaries,"coffee",sql.queryForList("SELECT p.id,p.match_id,p.kind,p.starts_at,p.status FROM coffee_proposals p JOIN collaboration_matches m ON m.id=p.match_id WHERE m.closed_at IS NULL AND (m.member_a=? OR m.member_b=?) AND p.starts_at>? AND p.status IN ('PROPOSED','ACCEPTED') ORDER BY p.starts_at LIMIT 50",actor,actor,java.sql.Timestamp.from(Instant.now())));
 }
 @GetMapping("/api/profiles/me/details") Details details(Authentication auth) { return detail(collaboration.owner(auth)); }
 private Details detail(UUID id) { var rows=sql.query("SELECT github_url,project_history FROM profile_details WHERE account_id=?",(r,n)->new Details(r.getString(1),r.getString(2)),id); return rows.isEmpty()?new Details("",""):rows.getFirst(); }
 @PutMapping("/api/profiles/me/details") @Transactional Details save(@Valid @RequestBody Details d,Authentication auth) {
  UUID actor=collaboration.owner(auth); sql.queryForList("SELECT id FROM accounts WHERE id=? FOR UPDATE",actor);
  sql.update("DELETE FROM profile_details WHERE account_id=?",actor); sql.update("INSERT INTO profile_details(account_id,github_url,project_history) VALUES(?,?,?)",actor,d.githubUrl(),d.projectHistory().strip()); return detail(actor);
 }
 @GetMapping("/api/people/{id}") Map<String,Object> person(@PathVariable UUID id,Authentication auth) throws Exception {
  UUID actor=collaboration.owner(auth);
  var profile=profiles.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  if (!actor.equals(id) && (decisions.blocked(actor,id) || !profile.discoverable && sql.queryForObject("SELECT COUNT(*) FROM collaboration_matches WHERE closed_at IS NULL AND ((member_a=? AND member_b=?) OR (member_a=? AND member_b=?))",Long.class,actor,id,id,actor)==0)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  var raw=json.readTree(profile.profileJson); var output=new LinkedHashMap<String,Object>(); output.put("id",id);
  for (String field:List.of("displayName","role","description","skills","neededSkills","interests","goal","entityType","matchingIntent","weeklyHours")) output.put(field,raw.get(field));
  output.put("details",detail(id)); return output;
 }
}
