ALTER TABLE seksjon_v2 DROP CONSTRAINT seksjon_v2_seksjon_id_soknad_id_unique;
ALTER TABLE seksjon_v2 DROP COLUMN soknad_id;
ALTER TABLE seksjon_v2 ADD COLUMN soknad_id UUID NOT NULL REFERENCES soknad (soknad_id) ON DELETE CASCADE;
ALTER TABLE seksjon_v2 ADD CONSTRAINT seksjon_v2_seksjon_id_soknad_id_unique UNIQUE (seksjon_id, soknad_id);
