package com.jaedaero.domain.mypage.mapper;

import com.jaedaero.domain.mypage.vo.MyPageProfileVo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MyPageMapper {

  MyPageProfileVo findActiveProfile(@Param("userId") long userId);

  int countEarnedBadges(@Param("userId") long userId);

  List<String> findRecentBadgeCodes(@Param("userId") long userId);

  int withdraw(@Param("userId") long userId);
}
