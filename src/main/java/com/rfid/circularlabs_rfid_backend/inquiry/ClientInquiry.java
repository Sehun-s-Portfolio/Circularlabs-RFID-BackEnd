package com.rfid.circularlabs_rfid_backend.inquiry;


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
public class ClientInquiry extends TimeStamped {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long clientInquiryId; // 인덱스

    @Column(nullable = false)
    private String motherCode; // 공급사 코드

    @Column(nullable = false)
    private String classificationCode; // 고객사 코드

    @Column(nullable = false)
    private String title; // 문의 제목

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content; // 문의 내용

    @Column(columnDefinition = "LONGTEXT")
    private String answerContent; // 답변 내용

    @Column
    private String answerStatus; // 답변 상태 유무

    @Column
    private LocalDateTime answerAt; // 답변 일자
}
