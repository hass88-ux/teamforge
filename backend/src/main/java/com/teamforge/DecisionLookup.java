package com.teamforge;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
@Component
class DecisionLookup {
 private final JdbcTemplate sql;
 DecisionLookup(JdbcTemplate sql) { this.sql=sql; }
 java.util.Set<UUID> targets(UUID actor) {
  return new java.util.HashSet<>(sql.query("SELECT target_id FROM profile_decisions WHERE actor_id=? UNION SELECT target_id FROM account_blocks WHERE actor_id=? UNION SELECT actor_id AS target_id FROM account_blocks WHERE target_id=?",(row,n) -> row.getObject("target_id",UUID.class),actor,actor,actor));
 }
 boolean blocked(UUID a,UUID b) { return sql.queryForObject("SELECT COUNT(*) FROM account_blocks WHERE (actor_id=? AND target_id=?) OR (actor_id=? AND target_id=?)",Long.class,a,b,b,a)>0; }
}
