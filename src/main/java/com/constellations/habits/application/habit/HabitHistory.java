package com.constellations.habits.application.habit;

import java.time.LocalDate;
import java.util.List;

/**
 * Las fechas cumplidas de un habito dentro de una ventana ya normalizada.
 *
 * <p>Devuelve la ventana efectiva ademas de las fechas: el cliente pide un tramo y el
 * servicio puede recortarlo (al hoy del usuario, o al tope de tamano), asi que necesita
 * saber que tramo le respondieron para pintar el calendario sin huecos falsos.
 *
 * @param from  primer dia incluido de la ventana efectiva
 * @param to    ultimo dia incluido
 * @param dates fechas cumplidas dentro de la ventana, en orden ascendente
 */
public record HabitHistory(LocalDate from, LocalDate to, List<LocalDate> dates) {}
