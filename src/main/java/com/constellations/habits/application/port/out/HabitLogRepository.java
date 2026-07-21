package com.constellations.habits.application.port.out;

import com.constellations.habits.domain.habit.HabitLog;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface HabitLogRepository {

    /**
     * Registra el cumplimiento si ese dia no estaba ya marcado.
     *
     * <p>La comprobacion la resuelve el adaptador y no el caso de uso porque quien
     * garantiza la unicidad de {@code (habit_id, log_date)} es la base de datos: entre
     * un "existe?" y un "inserta" cabe otra peticion, y un doble toque del cliente
     * acabaria en un error 500 en vez de en la operacion idempotente que se promete.
     *
     * @return true si este registro creo la estrella; false si ya existia
     */
    boolean saveIfAbsent(HabitLog log);

    /**
     * Todas las fechas cumplidas de un habito. El motor de rachas necesita el historial
     * completo para calcular la mejor racha y las constelaciones ya ganadas.
     */
    List<LocalDate> findDatesByHabit(UUID habitId);

    /**
     * Version por lotes para no lanzar una consulta por habito al listar el panel
     * del usuario (problema N+1).
     */
    Map<UUID, List<LocalDate>> findDatesByHabits(List<UUID> habitIds);

    /**
     * Igual que la anterior, pero acotada a una ventana.
     *
     * <p>El mapa de brillo de una galaxia solo mira los ultimos dias, asi que traer el
     * historial completo de cada miembro es trabajo tirado: con un grupo grande y anos
     * de uso, la mayor parte de lo leido se descarta al instante. El filtro va en SQL,
     * donde ademas puede aprovechar el indice.
     */
    Map<UUID, List<LocalDate>> findDatesByHabitsBetween(
            List<UUID> habitIds, LocalDate from, LocalDate to);

    /** @return true si habia un log que borrar. */
    boolean deleteByHabitAndDate(UUID habitId, LocalDate logDate);
}
