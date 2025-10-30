ALTER TABLE seksjon_v2
    ADD COLUMN oppdatert TIMESTAMP WITH TIME ZONE NULL;
ALTER TABLE seksjon_v2
    ADD COLUMN dokumentasjonskrav json NULL;