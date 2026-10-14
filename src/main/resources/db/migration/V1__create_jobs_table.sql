CREATE TABLE jobs (
                      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                      type VARCHAR(100) NOT NULL,
                      payload JSONB NOT NULL,
                      status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                      attempts INT NOT NULL DEFAULT 0,
                      max_attempts INT NOT NULL DEFAULT 5,
                      run_at TIMESTAMP NOT NULL DEFAULT NOW(),
                      created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                      updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                      completed_at TIMESTAMP,
                      last_error TEXT
);

CREATE INDEX idx_jobs_status_run_at
    ON jobs (status, run_at)
    WHERE status IN ('PENDING', 'FAILED');

INSERT INTO jobs (type, payload) VALUES
                                     ('SEND_EMAIL', '{"to":"demo@example.com","subject":"Hello","body":"Test"}'),
                                     ('SEND_WEBHOOK', '{"url":"http://localhost:8080/test/receive","body":{"event":"test"}}'),
                                     ('GENERATE_PDF', '{"title":"Demo Report","content":"This is a test report"}');