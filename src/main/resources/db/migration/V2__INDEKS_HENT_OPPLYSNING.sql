CREATE INDEX idx_opplysning_beskrivende_ident_soknad ON opplysning (beskrivende_id, ident, soknad_id);

CREATE INDEX idx_tekst_opplysning_id ON tekst (opplysning_id);
CREATE INDEX idx_heltall_opplysning_id ON heltall (opplysning_id);
CREATE INDEX idx_desimaltall_opplysning_id ON desimaltall (opplysning_id);
CREATE INDEX idx_boolsk_opplysning_id ON boolsk (opplysning_id);
CREATE INDEX idx_dato_opplysning_id ON dato (opplysning_id);
CREATE INDEX idx_flervalg_opplysning_id ON flervalg (opplysning_id);
CREATE INDEX idx_periode_opplysning_id ON periode (opplysning_id);
CREATE INDEX idx_arbeidsforhold_opplysning_id ON arbeidsforhold (opplysning_id);
CREATE INDEX idx_egen_næring_opplysning_id ON egen_næring (opplysning_id);
CREATE INDEX idx_barn_opplysning_id ON barn (opplysning_id);