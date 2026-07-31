package com.jaedaero.domain.aianalysis.mapper;

import com.jaedaero.domain.aianalysis.vo.AiAnalysisVo;
import com.jaedaero.domain.aianalysis.vo.AiRecommendedScenarioVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiAnalysisMapper {

  int insertAnalysis(AiAnalysisVo analysis);

  AiAnalysisVo findAnalysisByIdAndUserId(
      @Param("analysisId") long analysisId, @Param("userId") long userId);

  AiAnalysisVo findLatestByUserIdAndInputDataHash(
      @Param("userId") long userId, @Param("inputDataHash") String inputDataHash);

  int insertRecommendedScenario(AiRecommendedScenarioVo scenario);

  AiRecommendedScenarioVo findRecommendedScenarioByIdAndUserId(
      @Param("scenarioId") long scenarioId, @Param("userId") long userId);
}
