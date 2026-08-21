package com.jaedaero.domain.notification.mapper;

import com.jaedaero.domain.notification.vo.DeviceTokenVo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DeviceTokenMapper {
  int upsert(DeviceTokenVo token);

  DeviceTokenVo findByFcmToken(@Param("fcmToken") String fcmToken);

  List<DeviceTokenVo> findActiveByUserId(@Param("userId") long userId);

  int deactivateByIdAndUserId(
      @Param("deviceTokenId") long deviceTokenId, @Param("userId") long userId);

  int deactivateByFcmToken(@Param("fcmToken") String fcmToken);
}
