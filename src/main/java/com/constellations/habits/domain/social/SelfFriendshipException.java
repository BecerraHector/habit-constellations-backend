package com.constellations.habits.domain.social;

import com.constellations.habits.domain.DomainException;

public class SelfFriendshipException extends DomainException {

    public SelfFriendshipException() {
        super("No puedes enviarte una solicitud a ti mismo");
    }
}
