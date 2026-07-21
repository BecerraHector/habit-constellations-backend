-- Esquema base: usuarios, habitos diarios y su registro de cumplimiento.

CREATE TABLE users (
    id             UUID         PRIMARY KEY,
    email          VARCHAR(255) NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    display_name   VARCHAR(60)  NOT NULL,
    -- Zona horaria IANA (ej. "America/Lima"). Define donde cae la medianoche
    -- del usuario, que es el corte con el que se evaluan las rachas.
    zone_id        VARCHAR(64)  NOT NULL DEFAULT 'UTC',
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- El email es unico sin distinguir mayusculas: dos cuentas no pueden diferir solo en case.
CREATE UNIQUE INDEX ux_users_email_lower ON users (LOWER(email));

CREATE TABLE habits (
    id          UUID         PRIMARY KEY,
    owner_id    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name        VARCHAR(80)  NOT NULL,
    description VARCHAR(280),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- Los habitos se archivan, nunca se borran: sus logs son historia del usuario.
    archived_at TIMESTAMPTZ
);

CREATE INDEX ix_habits_owner ON habits (owner_id) WHERE archived_at IS NULL;

CREATE TABLE habit_logs (
    id            UUID        PRIMARY KEY,
    habit_id      UUID        NOT NULL REFERENCES habits (id) ON DELETE CASCADE,
    -- Fecha local del usuario, no un instante: "el dia que cumplio el habito".
    log_date      DATE        NOT NULL,
    completed_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_habit_logs_habit_date UNIQUE (habit_id, log_date)
);

CREATE INDEX ix_habit_logs_habit_date ON habit_logs (habit_id, log_date DESC);
