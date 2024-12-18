package com.rfid.circularlabs_rfid_backend.scan.request;

import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class SendProductCode {
    private String rfidChipCode;
    private String filteringCode;
    private String productCode;
    private String productSerialCode;
}
