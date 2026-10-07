package com.teamforge;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
class AccountSettingsController {
 private final CollaborationController collaboration; private final JdbcTemplate sql; private final PasswordEncoder passwords; private final AuthController auth; private final TransactionTemplate tx;
 AccountSettingsController(CollaborationController c,JdbcTemplate sql,PasswordEncoder passwords,AuthController auth,PlatformTransactionManager m) { collaboration=c; this.sql=sql; this.passwords=passwords; this.auth=auth; tx=new TransactionTemplate(m); }
 record Password(@NotBlank @Size(max=72) String password) {}
 record Recovery(@NotBlank @Email @Size(max=254) String email,@NotBlank @Size(max=100) String key,@NotBlank @Size(min=12,max=72) String password) {}
 private ResponseStatusException error(int code) { return new ResponseStatusException(HttpStatus.valueOf(code)); }
 private static String hash(String key) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8))); } catch (Exception e) { throw new IllegalStateException(e); } }
 private void verify(UUID actor,String password) { if (password.getBytes(StandardCharsets.UTF_8).length>72) throw error(400); var rows=sql.queryForList("SELECT password_hash FROM accounts WHERE id=? FOR UPDATE",actor); if (rows.isEmpty() || !passwords.matches(password,(String)rows.getFirst().get("password_hash"))) throw error(403); }
 @PostMapping("/api/account/recovery-key") Map<String,String> generate(@Valid @RequestBody Password p,Authentication authentication,HttpServletRequest request) {
  auth.throttle(request); UUID actor=collaboration.owner(authentication);
  return tx.execute(s->{ verify(actor,p.password()); byte[] bytes=new byte[32]; new SecureRandom().nextBytes(bytes); String key=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); sql.update("DELETE FROM recovery_keys WHERE account_id=?",actor); sql.update("INSERT INTO recovery_keys(account_id,key_hash) VALUES(?,?)",actor,hash(key)); return Map.of("key",key); });
 }
 @PostMapping("/api/auth/recover") @ResponseStatus(HttpStatus.NO_CONTENT)
 void recover(@Valid @RequestBody Recovery r,HttpServletRequest request) {
  auth.throttle(request); if (r.password().getBytes(StandardCharsets.UTF_8).length>72) throw error(400);
  tx.executeWithoutResult(s->{
   var rows=sql.queryForList("SELECT id FROM accounts WHERE email=? FOR UPDATE",r.email().trim().toLowerCase(Locale.ROOT));
   if (rows.isEmpty()) throw error(400); UUID id=(UUID)rows.getFirst().get("id");
   var keys=sql.queryForList("SELECT key_hash FROM recovery_keys WHERE account_id=?",id);
   if (keys.isEmpty() || !MessageDigest.isEqual(hash(r.key()).getBytes(StandardCharsets.UTF_8),((String)keys.getFirst().get("key_hash")).getBytes(StandardCharsets.UTF_8))) throw error(400);
   sql.update("UPDATE accounts SET password_hash=? WHERE id=?",passwords.encode(r.password()),id); sql.update("DELETE FROM recovery_keys WHERE account_id=?",id);
   sql.update("DELETE FROM spring_session WHERE principal_name=?",r.email().trim().toLowerCase(Locale.ROOT));
  });
 }
 @PostMapping("/api/account/delete") @ResponseStatus(HttpStatus.NO_CONTENT)
 void delete(@Valid @RequestBody Password p,Authentication authentication,HttpServletRequest request) {
  auth.throttle(request); UUID actor=collaboration.owner(authentication);
  tx.executeWithoutResult(s->{ verify(actor,p.password()); sql.update("DELETE FROM spring_session WHERE principal_name=?",authentication.getName()); sql.update("DELETE FROM accounts WHERE id=?",actor); });
  request.getSession().invalidate();
 }
 @GetMapping("/api/account/blocks") List<Map<String,Object>> blocks(Authentication authentication) { return sql.queryForList("SELECT b.target_id, b.created_at FROM account_blocks b WHERE actor_id=? ORDER BY created_at",collaboration.owner(authentication)); }
 record Target(@NotNull UUID targetId) {}
 @PostMapping("/api/account/unblock") @ResponseStatus(HttpStatus.NO_CONTENT)
 void unblock(@Valid @RequestBody Target target,Authentication authentication) {
  UUID actor=collaboration.owner(authentication);
  tx.executeWithoutResult(s->{ var ids=new ArrayList<>(List.of(actor,target.targetId())); ids.sort(Comparator.comparing(UUID::toString)); for (UUID id:ids) sql.queryForList("SELECT id FROM accounts WHERE id=? FOR UPDATE",id); sql.update("DELETE FROM account_blocks WHERE actor_id=? AND target_id=?",actor,target.targetId()); });
 }
}
