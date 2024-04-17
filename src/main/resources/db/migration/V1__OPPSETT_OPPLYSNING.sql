CREATE TABLE IF NOT EXISTS opplysning
(
    id             BIGSERIAL                NOT NULL PRIMARY KEY,
    opprettet      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    beskrivende_id VARCHAR(255)             NOT NULL,
    type           TEXT                     NOT NULL,
    ident          VARCHAR(11)              NOT NULL,
    soknad_id      uuid                     NOT NULL
);

CREATE TABLE IF NOT EXISTS tekst
(
    id            BIGSERIAL NOT NULL PRIMARY KEY,
    opplysning_id BIGINT    NOT NULL,
    svar          TEXT      NOT NULL,
    FOREIGN KEY (opplysning_id) REFERENCES opplysning (id)
);

CREATE TABLE IF NOT EXISTS heltall
(
    id            BIGSERIAL NOT NULL PRIMARY KEY,
    opplysning_id BIGINT    NOT NULL,
    svar          INTEGER   NOT NULL,
    FOREIGN KEY (opplysning_id) REFERENCES opplysning (id)
);

CREATE TABLE IF NOT EXISTS desimaltall
(
    id            BIGSERIAL        NOT NULL PRIMARY KEY,
    opplysning_id BIGINT           NOT NULL,
    svar          DOUBLE PRECISION NOT NULL,
    FOREIGN KEY (opplysning_id) REFERENCES opplysning (id)
);

CREATE TABLE IF NOT EXISTS boolsk
(
    id            BIGSERIAL NOT NULL PRIMARY KEY,
    opplysning_id BIGINT    NOT NULL,
    svar          BOOLEAN   NOT NULL,
    FOREIGN KEY (opplysning_id) REFERENCES opplysning (id)
);

CREATE TABLE IF NOT EXISTS dato
(
    id            BIGSERIAL NOT NULL PRIMARY KEY,
    opplysning_id BIGINT    NOT NULL,
    svar          DATE      NOT NULL,
    FOREIGN KEY (opplysning_id) REFERENCES opplysning (id)
);

CREATE TABLE IF NOT EXISTS flervalg
(
    id            BIGSERIAL NOT NULL PRIMARY KEY,
    opplysning_id BIGINT    NOT NULL,
    FOREIGN KEY (opplysning_id) REFERENCES opplysning (id)
);

CREATE TABLE IF NOT EXISTS flervalg_svar
(
    id          BIGSERIAL NOT NULL PRIMARY KEY,
    flervalg_id BIGINT    NOT NULL,
    svar        TEXT      NOT NULL,
    FOREIGN KEY (flervalg_id) REFERENCES flervalg (id)
);

CREATE TABLE IF NOT EXISTS periode
(
    id            BIGSERIAL NOT NULL PRIMARY KEY,
    opplysning_id BIGINT    NOT NULL,
    fom           DATE      NOT NULL,
    tom           DATE,
    FOREIGN KEY (opplysning_id) REFERENCES opplysning (id)
);

CREATE TABLE IF NOT EXISTS arbeidsforhold
(
    id            BIGSERIAL NOT NULL PRIMARY KEY,
    opplysning_id BIGINT    NOT NULL,
    FOREIGN KEY (opplysning_id) REFERENCES opplysning (id)
);

CREATE TABLE IF NOT EXISTS arbeidsforhold_svar
(
    id                 BIGSERIAL NOT NULL PRIMARY KEY,
    arbeidsforhold_id  BIGINT    NOT NULL,
    navn_svar_id       BIGINT    NOT NULL,
    land_svar_id       BIGINT    NOT NULL,
    sluttårsak_svar_id BIGINT    NOT NULL,
    FOREIGN KEY (arbeidsforhold_id) REFERENCES arbeidsforhold (id),
    FOREIGN KEY (navn_svar_id) REFERENCES tekst (id),
    FOREIGN KEY (land_svar_id) REFERENCES tekst (id),
    FOREIGN KEY (sluttårsak_svar_id) REFERENCES tekst (id)
);

CREATE TABLE IF NOT EXISTS eøs_arbeidsforhold_svar
(
    id                   BIGSERIAL NOT NULL PRIMARY KEY,
    arbeidsforhold_id    BIGINT    NOT NULL,
    bedrift_navn_svar_id BIGINT    NOT NULL,
    land_svar_id         BIGINT    NOT NULL,
    personnummer_svar_id BIGINT    NOT NULL,
    varighet_svar_id     BIGINT    NOT NULL,
    FOREIGN KEY (arbeidsforhold_id) REFERENCES arbeidsforhold (id),
    FOREIGN KEY (bedrift_navn_svar_id) REFERENCES tekst (id),
    FOREIGN KEY (land_svar_id) REFERENCES tekst (id),
    FOREIGN KEY (personnummer_svar_id) REFERENCES tekst (id),
    FOREIGN KEY (varighet_svar_id) REFERENCES periode (id)
);

CREATE TABLE IF NOT EXISTS egen_næring
(
    id            BIGSERIAL NOT NULL PRIMARY KEY,
    opplysning_id BIGINT    NOT NULL,
    FOREIGN KEY (opplysning_id) REFERENCES opplysning (id)
);

CREATE TABLE IF NOT EXISTS egen_næring_svar
(
    id                  BIGSERIAL NOT NULL PRIMARY KEY,
    egen_næring_id      BIGINT    NOT NULL,
    organisasjonsnummer BIGINT    NOT NULL,
    FOREIGN KEY (egen_næring_id) REFERENCES egen_næring (id)
);

CREATE TABLE IF NOT EXISTS barn
(
    id            BIGSERIAL NOT NULL PRIMARY KEY,
    opplysning_id BIGINT    NOT NULL,
    FOREIGN KEY (opplysning_id) REFERENCES opplysning (id)
);

CREATE TABLE IF NOT EXISTS barn_svar
(
    id                    BIGSERIAL NOT NULL PRIMARY KEY,
    barn_id               BIGINT    NOT NULL,
    fornavn_mellomnavn_id BIGINT    NOT NULL,
    etternavn_id          BIGINT    NOT NULL,
    fødselsdato_id        BIGINT    NOT NULL,
    statsborgerskap_id    BIGINT    NOT NULL,
    forsørger_barnet_id   BIGINT    NOT NULL,
    fra_register          BOOLEAN   NOT NULL,
    FOREIGN KEY (barn_id) REFERENCES barn (id),
    FOREIGN KEY (fornavn_mellomnavn_id) REFERENCES tekst (id),
    FOREIGN KEY (etternavn_id) REFERENCES tekst (id),
    FOREIGN KEY (fødselsdato_id) REFERENCES dato (id),
    FOREIGN KEY (statsborgerskap_id) REFERENCES tekst (id),
    FOREIGN KEY (forsørger_barnet_id) REFERENCES boolsk (id)
);