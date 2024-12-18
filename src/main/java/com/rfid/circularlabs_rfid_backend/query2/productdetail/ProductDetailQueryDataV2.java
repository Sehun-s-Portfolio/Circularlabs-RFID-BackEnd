package com.rfid.circularlabs_rfid_backend.query2.productdetail;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.rfid.circularlabs_rfid_backend.product.domain.ProductDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

import static com.rfid.circularlabs_rfid_backend.product.domain.QProductDetail.productDetail;

@Slf4j
@RequiredArgsConstructor
@Component
public class ProductDetailQueryDataV2 {

    private final JPAQueryFactory jpaQueryFactory;
    private final EntityManager entityManager;

    // 데이터 스캔 후 작업 처리 시 기존에 대분류 제품 스캔 이력(ProductDetail)이 존재했는지 확인하기 위한 함수
    public ProductDetail checkRemainProductDetail(String productSerialCode, String productCode, String supplierCode, String clientCode) {
        return jpaQueryFactory
                .selectFrom(productDetail)
                .where(productDetail.productSerialCode.eq(productSerialCode)
                        .and(productDetail.productCode.eq(productCode))
                        .and(productDetail.clientCode.eq(clientCode))
                        .and(productDetail.supplierCode.eq(supplierCode)))
                .orderBy(productDetail.latestReadingAt.desc())
                .limit(1)
                .fetchOne();
    }

    // 회수 및 세척 처리 시 기존에 대분류 제품 스캔 이력(ProductDetail)을 확인하기 위한 함수
    public ProductDetail checkBeforeStatusProductDetail(String productSerialCode, String productCode, String supplierCode) {

        return jpaQueryFactory
                .selectFrom(productDetail)
                .where(productDetail.productSerialCode.eq(productSerialCode)
                        .and(productDetail.productCode.eq(productCode))
                        .and(productDetail.supplierCode.eq(supplierCode))
                        .and(productDetail.clientCode.ne("null")))
                .orderBy(productDetail.latestReadingAt.desc())
                .limit(1)
                .fetchOne();
    }

    // 회수 및 세척 처리 시 기존에 대분류 제품 스캔 이력(ProductDetail)을 확인하기 위한 함수
    public ProductDetail checkBeforeStatusProductDetailAboutClean(String productSerialCode, String productCode, String supplierCode) {

        return jpaQueryFactory
                .selectFrom(productDetail)
                .where(productDetail.productSerialCode.eq(productSerialCode)
                        .and(productDetail.productCode.eq(productCode))
                        .and(productDetail.supplierCode.eq(supplierCode)))
                .orderBy(productDetail.latestReadingAt.desc())
                .limit(1)
                .fetchOne();
    }

    // ProductDetailHistory를 저장하기 이전에 이미 저장 진행 중인 데이터가 존재하는지 확인
    public boolean checkRelatedProductDetail(String productCode, String supplierCode, String clientCode, String status, int cycle) {
        if (jpaQueryFactory
                .selectFrom(productDetail)
                .where(productDetail.productCode.eq(productCode)
                        .and(productDetail.supplierCode.eq(supplierCode))
                        .and(productDetail.clientCode.eq(clientCode))
                        .and(productDetail.status.eq(status))
                        .and(productDetail.cycle.eq(cycle)))
                .limit(1)
                .fetchOne() == null) {
            return true;
        }

        return false;
    }


    // 스캔했을 때, 대분류 제품 스캔 이력(ProductDetail)이 기존에 존재하고, 상태가 한 번 사이클이 완료된 상태(대기?) 이면 상태와 최신 리딩 시간을 출고로 변경
    @Transactional
    public ProductDetail scanOutUpdateStatusAndReadingAt(ProductDetail outProductDetail) {
        jpaQueryFactory
                .update(productDetail)
                .set(productDetail.status, "출고")
                .set(productDetail.latestReadingAt, LocalDateTime.now())
                .where(productDetail.productDetailId.eq(outProductDetail.getProductDetailId()))
                .execute();

        entityManager.flush();
        entityManager.clear();

        log.info("스캔했을 때, 대분류 제품 스캔 이력(ProductDetail)이 기존에 존재하거나, 상태가 한 번 사이클이 완료된 상태(대기?) 이면 상태를 출고로 변경");

        return jpaQueryFactory
                .selectFrom(productDetail)
                .where(productDetail.productDetailId.eq(outProductDetail.getProductDetailId()))
                .fetchOne();
    }

    @Transactional
    public void needUpdateProductDetailStatusIn(List<Long> updateProductDetailIds){
        jpaQueryFactory
                .update(productDetail)
                .set(productDetail.status, "입고")
                .set(productDetail.latestReadingAt, LocalDateTime.now())
                .where(productDetail.productDetailId.in(updateProductDetailIds))
                .execute();

        entityManager.flush();
        entityManager.clear();
    }

    @Transactional
    public void needUpdateProductDetailStatusTurnBack(List<Long> updateProductDetailIds){
        jpaQueryFactory
                .update(productDetail)
                .set(productDetail.status, "회수")
                .set(productDetail.latestReadingAt, LocalDateTime.now())
                .set(productDetail.cycle, productDetail.cycle.add(1))
                .where(productDetail.productDetailId.in(updateProductDetailIds))
                .execute();

        entityManager.flush();
        entityManager.clear();
    }


    // 스캔했을 때, 대분류 제품 스캔 이력(ProductDetail)이 기존에 존재하면, 상태를 입고 상태로, 최신 리딩 시간을 업데이트
    @Transactional
    public ProductDetail scanInUpdateStatusAndReadingAt(ProductDetail inProductDetail) {
        jpaQueryFactory
                .update(productDetail)
                .set(productDetail.status, "입고")
                .set(productDetail.latestReadingAt, LocalDateTime.now())
                .where(productDetail.productDetailId.eq(inProductDetail.getProductDetailId()))
                .execute();

        entityManager.flush();
        entityManager.clear();

        log.info("스캔했을 때, 대분류 제품 스캔 이력(ProductDetail)이 기존에 존재하면, 상태를 입고 상태로, 최신 리딩 시간을 업데이트");

        return jpaQueryFactory
                .selectFrom(productDetail)
                .where(productDetail.productDetailId.eq(inProductDetail.getProductDetailId()))
                .fetchOne();
    }


    // 스캔했을 때, 대분류 제품 스캔 이력(ProductDetail)이 기존에 존재하면, 상태를 회수 상태로, 최신 리딩 시간을 업데이트
    @Transactional
    public ProductDetail scanReturnUpdateStatusAndReadingAt(ProductDetail returnProductDetail) {
        jpaQueryFactory
                .update(productDetail)
                .set(productDetail.status, "회수")
                .set(productDetail.latestReadingAt, LocalDateTime.now())
                .set(productDetail.cycle, returnProductDetail.getCycle() + 1)
                .where(productDetail.productDetailId.eq(returnProductDetail.getProductDetailId()))
                .execute();

        entityManager.flush();
        entityManager.clear();

        log.info("스캔했을 때, 대분류 제품 스캔 이력(ProductDetail)이 기존에 존재하면, 상태를 회수 상태로, 최신 리딩 시간을 업데이트");

        return jpaQueryFactory
                .selectFrom(productDetail)
                .where(productDetail.productDetailId.eq(returnProductDetail.getProductDetailId()))
                .fetchOne();
    }


    // 스캔했을 때, 대분류 제품 스캔 이력(ProductDetail)이 기존에 존재하면, 상태를 세척 상태로, 최신 리딩 시간을 업데이트
    @Transactional
    public ProductDetail scanCleanUpdateStatusAndReadingAt(ProductDetail cleanProductDetail) {
        jpaQueryFactory
                .update(productDetail)
                .set(productDetail.status, "세척")
                .set(productDetail.latestReadingAt, LocalDateTime.now())
                .where(productDetail.productDetailId.eq(cleanProductDetail.getProductDetailId()))
                .execute();

        entityManager.flush();
        entityManager.clear();

        log.info("스캔했을 때, 대분류 제품 스캔 이력(ProductDetail)이 기존에 존재하면, 상태를 세척 상태로, 최신 리딩 시간을 업데이트");

        return jpaQueryFactory
                .selectFrom(productDetail)
                .where(productDetail.productDetailId.eq(cleanProductDetail.getProductDetailId()))
                .fetchOne();
    }


    // 폐기 처리 업데이트
    @Transactional
    public ProductDetail scanDiscardUpdateStatusAndReadingAt(ProductDetail discardProductDetail) {
        jpaQueryFactory
                .update(productDetail)
                .set(productDetail.status, "폐기")
                .set(productDetail.latestReadingAt, LocalDateTime.now())
                .where(productDetail.productDetailId.eq(discardProductDetail.getProductDetailId()))
                .execute();

        entityManager.flush();
        entityManager.clear();

        log.info("폐기 처리 업데이트");

        return jpaQueryFactory
                .selectFrom(productDetail)
                .where(productDetail.productDetailId.eq(discardProductDetail.getProductDetailId()))
                .fetchOne();
    }

    // null 이 되기 이전의 존재하는 clientCode 추출
    public HashMap<String, String> nullBeforeClientCodeOfProductDetail(List<ProductDetail> nonClientCodeProductDetails) {

        HashMap<String, String> nullDataHashMap = new HashMap<>();

        nonClientCodeProductDetails.forEach(exportInfo -> {
            String prevClientCode = jpaQueryFactory
                    .select(productDetail.clientCode)
                    .from(productDetail)
                    .where(productDetail.productCode.eq(exportInfo.getProductCode())
                            .and(productDetail.productSerialCode.eq(exportInfo.getProductSerialCode()))
                            .and(productDetail.clientCode.ne("null")))
                    .orderBy(productDetail.createdAt.desc())
                    .limit(1)
                    .fetchOne();

            String keyValue = exportInfo.getProductCode() + ":" + exportInfo.getProductSerialCode();

            if (prevClientCode != null &&
                    (nullDataHashMap.isEmpty() ||
                            nullDataHashMap
                                    .get(keyValue) == null ||
                            !nullDataHashMap
                                    .get(keyValue)
                                    .equals(prevClientCode))) {

                // 같이 들어온 정상 데이터들의 StatusCount를 계산하기 위해 추가적으로 필요한 null 처리된 데이터들의 이전 clientCode 데이터
                // 데이터 형식 - <ProductCode:ProductSerialCode, 이전 ClientCode>
                nullDataHashMap.put(keyValue, prevClientCode);
            }
        });

        log.info("null 데이터들의 이전 고객사 정보 : {}", nullDataHashMap);

        return nullDataHashMap;
    }

}
