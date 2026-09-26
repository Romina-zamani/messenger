CREATE TABLE messages (
  id BIGSERIAL PRIMARY KEY,
  nickname VARCHAR(30) NOT NULL,
  content VARCHAR(500) NOT NULL,
  sent_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_messages_sent_at ON messages (sent_at DESC);

