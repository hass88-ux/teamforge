CREATE TABLE match_reads (
 match_id UUID NOT NULL REFERENCES collaboration_matches(id) ON DELETE CASCADE,
 account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
 last_sequence BIGINT NOT NULL DEFAULT 0,
 PRIMARY KEY(match_id,account_id)
);
CREATE TABLE profile_details (
 account_id UUID PRIMARY KEY REFERENCES accounts(id) ON DELETE CASCADE,
 github_url VARCHAR(100) NOT NULL,
 project_history VARCHAR(1000) NOT NULL
);
CREATE TABLE project_tasks (
 id UUID PRIMARY KEY,
 project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
 creator_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
 client_id UUID NOT NULL,
 title VARCHAR(120) NOT NULL,
 kind VARCHAR(16) NOT NULL CHECK(kind IN ('TASK','MILESTONE')),
 due_date DATE,
 done BOOLEAN NOT NULL DEFAULT FALSE,
 revision BIGINT NOT NULL DEFAULT 0,
 UNIQUE(project_id,creator_id,client_id)
);
CREATE TABLE recovery_keys (
 account_id UUID PRIMARY KEY REFERENCES accounts(id) ON DELETE CASCADE,
 key_hash VARCHAR(64) NOT NULL
);
