package com.constellations.habits.infrastructure.web;

import com.constellations.habits.application.exception.AlreadyMemberException;
import com.constellations.habits.application.exception.EmailAlreadyUsedException;
import com.constellations.habits.application.exception.FriendshipAlreadyExistsException;
import com.constellations.habits.application.exception.FriendshipNotFoundException;
import com.constellations.habits.application.exception.GalaxyNotFoundException;
import com.constellations.habits.application.exception.HabitNotFoundException;
import com.constellations.habits.application.exception.InvalidCredentialsException;
import com.constellations.habits.application.exception.InvalidRefreshTokenException;
import com.constellations.habits.application.exception.InviteCodeNotFoundException;
import com.constellations.habits.application.exception.UserNotFoundException;
import com.constellations.habits.domain.ValidationException;
import com.constellations.habits.domain.galaxy.NotAMemberException;
import com.constellations.habits.domain.habit.ArchivedHabitException;
import com.constellations.habits.domain.habit.InvalidLogDateException;
import com.constellations.habits.domain.social.FriendshipStateException;
import com.constellations.habits.domain.social.SelfFriendshipException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Traduce las excepciones de dominio y aplicacion a respuestas HTTP.
 *
 * <p>Es el unico sitio donde las capas internas se convierten en codigos de estado: ni el
 * dominio ni los casos de uso conocen HTTP.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail onBeanValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, detail.isBlank() ? "Peticion invalida" : detail);
    }

    @ExceptionHandler(ValidationException.class)
    ProblemDetail onDomainValidation(ValidationException e) {
        return problem(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(InvalidLogDateException.class)
    ProblemDetail onInvalidLogDate(InvalidLogDateException e) {
        // 422: la peticion esta bien formada, pero la regla de negocio la rechaza.
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    @ExceptionHandler({
        EmailAlreadyUsedException.class,
        ArchivedHabitException.class,
        FriendshipAlreadyExistsException.class,
        FriendshipStateException.class,
        AlreadyMemberException.class
    })
    ProblemDetail onConflict(RuntimeException e) {
        return problem(HttpStatus.CONFLICT, e.getMessage());
    }

    /** Invitarse a uno mismo es una peticion mal planteada, no un conflicto de estado. */
    @ExceptionHandler(SelfFriendshipException.class)
    ProblemDetail onSelfFriendship(SelfFriendshipException e) {
        return problem(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler({InvalidCredentialsException.class, InvalidRefreshTokenException.class})
    ProblemDetail onInvalidCredentials(RuntimeException e) {
        return problem(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler({
        HabitNotFoundException.class,
        UserNotFoundException.class,
        FriendshipNotFoundException.class,
        InviteCodeNotFoundException.class,
        GalaxyNotFoundException.class,
        NotAMemberException.class
    })
    ProblemDetail onNotFound(RuntimeException e) {
        return problem(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /**
     * Ultima linea de defensa de las restricciones del esquema.
     *
     * <p>Los casos de uso comprueban antes de escribir, pero entre la comprobacion y la
     * escritura cabe otra peticion. Cuando eso pasa, quien rechaza es el indice unico, y
     * el resultado correcto es un conflicto y no un 500: la peticion era legitima, solo
     * llego segunda. No se propaga el mensaje de la base de datos, que revelaria nombres
     * de tablas e indices.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail onConstraintViolation(DataIntegrityViolationException e) {
        return problem(HttpStatus.CONFLICT, "La operacion choca con un registro existente");
    }

    private static ProblemDetail problem(HttpStatus status, String detail) {
        return ProblemDetail.forStatusAndDetail(status, detail);
    }
}
