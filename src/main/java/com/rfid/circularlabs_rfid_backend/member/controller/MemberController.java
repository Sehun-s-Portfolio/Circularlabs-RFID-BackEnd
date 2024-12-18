package com.rfid.circularlabs_rfid_backend.member.controller;

import com.rfid.circularlabs_rfid_backend.member.response.GetClientsResponseDto;
import com.rfid.circularlabs_rfid_backend.member.service.MemberService;
import com.rfid.circularlabs_rfid_backend.share.ResponseBody;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/cl/member")
@RestController
public class MemberController {

    private final MemberService memberService;

    // 출고 / 입고 / 회수 / 세척 / 폐기 시 해당되는 고객사들 호출 조회
    @GetMapping("/clients")
    public List<GetClientsResponseDto> getClients(@RequestParam String sc){
        log.info("[controller] 작업 수행 시 고객사 리스트 호출 api - 코드 : {}", sc);

        return memberService.getClients(sc);
    }
}
