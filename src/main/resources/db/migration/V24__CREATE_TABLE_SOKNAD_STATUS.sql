CREATE TABLE IF NOT EXISTS soknad_status
(
    id                 BIGSERIAL   NOT NULL PRIMARY KEY,
    soknad_id          UUID        NOT NULL REFERENCES soknad (soknad_id),
    ident              VARCHAR(11) NOT NULL,
    behandling_id      TEXT        NOT NULL,
    forte_til          TEXT        NOT NULL,
    rettighetsperioder JSON        NOT NULL
);
