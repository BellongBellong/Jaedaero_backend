package com.jaedaero.domain.codef.persistence;

/** 이후 계좌 갱신에 필요한 기관과 CODEF 업무 구분입니다. */
public record StoredInstitutionSyncTarget(String institutionCode, String businessType) {}
