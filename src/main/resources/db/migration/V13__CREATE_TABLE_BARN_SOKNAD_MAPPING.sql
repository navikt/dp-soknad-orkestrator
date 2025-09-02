CREATE TABLE barn_soknad_mapping
(
    id               BIGSERIAL NOT NULL PRIMARY KEY,
    soknad_id        uuid      NOT NULL UNIQUE,
    soknadbarn_id    uuid      NOT NULL UNIQUE
);