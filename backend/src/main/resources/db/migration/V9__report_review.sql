ALTER TABLE safety_reports ADD COLUMN review_status VARCHAR(24) NOT NULL DEFAULT 'OPEN';
ALTER TABLE safety_reports ADD COLUMN review_note VARCHAR(1000) NOT NULL DEFAULT '';
ALTER TABLE safety_reports ADD COLUMN reviewed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE safety_reports ADD COLUMN reviewed_by UUID REFERENCES accounts(id) ON DELETE SET NULL;
ALTER TABLE safety_reports ADD COLUMN revision BIGINT NOT NULL DEFAULT 0;
CREATE INDEX reports_review_queue ON safety_reports(review_status,created_at,id);
