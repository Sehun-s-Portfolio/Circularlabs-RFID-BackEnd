package com.rfid.circularlabs_rfid_backend.query.member;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.rfid.circularlabs_rfid_backend.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.rfid.circularlabs_rfid_backend.member.domain.QMember.member;

@RequiredArgsConstructor
@Component
public class MemberQueryData {

    private final JPAQueryFactory jpaQueryFactory;

    /** 공급사에 속한 고객사 리스트 호출 **/
    public List<Member> getClients(String supplierCode){

        return jpaQueryFactory
                .selectFrom(member)
                .where(member.grade.eq(2).and(member.motherCode.eq(supplierCode))
                        .and(member.withDrawal.eq("N")))
                .fetch();
    }


    /** 공급사 id 조회 **/
    public Tuple getSupplierId(String classificationCode){
        return jpaQueryFactory
                .select(member.memberId, member.clientCompany)
                .from(member)
                .where(member.classificationCode.eq(classificationCode))
                .limit(1)
                .fetchOne();
    }
}
