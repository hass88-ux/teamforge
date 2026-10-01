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
 @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
  http.authorizeHttpRequests(auth -> auth
    .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR).permitAll()
    .requestMatchers("/api/auth/csrf", "/api/auth/signup", "/api/auth/login", "/api/demo/**", "/api/onboarding/validate", "/actuator/health", "/actuator/health/**").permitAll()
    .requestMatchers("/api/auth/me", "/api/profiles/me").authenticated().anyRequest().denyAll())
   .csrf(csrf -> csrf.ignoringRequestMatchers("/api/demo/**", "/api/onboarding/validate"))
   .formLogin(form -> form.disable()).httpBasic(basic -> basic.disable())
   .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, ex) -> response.sendError(401)))
   .logout(logout -> logout.logoutUrl("/api/auth/logout").deleteCookies("JSESSIONID").logoutSuccessHandler((request, response, auth) -> response.setStatus(204)));
  return http.build();
 }
}
