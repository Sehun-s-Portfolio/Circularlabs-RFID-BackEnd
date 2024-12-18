package com.rfid.circularlabs_rfid_backend.scan.response;

import lombok.Builder;
import lombok.Getter;


@Builder
@Getter
public class ProductDetailscanResponseDto {
    private Long productDetailId; // 인덱스
    private String rfidChipCode; // RFID 칩 코드
    private String productSerialCode; // 각 제품 고유 코드
    private String productCode; // 제품 분류 코드
    private String supplierCode; // 공급사 코드
    private String clientCode; // 고객사 코드
    private String status; // 상태
    private int cycle;
    private String latestReadingAt; // 마지막 리딩 시간
    private int dataState;
}
