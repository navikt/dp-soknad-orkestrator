CREATE TABLE IF NOT EXISTS seksjon
(
    id          BIGSERIAL NOT NULL PRIMARY KEY,
    versjon     TEXT      NOT NULL,
    er_fullfort BOOLEAN   NOT NULL DEFAULT FALSE,
    soknad_id   BIGINT    NOT NULL REFERENCES soknad (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS opplysning
(
    id                    BIGSERIAL                NOT NULL PRIMARY KEY,
    opprettet             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    opplysning_id         uuid                     NOT NULL UNIQUE,
    sist_endret_av_bruker TIMESTAMP WITH TIME ZONE,
    seksjon_id            BIGINT                   NOT NULL REFERENCES seksjon (id) ON DELETE CASCADE,
    opplysningsbehov_id   BIGINT                   NOT NULL,
    type                  TEXT                     NOT NULL,
    svar                  TEXT -- TODO: jsonb/text
);

