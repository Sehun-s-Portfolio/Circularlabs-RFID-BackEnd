package com.rfid.circularlabs_rfid_backend.scan.response;

import com.rfid.circularlabs_rfid_backend.product.response.ProductDetailResponseDto;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class RfidScanDataDiscardResponseDto {
    private String machineId;
    private String tag;
    private String selectClientCode;
    private String supplierCode;
    private String status;
    private String discardAt;
    private List<ProductDetailResponseDto> productDetails;
}
