package com.jaedaero.domain.simulation.service;

import java.time.LocalDate;

public record SimulationCalculationResult(
    long expectedAsset,
    LocalDate financialDischargeDate,
    int calculationMonths,
    long baseAsset,
    long expectedSalary,
    long expectedSpending,
    long soldierSavingPrincipal,
    long soldierSavingInterest,
    long governmentMatchingSupport,
    long investmentPrincipal,
    long expectedInvestmentReturn,
    long unallocatedPrincipal,
    long potentialExpectedAsset,
    String calculationPolicyVersion) {}
