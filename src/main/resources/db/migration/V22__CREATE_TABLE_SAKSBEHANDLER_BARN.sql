CREATE TABLE saksbehandler_barn
(
    id        BIGSERIAL PRIMARY KEY,
    soknad_id UUID                     NOT NULL REFERENCES soknad (soknad_id),
    barn      JSON                     NOT NULL,
    endret_av TEXT                     NOT NULL,
    opprettet TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_saksbehandler_barn_soknad_id ON saksbehandler_barn (soknad_id);
