package com.constellations.habits.application.exception;

public class AlreadyMemberException extends ApplicationException {

    public AlreadyMemberException() {
        super("Ya perteneces a esta galaxia");
    }
}
