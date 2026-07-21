package com.constellations.habits.infrastructure.web;

import com.constellations.habits.application.galaxy.CreateGalaxyCommand;
import com.constellations.habits.application.galaxy.GalaxyService;
import com.constellations.habits.infrastructure.security.AuthenticatedUserId;
import com.constellations.habits.infrastructure.web.dto.GalaxyDtos.CreateGalaxyBody;
import com.constellations.habits.infrastructure.web.dto.GalaxyDtos.GalaxyDayDetailResponse;
import com.constellations.habits.infrastructure.web.dto.GalaxyDtos.GalaxyDetailResponse;
import com.constellations.habits.infrastructure.web.dto.GalaxyDtos.GalaxyResponse;
import com.constellations.habits.infrastructure.web.dto.GalaxyDtos.JoinGalaxyBody;
import com.constellations.habits.infrastructure.web.dto.GalaxyDtos.ThemeResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Constelaciones compartidas.
 *
 * <p>Son abiertas por decision de producto: cualquiera puede descubrirlas y unirse. Lo
 * que se expone dentro esta acotado a proposito — nombre visible y cumplimiento del
 * habito compartido, nada mas. Ni el email, ni el codigo de invitacion, ni el resto de
 * habitos de nadie.
 */
@RestController
@RequestMapping("/api/v1/galaxies")
class GalaxyController {

    private final GalaxyService galaxies;

    GalaxyController(GalaxyService galaxies) {
        this.galaxies = galaxies;
    }

    @PostMapping
    ResponseEntity<GalaxyResponse> create(
            @AuthenticationPrincipal AuthenticatedUserId principal,
            @Valid @RequestBody CreateGalaxyBody body) {

        var view = galaxies.create(principal.value(), new CreateGalaxyCommand(
                body.name(), body.description(), body.theme(), body.habitId()));

        return ResponseEntity.status(HttpStatus.CREATED).body(GalaxyResponse.from(view));
    }

    /** Las galaxias a las que pertenece quien pregunta. */
    @GetMapping
    List<GalaxyResponse> mine(@AuthenticationPrincipal AuthenticatedUserId principal) {
        return galaxies.listMine(principal.value()).stream().map(GalaxyResponse::from).toList();
    }

    /** Temas ordenados por cuanta gente los sostiene, con sugerencias al final. */
    @GetMapping("/catalog")
    List<ThemeResponse> catalog(@RequestParam(required = false) Integer limit) {
        return galaxies.catalog(limit).stream().map(ThemeResponse::from).toList();
    }

    @GetMapping("/discover")
    List<GalaxyResponse> discover(
            @AuthenticationPrincipal AuthenticatedUserId principal,
            @RequestParam(required = false) String theme,
            @RequestParam(required = false) Integer limit) {

        return galaxies.discover(principal.value(), theme, limit).stream()
                .map(GalaxyResponse::from)
                .toList();
    }

    /** La galaxia con su mapa de brillo. {@code days} recorta la ventana; por defecto 30. */
    @GetMapping("/{galaxyId}")
    GalaxyDetailResponse detail(
            @AuthenticationPrincipal AuthenticatedUserId principal,
            @PathVariable UUID galaxyId,
            @RequestParam(required = false) Integer days) {

        return GalaxyDetailResponse.from(galaxies.get(principal.value(), galaxyId, days));
    }

    @GetMapping("/{galaxyId}/days/{date}")
    GalaxyDayDetailResponse day(
            @PathVariable UUID galaxyId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return GalaxyDayDetailResponse.from(galaxies.dayDetail(galaxyId, date));
    }

    @PostMapping("/{galaxyId}/members")
    ResponseEntity<GalaxyResponse> join(
            @AuthenticationPrincipal AuthenticatedUserId principal,
            @PathVariable UUID galaxyId,
            @RequestBody(required = false) JoinGalaxyBody body) {

        UUID habitId = body != null ? body.habitId() : null;
        var view = galaxies.join(principal.value(), galaxyId, habitId);

        return ResponseEntity.status(HttpStatus.CREATED).body(GalaxyResponse.from(view));
    }

    /** El habito no se toca al salir: es del usuario, no del grupo. */
    @DeleteMapping("/{galaxyId}/members/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void leave(
            @AuthenticationPrincipal AuthenticatedUserId principal, @PathVariable UUID galaxyId) {
        galaxies.leave(principal.value(), galaxyId);
    }
}
