package com.teamforge;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/auth")
class AuthController {
 private final AccountRepository accounts;
 private final PasswordEncoder passwords;
 private final AuthenticationManager authentication;
 private final HttpSessionSecurityContextRepository contexts = new HttpSessionSecurityContextRepository();
 private final HttpSessionCsrfTokenRepository csrf = new HttpSessionCsrfTokenRepository();
 private record Window(long startsAt, int attempts) {}
 private final Map<String,Window> attempts = new ConcurrentHashMap<>();
 AuthController(AccountRepository accounts, PasswordEncoder passwords, AuthenticationManager authentication) { this.accounts=accounts; this.passwords=passwords; this.authentication=authentication; }
 record Credentials(@NotBlank @Email @Size(max=254) String email, @NotBlank @Size(min=12,max=72) String password) {}
 record AccountView(UUID id, String email, String accountType) {}
 private String email(Credentials credentials) { return credentials.email().trim().toLowerCase(Locale.ROOT); }
 private void throttle(HttpServletRequest request) {
  long now=System.currentTimeMillis();
  if (attempts.size() > 10000) { attempts.entrySet().removeIf(entry -> now-entry.getValue().startsAt() > 300000); if (attempts.size() > 10000) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS); }
  Window window=attempts.compute(request.getRemoteAddr(), (key, previous) -> previous==null || now-previous.startsAt()>300000 ? new Window(now,1) : new Window(previous.startsAt(),previous.attempts()+1));
  if (window.attempts()>30) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Try again later");
 }
 @GetMapping("/csrf") Map<String,String> csrf(CsrfToken token) { return Map.of("token",token.getToken(),"headerName",token.getHeaderName()); }
 @PostMapping("/signup") @ResponseStatus(HttpStatus.CREATED)
 AccountView signup(@Valid @RequestBody Credentials credentials, HttpServletRequest request, HttpServletResponse response) {
  throttle(request);
  if (credentials.password().getBytes(StandardCharsets.UTF_8).length > 72) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password exceeds supported byte length");
  try { accounts.saveAndFlush(new Account(email(credentials),passwords.encode(credentials.password()))); }
  catch (DataIntegrityViolationException ex) { throw new ResponseStatusException(HttpStatus.CONFLICT,"Unable to register with these details"); }
  return establish(credentials,request,response);
 }
 @PostMapping("/login")
 AccountView login(@Valid @RequestBody Credentials credentials, HttpServletRequest request, HttpServletResponse response) { throttle(request); return establish(credentials,request,response); }
 private AccountView establish(Credentials credentials, HttpServletRequest request, HttpServletResponse response) {
  if (credentials.password().getBytes(StandardCharsets.UTF_8).length > 72) throw new BadCredentialsException("Invalid credentials");
  Authentication auth=authentication.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email(credentials),credentials.password()));
  request.getSession(true); request.changeSessionId();
  var context=SecurityContextHolder.createEmptyContext(); context.setAuthentication(auth); SecurityContextHolder.setContext(context);
  contexts.saveContext(context,request,response); csrf.saveToken(null,request,response);
  return view(email(credentials));
 }
 @GetMapping("/me") AccountView me(Authentication authentication) { return view(authentication.getName()); }
 private AccountView view(String email) { var account=accounts.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED)); return new AccountView(account.id,account.email,account.accountType); }
}
