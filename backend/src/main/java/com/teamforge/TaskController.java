package com.teamforge;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController @RequestMapping("/api/projects/{project}/tasks")
class TaskController {
 private final CollaborationController collaboration; private final ProjectController projects; private final JdbcTemplate sql; private final TransactionTemplate tx;
 TaskController(CollaborationController c,ProjectController p,JdbcTemplate sql,PlatformTransactionManager manager) { collaboration=c; projects=p; this.sql=sql; tx=new TransactionTemplate(manager); }
 record Draft(@NotNull UUID clientId,@NotBlank @Size(max=120) String title,@NotBlank @Pattern(regexp="TASK|MILESTONE") String kind,LocalDate dueDate) {}
 record Update(@NotNull Boolean done,@NotNull @Min(0) Long revision) {}
 record Task(UUID id,String title,String kind,LocalDate dueDate,boolean done,long revision) {}
 private ResponseStatusException error(int code) { return new ResponseStatusException(HttpStatus.valueOf(code)); }
 private void access(UUID project,UUID actor,boolean write) { var p=projects.access(project,actor,false); if (write && !actor.equals(p.get("owner_id")) && sql.queryForObject("SELECT COUNT(*) FROM project_members WHERE project_id=? AND account_id=? AND status='ACCEPTED'",Long.class,project,actor)==0) throw error(403); }
 private List<Task> rows(UUID project) { return sql.query("SELECT id,title,kind,due_date,done,revision FROM project_tasks WHERE project_id=? ORDER BY kind,due_date,id",(r,n)->new Task(r.getObject(1,UUID.class),r.getString(2),r.getString(3),r.getObject(4,LocalDate.class),r.getBoolean(5),r.getLong(6)),project); }
 @GetMapping List<Task> list(@PathVariable UUID project,Authentication auth) { return tx.execute(s->{ access(project,collaboration.owner(auth),false); return rows(project); }); }
 @PostMapping Task create(@PathVariable UUID project,@Valid @RequestBody Draft d,Authentication auth) {
  UUID actor=collaboration.owner(auth);
  return tx.execute(s->{ access(project,actor,true);
   var prior=sql.queryForList("SELECT id FROM project_tasks WHERE project_id=? AND creator_id=? AND client_id=?",project,actor,d.clientId());
   if (!prior.isEmpty()) { var t=rows(project).stream().filter(x->x.id().equals(prior.getFirst().get("id"))).findFirst().orElseThrow(); if (!t.title().equals(d.title().strip()) || !t.kind().equals(d.kind()) || !Objects.equals(t.dueDate(),d.dueDate())) throw error(409); return t; }
   if (sql.queryForObject("SELECT COUNT(*) FROM project_tasks WHERE project_id=?",Long.class,project)>=100) throw error(429);
   UUID id=UUID.randomUUID(); sql.update("INSERT INTO project_tasks(id,project_id,creator_id,client_id,title,kind,due_date) VALUES(?,?,?,?,?,?,?)",id,project,actor,d.clientId(),d.title().strip(),d.kind(),d.dueDate()); return rows(project).stream().filter(t->t.id().equals(id)).findFirst().orElseThrow();
  });
 }
 @PutMapping("/{id}") Task update(@PathVariable UUID project,@PathVariable UUID id,@Valid @RequestBody Update u,Authentication auth) {
  return tx.execute(s->{ access(project,collaboration.owner(auth),true); var t=rows(project).stream().filter(x->x.id().equals(id)).findFirst().orElseThrow(()->error(404)); if (t.revision()!=u.revision()) throw error(409); sql.update("UPDATE project_tasks SET done=?,revision=revision+1 WHERE id=?",u.done(),id); return rows(project).stream().filter(x->x.id().equals(id)).findFirst().orElseThrow(); });
 }
}
