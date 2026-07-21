package com.constellations.habits.infrastructure.web;

import com.constellations.habits.application.exception.UserNotFoundException;
import com.constellations.habits.application.port.out.UserRepository;
import com.constellations.habits.application.social.FriendshipService;
import com.constellations.habits.infrastructure.security.AuthenticatedUserId;
import com.constellations.habits.infrastructure.web.dto.SocialDtos.FriendRequestResponse;
import com.constellations.habits.infrastructure.web.dto.SocialDtos.FriendResponse;
import com.constellations.habits.infrastructure.web.dto.SocialDtos.InviteCodeResponse;
import com.constellations.habits.infrastructure.web.dto.SocialDtos.SendRequestBody;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
class SocialController {

    private final FriendshipService friendships;
    private final UserRepository users;

    SocialController(FriendshipService friendships, UserRepository users) {
        this.friendships = friendships;
        this.users = users;
    }

    @GetMapping("/me/invite-code")
    InviteCodeResponse myInviteCode(@AuthenticationPrincipal AuthenticatedUserId principal) {
        return users.findById(principal.value())
                .map(InviteCodeResponse::from)
                .orElseThrow(() -> new UserNotFoundException(principal.value()));
    }

    /** Invalida el codigo anterior; quien lo tuviera ya no podra usarlo. */
    @PostMapping("/me/invite-code")
    InviteCodeResponse regenerateInviteCode(
            @AuthenticationPrincipal AuthenticatedUserId principal) {
        return InviteCodeResponse.from(friendships.regenerateInviteCode(principal.value()));
    }

    @PostMapping("/friend-requests")
    ResponseEntity<FriendRequestResponse> sendRequest(
            @AuthenticationPrincipal AuthenticatedUserId principal,
            @Valid @RequestBody SendRequestBody body) {

        var view = friendships.sendRequest(principal.value(), body.inviteCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(FriendRequestResponse.from(view));
    }

    /** Solicitudes recibidas y pendientes de respuesta. */
    @GetMapping("/friend-requests")
    List<FriendRequestResponse> incomingRequests(
            @AuthenticationPrincipal AuthenticatedUserId principal) {
        return friendships.listIncomingRequests(principal.value()).stream()
                .map(FriendRequestResponse::from)
                .toList();
    }

    @GetMapping("/friend-requests/sent")
    List<FriendRequestResponse> outgoingRequests(
            @AuthenticationPrincipal AuthenticatedUserId principal) {
        return friendships.listOutgoingRequests(principal.value()).stream()
                .map(FriendRequestResponse::from)
                .toList();
    }

    @PostMapping("/friend-requests/{requestId}/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void accept(
            @AuthenticationPrincipal AuthenticatedUserId principal, @PathVariable UUID requestId) {
        friendships.acceptRequest(principal.value(), requestId);
    }

    @PostMapping("/friend-requests/{requestId}/decline")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void decline(
            @AuthenticationPrincipal AuthenticatedUserId principal, @PathVariable UUID requestId) {
        friendships.declineRequest(principal.value(), requestId);
    }

    @GetMapping("/friends")
    List<FriendResponse> friends(@AuthenticationPrincipal AuthenticatedUserId principal) {
        return friendships.listFriends(principal.value()).stream()
                .map(FriendResponse::from)
                .toList();
    }

    @DeleteMapping("/friends/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeFriend(
            @AuthenticationPrincipal AuthenticatedUserId principal, @PathVariable UUID userId) {
        friendships.removeFriend(principal.value(), userId);
    }
}
