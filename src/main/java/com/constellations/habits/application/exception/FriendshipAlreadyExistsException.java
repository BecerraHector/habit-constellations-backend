package com.constellations.habits.application.exception;

/** Ya hay una relacion entre esos dos usuarios, en cualquier estado. */
public class FriendshipAlreadyExistsException extends ApplicationException {

    public FriendshipAlreadyExistsException(String detail) {
        super(detail);
    }
}
