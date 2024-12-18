package com.rfid.circularlabs_rfid_backend.product.domain;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rfid.circularlabs_rfid_backend.scan.domain.RfidScanHistory;
import com.rfid.circularlabs_rfid_backend.share.TimeStamped;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Entity
public class ProductDetail extends TimeStamped {

    ///////////////////////////////////////////////////////////////////////////////////////////
    // [제품 상세 이력]
    // 제품 상세 이력은 RFID 기기로 스캔한 데이터들의 이력을 초기에 한 번만 저장하여 놓고 작업이 변경될 때마다
    // 추가하여 적립 저장하는 것이 아닌 이미 한 번 저장된 데이터들에 한해 상태값을 update 하기 위한 용도
    ///////////////////////////////////////////////////////////////////////////////////////////

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long productDetailId; // 인덱스

    @Column(nullable = false)
    private String rfidChipCode; // RFID 칩 코드

    @Column(nullable = false)
    private String productSerialCode; // 각 제품 고유 코드

    @Column(nullable = false)
    private String productCode; // 제품 분류 코드

    @Column(nullable = false)
    private String supplierCode; // 공급사 코드

    @Column(nullable = false)
    private String clientCode; // 고객사 코드

    @Column(nullable = false)
    private String status; // 상태

    @Column(nullable = false)
    private int cycle; // 사이클

    @Column(nullable = false)
    private LocalDateTime latestReadingAt; // 마지막 리딩 시간
}
