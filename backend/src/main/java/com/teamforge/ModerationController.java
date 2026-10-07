package com.teamforge;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/moderation")
class ModerationController {
 private final Set<UUID> moderators;
 private final CollaborationController collaboration;
 private final JdbcTemplate sql;
 private final TransactionTemplate transaction;
 ModerationController(@Value("${teamforge.moderator-ids:}") String ids, CollaborationController collaboration, JdbcTemplate sql, PlatformTransactionManager manager) {
  moderators=new HashSet<>();
  for (String id:ids.split(",")) if (!id.isBlank()) moderators.add(UUID.fromString(id.strip()));
  this.collaboration=collaboration; this.sql=sql; transaction=new TransactionTemplate(manager);
 }
 private UUID moderator(Authentication auth) {
  UUID id=collaboration.owner(auth);
  if (!moderators.contains(id)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  return id;
 }
 @GetMapping("/access") Map<String,Boolean> access(Authentication auth) { return Map.of("allowed",moderators.contains(collaboration.owner(auth))); }
 @GetMapping("/reports") Map<String,Object> reports(@RequestParam(defaultValue="0") int offset, @RequestParam(defaultValue="OPEN") String status, Authentication auth) {
  moderator(auth);
  if (offset<0 || offset>100000 || !Set.of("OPEN","DISMISSED","MATCH_CLOSED").contains(status)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
  var rows=sql.queryForList("SELECT r.id,r.reporter_id,r.match_id,r.reason,r.details,r.created_at,r.review_status,r.review_note,r.reviewed_at,r.revision,CASE WHEN m.member_a=r.reporter_id THEN m.member_b ELSE m.member_a END AS reported_id FROM safety_reports r JOIN collaboration_matches m ON m.id=r.match_id WHERE r.review_status=? ORDER BY r.created_at,r.id LIMIT 26 OFFSET ?",status,offset);
  boolean more=rows.size()>25;
  return Map.of("reports",more?rows.subList(0,25):rows,"hasMore",more,"offset",offset);
 }
 record Review(@NotNull @Min(0) Long revision,@NotBlank @Pattern(regexp="DISMISSED|MATCH_CLOSED") String outcome,@NotBlank @Size(max=1000) String note) {}
 @PostMapping("/reports/{id}/review") Map<String,Object> review(@PathVariable UUID id,@Valid @RequestBody Review review,Authentication auth) {
  UUID actor=moderator(auth);
  return transaction.execute(state->{
   var rows=sql.queryForList("SELECT match_id,revision,review_status FROM safety_reports WHERE id=? FOR UPDATE",id);
   if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
   var row=rows.getFirst();
   if (((Number)row.get("revision")).longValue()!=review.revision() || !row.get("review_status").equals("OPEN")) throw new ResponseStatusException(HttpStatus.CONFLICT);
   Timestamp now=Timestamp.from(Instant.now());
   if (review.outcome().equals("MATCH_CLOSED")) sql.update("UPDATE collaboration_matches SET closed_at=? WHERE id=? AND closed_at IS NULL",now,row.get("match_id"));
   sql.update("UPDATE safety_reports SET review_status=?,review_note=?,reviewed_at=?,reviewed_by=?,revision=revision+1 WHERE id=?",review.outcome(),review.note().strip(),now,actor,id);
   return Map.of("id",id,"status",review.outcome());
  });
 }
}
