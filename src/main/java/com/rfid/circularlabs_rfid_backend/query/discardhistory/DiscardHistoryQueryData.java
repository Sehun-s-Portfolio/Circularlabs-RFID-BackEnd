package com.rfid.circularlabs_rfid_backend.query.discardhistory;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.rfid.circularlabs_rfid_backend.process.domain.DiscardHistory;
import com.rfid.circularlabs_rfid_backend.scan.request.SendProductCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.rfid.circularlabs_rfid_backend.process.domain.QDiscardHistory.discardHistory;

@Slf4j
@RequiredArgsConstructor
@Component
public class DiscardHistoryQueryData {

    private final JPAQueryFactory jpaQueryFactory;


    // 폐기 이력 조회
    public int getDiscardHistory(String supplierCode, String productCode) {
        Long discardMount = jpaQueryFactory
                .select(discardHistory.count())
                .from(discardHistory)
                .where(discardHistory.supplierCode.eq(supplierCode)
                        .and(discardHistory.productCode.eq(productCode)))
                .limit(1)
                .fetchOne();

        if(discardMount != null || discardMount != 0L){
            return discardMount.intValue();
        }else {
            return 0;
        }
    }


    // 특정 제품이 폐기된 이력이 존재하는지 확인
    public Boolean checkProductDiscard(String productCode, String productSerialCode){
        if(jpaQueryFactory
                .selectFrom(discardHistory)
                .where(discardHistory.productCode.eq(productCode)
                        .and(discardHistory.productSerialCode.eq(productSerialCode)))
                .limit(1)
                .fetchOne() != null){
            return true;
        }else{
            return false;
        }

    }


    // 처음 제품 카테고리에 대한 폐기 이력이 존재하는지 확인
    public Boolean firstCheckDiscardHistory(String productCode){
        if(jpaQueryFactory
                .selectFrom(discardHistory)
                .where(discardHistory.productCode.eq(productCode))
                .limit(1)
                .fetchOne() != null){
            return true;
        }else{
            return false;
        }
    }


    // 특정 카테고리의 폐기 이력이 존재하는 제품들 정보 리스트 추출
    public List<SendProductCode> getCategoryDiscardProducts(String productCode){
        List<Tuple> discardCategoryProductsInfo = jpaQueryFactory
                .select(discardHistory.rfidChipCode, discardHistory.productCode, discardHistory.productSerialCode)
                .from(discardHistory)
                .where(discardHistory.productCode.eq(productCode))
                .fetch();

        List<SendProductCode> checkDiscardProducts = new ArrayList<>();

        for(Tuple eachDiscardProductInfo : discardCategoryProductsInfo){
            checkDiscardProducts.add(
                    SendProductCode.builder()
                            .rfidChipCode(eachDiscardProductInfo.get(discardHistory.rfidChipCode))
                            .filteringCode("CCA2310")
                            .productCode(eachDiscardProductInfo.get(discardHistory.productCode))
                            .productSerialCode(eachDiscardProductInfo.get(discardHistory.productSerialCode))
                            .build()
            );
        }

        return checkDiscardProducts;
    }

}
