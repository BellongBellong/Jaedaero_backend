package com.jaedaero.domain.codef.persistence;

/** 사용자의 CODEF Connected ID에 이미 등록된 기관 한 곳입니다. */
public record StoredInstitutionConnection(
    long institutionConnectionId,
    long connectionId,
    String institutionCode,
    String businessType,
    String status,
    String loginIdEncrypted,
    String loginPasswordEncrypted,
    String birthDateEncrypted) {}
