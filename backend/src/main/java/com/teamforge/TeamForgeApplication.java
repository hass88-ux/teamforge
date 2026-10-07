package com.teamforge;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication(exclude = org.springframework.boot.autoconfigure.session.SessionAutoConfiguration.class)
public class TeamForgeApplication {
 public static void main(String[] args) { SpringApplication.run(TeamForgeApplication.class, args); }
}
