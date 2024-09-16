-- Drop indekser
DROP INDEX IF EXISTS idx_opplysning_beskrivende_ident_soknad;

DROP INDEX IF EXISTS idx_tekst_opplysning_id;
DROP INDEX IF EXISTS idx_heltall_opplysning_id;
DROP INDEX IF EXISTS idx_desimaltall_opplysning_id;
DROP INDEX IF EXISTS idx_boolsk_opplysning_id;
DROP INDEX IF EXISTS idx_dato_opplysning_id;
DROP INDEX IF EXISTS idx_flervalg_opplysning_id;
DROP INDEX IF EXISTS idx_periode_opplysning_id;
DROP INDEX IF EXISTS idx_arbeidsforhold_opplysning_id;
DROP INDEX IF EXISTS idx_egen_næring_opplysning_id;
DROP INDEX IF EXISTS idx_barn_opplysning_id;

DROP INDEX IF EXISTS idx_opplysning_soknad;

-- Drop fremmednøkler
ALTER TABLE tekst DROP CONSTRAINT tekst_opplysning_id_fkey;
ALTER TABLE heltall DROP CONSTRAINT heltall_opplysning_id_fkey;
ALTER TABLE desimaltall DROP CONSTRAINT desimaltall_opplysning_id_fkey;
ALTER TABLE boolsk DROP CONSTRAINT boolsk_opplysning_id_fkey;
ALTER TABLE dato DROP CONSTRAINT dato_opplysning_id_fkey;
ALTER TABLE flervalg DROP CONSTRAINT flervalg_opplysning_id_fkey;
ALTER TABLE periode DROP CONSTRAINT periode_opplysning_id_fkey;
ALTER TABLE arbeidsforhold DROP CONSTRAINT arbeidsforhold_opplysning_id_fkey;
ALTER TABLE egen_næring DROP CONSTRAINT egen_næring_opplysning_id_fkey;
ALTER TABLE barn DROP CONSTRAINT barn_opplysning_id_fkey;

-- Rename tabell
ALTER TABLE opplysning RENAME TO quiz_opplysning;

-- Rename kolonner
ALTER TABLE tekst RENAME COLUMN opplysning_id TO quiz_opplysning_id;
ALTER TABLE heltall RENAME COLUMN opplysning_id TO quiz_opplysning_id;
ALTER TABLE desimaltall RENAME COLUMN opplysning_id TO quiz_opplysning_id;
ALTER TABLE boolsk RENAME COLUMN opplysning_id TO quiz_opplysning_id;
ALTER TABLE dato RENAME COLUMN opplysning_id TO quiz_opplysning_id;
ALTER TABLE flervalg RENAME COLUMN opplysning_id TO quiz_opplysning_id;
ALTER TABLE periode RENAME COLUMN opplysning_id TO quiz_opplysning_id;
ALTER TABLE arbeidsforhold RENAME COLUMN opplysning_id TO quiz_opplysning_id;
ALTER TABLE egen_næring RENAME COLUMN opplysning_id TO quiz_opplysning_id;
ALTER TABLE barn RENAME COLUMN opplysning_id TO quiz_opplysning_id;

-- Lag fremmednøkler
ALTER TABLE tekst ADD CONSTRAINT tekst_quiz_opplysning_id_fkey FOREIGN KEY (quiz_opplysning_id) REFERENCES quiz_opplysning (id) ON DELETE CASCADE;
ALTER TABLE heltall ADD CONSTRAINT heltall_quiz_opplysning_id_fkey FOREIGN KEY (quiz_opplysning_id) REFERENCES quiz_opplysning (id) ON DELETE CASCADE;
ALTER TABLE desimaltall ADD CONSTRAINT desimaltall_quiz_opplysning_id_fkey FOREIGN KEY (quiz_opplysning_id) REFERENCES quiz_opplysning (id) ON DELETE CASCADE;
ALTER TABLE boolsk ADD CONSTRAINT boolsk_quiz_opplysning_id_fkey FOREIGN KEY (quiz_opplysning_id) REFERENCES quiz_opplysning (id) ON DELETE CASCADE;
ALTER TABLE dato ADD CONSTRAINT dato_quiz_opplysning_id_fkey FOREIGN KEY (quiz_opplysning_id) REFERENCES quiz_opplysning (id) ON DELETE CASCADE;
ALTER TABLE flervalg ADD CONSTRAINT flervalg_quiz_opplysning_id_fkey FOREIGN KEY (quiz_opplysning_id) REFERENCES quiz_opplysning (id) ON DELETE CASCADE;
ALTER TABLE periode ADD CONSTRAINT periode_quiz_opplysning_id_fkey FOREIGN KEY (quiz_opplysning_id) REFERENCES quiz_opplysning (id) ON DELETE CASCADE;
ALTER TABLE arbeidsforhold ADD CONSTRAINT arbeidsforhold_quiz_opplysning_id_fkey FOREIGN KEY (quiz_opplysning_id) REFERENCES quiz_opplysning (id) ON DELETE CASCADE;
ALTER TABLE egen_næring ADD CONSTRAINT egen_næring_quiz_opplysning_id_fkey FOREIGN KEY (quiz_opplysning_id) REFERENCES quiz_opplysning (id) ON DELETE CASCADE;
ALTER TABLE barn ADD CONSTRAINT barn_quiz_opplysning_id_fkey FOREIGN KEY (quiz_opplysning_id) REFERENCES quiz_opplysning (id) ON DELETE CASCADE;

-- Lag indekser
CREATE INDEX idx_quiz_opplysning_beskrivende_ident_soknad ON quiz_opplysning (beskrivende_id, ident, soknad_id);

CREATE INDEX idx_tekst_quiz_opplysning_id ON tekst (quiz_opplysning_id);
CREATE INDEX idx_heltall_quiz_opplysning_id ON heltall (quiz_opplysning_id);
CREATE INDEX idx_desimaltall_quiz_opplysning_id ON desimaltall (quiz_opplysning_id);
CREATE INDEX idx_boolsk_quiz_opplysning_id ON boolsk (quiz_opplysning_id);
CREATE INDEX idx_dato_quiz_opplysning_id ON dato (quiz_opplysning_id);
CREATE INDEX idx_flervalg_quiz_opplysning_id ON flervalg (quiz_opplysning_id);
CREATE INDEX idx_periode_quiz_opplysning_id ON periode (quiz_opplysning_id);
CREATE INDEX idx_arbeidsforhold_quiz_opplysning_id ON arbeidsforhold (quiz_opplysning_id);
CREATE INDEX idx_egen_næring_quiz_opplysning_id ON egen_næring (quiz_opplysning_id);
CREATE INDEX idx_barn_quiz_opplysning_id ON barn (quiz_opplysning_id);

CREATE INDEX idx_quiz_opplysning_soknad_id ON quiz_opplysning (soknad_id);
