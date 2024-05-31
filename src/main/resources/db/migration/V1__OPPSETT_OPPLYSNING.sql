CREATE TABLE IF NOT EXISTS soknad
(
    id        BIGSERIAL                NOT NULL PRIMARY KEY,
    opprettet TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    soknad_id uuid                     NOT NULL,
    ident     VARCHAR(11)              NOT NULL
);


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
    opplysning_id BIGINT    NOT NULL REFERENCES opplysning (id) ON DELETE CASCADE,
    svar          TEXT      NOT NULL
);

CREATE TABLE IF NOT EXISTS heltall
(
    id            BIGSERIAL NOT NULL PRIMARY KEY,
    opplysning_id BIGINT    NOT NULL REFERENCES opplysning (id) ON DELETE CASCADE,
    svar          INTEGER   NOT NULL
);

CREATE TABLE IF NOT EXISTS desimaltall
(
    id            BIGSERIAL NOT NULL PRIMARY KEY,
    opplysning_id BIGINT    NOT NULL REFERENCES opplysning (id) ON DELETE CASCADE,
    svar          DECIMAL   NOT NULL
);

CREATE TABLE IF NOT EXISTS boolsk
(
    id            BIGSERIAL NOT NULL PRIMARY KEY,
    opplysning_id BIGINT    NOT NULL REFERENCES opplysning (id) ON DELETE CASCADE,
    svar          BOOLEAN   NOT NULL
);

CREATE TABLE IF NOT EXISTS dato
(
    id            BIGSERIAL NOT NULL PRIMARY KEY,
    opplysning_id BIGINT    NOT NULL REFERENCES opplysning (id) ON DELETE CASCADE,
    svar          DATE      NOT NULL
);

CREATE TABLE IF NOT EXISTS flervalg
(
    id            BIGSERIAL NOT NULL PRIMARY KEY,
    opplysning_id BIGINT    NOT NULL REFERENCES opplysning (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS flervalg_svar
(
    id          BIGSERIAL NOT NULL PRIMARY KEY,
    flervalg_id BIGINT    NOT NULL REFERENCES flervalg (id) ON DELETE CASCADE,
    svar        TEXT      NOT NULL
);

CREATE TABLE IF NOT EXISTS periode
(
    id            BIGSERIAL NOT NULL PRIMARY KEY,
    opplysning_id BIGINT    NOT NULL REFERENCES opplysning (id) ON DELETE CASCADE,
    fom           DATE      NOT NULL,
    tom           DATE
);

CREATE TABLE IF NOT EXISTS arbeidsforhold
(
    id            BIGSERIAL NOT NULL PRIMARY KEY,
    opplysning_id BIGINT    NOT NULL REFERENCES opplysning (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS arbeidsforhold_svar
(
    id                BIGSERIAL NOT NULL PRIMARY KEY,
    arbeidsforhold_id BIGINT    NOT NULL REFERENCES arbeidsforhold (id) ON DELETE CASCADE,
    navn              TEXT      NOT NULL,
    land              TEXT      NOT NULL,
    sluttårsak        TEXT      NOT NULL
);

CREATE TABLE IF NOT EXISTS eøs_arbeidsforhold_svar
(
    id                BIGSERIAL NOT NULL PRIMARY KEY,
    arbeidsforhold_id BIGINT    NOT NULL REFERENCES arbeidsforhold (id) ON DELETE CASCADE,
    bedriftsnavn      TEXT      NOT NULL,
    land              TEXT      NOT NULL,
    personnummer      TEXT      NOT NULL,
    fom               DATE      NOT NULL,
    tom               DATE
);

CREATE TABLE IF NOT EXISTS egen_næring
(
    id            BIGSERIAL NOT NULL PRIMARY KEY,
    opplysning_id BIGINT    NOT NULL REFERENCES opplysning (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS egen_næring_svar
(
    id                  BIGSERIAL NOT NULL PRIMARY KEY,
    egen_næring_id      BIGINT    NOT NULL REFERENCES egen_næring (id) ON DELETE CASCADE,
    organisasjonsnummer BIGINT    NOT NULL
);

CREATE TABLE IF NOT EXISTS barn
(
    id            BIGSERIAL NOT NULL PRIMARY KEY,
    opplysning_id BIGINT    NOT NULL REFERENCES opplysning (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS barn_svar
(
    id                 BIGSERIAL NOT NULL PRIMARY KEY,
    barn_id            BIGINT    NOT NULL REFERENCES barn (id) ON DELETE CASCADE,
    fornavn_mellomnavn TEXT      NOT NULL,
    etternavn          TEXT      NOT NULL,
    fødselsdato        DATE      NOT NULL,
    statsborgerskap    TEXT      NOT NULL,
    forsørger_barnet   BOOLEAN   NOT NULL,
    fra_register       BOOLEAN   NOT NULL
);