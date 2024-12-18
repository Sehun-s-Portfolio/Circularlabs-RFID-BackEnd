package com.rfid.circularlabs_rfid_backend.member.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class GetClientsResponseDto {
    private Long memberId;
    private String companyName;
    private String classificationCode;
}
