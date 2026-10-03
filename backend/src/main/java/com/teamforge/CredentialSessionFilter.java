package com.teamforge;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
class CredentialSessionFilter extends OncePerRequestFilter {
 private final AccountRepository accounts;
 CredentialSessionFilter(AccountRepository accounts) { this.accounts=accounts; }
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
  var session=request.getSession(false); var auth=SecurityContextHolder.getContext().getAuthentication();
  if (session!=null && auth!=null && session.getAttribute("accountCredential")!=null) {
   var account=accounts.findByEmail(auth.getName());
   if (account.isEmpty() || !session.getAttribute("accountCredential").equals(account.get().id+":"+account.get().passwordHash)) {
    session.invalidate(); SecurityContextHolder.clearContext(); response.sendError(401); return;
   }
  }
  chain.doFilter(request,response);
 }
}
