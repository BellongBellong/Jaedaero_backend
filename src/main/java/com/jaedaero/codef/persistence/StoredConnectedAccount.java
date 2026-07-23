package com.jaedaero.codef.persistence;

public record StoredConnectedAccount(
        long accountId,
        long userId,
        long connectionId,
        String institutionCode,
        String institutionName,
        String accountNumberEncrypted,
        String accountMasked,
        String accountType,
        String productName,
        long currentBalance,
        Long availableBalance,
        String maturityDate) {}
