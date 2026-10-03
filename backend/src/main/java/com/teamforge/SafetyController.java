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

@RestController @RequestMapping("/api/safety")
class SafetyController {
 private final CollaborationController collaboration;
 private final JdbcTemplate sql;
 private final TransactionTemplate transaction;
 SafetyController(CollaborationController collaboration,JdbcTemplate sql,PlatformTransactionManager manager) { this.collaboration=collaboration; this.sql=sql; transaction=new TransactionTemplate(manager); }
 record Block(@NotNull UUID targetId) {}
 record Report(@NotNull UUID clientId,@NotNull UUID matchId,@NotBlank @Pattern(regexp="Harassment|Spam|Impersonation|Other") String reason,@NotNull @Size(max=1000) String details) {}
 private ResponseStatusException error(int code) { return new ResponseStatusException(HttpStatus.valueOf(code)); }
 @PostMapping("/blocks") @ResponseStatus(HttpStatus.NO_CONTENT)
 void block(@Valid @RequestBody Block block,Authentication auth) {
  UUID actor=collaboration.owner(auth), target=block.targetId(); if (actor.equals(target)) throw error(400);
  transaction.executeWithoutResult(s->{
   var ids=new ArrayList<>(List.of(actor,target)); ids.sort(Comparator.comparing(UUID::toString));
   for (UUID id:ids) if (sql.queryForList("SELECT id FROM accounts WHERE id=? FOR UPDATE",id).isEmpty()) throw error(404);
   if (sql.queryForObject("SELECT COUNT(*) FROM account_blocks WHERE actor_id=? AND target_id=?",Long.class,actor,target)==0)
    sql.update("INSERT INTO account_blocks(actor_id,target_id,created_at) VALUES(?,?,?)",actor,target,Timestamp.from(Instant.now()));
   sql.update("UPDATE collaboration_matches SET closed_at=? WHERE closed_at IS NULL AND ((member_a=? AND member_b=?) OR (member_a=? AND member_b=?))",Timestamp.from(Instant.now()),actor,target,target,actor);
  });
 }
 @PostMapping("/reports") Map<String,Object> report(@Valid @RequestBody Report report,Authentication auth) {
  UUID actor=collaboration.owner(auth);
  return transaction.execute(s->{
   sql.queryForList("SELECT id FROM accounts WHERE id=? FOR UPDATE",actor);
   if (sql.queryForObject("SELECT COUNT(*) FROM collaboration_matches WHERE id=? AND (member_a=? OR member_b=?)",Long.class,report.matchId(),actor,actor)==0) throw error(404);
   var prior=sql.queryForList("SELECT id,match_id,reason,details FROM safety_reports WHERE reporter_id=? AND client_id=?",actor,report.clientId());
   if (!prior.isEmpty()) { var p=prior.getFirst(); if (!p.get("match_id").equals(report.matchId()) || !p.get("reason").equals(report.reason()) || !p.get("details").equals(report.details().strip())) throw error(409); return Map.of("id",p.get("id"),"status","RECORDED"); }
   if (sql.queryForObject("SELECT COUNT(*) FROM safety_reports WHERE reporter_id=? AND created_at>?",Long.class,actor,Timestamp.from(Instant.now().minusSeconds(86400)))>=10) throw error(429);
   UUID id=UUID.randomUUID(); sql.update("INSERT INTO safety_reports(id,reporter_id,match_id,client_id,reason,details,created_at) VALUES(?,?,?,?,?,?,?)",id,actor,report.matchId(),report.clientId(),report.reason(),report.details().strip(),Timestamp.from(Instant.now()));
   return Map.of("id",id,"status","RECORDED");
  });
 }
}
