package com.teamforge;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
class CollaborationController {
 private final AccountRepository accounts;
 private final ProfileRepository profiles;
 private final DiscoveryController ranking;
 private final ObjectMapper json;
 private final JdbcTemplate sql;
 private final TransactionTemplate transaction;
 CollaborationController(AccountRepository accounts,ProfileRepository profiles,DiscoveryController ranking,ObjectMapper json,JdbcTemplate sql,PlatformTransactionManager manager) {
  this.accounts=accounts; this.profiles=profiles; this.ranking=ranking; this.json=json; this.sql=sql; transaction=new TransactionTemplate(manager);
 }
 record Decision(@NotNull UUID candidateId,@NotBlank @Pattern(regexp="LIKE|PASS") String decision) {}
 record MessageDraft(@NotNull UUID clientId,@NotBlank @Size(max=1000) String text) {}
 record Message(long sequence,UUID clientId,boolean fromYou,String text,Instant sentAt) {}
 record MatchView(UUID id,String displayName,String entityType,String matchingIntent,int compatibility,Instant createdAt) {}
 UUID owner(Authentication auth) { return accounts.findByEmail(auth.getName()).orElseThrow(() -> error(401)).id; }
 private ResponseStatusException error(int status) { return new ResponseStatusException(HttpStatus.valueOf(status)); }
 private OnboardingController.ProfileDraft draft(StoredProfile profile) {
  try { return json.readValue(profile.profileJson,OnboardingController.ProfileDraft.class); }
  catch (Exception ex) { throw error(500); }
 }
 private boolean compatible(StoredProfile a,StoredProfile b) {
  var left=draft(a).matchingIntent(); var right=draft(b).matchingIntent();
  return !left.equals(right) || left.equals("COLLABORATOR");
 }
 private List<Map<String,Object>> pair(UUID a,UUID b,boolean lock) {
  UUID first=a.toString().compareTo(b.toString())<0?a:b; UUID second=first.equals(a)?b:a;
  return sql.queryForList("SELECT * FROM collaboration_matches WHERE member_a=? AND member_b=?"+(lock?" FOR UPDATE":""),first,second);
 }
 UUID member(UUID match,UUID owner,boolean lock) {
  var rows=sql.queryForList("SELECT member_a,member_b FROM collaboration_matches WHERE id=? AND closed_at IS NULL AND (member_a=? OR member_b=?)"+(lock?" FOR UPDATE":""),match,owner,owner);
  if (rows.isEmpty()) throw error(404);
  return owner.equals(rows.getFirst().get("member_a"))?(UUID)rows.getFirst().get("member_b"):(UUID)rows.getFirst().get("member_a");
 }
 @PostMapping("/api/discovery/decisions")
 Map<String,Object> decide(@Valid @RequestBody Decision request,Authentication auth) {
  UUID actor=owner(auth),target=request.candidateId(); if (actor.equals(target)) throw error(400);
  var own=profiles.findById(actor).orElseThrow(() -> error(409));
  var other=profiles.findById(target).orElseThrow(() -> error(404));
  if (!other.discoverable) throw error(404);
  if (!compatible(own,other)) throw error(409);
  if (request.decision().equals("LIKE") && !own.discoverable) throw error(409);
  var existing=pair(actor,target,false);
  if (!existing.isEmpty() && existing.getFirst().get("closed_at")!=null) throw error(409);
  int score=request.decision().equals("LIKE")?ranking.compatibility(own,other):0;
  if (request.decision().equals("LIKE") && score<=50) throw error(409);
  return transaction.execute(status -> {
   var ids=new ArrayList<>(List.of(actor,target)); ids.sort(Comparator.comparing(UUID::toString));
   // Serialize both directions in canonical order, including simultaneous likes.
   for (UUID id:ids) sql.queryForList("SELECT id FROM accounts WHERE id=? FOR UPDATE",id);
   for (UUID id:ids) {
    var locked=sql.queryForList("SELECT row_version,discoverable FROM profiles WHERE account_id=? FOR UPDATE",id);
    var snapshot=id.equals(actor)?own:other;
    if (locked.isEmpty() || ((Number)locked.getFirst().get("row_version")).longValue()!=snapshot.version) throw error(409);
   }
   var matched=pair(actor,target,true);
   if (!matched.isEmpty()) {
    if (matched.getFirst().get("closed_at")!=null) throw error(409);
    return Map.of("decision","LIKE","matched",true,"matchId",matched.getFirst().get("id"));
   }
   sql.update("DELETE FROM profile_decisions WHERE actor_id=? AND target_id=?",actor,target);
   sql.update("INSERT INTO profile_decisions(actor_id,target_id,decision,created_at) VALUES(?,?,?,?)",actor,target,request.decision(),Timestamp.from(Instant.now()));
   boolean reverse=sql.queryForObject("SELECT COUNT(*) FROM profile_decisions WHERE actor_id=? AND target_id=? AND decision='LIKE'",Long.class,target,actor)>0;
   if (request.decision().equals("LIKE") && reverse) {
    UUID match=UUID.randomUUID(); sql.update("INSERT INTO collaboration_matches(id,member_a,member_b,compatibility,created_at) VALUES(?,?,?,?,?)",match,ids.get(0),ids.get(1),score,Timestamp.from(Instant.now()));
    return Map.of("decision","LIKE","matched",true,"matchId",match);
   }
   return Map.of("decision",request.decision(),"matched",false);
  });
 }
 @GetMapping("/api/matches")
 List<MatchView> matches(Authentication auth) {
  UUID actor=owner(auth);
  return sql.query("SELECT id,member_a,member_b,compatibility,created_at FROM collaboration_matches WHERE closed_at IS NULL AND (member_a=? OR member_b=?) ORDER BY created_at DESC,id LIMIT 100",(row,n) -> {
   UUID other=actor.equals(row.getObject("member_a",UUID.class))?row.getObject("member_b",UUID.class):row.getObject("member_a",UUID.class);
   var profile=draft(profiles.findById(other).orElseThrow(() -> error(404)));
   return new MatchView(row.getObject("id",UUID.class),profile.displayName(),profile.entityType(),profile.matchingIntent(),row.getInt("compatibility"),row.getTimestamp("created_at").toInstant());
  },actor,actor);
 }
 @GetMapping("/api/matches/{match}/messages")
 Map<String,Object> messages(@PathVariable UUID match,@RequestParam(defaultValue="0") long after,Authentication auth) {
  if (after<0) throw error(400); UUID actor=owner(auth);
  return transaction.execute(status -> { member(match,actor,true);
  var rows=sql.query("SELECT sequence_id,client_id,sender_id,body,created_at FROM collaboration_messages WHERE match_id=? AND sequence_id>? ORDER BY sequence_id LIMIT 51",(row,n) -> new Message(row.getLong("sequence_id"),row.getObject("client_id",UUID.class),actor.equals(row.getObject("sender_id",UUID.class)),row.getString("body"),row.getTimestamp("created_at").toInstant()),match,after);
  boolean more=rows.size()>50; var page=rows.stream().limit(50).toList();
  return Map.of("messages",page,"hasMore",more,"nextAfter",page.isEmpty()?after:page.getLast().sequence());
  });
 }
 @PostMapping("/api/matches/{match}/messages")
 Message send(@PathVariable UUID match,@Valid @RequestBody MessageDraft request,Authentication auth) {
  UUID actor=owner(auth); String text=request.text().strip(); if (text.isBlank()) throw error(400);
  return transaction.execute(status -> {
   sql.queryForList("SELECT id FROM accounts WHERE id=? FOR UPDATE",actor); member(match,actor,true);
   var prior=sql.query("SELECT sequence_id,body,created_at FROM collaboration_messages WHERE match_id=? AND sender_id=? AND client_id=?",(row,n) -> new Message(row.getLong("sequence_id"),request.clientId(),true,row.getString("body"),row.getTimestamp("created_at").toInstant()),match,actor,request.clientId());
   if (!prior.isEmpty()) { if (!prior.getFirst().text().equals(text)) throw error(409); return prior.getFirst(); }
   long count=sql.queryForObject("SELECT COUNT(*) FROM collaboration_messages WHERE sender_id=? AND created_at>?",Long.class,actor,Timestamp.from(Instant.now().minusSeconds(60)));
   if (count>=30) throw error(429);
   Instant now=Instant.now(); sql.update("INSERT INTO collaboration_messages(match_id,sender_id,client_id,body,created_at) VALUES(?,?,?,?,?)",match,actor,request.clientId(),text,Timestamp.from(now));
   long sequence=sql.queryForObject("SELECT sequence_id FROM collaboration_messages WHERE match_id=? AND sender_id=? AND client_id=?",Long.class,match,actor,request.clientId());
   return new Message(sequence,request.clientId(),true,text,now);
  });
 }
 @DeleteMapping("/api/matches/{match}") @ResponseStatus(HttpStatus.NO_CONTENT)
 void unmatch(@PathVariable UUID match,Authentication auth) {
  UUID actor=owner(auth);
  transaction.executeWithoutResult(status -> { member(match,actor,true); sql.update("UPDATE collaboration_matches SET closed_at=? WHERE id=?",Timestamp.from(Instant.now()),match); });
 }
}
