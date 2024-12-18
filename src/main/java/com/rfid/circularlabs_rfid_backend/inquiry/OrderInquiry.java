package com.rfid.circularlabs_rfid_backend.inquiry;


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
public class OrderInquiry extends TimeStamped {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long orderInquiryId; // 인덱스

    @Column(nullable = false)
    private String classificationCode; // 공급사 코드

    @Column(nullable = false)
    private String title; // 주문 문의 제목

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content; // 주문 문의 내용

}
