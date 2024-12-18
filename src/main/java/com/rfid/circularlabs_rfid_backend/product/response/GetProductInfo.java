package com.rfid.circularlabs_rfid_backend.product.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class GetProductInfo {
    private String productCode;
    private String productName;
}
