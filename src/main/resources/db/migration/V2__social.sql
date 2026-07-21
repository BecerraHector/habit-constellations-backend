-- Capa social: codigo de invitacion personal y relaciones de amistad.

ALTER TABLE users ADD COLUMN invite_code VARCHAR(8);

-- Relleno para las filas que ya existan. Se deriva del id (determinista, distinto por
-- usuario) y se traduce a un alfabeto sin O/0/I/1, el mismo que usa InviteCode.
-- Es un mejor esfuerzo para datos preexistentes: los codigos nuevos los genera la
-- aplicacion con SecureRandom y reintenta si colisionan. El indice unico de abajo es
-- quien garantiza la correccion en cualquier caso.
UPDATE users
SET invite_code = UPPER(TRANSLATE(SUBSTR(MD5(id::text), 1, 8), '01', 'xy'))
WHERE invite_code IS NULL;

ALTER TABLE users ALTER COLUMN invite_code SET NOT NULL;

CREATE UNIQUE INDEX ux_users_invite_code ON users (invite_code);

CREATE TABLE friendships (
    id           UUID        PRIMARY KEY,
    -- Se conserva quien pidio a quien: define quien puede aceptar o rechazar.
    requester_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    addressee_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status       VARCHAR(16) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    responded_at TIMESTAMPTZ,

    CONSTRAINT ck_friendships_not_self CHECK (requester_id <> addressee_id),
    CONSTRAINT ck_friendships_status CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED'))
);

-- Una sola relacion por pareja, en cualquier orden: normalizar con LEAST/GREATEST hace
-- que (A,B) y (B,A) colisionen, de modo que dos solicitudes cruzadas simultaneas no
-- pueden crear dos filas.
CREATE UNIQUE INDEX ux_friendships_pair
    ON friendships (LEAST(requester_id, addressee_id), GREATEST(requester_id, addressee_id));

-- Sirve al listado de amigos y al de solicitudes recibidas.
CREATE INDEX ix_friendships_addressee ON friendships (addressee_id, status);
CREATE INDEX ix_friendships_requester ON friendships (requester_id, status);
