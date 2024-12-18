package com.rfid.circularlabs_rfid_backend.query.productdetail;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.rfid.circularlabs_rfid_backend.product.domain.ProductDetail;
import com.rfid.circularlabs_rfid_backend.scan.domain.RfidScanHistory;
import com.rfid.circularlabs_rfid_backend.scan.response.TestProductDetailResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.rfid.circularlabs_rfid_backend.product.domain.QProductDetail.productDetail;

@Slf4j
@RequiredArgsConstructor
@Component
public class ProductDetailQueryData {

    private final JPAQueryFactory jpaQueryFactory;
    private final EntityManager entityManager;

    // 데이터 스캔 후 작업 처리 시 기존에 대분류 제품 스캔 이력(ProductDetail)이 존재했는지 확인하기 위한 함수
    public ProductDetail checkRemainProductDetail(String rfidChipCode, String productSerialCode, String productCode, String supplierCode, String clientCode) {
        return jpaQueryFactory
                .selectFrom(productDetail)
                .where(productDetail.rfidChipCode.eq(rfidChipCode)
                        .and(productDetail.productSerialCode.eq(productSerialCode))
                        .and(productDetail.productCode.eq(productCode))
                        .and(productDetail.supplierCode.eq(supplierCode)))
                .fetchOne();
    }

    // 회수 및 세척 처리 시 기존에 대분류 제품 스캔 이력(ProductDetail)을 확인하기 위한 함수
    public ProductDetail checkBeforeStatusProductDetail(String rfidChipCode, String productSerialCode, String productCode, String supplierCode) {
        return jpaQueryFactory
                .selectFrom(productDetail)
                .where(productDetail.rfidChipCode.eq(rfidChipCode)
                        .and(productDetail.productSerialCode.eq(productSerialCode))
                        .and(productDetail.productCode.eq(productCode))
                        .and(productDetail.supplierCode.eq(supplierCode)))
                .fetchOne();
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


    // 스캔했을 때, 대분류 제품 스캔 이력(ProductDetail)이 기존에 존재하면, 상태를 입고 상태로, 최신 리딩 시간을 업데이트
    @Transactional
    public ProductDetail scanInUpdateStatusAndReadingAt(ProductDetail inProductDetail) {
        jpaQueryFactory
                .update(productDetail)
                .set(productDetail.status, "입고")
                .set(productDetail.cycle, inProductDetail.getCycle() + 1)
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




}
