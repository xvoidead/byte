-- Анонимные события прохождения курса. IP-адреса и cookies не хранятся.
CREATE TABLE IF NOT EXISTS events (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    ts      TIMESTAMP    NOT NULL,
    event_day DATE       NOT NULL,
    visitor VARCHAR(36)  NOT NULL,
    type    VARCHAR(32)  NOT NULL,
    lesson  VARCHAR(64),
    item    VARCHAR(200),
    val     INT
);
CREATE INDEX IF NOT EXISTS idx_events_type_lesson ON events (type, lesson);
CREATE INDEX IF NOT EXISTS idx_events_day ON events (event_day);
