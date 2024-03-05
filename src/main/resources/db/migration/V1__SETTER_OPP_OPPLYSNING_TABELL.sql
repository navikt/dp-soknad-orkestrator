CREATE TABLE IF NOT EXISTS opplysning
(
    id             BIGSERIAL                NOT NULL,
    opprettet      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    beskrivende_id VARCHAR(255)             NOT NULL,
    svar           VARCHAR(255)             NOT NULL,
    fodselsnummer  VARCHAR(11)              NOT NULL,
    soknads_id     uuid,
    behandlings_id uuid
);
