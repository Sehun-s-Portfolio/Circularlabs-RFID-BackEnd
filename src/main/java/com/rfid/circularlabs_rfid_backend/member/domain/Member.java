package com.rfid.circularlabs_rfid_backend.member.domain;


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
public class Member extends TimeStamped {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long memberId; // 인덱스

    @Column(nullable = false)
    private int grade; // 등급 (0-관리사 / 1-공급사 / 2-고객사)

    @Column(nullable = false)
    private String motherCode; // 상위 코드 - (1) 공급사 가입의 경우 관리사 코드가 들어감 / (2) 고객사 가입의 경우 공급사 코드가 들어감

    @Column(nullable = false)
    private String classificationCode; // 본인 코드 - (1) 공급사 가입의 경우 공급사 본인의 코드가 들어감 / (2) 고객사 가입의 경우 고객사 본인의 코드가 들어감

    @Column(nullable = false)
    private String loginId; // 아이디

    @Column(nullable = false)
    private String password; // 패스워드

    @Column
    private String clientCompany; // 상호명 - (1) 공급사의 경우 공급사 본인의 상호명 / (2) 고객사의 경우 고객사 본인의 상호명

    @Column
    private String address; // 주소

    @Column(nullable = false)
    private String represent; // 대표자

    @Column(nullable = false)
    private String businessNumber; // 사업자 번호

    @Column
    private String stateOfBusiness; // 업태

    @Column
    private String categoryBusiness; // 업종

    @Column(nullable = false)
    private String bankName; // 은행

    @Column(nullable = false)
    private String bankAccountNumber; // 계좌 번호

    @Column(nullable = false)
    private String bankAccountName; // 계좌 명

    @Column(nullable = false)
    private String manager; // 담당자

    @Column(nullable = false)
    private String phone; // 연락처

    @Column
    private String position; // 직급

    @Column(nullable = false)
    private String email; // 이메일

    @Column
    private String etc; // 비고

    @Column
    private String withDrawal; // 탈퇴 유무 (Y/N)

    @Column
    private LocalDateTime withDrawalDate; // 탈퇴 일자

    @Column
    private String lastOrders; // 주문 마감시간

}
