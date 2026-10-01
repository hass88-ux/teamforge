package com.teamforge;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
@Component
class DecisionLookup {
 private final JdbcTemplate sql;
 DecisionLookup(JdbcTemplate sql) { this.sql=sql; }
 java.util.Set<UUID> targets(UUID actor) {
  return new java.util.HashSet<>(sql.query("SELECT target_id FROM profile_decisions WHERE actor_id=?",(row,n) -> row.getObject("target_id",UUID.class),actor));
 }
}
