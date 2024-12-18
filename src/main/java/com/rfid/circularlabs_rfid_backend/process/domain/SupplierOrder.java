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
public class SupplierOrder extends TimeStamped {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long orderId; // 인덱스

    @Column(nullable = false)
    private String classificationCode; // 공급사 구분 코드

    @Column(nullable = false)
    private String productCode; // 제품 분류 코드

    @Column(nullable = false)
    private int orderMount; // 주문 요청량

    @Column
    private LocalDateTime deliveryAt; // 수령 일자

    @Column
    private String statementnumber; // 전표번호
}
