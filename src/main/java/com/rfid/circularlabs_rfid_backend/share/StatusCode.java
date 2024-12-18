package com.rfid.circularlabs_rfid_backend.share;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StatusCode {

    // success response
    OK("정상 수행", "C-200"),

    // bad response
    NOT_EXIST_SCAN_OUT_DATA("출고 데이터가 존재하지 않습니다.", "C-401"),
    NOT_MATCH_SCAN_ORDER("요청한 주문 수량과 스캔 수량이 일치하지 않아 요청 주문 완료 처리를 진행할 수 없습니다.", "C-402"),
    NOT_RIGHT_REGISTER_INFO("입력한 회원가입 정보가 옳바르지 않습니다.", "C-499");

    private final String message;
    private final String code;
}
