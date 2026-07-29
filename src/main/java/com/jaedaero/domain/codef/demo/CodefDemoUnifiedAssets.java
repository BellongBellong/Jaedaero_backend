package com.jaedaero.domain.codef.demo;

import java.util.List;

/** Aggregate result across every account currently registered for the user. */
public class CodefDemoUnifiedAssets {
  private final String totalAmount;
  private final int refreshedAccountCount;
  private final List<CodefDemoInstitutionAssetGroup> institutionGroups;

  public CodefDemoUnifiedAssets(
      String totalAmount,
      int refreshedAccountCount,
      List<CodefDemoInstitutionAssetGroup> institutionGroups) {
    this.totalAmount = totalAmount;
    this.refreshedAccountCount = refreshedAccountCount;
    this.institutionGroups = List.copyOf(institutionGroups);
  }

  public String getTotalAmount() {
    return totalAmount;
  }

  public int getRefreshedAccountCount() {
    return refreshedAccountCount;
  }

  public List<CodefDemoInstitutionAssetGroup> getInstitutionGroups() {
    return institutionGroups;
  }
}
