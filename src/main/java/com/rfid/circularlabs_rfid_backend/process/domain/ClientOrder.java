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
public class ClientOrder extends TimeStamped {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long clientOrderId; // 인덱스

    @Column(nullable = false)
    private String motherCode; // 상위 코드

    @Column(nullable = false)
    private String classificationCode; // 고객사 본인 코드

    @Column(nullable = false)
    private String productCode; // 제품 분류 코드

    @Column(nullable = false)
    private int orderMount; // 주문 요청량

    @Column
    private String status; // 주문 상태

    @Column
    private LocalDateTime deliveryAt; // 수령 일자

    @Column
    private LocalDateTime hopeDeliveryAt; // 희망 수령 일자

    @Column
    private String modifiedYN; // 수정 유무

    @Column
    private String statementnumber; // 전표번호

}
