package com.teamforge;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
@Configuration
class SecurityConfiguration {
 @Bean PasswordEncoder passwords() { return new BCryptPasswordEncoder(12); }
 @Bean UserDetailsService users(AccountRepository accounts) {
  return email -> accounts.findByEmail(email).map(a -> User.withUsername(a.email).password(a.passwordHash).roles("USER").build()).orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
 }
 @Bean AuthenticationManager authentication(UserDetailsService users, PasswordEncoder passwords) {
  var provider = new DaoAuthenticationProvider(users); provider.setPasswordEncoder(passwords);
  return new ProviderManager(provider);
 }
 @Bean SecurityFilterChain security(HttpSecurity http,AccountRepository accounts) throws Exception {
  http.authorizeHttpRequests(auth -> auth
    .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR).permitAll()
    .requestMatchers(org.springframework.http.HttpMethod.GET,"/", "/index.html", "/assets/**", "/favicon.svg").permitAll()
    .requestMatchers(org.springframework.http.HttpMethod.HEAD,"/", "/index.html", "/assets/**", "/favicon.svg").permitAll()
    .requestMatchers("/api/auth/csrf", "/api/auth/signup", "/api/auth/login", "/api/auth/recover", "/api/demo/**", "/api/onboarding/validate", "/api/onboarding/skills", "/actuator/health", "/actuator/health/**").permitAll()
    .requestMatchers("/api/auth/me", "/api/dashboard", "/api/people/**", "/api/profiles/me/details", "/api/account/**", "/api/account/export", "/api/safety/**", "/api/profiles/me", "/api/profiles/me/visibility", "/api/discovery/**", "/api/matches/**", "/api/projects", "/api/projects/**").authenticated().anyRequest().denyAll())
   .csrf(csrf -> csrf.ignoringRequestMatchers("/api/demo/**", "/api/onboarding/validate", "/api/onboarding/skills"))
   .formLogin(form -> form.disable()).httpBasic(basic -> basic.disable())
   .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, ex) -> response.sendError(401)))
   .logout(logout -> logout.logoutUrl("/api/auth/logout").deleteCookies("JSESSIONID").logoutSuccessHandler((request, response, auth) -> response.setStatus(204)));
  http.addFilterAfter(new CredentialSessionFilter(accounts),org.springframework.security.web.context.SecurityContextHolderFilter.class);
  return http.build();
 }
}
