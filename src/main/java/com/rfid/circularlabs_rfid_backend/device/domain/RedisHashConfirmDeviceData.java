/**
package com.rfid.circularlabs_rfid_backend.device.domain;

import com.rfid.circularlabs_rfid_backend.member.response.GetClientsResponseDto;
import com.rfid.circularlabs_rfid_backend.product.response.GetProductInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash(value="redishash-confirmdevicedata", timeToLive = 30L)
public class RedisHashConfirmDeviceData {
    @Id
    private Long confirmDeviceDataId;
    @Indexed // RedisHash를 사용할 때 특정 필드에 Indexed 어노테이션을 적용하면 Redis에 데이터가 생성될 때 Indexed로 지정한 필드가 같이 키값으로 등록된다.
    private String deviceCode;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private List<GetClientsResponseDto> clients;
    private List<GetProductInfo> productsInfo;
}
 **/
