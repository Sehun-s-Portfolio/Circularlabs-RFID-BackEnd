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
public class Product extends TimeStamped {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long productId; // 인덱스

    @Column(nullable = false)
    private String productCode; // 제품 고유 코드 (관리사 계정의 요청값을 입력받는 것이 아니라 백엔드 로직 상에서 제품 고유코드 값을 자동으로 부여)

    @Column(nullable = false)
    private String productName; // 제품 명

    @Column(nullable = false)
    private int purchasePrice; // 단가

    @Column(nullable = false)
    private String productImgName; // 제품 이미지 명

    @Column(nullable = false)
    private String productImgUrl; // 제품 이미지 호출 url

    @Column(nullable = false)
    private String status; // 제품 상태

    @Column(nullable = false)
    private int productQtt; // 제품 구매 단위
}
