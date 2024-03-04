CREATE TABLE IF NOT EXISTS opplysning
(
    id             BIGSERIAL                NOT NULL,
    opprettet      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    beksrivende_id VARCHAR(255)             NOT NULL,
    svar           VARCHAR(255)             NOT NULL,
    fødselsnummer  VARCHAR(11)              NOT NULL,
    søknads_id      VARCHAR(255),
    behandlings_id  VARCHAR(255)
);
