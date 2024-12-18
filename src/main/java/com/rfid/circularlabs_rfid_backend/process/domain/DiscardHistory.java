package com.rfid.circularlabs_rfid_backend.process.domain;


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
public class DiscardHistory extends TimeStamped {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long discardHistoryId; // 인덱스

    @Column(nullable = false)
    private String supplierCode; // 공급사 코드

    @Column(nullable = false)
    private String clientCode; // 고객사 코드

    @Column(nullable = false)
    private String productCode; // 제품 분류 코드

    @Column(nullable = false)
    private String productSerialCode; // 각 제품 고유 제품 코드

    @Column(nullable = false)
    private String rfidChipCode; // RFID 칩 코드

    @Column
    private LocalDateTime discardAt; // 폐기 일시

    @Column
    private String reason; // 폐기 사유
}
