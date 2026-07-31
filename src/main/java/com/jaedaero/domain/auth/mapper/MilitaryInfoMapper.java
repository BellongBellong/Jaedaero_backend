package com.jaedaero.domain.auth.mapper;

import com.jaedaero.domain.auth.vo.SoldierProfileVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MilitaryInfoMapper {

  /** 활성 사용자 존재 여부를 조회합니다. */
  int countActiveUserByUserId(@Param("userId") long userId);

  /** 장병내일준비적금 가입 여부를 조회합니다. */
  int countSoldierSavingByUserId(@Param("userId") long userId);

  /** 군 복무 프로필을 등록하거나 갱신합니다. */
  void upsertSoldierProfile(SoldierProfileVo soldierProfile);
}
