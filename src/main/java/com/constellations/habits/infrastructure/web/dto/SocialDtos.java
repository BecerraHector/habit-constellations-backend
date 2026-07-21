package com.constellations.habits.infrastructure.web.dto;

import com.constellations.habits.application.social.FriendRequestView;
import com.constellations.habits.application.social.FriendSummary;
import com.constellations.habits.domain.user.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class SocialDtos {

    private SocialDtos() {}

    /** El guion es opcional: el dominio normaliza "abcd-2345" y "ABCD2345" por igual. */
    public record SendRequestBody(@NotBlank @Size(max = 16) String inviteCode) {}

    public record InviteCodeResponse(String inviteCode) {

        public static InviteCodeResponse from(User user) {
            return new InviteCodeResponse(user.inviteCode().formatted());
        }
    }

    public record FriendRequestResponse(
            UUID requestId,
            UUID userId,
            String displayName,
            String direction,
            Instant createdAt) {

        public static FriendRequestResponse from(FriendRequestView view) {
            return new FriendRequestResponse(
                    view.requestId(),
                    view.otherUserId(),
                    view.otherUserDisplayName(),
                    view.direction().name(),
                    view.createdAt());
        }
    }

    /**
     * Resumen de un amigo. Nota deliberada: no expone los nombres de sus habitos ni su
     * email, solo cifras agregadas.
     */
    public record FriendResponse(
            UUID userId,
            String displayName,
            Instant friendsSince,
            int activeHabits,
            int bestCurrentStreak,
            int longestStreakEver,
            int totalStars,
            int totalConstellations,
            int completedToday) {

        public static FriendResponse from(FriendSummary summary) {
            return new FriendResponse(
                    summary.userId(),
                    summary.displayName(),
                    summary.friendsSince(),
                    summary.activeHabits(),
                    summary.bestCurrentStreak(),
                    summary.longestStreakEver(),
                    summary.totalStars(),
                    summary.totalConstellations(),
                    summary.completedToday());
        }
    }
}
