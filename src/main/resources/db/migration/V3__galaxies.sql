-- Constelaciones compartidas: un habito que varios sostienen a la vez, con el brillo de
-- cada dia proporcional a cuantos cumplieron.

CREATE TABLE galaxies (
    id          UUID        PRIMARY KEY,
    name        VARCHAR(80) NOT NULL,
    description VARCHAR(280),
    -- Slug normalizado (minusculas, sin tildes). Es lo que agrupa el catalogo: sin
    -- normalizar, "Gym" y "gym" serian dos temas distintos y ninguno pareceria popular.
    theme       VARCHAR(32) NOT NULL,
    creator_id  UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_galaxies_theme ON galaxies (theme);

CREATE TABLE galaxy_memberships (
    id         UUID        PRIMARY KEY,
    galaxy_id  UUID        NOT NULL REFERENCES galaxies (id) ON DELETE CASCADE,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    -- El habito personal que alimenta esta pertenencia. Un unico registro sirve a la
    -- racha propia y al brillo del grupo, para que no existan dos seguimientos del
    -- mismo habito que puedan discrepar.
    habit_id   UUID        NOT NULL REFERENCES habits (id) ON DELETE CASCADE,
    -- Fechas LOCALES del usuario, como en habit_logs: el dia corta en su medianoche.
    joined_on  DATE        NOT NULL,
    joined_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    left_on    DATE,
    left_at    TIMESTAMPTZ,

    CONSTRAINT ck_memberships_left_after_joined CHECK (left_on IS NULL OR left_on >= joined_on)
);

-- Las filas de quienes se fueron NO se borran: el brillo de un dia pasado se divide
-- entre los miembros que habia ese dia, asi que hace falta el historial de altas y
-- bajas. Por eso el indice unico es parcial: impide pertenecer dos veces a la vez, pero
-- permite volver a entrar despues de haber salido.
CREATE UNIQUE INDEX ux_memberships_active
    ON galaxy_memberships (galaxy_id, user_id)
    WHERE left_on IS NULL;

CREATE INDEX ix_memberships_galaxy ON galaxy_memberships (galaxy_id);
CREATE INDEX ix_memberships_user_active ON galaxy_memberships (user_id) WHERE left_on IS NULL;
CREATE INDEX ix_memberships_habit_active ON galaxy_memberships (habit_id) WHERE left_on IS NULL;
