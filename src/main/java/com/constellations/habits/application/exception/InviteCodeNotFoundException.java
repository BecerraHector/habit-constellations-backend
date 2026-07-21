package com.constellations.habits.application.exception;

public class InviteCodeNotFoundException extends ApplicationException {

    public InviteCodeNotFoundException() {
        super("No existe ningun usuario con ese codigo de invitacion");
    }
}
