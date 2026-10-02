CREATE TABLE projects (
 id UUID PRIMARY KEY,
 owner_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
 client_id UUID NOT NULL,
 name VARCHAR(80) NOT NULL,
 description VARCHAR(1000) NOT NULL,
 stage VARCHAR(24) NOT NULL,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 UNIQUE(owner_id,client_id)
);
CREATE TABLE project_members (
 project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
 account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
 status VARCHAR(16) NOT NULL CHECK(status IN ('INVITED','ACCEPTED','DECLINED')),
 PRIMARY KEY(project_id,account_id)
);
