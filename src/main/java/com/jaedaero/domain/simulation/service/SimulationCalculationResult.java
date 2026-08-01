package com.jaedaero.domain.simulation.service;

import java.time.LocalDate;

public record SimulationCalculationResult(long expectedAsset, LocalDate financialDischargeDate) {}
