package com.rfid.circularlabs_rfid_backend.query2.productdetailhistory;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.rfid.circularlabs_rfid_backend.product.domain.ProductDetail;
import com.rfid.circularlabs_rfid_backend.product.domain.ProductDetailHistory;
import com.rfid.circularlabs_rfid_backend.scan.domain.RfidScanHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import java.util.List;

import static com.rfid.circularlabs_rfid_backend.product.domain.QProductDetail.productDetail;
import static com.rfid.circularlabs_rfid_backend.product.domain.QProductDetailHistory.productDetailHistory;

@Slf4j
@RequiredArgsConstructor
@Component
public class ProductDetailHistoryQueryDataV2 {

    private final JPAQueryFactory jpaQueryFactory;
    private final EntityManager entityManager;

    // 이전에 저장한 이력이 있는지 확인
    public Boolean checkPreviewHistory(String productSerialCode, String productCode, String supplierCode, String clientCode, String status, int cycle){

        if(jpaQueryFactory
                .selectFrom(productDetailHistory)
                .where(productDetailHistory.productSerialCode.eq(productSerialCode)
                        .and(productDetailHistory.productCode.eq(productCode))
                        .and(productDetailHistory.supplierCode.eq(supplierCode))
                        .and(productDetailHistory.clientCode.eq(clientCode))
                        .and(productDetailHistory.status.eq(status))
                        .and(productDetailHistory.cycle.eq(cycle)))
                .limit(1)
                .fetchOne() == null){
            return true;
        }

        return false;
    }

    // 제품 카테고리 기준으로 이전에 저장한 ProductDetailHistory가 존재하는지 확인
    public Boolean checkPrevProductDetailHistoryCategory(String productCode, String supplierCode, String clientCode, String status, int cycle){

        if(jpaQueryFactory
                .selectFrom(productDetailHistory)
                .where(productDetailHistory.productCode.eq(productCode)
                        .and(productDetailHistory.supplierCode.eq(supplierCode))
                        .and(productDetailHistory.clientCode.eq(clientCode))
                        .and(productDetailHistory.status.eq(status))
                        .and(productDetailHistory.cycle.eq(cycle)))
                .limit(1)
                .fetchOne() == null){
            return true;
        }

        return false;
    }

    // 제품 카테고리 기준으로 이전에 저장한 ProductDetailHistory가 존재하는지 확인
    public Boolean checkPrevProductDetailHistoryCategory2(ProductDetail checkProductDetail){

        if(jpaQueryFactory
                .selectFrom(productDetailHistory)
                .where(productDetailHistory.productCode.eq(checkProductDetail.getProductCode())
                        .and(productDetailHistory.supplierCode.eq(checkProductDetail.getSupplierCode()))
                        .and(productDetailHistory.clientCode.eq(checkProductDetail.getClientCode()))
                        .and(productDetailHistory.status.eq(checkProductDetail.getStatus()))
                        .and(productDetailHistory.cycle.eq(checkProductDetail.getCycle())))
                .limit(1)
                .fetchOne() == null){
            return true;
        }

        return false;
    }


    // RFID 스캔 이력과 연관 지은 ProductDetailHistory 매핑 업데이트
    public void updateRfidHistoryRelatedProductDetails(List<RfidScanHistory> rfidScanHistoryList){

        for(RfidScanHistory eachScanHistory : rfidScanHistoryList){
            List<ProductDetailHistory> productDetailHistoryList = eachScanHistory.getProductDetailHistories();

            for(int i = 0 ; i < productDetailHistoryList.size() ; i++){
                jpaQueryFactory
                        .update(productDetailHistory)
                        .set(productDetailHistory.rfidScanHistory, eachScanHistory)
                        .where(productDetailHistory.productDetailHistoryId.eq(productDetailHistoryList.get(i).getProductDetailHistoryId()))
                        .execute();
            }
        }

        entityManager.flush();
        entityManager.clear();
    }
}
