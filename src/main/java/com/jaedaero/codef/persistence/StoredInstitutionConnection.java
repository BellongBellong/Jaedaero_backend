package com.jaedaero.codef.persistence;

/** One institution already registered in a user's CODEF Connected ID. */
public record StoredInstitutionConnection(
        long institutionConnectionId,
        long connectionId,
        String institutionCode,
        String businessType,
        String status,
        String loginIdEncrypted,
        String loginPasswordEncrypted,
        String birthDateEncrypted) {}
