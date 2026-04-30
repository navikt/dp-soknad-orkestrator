CREATE TABLE ettersending_journalpost
(
    id             SERIAL PRIMARY KEY,
    soknad_id      UUID        NOT NULL REFERENCES soknad (soknad_id) ON DELETE CASCADE,
    journalpost_id VARCHAR(32) NOT NULL UNIQUE,
    opprettet      TIMESTAMP   NOT NULL DEFAULT NOW()
);
