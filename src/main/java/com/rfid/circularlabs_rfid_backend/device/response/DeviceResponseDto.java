package com.rfid.circularlabs_rfid_backend.device.response;

import com.rfid.circularlabs_rfid_backend.member.response.GetClientsResponseDto;
import com.rfid.circularlabs_rfid_backend.product.response.GetProductInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class DeviceResponseDto {
    /**
     * Redis에 Dto, Vo와 같은 json 형식의 데이터 객체를 넣으려면 최소한 기본 생성자가 존재해야 한다.
     */

    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private List<GetClientsResponseDto> clients;
    private List<GetProductInfo> productsInfo;
}
