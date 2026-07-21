package com.constellations.habits.infrastructure.web.dto;

import com.constellations.habits.application.galaxy.GalaxyDayDetail;
import com.constellations.habits.application.galaxy.GalaxyDetail;
import com.constellations.habits.application.galaxy.GalaxyMemberView;
import com.constellations.habits.application.galaxy.GalaxyView;
import com.constellations.habits.application.galaxy.ThemeCount;
import com.constellations.habits.domain.galaxy.GalaxyDay;
import com.constellations.habits.domain.galaxy.GalaxyMap;
import com.constellations.habits.domain.galaxy.Luminosity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class GalaxyDtos {

    private GalaxyDtos() {}

    /**
     * @param habitId habito propio a enlazar. Si se omite se crea uno nuevo con el
     *                nombre de la galaxia, para que el usuario no acabe con dos habitos
     *                iguales ni con dos rachas del mismo esfuerzo.
     */
    public record CreateGalaxyBody(
            @NotBlank @Size(max = 80) String name,
            @Size(max = 280) String description,
            @NotBlank @Size(max = 32) String theme,
            UUID habitId) {}

    public record JoinGalaxyBody(UUID habitId) {}

    public record GalaxyResponse(
            UUID id,
            String name,
            String description,
            String theme,
            UUID creatorId,
            Instant createdAt,
            int activeMembers,
            boolean member,
            LocalDate joinedOn,
            UUID habitId) {

        public static GalaxyResponse from(GalaxyView view) {
            return new GalaxyResponse(
                    view.id(),
                    view.name(),
                    view.description(),
                    view.theme(),
                    view.creatorId(),
                    view.createdAt(),
                    view.activeMembers(),
                    view.member(),
                    view.joinedOn(),
                    view.habitId());
        }
    }

    /**
     * Una estrella del mapa de calor. Se devuelven a la vez el nivel y las cifras
     * crudas: el nivel para pintar sin recalcular nada, y {@code completions} sobre
     * {@code activeMembers} para poder explicar por que ese dia luce asi.
     */
    public record GalaxyDayResponse(
            LocalDate date, int activeMembers, int completions, int level) {

        public static GalaxyDayResponse from(GalaxyDay day) {
            return new GalaxyDayResponse(
                    day.date(), day.activeMembers(), day.completions(), day.level());
        }
    }

    public record GalaxyMapResponse(
            LocalDate from,
            LocalDate to,
            int maxLevel,
            int perfectDays,
            int totalStars,
            double averageRatio,
            List<GalaxyDayResponse> days) {

        public static GalaxyMapResponse from(GalaxyMap map) {
            return new GalaxyMapResponse(
                    map.from(),
                    map.to(),
                    Luminosity.MAX_LEVEL,
                    map.perfectDays(),
                    map.totalStars(),
                    map.averageRatio(),
                    map.days().stream().map(GalaxyDayResponse::from).toList());
        }
    }

    /** Sin rachas ni email: solo quien esta y desde cuando. */
    public record GalaxyMemberResponse(UUID userId, String displayName, LocalDate joinedOn) {

        public static GalaxyMemberResponse from(GalaxyMemberView view) {
            return new GalaxyMemberResponse(view.userId(), view.displayName(), view.joinedOn());
        }
    }

    public record GalaxyDetailResponse(
            GalaxyResponse galaxy, GalaxyMapResponse map, List<GalaxyMemberResponse> members) {

        public static GalaxyDetailResponse from(GalaxyDetail detail) {
            return new GalaxyDetailResponse(
                    GalaxyResponse.from(detail.galaxy()),
                    GalaxyMapResponse.from(detail.map()),
                    detail.members().stream().map(GalaxyMemberResponse::from).toList());
        }
    }

    public record GalaxyDayDetailResponse(
            LocalDate date,
            int activeMembers,
            int completions,
            int level,
            List<String> completedBy) {

        public static GalaxyDayDetailResponse from(GalaxyDayDetail detail) {
            return new GalaxyDayDetailResponse(
                    detail.date(),
                    detail.activeMembers(),
                    detail.completions(),
                    detail.level(),
                    detail.completedBy());
        }
    }

    public record ThemeResponse(String theme, int galaxies, int members) {

        public static ThemeResponse from(ThemeCount count) {
            return new ThemeResponse(count.theme(), count.galaxies(), count.members());
        }
    }
}
