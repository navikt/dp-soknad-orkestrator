CREATE TABLE IF NOT EXISTS soknad_data
(
    id          BIGSERIAL NOT NULL PRIMARY KEY,
    opprettet   TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    soknad_id   uuid      NOT NULL UNIQUE REFERENCES soknad (soknad_id) ON DELETE CASCADE,
    soknad_data jsonb     NOT NULL
);