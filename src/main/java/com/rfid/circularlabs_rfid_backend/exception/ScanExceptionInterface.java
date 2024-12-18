package com.rfid.circularlabs_rfid_backend.exception;

import org.springframework.stereotype.Component;

@Component
public interface ScanExceptionInterface {

    /** 작업 처리를 수행하기 이전에 방금 탈퇴 처리된 고객사인지 선행 확인 **/
    boolean checkWithDrawalMember(String clientCode);
}
