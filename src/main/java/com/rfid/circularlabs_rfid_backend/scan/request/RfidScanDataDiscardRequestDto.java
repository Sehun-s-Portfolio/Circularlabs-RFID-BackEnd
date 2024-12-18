package com.rfid.circularlabs_rfid_backend.scan.request;

import lombok.Getter;

import java.util.List;

@Getter
public class RfidScanDataDiscardRequestDto {
    private String machineId;
    private String tag;
    private String selectClientCode;
    private String supplierCode;
    private List<SendProductCode> productCodes;
}
