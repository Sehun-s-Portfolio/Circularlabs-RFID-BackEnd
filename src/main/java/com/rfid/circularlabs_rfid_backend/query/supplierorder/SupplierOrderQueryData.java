package com.rfid.circularlabs_rfid_backend.query.supplierorder;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.rfid.circularlabs_rfid_backend.process.domain.QSupplierOrder.supplierOrder;

@Slf4j
@RequiredArgsConstructor
@Component
public class SupplierOrderQueryData {

    private final JPAQueryFactory jpaQueryFactory;

    // 총 재고 수량 추출
    public Integer getTotalRemainCount(String supplierCode, String productCode){

        return jpaQueryFactory
                .select(supplierOrder.orderMount.sum())
                .from(supplierOrder)
                .where(supplierOrder.classificationCode.eq(supplierCode)
                        .and(supplierOrder.productCode.eq(productCode)))
                .limit(1)
                .fetchOne();
    }
}
