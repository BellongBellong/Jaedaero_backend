package com.jaedaero.codef.persistence;

public record StoredCodefConnection(long connectionId, long userId, String connectedIdEncrypted) {}
