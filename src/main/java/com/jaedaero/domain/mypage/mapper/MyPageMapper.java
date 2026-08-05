package com.jaedaero.domain.mypage.mapper;

import com.jaedaero.domain.mypage.vo.InvestmentBadgeVo;
import com.jaedaero.domain.mypage.vo.InvestmentBadgeStatusVo;
import com.jaedaero.domain.mypage.vo.MyPageProfileVo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MyPageMapper {

  /** 활성 사용자의 마이페이지 프로필 정보를 조회합니다. */
  MyPageProfileVo findActiveProfile(@Param("userId") long userId);

  /** 사용자가 획득한 투자 뱃지 수를 조회합니다. */
  int countEarnedBadges(@Param("userId") long userId);

  /** 최근 획득한 투자 뱃지 코드를 조회합니다. */
  List<String> findRecentBadgeCodes(@Param("userId") long userId);

  /** 사용자의 투자 뱃지 획득 이력을 조회합니다. */
  List<InvestmentBadgeVo> findInvestmentBadges(@Param("userId") long userId);

  /** 사용자의 투자 성향과 미션 기반 뱃지 현황을 조회합니다. */
  InvestmentBadgeStatusVo findInvestmentBadgeStatus(@Param("userId") long userId);

  /** 사용자 계정을 소프트 삭제합니다. */
  int withdraw(@Param("userId") long userId);
}
