package com.constellations.habits.domain.streak;

import com.constellations.habits.domain.ValidationException;

import java.time.LocalDate;
import java.util.UUID;

/**
 * La vida de un habito en fechas locales del usuario: desde que se creo hasta que se
 * archivo, si llego a archivarse.
 *
 * <p>Es lo unico que el mapa del cielo necesita saber de un habito para decidir si pesa
 * en el denominador de un dia. Las fechas llegan ya convertidas a la zona del usuario:
 * el dominio no toca relojes ni zonas.
 *
 * @param habitId    el habito
 * @param createdOn  fecha local de creacion
 * @param archivedOn fecha local de archivo, o null si sigue vivo
 */
public record HabitSpan(UUID habitId, LocalDate createdOn, LocalDate archivedOn) {

    public HabitSpan {
        ValidationException.requirePresent(habitId, "habitId");
        ValidationException.requirePresent(createdOn, "createdOn");
    }

    /**
     * Mismo criterio que la pertenencia a una galaxia: una estrella ganada siempre
     * cuenta, y los dias sin cumplir solo pesan dentro de la vida del habito.
     *
     * <p>El cumplimiento manda por dos razones concretas. Al archivar: la estrella
     * ganada por la manana no se borra por la tarde, pero archivar sin cumplir no
     * apunta un fallo en un habito ya soltado. Al crear: la ventana de repaso permite
     * rellenar ayer en un habito recien creado, y esa estrella debe verse aunque el
     * habito "no existiera" ese dia.
     */
    public boolean countsOn(LocalDate day, boolean completedThatDay) {
        if (completedThatDay) {
            return true;
        }
        if (day.isBefore(createdOn)) {
            return false;
        }
        return archivedOn == null || day.isBefore(archivedOn);
    }
}
