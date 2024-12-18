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
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Entity
public class ProductDetailHistory extends TimeStamped {
    ///////////////////////////////////////////////////////////////////
    // [제품 심화 상세 이력]
    // 제품 심화 상세 이력은 RFID 기기로 스캔한 데이터들을
    // ProductDetail(제품 상세 이력)에 존재하는 각 제품의 상세 이력을 스캔하여
    // 작업 처리를 수행할 때마다 매 번 insert하여 이력을 저장하기 위한 용도
    ///////////////////////////////////////////////////////////////////

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long productDetailHistoryId; // 인덱스

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

    @JsonIgnore
    @JoinColumn(name = "rfidScanHistoryId")
    @ManyToOne(fetch = FetchType.LAZY)
    private RfidScanHistory rfidScanHistory;
}
