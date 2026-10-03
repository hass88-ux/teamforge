CREATE TABLE account_blocks (
 actor_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
 target_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 PRIMARY KEY(actor_id,target_id), CHECK(actor_id <> target_id)
);
CREATE INDEX blocks_target ON account_blocks(target_id,actor_id);
CREATE TABLE safety_reports (
 id UUID PRIMARY KEY,
 reporter_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
 match_id UUID NOT NULL REFERENCES collaboration_matches(id) ON DELETE CASCADE,
 client_id UUID NOT NULL,
 reason VARCHAR(24) NOT NULL,
 details VARCHAR(1000) NOT NULL,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 UNIQUE(reporter_id,client_id)
);
