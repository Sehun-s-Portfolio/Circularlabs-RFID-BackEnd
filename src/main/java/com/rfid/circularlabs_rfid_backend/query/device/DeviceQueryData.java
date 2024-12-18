package com.rfid.circularlabs_rfid_backend.query.device;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.rfid.circularlabs_rfid_backend.device.domain.QDevice.device;

@RequiredArgsConstructor
@Component
public class DeviceQueryData {

    private final JPAQueryFactory jpaQueryFactory;

    // 기기 코드를 통해 매핑된 공급사 코드 추출
    public Tuple getSeveralDeviceInfo(String dc){

        return jpaQueryFactory
                .select(device.deviceId, device.supplierCode)
                .from(device)
                .where(device.deviceCode.eq(dc))
                .limit(1)
                .fetchOne();
    }
}
