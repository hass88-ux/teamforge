CREATE TABLE coffee_proposals (
 id UUID PRIMARY KEY,
 match_id UUID NOT NULL REFERENCES collaboration_matches(id) ON DELETE CASCADE,
 proposer_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
 client_id UUID NOT NULL,
 kind VARCHAR(24) NOT NULL,
 starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
 timezone VARCHAR(80) NOT NULL,
 note VARCHAR(500) NOT NULL,
 status VARCHAR(10) NOT NULL CHECK(status IN ('PROPOSED','ACCEPTED','DECLINED','CANCELLED')),
 created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 UNIQUE(match_id,proposer_id,client_id)
);
CREATE INDEX proposals_match_time ON coffee_proposals(match_id,created_at);
