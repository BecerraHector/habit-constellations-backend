# Pendientes

Carencias conocidas, anotadas a propósito para que no se pierdan. No son ideas sueltas:
cada una salió de una revisión concreta y lleva el porqué, para poder retomarla sin volver
a razonar desde cero.

Orden por importancia, no por esfuerzo.

---

## 1. El login no tiene límite de intentos

**Estado:** pendiente · **Riesgo:** alto

Nada impide probar contraseñas contra `POST /api/v1/auth/login` a máxima velocidad. BCrypt
encarece cada intento lo suficiente para que no sea gratis, pero no sustituye a un límite:
con paralelismo, una contraseña débil sigue cayendo.

Es la carencia de seguridad más seria que queda, y **debería cerrarse antes de exponer la
API en un servidor público**.

Hace falta limitar por IP y por cuenta a la vez. Solo por IP no protege de un ataque
distribuido contra una cuenta concreta; solo por cuenta permite que alguien bloquee a otro
a base de fallar sus intentos, que es una denegación de servicio con otro nombre. Lo
razonable es un retardo creciente por cuenta y un tope duro por IP.

Afecta también a `POST /api/v1/friend-requests`: probar códigos de invitación al azar es
otro espacio de búsqueda, aunque de 32⁸ y por tanto mucho menos urgente.

## 2. Las filas de `refresh_tokens` no se limpian nunca

**Estado:** pendiente · **Riesgo:** medio, creciente con el tiempo

Cada login inserta una fila y ninguna se borra jamás, ni siquiera al caducar. La tabla
crece de forma indefinida.

No se pueden borrar las revocadas sin más: son justo las que permiten detectar que un
token ya usado vuelve a presentarse. Lo correcto es borrar por **fecha de caducidad**
—una fila caducada hace meses ya no detecta nada útil— con una tarea periódica.

Conviene decidir a la vez si un usuario debe poder ver y cerrar sus sesiones activas
desde la aplicación, porque reutiliza exactamente los mismos datos.

## 3. La capa de aplicación no tiene pruebas unitarias

**Estado:** pendiente · **Riesgo:** medio

El dominio está muy cubierto y hay pruebas de integración de extremo a extremo, pero los
servicios de aplicación solo se ejercitan a través de HTTP y PostgreSQL. Eso encarece
probar los casos raros: un fallo a mitad de transacción o un reloj en una fecha concreta
exigen montar el mundo entero.

Los puertos ya están definidos, así que meter dobles es barato. Los casos que más lo
piden son `GalaxyService.windowStart` (la ventana con miembros que entran y salen) y
`UserAccountService.refresh` (la rotación y la detección de reutilización).

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
- **Un usuario no puede borrar su cuenta.** Las claves foráneas ya están en `ON DELETE
  CASCADE`, así que el esquema aguanta; falta decidir qué pasa con las galaxias que creó y
  con el brillo pasado de los días en que sí cumplió.
