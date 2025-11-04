CREATE TABLE IF NOT EXISTS soknad_personalia
(
    id              BIGSERIAL           NOT NULL PRIMARY KEY,
    soknad_id       UUID                NOT NULL REFERENCES soknad (soknad_id) ON DELETE CASCADE,
    ident           VARCHAR(11)         NOT NULL,
    fornavn         VARCHAR(255)        NOT NULL,
    mellomnavn      VARCHAR(255)        NULL,
    etternavn       VARCHAR(255)        NOT NULL,
    alder           VARCHAR(3)          NOT NULL,
    adresselinje1   VARCHAR(255)        NULL,
    adresselinje2   VARCHAR(255)        NULL,
    adresselinje3   VARCHAR(255)        NULL,
    postnummer      VARCHAR(255)        NULL,
    poststed        VARCHAR(255)        NULL,
    landkode        VARCHAR(3)          NULL,
    land            VARCHAR(255)        NULL,
    kontonummer     VARCHAR(11)         NULL
);

ALTER TABLE soknad_personalia ADD CONSTRAINT soknad_personalia_soknad_id_ident_unique UNIQUE (soknad_id, ident);
