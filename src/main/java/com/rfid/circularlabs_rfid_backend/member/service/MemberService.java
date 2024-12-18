package com.rfid.circularlabs_rfid_backend.member.service;

import com.querydsl.core.Tuple;
import com.rfid.circularlabs_rfid_backend.member.domain.Member;
import com.rfid.circularlabs_rfid_backend.member.response.GetClientsResponseDto;
import com.rfid.circularlabs_rfid_backend.query.member.MemberQueryData;
import com.rfid.circularlabs_rfid_backend.share.ResponseBody;
import com.rfid.circularlabs_rfid_backend.share.StatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class MemberService {

    private final MemberQueryData memberQueryData;

    // 출고 / 입고 / 회수 / 세척 / 폐기 시 해당되는 고객사들 호출 조회
    public List<GetClientsResponseDto> getClients(String sc){
        log.info("[service] 작업 수행 시 고객사 리스트 호출 api - 코드 : {}", sc);

        List<Member> clients = memberQueryData.getClients(sc);
        List<GetClientsResponseDto> responseClients = new ArrayList<>();

        for (Member client : clients) {
            responseClients.add(
                    GetClientsResponseDto.builder()
                            .memberId(client.getMemberId())
                            .companyName(client.getClientCompany())
                            .classificationCode(client.getClassificationCode())
                            .build()
            );
        }

        return responseClients;
    }
}
