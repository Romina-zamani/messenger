ALTER TABLE messages ADD COLUMN sender_id BIGINT REFERENCES users(id);
CREATE INDEX idx_messages_sender_id ON messages(sender_id);
