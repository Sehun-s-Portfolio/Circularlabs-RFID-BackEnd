package com.rfid.circularlabs_rfid_backend.exception;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static com.rfid.circularlabs_rfid_backend.member.domain.QMember.member;

@RequiredArgsConstructor
@Component
public class ScanException implements ScanExceptionInterface{

    private final JPAQueryFactory jpaQueryFactory;

    /** 작업 처리를 수행하기 이전에 방금 탈퇴 처리된 고객사인지 선행 확인 **/
    @Override
    public boolean checkWithDrawalMember(String clientCode) {

        if(Objects.equals(jpaQueryFactory
                .select(member.withDrawal)
                .from(member)
                .where(member.classificationCode.eq(clientCode))
                .limit(1)
                .fetchOne(), "Y")){
            return true;
        }

        return false;
    }
}
