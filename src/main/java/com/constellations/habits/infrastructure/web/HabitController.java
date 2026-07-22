package com.constellations.habits.infrastructure.web;

import com.constellations.habits.application.habit.CreateHabitCommand;
import com.constellations.habits.application.habit.HabitService;
import com.constellations.habits.infrastructure.security.AuthenticatedUserId;
import com.constellations.habits.infrastructure.web.dto.HabitDtos.CompleteHabitRequest;
import com.constellations.habits.infrastructure.web.dto.HabitDtos.HabitHistoryResponse;
import com.constellations.habits.infrastructure.web.dto.HabitDtos.HabitResponse;
import com.constellations.habits.infrastructure.web.dto.HabitDtos.SaveHabitRequest;
import com.constellations.habits.infrastructure.web.dto.HabitDtos.SkyResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/habits")
class HabitController {

    private final HabitService habits;

    HabitController(HabitService habits) {
        this.habits = habits;
    }

    @GetMapping
    List<HabitResponse> list(@AuthenticationPrincipal AuthenticatedUserId principal) {
        return habits.listActive(principal.value()).stream().map(HabitResponse::from).toList();
    }

    @PostMapping
    ResponseEntity<HabitResponse> create(
            @AuthenticationPrincipal AuthenticatedUserId principal,
            @Valid @RequestBody SaveHabitRequest request) {

        var view = habits.create(
                principal.value(), new CreateHabitCommand(request.name(), request.description()));
        return ResponseEntity.status(HttpStatus.CREATED).body(HabitResponse.from(view));
    }

    /**
     * El mapa del cielo: todos los habitos condensados en un nivel de brillo por dia.
     * El segmento literal gana al variable, asi que no choca con {@code /{habitId}}.
     */
    @GetMapping("/sky")
    SkyResponse sky(
            @AuthenticationPrincipal AuthenticatedUserId principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to) {

        return SkyResponse.from(habits.sky(principal.value(), from, to));
    }

    @GetMapping("/{habitId}")
    HabitResponse get(
            @AuthenticationPrincipal AuthenticatedUserId principal, @PathVariable UUID habitId) {
        return HabitResponse.from(habits.get(principal.value(), habitId));
    }

    @PutMapping("/{habitId}")
    HabitResponse update(
            @AuthenticationPrincipal AuthenticatedUserId principal,
            @PathVariable UUID habitId,
            @Valid @RequestBody SaveHabitRequest request) {

        return HabitResponse.from(habits.rename(
                principal.value(), habitId, new CreateHabitCommand(request.name(), request.description())));
    }

    /** Fechas cumplidas dentro de una ventana; sin parametros trae los ultimos 90 dias. */
    @GetMapping("/{habitId}/logs")
    HabitHistoryResponse history(
            @AuthenticationPrincipal AuthenticatedUserId principal,
            @PathVariable UUID habitId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to) {

        return HabitHistoryResponse.from(habits.history(principal.value(), habitId, from, to));
    }

    /** Archiva, no borra: los logs y las constelaciones ganadas se conservan. */
    @DeleteMapping("/{habitId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void archive(
            @AuthenticationPrincipal AuthenticatedUserId principal, @PathVariable UUID habitId) {
        habits.archive(principal.value(), habitId);
    }

    @PostMapping("/{habitId}/completions")
    HabitResponse complete(
            @AuthenticationPrincipal AuthenticatedUserId principal,
            @PathVariable UUID habitId,
            @RequestBody(required = false) CompleteHabitRequest request) {

        var date = request != null ? request.date() : null;
        return HabitResponse.from(habits.complete(principal.value(), habitId, date));
    }

    @DeleteMapping("/{habitId}/completions")
    HabitResponse uncomplete(
            @AuthenticationPrincipal AuthenticatedUserId principal,
            @PathVariable UUID habitId,
            @RequestBody(required = false) CompleteHabitRequest request) {

        var date = request != null ? request.date() : null;
        return HabitResponse.from(habits.uncomplete(principal.value(), habitId, date));
    }
}
