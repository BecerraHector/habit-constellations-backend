# Pendientes

Carencias conocidas, anotadas a propósito para que no se pierdan. No son ideas sueltas:
cada una salió de una revisión concreta y lleva el porqué, para poder retomarla sin volver
a razonar desde cero.

Orden por importancia, no por esfuerzo.

---

## 1. El login ya tiene límite de intentos

**Estado:** hecho, con una limitación conocida

Se cerró con las dos piezas que pedía la nota original: un **retardo creciente por
cuenta** (`InMemoryLoginAttemptLimiter`, detrás del puerto `LoginAttemptLimiter`: tres
fallos gratis, después 30 s que se duplican hasta 15 min; el éxito perdona) y un **tope
duro por IP** (`IpThrottleFilter`, ventana fija por minuto, corre antes que Spring
Security para que una IP bloqueada no cueste ni un BCrypt). El freno por cuenta actúa
exista o no el email, para no revelar cuáles existen por la cadencia. El filtro por IP
cubre también `POST /api/v1/friend-requests`, que era el otro espacio de búsqueda.

**Limitación:** ambos guardan su estado en memoria del proceso. Con una sola instancia es
correcto; si algún día hay varias detrás de un balanceador, cada una llevará su propia
cuenta y el límite efectivo se multiplica. La salida entonces es un almacén compartido
(Redis) detrás de los mismos puertos, sin tocar el caso de uso. Detrás de un proxy
inverso hay que activar `server.forward-headers-strategy` para que la IP vista sea la
real y no la del proxy.

## 2. Las filas de `refresh_tokens` ya se limpian

**Estado:** hecho

`RefreshTokenCleanupJob` barre a diario (04:00 UTC) las filas **ya caducadas**, revocadas
o no, mediante `deleteExpiredBefore`. Las revocadas aún vigentes se conservan tal como
pedía la nota: son las que delatan que un token robado vuelve a presentarse.

De la decisión pendiente que acompañaba a esta nota, la mitad ya existe: hay
`POST /api/v1/auth/logout-all` para cerrar todas las sesiones. Ver y enumerar las
sesiones activas una a una sigue sin hacerse, y hoy ninguna pantalla lo pide.

## 3. La capa de aplicación ya tiene pruebas unitarias

**Estado:** hecho, en lo que pedía la nota

Los dos casos que la nota señalaba están cubiertos con todos los puertos doblados y el
reloj fijo: `GalaxyServiceWindowTest` (la ventana del mapa: defecto de 30 días, recorte
al primer miembro —contando a quien ya se fue—, tope de 365 y valores inválidos) y
`UserAccountServiceRefreshTest` (la rotación, la reutilización que cierra todas las
sesiones *fuera* de transacción, y la caducidad que no cierra nada). `UserAccountServiceLoginTest`
cubre además el freno de intentos.

No es cobertura total de la capa y no pretende serlo: la regla práctica a partir de aquí
es que cada caso de uso nuevo con lógica temporal o transaccional traiga su test unitario
consigo, en lugar de fiarlo todo a la integración.

## 4. Los listados que crecen ya se paginan

**Estado:** hecho

`GET /api/v1/friends` y `GET /api/v1/galaxies/{id}/members` devuelven tramos, con el
tamaño acotado en `PageQuery` y no en el controlador, para que ningún endpoint nuevo pueda
saltarse el tope por olvido.

Quedan sin paginar `GET /api/v1/habits` y `GET /api/v1/galaxies` a propósito: su tamaño lo
fija el propio usuario con sus acciones, y paginarlos obligaría al frontend a iterar para
pintar un panel que siempre quiere el conjunto completo.

**Limitación conocida:** el panel de amigos ordena por racha *dentro* del tramo, no sobre
el total, porque la racha es un valor calculado que no existe en la base de datos y no se
puede ordenar por él en SQL. Con pocos amigos no se nota. Si llegara a importar, la salida
es materializar la racha en una columna mantenida por el propio motor — pero eso choca con
la invariante de no persistir valores derivados, así que hay que decidirlo a conciencia.

## 5. El mapa de brillo ya no lee el historial completo

**Estado:** hecho

`HabitLogRepository.findDatesByHabitsBetween` acota la consulta a la ventana que se va a
pintar, en SQL.

**Queda un resto:** para calcular el denominador de cada día hay que cargar todas las
pertenencias de la galaxia, incluidas las de quienes ya se fueron. Son filas ligeras y el
número está acotado por el tamaño del grupo, pero en una galaxia abierta muy grande esto
crece. La salida sería una consulta agregada que devuelva directamente
`(fecha, activos, cumplimientos)` en lugar de reconstruirlo en memoria.

## 6. Un commit mezcló funcionalidad con arreglos

**Estado:** anotado, no accionable

El commit que añadió sesiones de refresco, CORS y OpenAPI incluye además los arreglos de
atomicidad y de idempotencia. Estaban entrelazados en los mismos ficheros y separarlos
habría producido un commit que no compila, lo cual rompe `git bisect` peor que la mezcla.
El cuerpo del mensaje lo dice explícitamente.

No hay nada que arreglar: queda como recordatorio de hacer los arreglos en un commit
propio *antes* de construir encima, en lugar de al terminar.

---

## Además, cuando haya frontend

- **El brillo en galaxias muy concurridas** deja de ser presión de grupo y pasa a ser una
  estadística. Está explicado en el README; la salida es filtrar el mapa a los amigos que
  hay dentro del grupo, y las amistades ya existen.
- **Una galaxia puede quedarse sin ningún miembro** si todos la abandonan o se dan de baja.
  Hoy simplemente se queda a oscuras y sigue apareciendo en el catálogo. Habría que
  decidir si se archiva sola, si se ofrece a quien la descubra, o si se deja tal cual como
  registro de algo que existió.
