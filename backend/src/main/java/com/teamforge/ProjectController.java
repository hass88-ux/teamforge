package com.teamforge;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController @RequestMapping("/api/projects")
class ProjectController {
 private final CollaborationController collaboration;
 private final JdbcTemplate sql;
 private final TransactionTemplate transaction;
 ProjectController(CollaborationController collaboration,JdbcTemplate sql,PlatformTransactionManager manager) { this.collaboration=collaboration; this.sql=sql; transaction=new TransactionTemplate(manager); }
 record Draft(@NotNull UUID clientId,@NotBlank @Size(max=80) String name,@NotBlank @Size(max=1000) String description,@NotBlank @Pattern(regexp="Idea|Prototype|In progress") String stage) {}
 record Invite(@NotNull UUID matchId) {}
 record Response(@NotBlank @Pattern(regexp="ACCEPTED|DECLINED") String status) {}
 record Edit(@NotBlank @Size(max=80) String name,@NotBlank @Size(max=1000) String description,@NotBlank @Pattern(regexp="Idea|Prototype|In progress") String stage,@NotNull @Min(0) Long revision) {}
 record Member(UUID id,String displayName,String status) {}
 record Project(UUID id,boolean owned,String name,String description,String stage,long revision,String yourStatus,List<Member> members) {}
 private ResponseStatusException error(int status) { return new ResponseStatusException(HttpStatus.valueOf(status)); }
 Map<String,Object> access(UUID id,UUID actor,boolean owner) {
  var rows=sql.queryForList("SELECT * FROM projects WHERE id=? FOR UPDATE",id);
  if (rows.isEmpty()) throw error(404);
  var p=rows.getFirst();
  if (!actor.equals(p.get("owner_id")) && (owner || sql.queryForObject("SELECT COUNT(*) FROM project_members WHERE project_id=? AND account_id=? AND status IN ('INVITED','ACCEPTED')",Long.class,id,actor)==0)) throw error(404);
  return p;
 }
 private String name(UUID id) {
  var names=sql.query("SELECT profile_json FROM profiles WHERE account_id=?",(r,n)->r.getString(1),id);
  if (names.isEmpty()) return "Team member";
  try { return new com.fasterxml.jackson.databind.ObjectMapper().readTree(names.getFirst()).path("displayName").asText("Team member"); } catch (Exception e) { throw error(500); }
 }
 private Project view(Map<String,Object> p,UUID actor) {
  UUID id=(UUID)p.get("id"),owner=(UUID)p.get("owner_id"); boolean owned=actor.equals(owner);
  var members=new ArrayList<Member>(); members.add(new Member(owner,name(owner),"OWNER"));
  members.addAll(sql.query("SELECT account_id,COALESCE(ended_reason,status) FROM project_members WHERE project_id=? ORDER BY account_id",(r,n)->new Member(r.getObject(1,UUID.class),name(r.getObject(1,UUID.class)),r.getString(2)),id));
  String status=owned?"OWNER":members.stream().filter(m->m.id().equals(actor)).findFirst().orElseThrow().status();
  return new Project(id,owned,(String)p.get("name"),(String)p.get("description"),(String)p.get("stage"),((Number)p.get("revision")).longValue(),status,members);
 }
 @GetMapping List<Project> list(Authentication auth) {
  UUID actor=collaboration.owner(auth);
  return transaction.execute(s->{
   var rows=sql.queryForList("SELECT p.* FROM projects p WHERE owner_id=? OR EXISTS(SELECT 1 FROM project_members m WHERE m.project_id=p.id AND m.account_id=? AND m.status IN ('INVITED','ACCEPTED')) ORDER BY created_at DESC,id LIMIT 100 FOR UPDATE",actor,actor);
   if (rows.isEmpty()) return List.of();
   String placeholders=String.join(",",Collections.nCopies(rows.size(),"?"));
   var members=new HashMap<UUID,List<Member>>();
   var names=new HashMap<UUID,String>();
   var ownerIds=rows.stream().map(p->(UUID)p.get("owner_id")).distinct().toList();
   sql.query("SELECT account_id,profile_json FROM profiles WHERE account_id IN ("+String.join(",",Collections.nCopies(ownerIds.size(),"?"))+")",r->{ names.put(r.getObject(1,UUID.class),profileName(r.getString(2))); },ownerIds.toArray());
   sql.query("SELECT m.project_id,m.account_id,COALESCE(m.ended_reason,m.status),p.profile_json FROM project_members m LEFT JOIN profiles p ON p.account_id=m.account_id WHERE m.project_id IN ("+placeholders+") ORDER BY m.account_id",r->{
    members.computeIfAbsent(r.getObject(1,UUID.class),id->new ArrayList<>()).add(new Member(r.getObject(2,UUID.class),profileName(r.getString(4)),r.getString(3)));
   },rows.stream().map(p->p.get("id")).toArray());
   return rows.stream().map(p->{
    UUID id=(UUID)p.get("id"),owner=(UUID)p.get("owner_id"); boolean owned=actor.equals(owner);
    var team=new ArrayList<Member>(); team.add(new Member(owner,names.getOrDefault(owner,"Team member"),"OWNER")); team.addAll(members.getOrDefault(id,List.of()));
    String status=owned?"OWNER":team.stream().filter(m->m.id().equals(actor)).findFirst().orElseThrow().status();
    return new Project(id,owned,(String)p.get("name"),(String)p.get("description"),(String)p.get("stage"),((Number)p.get("revision")).longValue(),status,team);
   }).toList();
  });
 }
 private String profileName(String profile) {
  if (profile==null) return "Team member";
  try { return new com.fasterxml.jackson.databind.ObjectMapper().readTree(profile).path("displayName").asText("Team member"); } catch (Exception ex) { throw error(500); }
 }
 @PostMapping Project create(@Valid @RequestBody Draft draft,Authentication auth) {
  UUID actor=collaboration.owner(auth);
  return transaction.execute(s->{
   sql.queryForList("SELECT id FROM accounts WHERE id=? FOR UPDATE",actor);
   var prior=sql.queryForList("SELECT * FROM projects WHERE owner_id=? AND client_id=?",actor,draft.clientId());
   if (!prior.isEmpty()) { var p=prior.getFirst(); if (!p.get("name").equals(draft.name().strip()) || !p.get("description").equals(draft.description().strip()) || !p.get("stage").equals(draft.stage())) throw error(409); return view(p,actor); }
   if (sql.queryForObject("SELECT COUNT(*) FROM projects WHERE owner_id=?",Long.class,actor)>=20) throw error(429);
   UUID id=UUID.randomUUID(); sql.update("INSERT INTO projects(id,owner_id,client_id,name,description,stage,created_at) VALUES(?,?,?,?,?,?,?)",id,actor,draft.clientId(),draft.name().strip(),draft.description().strip(),draft.stage(),Timestamp.from(Instant.now()));
   return view(access(id,actor,true),actor);
  });
 }
 @PostMapping("/{id}/members") Project invite(@PathVariable UUID id,@Valid @RequestBody Invite invite,Authentication auth) {
  UUID actor=collaboration.owner(auth);
  return transaction.execute(s->{
   // Serialize invitations against unmatching, then lock the project for its member limit.
   UUID recipient=collaboration.member(invite.matchId(),actor,true);
   var p=access(id,actor,true);
   var prior=sql.queryForList("SELECT status FROM project_members WHERE project_id=? AND account_id=?",id,recipient);
   if (!prior.isEmpty()) { if (prior.getFirst().get("status").equals("DECLINED")) throw error(409); return view(p,actor); }
   if (sql.queryForObject("SELECT COUNT(*) FROM project_members WHERE project_id=?",Long.class,id)>=10) throw error(429);
   sql.update("INSERT INTO project_members(project_id,account_id,status) VALUES(?,?,'INVITED')",id,recipient); return view(p,actor);
  });
 }
 @PostMapping("/{id}/response") Project respond(@PathVariable UUID id,@Valid @RequestBody Response response,Authentication auth) {
  UUID actor=collaboration.owner(auth);
  return transaction.execute(s->{
   var p=access(id,actor,false); if (actor.equals(p.get("owner_id"))) throw error(403);
   String status=sql.queryForObject("SELECT status FROM project_members WHERE project_id=? AND account_id=?",String.class,id,actor);
   if (!status.equals(response.status())) { if (!status.equals("INVITED")) throw error(409); sql.update("UPDATE project_members SET status=? WHERE project_id=? AND account_id=?",response.status(),id,actor); }
   return view(p,actor);
  });
 }
 @PutMapping("/{id}") Project edit(@PathVariable UUID id,@Valid @RequestBody Edit edit,Authentication auth) {
  UUID actor=collaboration.owner(auth);
  return transaction.execute(s->{
   var p=access(id,actor,true);
   if (((Number)p.get("revision")).longValue()!=edit.revision()) throw error(409);
   sql.update("UPDATE projects SET name=?,description=?,stage=?,revision=revision+1 WHERE id=?",edit.name().strip(),edit.description().strip(),edit.stage(),id);
   return view(access(id,actor,true),actor);
  });
 }
 @PostMapping("/{id}/leave") @ResponseStatus(HttpStatus.NO_CONTENT) void leave(@PathVariable UUID id,Authentication auth) {
  UUID actor=collaboration.owner(auth);
  transaction.executeWithoutResult(s->{
   var p=access(id,actor,false); if (actor.equals(p.get("owner_id"))) throw error(403);
   sql.update("UPDATE project_members SET status='DECLINED',ended_reason='LEFT' WHERE project_id=? AND account_id=?",id,actor);
  });
 }
 @PostMapping("/{id}/members/{member}/remove") Project remove(@PathVariable UUID id,@PathVariable UUID member,Authentication auth) {
  UUID actor=collaboration.owner(auth);
  return transaction.execute(s->{
   var p=access(id,actor,true); if (actor.equals(member)) throw error(400);
   if (sql.queryForObject("SELECT COUNT(*) FROM project_members WHERE project_id=? AND account_id=?",Long.class,id,member)==0) throw error(404);
   sql.update("UPDATE project_members SET status='DECLINED',ended_reason='REMOVED' WHERE project_id=? AND account_id=? AND status IN ('INVITED','ACCEPTED')",id,member);
   return view(p,actor);
  });
 }
}
