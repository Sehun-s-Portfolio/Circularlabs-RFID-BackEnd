package com.rfid.circularlabs_rfid_backend.scan.request;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@Getter
public class SendOrderCount {
    private String product;
    private int orderCount;
}
