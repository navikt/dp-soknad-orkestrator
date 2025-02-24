ALTER TABLE barn_svar
    ADD COLUMN barn_svar_id                  uuid NOT NULL DEFAULT gen_random_uuid(),
    ADD COLUMN sist_endret                   TIMESTAMP WITH TIME ZONE,
    ADD COLUMN endret_av                     TEXT,
    ADD COLUMN begrunnelse                   TEXT,
    ADD COLUMN kvalifiserer_til_barnetillegg BOOLEAN,
    ADD COLUMN barnetillegg_fom              DATE,
    ADD COLUMN barnetillegg_tom              DATE;

-- Som default antar vi at alle barn som bruker sier hen forsørger kvalifiserer til barnetillegg
UPDATE barn_svar
SET kvalifiserer_til_barnetillegg = forsørger_barnet
WHERE barn_svar.kvalifiserer_til_barnetillegg IS NULL;

ALTER TABLE barn_svar
    ALTER COLUMN kvalifiserer_til_barnetillegg SET NOT NULL;
