package com.jaedaero.domain.codef.persistence;

public record StoredCodefConnection(long connectionId, long userId, String connectedIdEncrypted) {}
