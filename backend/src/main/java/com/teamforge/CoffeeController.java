package com.teamforge;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController @RequestMapping("/api/matches/{match}/proposals")
class CoffeeController {
 private final CollaborationController collaboration;
 private final JdbcTemplate sql;
 private final TransactionTemplate transaction;
 CoffeeController(CollaborationController collaboration,JdbcTemplate sql,PlatformTransactionManager manager) { this.collaboration=collaboration; this.sql=sql; transaction=new TransactionTemplate(manager); }
 record Draft(@NotNull UUID clientId,@NotBlank @Pattern(regexp="Virtual coffee|Intro call|Project discussion") String kind,@NotNull Instant startsAt,@NotBlank @Size(max=80) String timezone,@NotNull @Size(max=500) String note) { Draft { if (startsAt!=null) startsAt=startsAt.truncatedTo(java.time.temporal.ChronoUnit.MILLIS); } }
 record Response(@NotBlank @Pattern(regexp="ACCEPTED|DECLINED|CANCELLED") String status) {}
 record Proposal(UUID id,boolean fromYou,String kind,Instant startsAt,String timezone,String note,String status) {}
 private ResponseStatusException error(int code) { return new ResponseStatusException(HttpStatus.valueOf(code)); }
 private List<Proposal> list(UUID match,UUID actor,String extra,Object... parameters) {
  var args=new ArrayList<Object>(); args.add(match); args.addAll(Arrays.asList(parameters));
  return sql.query("SELECT * FROM coffee_proposals WHERE match_id=? "+extra,(row,n) -> new Proposal(row.getObject("id",UUID.class),actor.equals(row.getObject("proposer_id",UUID.class)),row.getString("kind"),row.getTimestamp("starts_at").toInstant(),row.getString("timezone"),row.getString("note"),row.getString("status")),args.toArray());
 }
 @GetMapping List<Proposal> get(@PathVariable UUID match,Authentication auth) {
  UUID actor=collaboration.owner(auth);
  return transaction.execute(state -> { collaboration.member(match,actor,true); return list(match,actor,"ORDER BY created_at DESC,id LIMIT 100"); });
 }
 @PostMapping Proposal create(@PathVariable UUID match,@Valid @RequestBody Draft request,Authentication auth) {
  UUID actor=collaboration.owner(auth);
  try { ZoneId.of(request.timezone()); } catch (DateTimeException ex) { throw error(400); }
  return transaction.execute(state -> {
   collaboration.member(match,actor,true);
   var prior=list(match,actor,"AND proposer_id=? AND client_id=?",actor,request.clientId());
   if (!prior.isEmpty()) {
    var p=prior.getFirst();
    if (!p.kind().equals(request.kind()) || !p.startsAt().equals(request.startsAt()) || !p.timezone().equals(request.timezone()) || !p.note().equals(request.note().strip())) throw error(409);
    return p;
   }
   if (!request.startsAt().isAfter(Instant.now()) || request.startsAt().isAfter(Instant.now().plusSeconds(180L*86400))) throw error(400);
   if (sql.queryForObject("SELECT COUNT(*) FROM coffee_proposals WHERE match_id=? AND status='PROPOSED' AND starts_at>?",Long.class,match,Timestamp.from(Instant.now()))>=10) throw error(429);
   UUID id=UUID.randomUUID(); sql.update("INSERT INTO coffee_proposals(id,match_id,proposer_id,client_id,kind,starts_at,timezone,note,status,created_at) VALUES(?,?,?,?,?,?,?,?,?,?)",id,match,actor,request.clientId(),request.kind(),Timestamp.from(request.startsAt()),request.timezone(),request.note().strip(),"PROPOSED",Timestamp.from(Instant.now()));
   return list(match,actor,"AND id=?",id).getFirst();
  });
 }
 @PostMapping("/{proposal}/response") Proposal respond(@PathVariable UUID match,@PathVariable UUID proposal,@Valid @RequestBody Response response,Authentication auth) {
  UUID actor=collaboration.owner(auth);
  return transaction.execute(state -> {
   collaboration.member(match,actor,true); var rows=list(match,actor,"AND id=?",proposal); if (rows.isEmpty()) throw error(404);
   var p=rows.getFirst();
   // Only the recipient accepts/declines. Either member may cancel an upcoming meeting.
   if (!response.status().equals("CANCELLED") && p.fromYou()) throw error(403);
   if (p.status().equals(response.status())) return p;
   if (!p.startsAt().isAfter(Instant.now()) || !(p.status().equals("PROPOSED") || p.status().equals("ACCEPTED") && response.status().equals("CANCELLED"))) throw error(409);
   sql.update("UPDATE coffee_proposals SET status=? WHERE id=?",response.status(),proposal);
   return list(match,actor,"AND id=?",proposal).getFirst();
  });
 }
}
