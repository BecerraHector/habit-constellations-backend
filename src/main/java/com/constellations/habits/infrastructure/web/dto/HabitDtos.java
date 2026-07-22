package com.constellations.habits.infrastructure.web.dto;

import com.constellations.habits.application.habit.HabitHistory;
import com.constellations.habits.application.habit.HabitView;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class HabitDtos {

    private HabitDtos() {}

    public record SaveHabitRequest(
            @NotBlank @Size(max = 80) String name,
            @Size(max = 280) String description) {}

    /** @param date null significa "hoy" en la zona horaria del usuario */
    public record CompleteHabitRequest(LocalDate date) {}

    public record HabitResponse(
            UUID id,
            String name,
            String description,
            boolean archived,
            ProgressResponse progress) {

        public static HabitResponse from(HabitView view) {
            return new HabitResponse(
                    view.habit().id(),
                    view.habit().name(),
                    view.habit().description(),
                    view.habit().isArchived(),
                    ProgressResponse.from(view));
        }
    }

    /** La ventana efectiva viaja en la respuesta: el servicio pudo recortar la pedida. */
    public record HabitHistoryResponse(LocalDate from, LocalDate to, List<LocalDate> dates) {

        public static HabitHistoryResponse from(HabitHistory history) {
            return new HabitHistoryResponse(history.from(), history.to(), history.dates());
        }
    }

    /** El estado de la constelacion tal y como el frontend necesita pintarla. */
    public record ProgressResponse(
            int currentStreak,
            int longestStreak,
            int totalCompletions,
            LocalDate lastCompletedDate,
            boolean completedToday,
            int starsInCurrentCycle,
            int completedConstellations,
            int daysToNextConstellation) {

        static ProgressResponse from(HabitView view) {
            var progress = view.progress();
            return new ProgressResponse(
                    progress.currentStreak(),
                    progress.longestStreak(),
                    progress.totalCompletions(),
                    progress.lastCompletedDate(),
                    progress.completedToday(),
                    progress.starsInCurrentCycle(),
                    progress.completedConstellations(),
                    progress.daysToNextConstellation());
        }
    }
}
