-- Sesiones de larga vida, para no obligar a teclear la contrasena cada media hora.

CREATE TABLE refresh_tokens (
    id          UUID         PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    -- Solo el hash del valor, igual que con las contrasenas: quien lea esta tabla no
    -- puede reconstruir las sesiones de nadie. El valor en claro se entrega una sola
    -- vez, al cliente, y no se guarda en ningun sitio.
    token_hash  VARCHAR(128) NOT NULL,
    issued_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at  TIMESTAMPTZ  NOT NULL,
    -- Se marca en vez de borrarse: una fila revocada es lo que permite detectar que un
    -- token ya usado vuelve a presentarse, senal de que existe una copia.
    revoked_at  TIMESTAMPTZ,

    CONSTRAINT ck_refresh_tokens_expiry CHECK (expires_at > issued_at)
);

-- La busqueda siempre es por hash: es la unica pista que llega desde el cliente.
CREATE UNIQUE INDEX ux_refresh_tokens_hash ON refresh_tokens (token_hash);

-- Revocar todas las sesiones vivas de un usuario de golpe.
CREATE INDEX ix_refresh_tokens_user_active
    ON refresh_tokens (user_id)
    WHERE revoked_at IS NULL;
