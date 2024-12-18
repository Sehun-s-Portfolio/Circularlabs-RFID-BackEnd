/**
package com.rfid.circularlabs_rfid_backend.device.repository;

import com.rfid.circularlabs_rfid_backend.device.domain.RedisHashConfirmDeviceData;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RedisHashConfirmDeviceDataRepository extends CrudRepository<RedisHashConfirmDeviceData, Long> {
    Optional<RedisHashConfirmDeviceData> findByDeviceCode(String deviceCode);
}
**/