ALTER TABLE seksjon_v2
    RENAME COLUMN json TO seksjonsvar;
ALTER TABLE seksjon_v2
    ADD COLUMN pdf_grunnlag json NOT NULL DEFAULT '{}';