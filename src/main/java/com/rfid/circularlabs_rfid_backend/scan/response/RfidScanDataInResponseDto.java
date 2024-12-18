package com.rfid.circularlabs_rfid_backend.scan.response;

import com.rfid.circularlabs_rfid_backend.product.response.ProductDetailResponseDto;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class RfidScanDataInResponseDto {
    private String machineId;
    private String tag;
    private String selectClientCode;
    private String supplierCode;
    private String status;
    private int statusCount;
    private int flowRemainQuantity;
    private int noReturnQuantity;
    private int totalRemainQuantity;
    private int cycle; // 사이클
    private String latestReadingAt;
    private List<ProductDetailResponseDto> productDetails;
}
