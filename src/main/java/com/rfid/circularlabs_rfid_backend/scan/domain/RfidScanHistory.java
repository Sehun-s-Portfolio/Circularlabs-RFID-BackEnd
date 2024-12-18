package com.rfid.circularlabs_rfid_backend.scan.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rfid.circularlabs_rfid_backend.product.domain.ProductDetail;
import com.rfid.circularlabs_rfid_backend.product.domain.ProductDetailHistory;
import com.rfid.circularlabs_rfid_backend.share.TimeStamped;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Entity
public class RfidScanHistory extends TimeStamped {

    //////////////////////////////////////////////////////////////////////////////////////////
    // [RFID 기기 스캔 이력 정보]
    // RFID 기기로 스캔한 데이터들에 대한 상태 이력을 하나의 row로 스캔한 전체 데이터 이력으로서 관리하는 용도
    //////////////////////////////////////////////////////////////////////////////////////////

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long rfidScanhistoryId; // 인덱스

    @Column(nullable = false)
    private String deviceCode; // 기기 코드

    @Column(nullable = false)
    private String rfidChipCode; // RFID 기기 코드

    @Column(nullable = false)
    private String productCode; // 제품 분류 코드

    @Column(nullable = false)
    private String supplierCode; // 공급사 코드

    @Column(nullable = false)
    private String clientCode; // 고객사 코드

    @Column(nullable = false)
    private String status; // 상태

    @Column
    private int statusCount; // 작업 수량

    @Column
    private int flowRemainQuantity; // 유동 재고 수량

    @Column
    private int noReturnQuantity; // 미회수 수량

    @Column
    private int totalRemainQuantity; // 총 재고 수량

    /**
    @Column(nullable = false)
    private int cycle; // 사이클
     **/

    @Column(nullable = false)
    private LocalDateTime latestReadingAt; // 마지막 리딩 시간

    @JsonIgnore
    @OneToMany(mappedBy = "rfidScanHistory", fetch = FetchType.LAZY)
    private List<ProductDetailHistory> productDetailHistories;
}
