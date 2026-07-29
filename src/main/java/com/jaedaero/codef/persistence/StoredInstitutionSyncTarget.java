package com.jaedaero.codef.persistence;

/** Institution and CODEF business type needed for a subsequent account refresh. */
public record StoredInstitutionSyncTarget(String institutionCode, String businessType) {}
