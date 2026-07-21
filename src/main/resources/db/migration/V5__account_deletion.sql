-- Baja de cuenta. La fila del usuario NO se borra: se vacia de todo lo que identifica a
-- la persona y queda como lapida.
--
-- Borrarla arrastraria en cascada sus pertenencias y sus registros, y con ellos el
-- numerador del brillo de los dias ya vividos: un martes que fue "3 de 3" se repintaria
-- como "2 de 3" en la pantalla de los otros dos. Darse de baja no puede reescribir el
-- pasado de gente que no pidio nada.

ALTER TABLE users ADD COLUMN deleted_at TIMESTAMPTZ;

-- Las consultas por codigo de invitacion y por email deben saltarse las lapidas: nadie
-- debe poder enviar una solicitud de amistad a una cuenta que ya no existe.
CREATE INDEX ix_users_active ON users (id) WHERE deleted_at IS NULL;
