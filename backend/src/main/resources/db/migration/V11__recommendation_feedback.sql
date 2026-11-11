CREATE TABLE recommendation_feedback (
 actor_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
 target_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
 rating VARCHAR(12) NOT NULL CHECK (rating IN ('RELEVANT','NOT_RELEVANT')),
 model_version VARCHAR(80) NOT NULL,
 compatibility INTEGER NOT NULL CHECK (compatibility BETWEEN 0 AND 100),
 features_json TEXT NOT NULL,
 updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
 PRIMARY KEY(actor_id,target_id),
 CHECK (actor_id <> target_id)
);
CREATE INDEX recommendation_feedback_target_idx ON recommendation_feedback(target_id);
