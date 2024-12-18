package com.rfid.circularlabs_rfid_backend.product.domain;


import com.rfid.circularlabs_rfid_backend.share.TimeStamped;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Entity
public class SupplyProduct extends TimeStamped {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long supplyProductId; // 인덱스

    @Column(nullable = false)
    private int rentalPrice; // 렌탈 가격

    @Column(nullable = false)
    private int maximumOrderMount; // 최대 주문 수량

    @Column(nullable = false)
    private String classificationCode; // 구분 코드

    @JoinColumn(name = "productId")
    @ManyToOne
    private Product product; // 공급사 상품으로 매핑된 상품 id
}
