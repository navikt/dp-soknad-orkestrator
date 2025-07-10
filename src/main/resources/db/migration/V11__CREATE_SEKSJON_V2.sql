CREATE TABLE IF NOT EXISTS seksjon_v2
(
    id          BIGSERIAL                   NOT NULL PRIMARY KEY,
    seksjon_id  TEXT                        NOT NULL,
    soknad_id   BIGINT                      NOT NULL REFERENCES soknad (id) ON DELETE CASCADE,
    opprettet   TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT NOW(),
    json        jsonb                       NOT NULL
);

ALTER TABLE seksjon_v2 ADD CONSTRAINT seksjon_v2_seksjon_id_soknad_id_unique UNIQUE (seksjon_id, soknad_id);
