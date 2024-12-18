package com.rfid.circularlabs_rfid_backend.device.domain;

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
public class Device extends TimeStamped {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long deviceId; // 인덱스

    @Column(nullable = false)
    private String supplierCode; // 기기를 소유한 공급사 본인 코드

    @Column(nullable = false)
    private String deviceType; // 리더기 종류

    @Column(nullable = false)
    private String deviceCode; // 리더기 코드

    @Column(nullable = false)
    private String manager; // 담당자

    @Column(nullable = false)
    private String phone; // 연락처

    @Column
    private String email; // 이메일

}
