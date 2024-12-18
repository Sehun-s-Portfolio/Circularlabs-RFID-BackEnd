package com.rfid.circularlabs_rfid_backend.scan.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ResponseProductCode {
    private String productCode;
    private String productSerialCode;
}
