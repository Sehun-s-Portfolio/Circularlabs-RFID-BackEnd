package com.rfid.circularlabs_rfid_backend.query.scandata;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.rfid.circularlabs_rfid_backend.product.domain.ProductDetail;
import com.rfid.circularlabs_rfid_backend.product.domain.ProductDetailHistory;
import com.rfid.circularlabs_rfid_backend.scan.domain.RfidScanHistory;
import com.rfid.circularlabs_rfid_backend.scan.response.TestProductDetailResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.rfid.circularlabs_rfid_backend.product.domain.QProductDetail.productDetail;
import static com.rfid.circularlabs_rfid_backend.product.domain.QProductDetailHistory.productDetailHistory;
import static com.rfid.circularlabs_rfid_backend.scan.domain.QRfidScanHistory.rfidScanHistory;

@RequiredArgsConstructor
@Component
public class ScanDataQueryData {

    private final JPAQueryFactory jpaQueryFactory;
    private final EntityManager entityManager;
    //private final RedisTemplate<String, Object> redisTemplate;

    // 데이터 스캔 후 작업 처리 시 기존에 RFID 기기 스캔 이력 정보가 존재했는지 확인하기 위한 함수
    public RfidScanHistory checkRemainScanData(String supplierCode, String clientCode, String deviceCode, String productCode, String status) {
        return jpaQueryFactory
                .selectFrom(rfidScanHistory)
                .where(rfidScanHistory.supplierCode.eq(supplierCode)
                        .and(rfidScanHistory.clientCode.eq(clientCode))
                        .and(rfidScanHistory.rfidChipCode.eq(deviceCode))
                        .and(rfidScanHistory.productCode.eq(productCode))
                        .and(rfidScanHistory.status.eq(status)))
                .fetchOne();
    }

    // RFID 스캔 이력을 업데이트 시키기 위해 가장 최신의 마지막 이력을 조회
    public RfidScanHistory getLatestRfidScanHistory(String productCode, String supplierCode) {
        RfidScanHistory history = jpaQueryFactory
                .selectFrom(rfidScanHistory)
                .where(rfidScanHistory.supplierCode.eq(supplierCode)
                        .and(rfidScanHistory.productCode.eq(productCode)))
                .orderBy(rfidScanHistory.createdAt.desc())
                .limit(1)
                .fetchOne();

        return history;
    }


    // 이미 이전에 완전 똑같은 동일한 내용의 이력을 가지고 있는지 확인
    public Boolean checkSameCycleScanHistory(String productCode, String clientCode, String supplierCode, int cycle, String status) {

        if (jpaQueryFactory
                .selectFrom(rfidScanHistory)
                .where(rfidScanHistory.productCode.eq(productCode)
                        .and(rfidScanHistory.clientCode.eq(clientCode))
                        .and(rfidScanHistory.supplierCode.eq(supplierCode))
                        //.and(rfidScanHistory.cycle.eq(cycle))
                        .and(rfidScanHistory.status.eq(status)))
                .fetchOne() == null) {
            return true;
        }

        return false;
    }


    // 잘못된 출고 상태 처리로 인해 바로 다시 재차 스캔하여 상태 처리 했을 경우를 확인하기 위한 이력 조회
    @Transactional
    public boolean asapScanForPrevHistory(String deviceCode, String productCode, String clientCode, String supplierCode, int cycle, int statusCount) {

        if (jpaQueryFactory
                .selectFrom(rfidScanHistory)
                .where(rfidScanHistory.productCode.eq(productCode)
                        .and(rfidScanHistory.deviceCode.eq(deviceCode))
                        .and(rfidScanHistory.clientCode.eq(clientCode))
                        .and(rfidScanHistory.supplierCode.eq(supplierCode))
                        //.and(rfidScanHistory.cycle.eq(cycle + 1))
                        .and(rfidScanHistory.status.eq("입고")))
                .fetch() == null) {

            jpaQueryFactory
                    .update(rfidScanHistory)
                    .set(rfidScanHistory.statusCount, statusCount)
                    .where(rfidScanHistory.productCode.eq(productCode)
                            .and(rfidScanHistory.deviceCode.eq(deviceCode))
                            .and(rfidScanHistory.clientCode.eq(clientCode))
                            .and(rfidScanHistory.supplierCode.eq(supplierCode))
                            //.and(rfidScanHistory.cycle.eq(cycle))
                            .and(rfidScanHistory.status.eq("출고")))
                    .execute();

            entityManager.flush();
            entityManager.clear();

            return true;
        }

        return false;
    }


    // 잘못된 출고 상태 처리로 인해 바로 다시 재차 스캔하여 상태 처리 했을 경우를 확인하기 위한 이력 조회
    @Transactional
    public boolean testAsapScanForPrevHistory(String deviceCode, ProductDetail callProductDetail, int statusCount) {

        List<RfidScanHistory> asapScanHistory = jpaQueryFactory
                .selectFrom(rfidScanHistory)
                .where(rfidScanHistory.productCode.eq(callProductDetail.getProductCode())
                        .and(rfidScanHistory.deviceCode.eq(deviceCode))
                        .and(rfidScanHistory.clientCode.eq(callProductDetail.getClientCode()))
                        .and(rfidScanHistory.supplierCode.eq(callProductDetail.getSupplierCode()))
                        //.and(rfidScanHistory.cycle.eq(callProductDetail.getCycle() + 1))
                        .and(rfidScanHistory.status.eq("입고")))
                .fetch();

        if (asapScanHistory == null) {

            jpaQueryFactory
                    .update(rfidScanHistory)
                    .set(rfidScanHistory.statusCount, statusCount)
                    .where(rfidScanHistory.productCode.eq(callProductDetail.getProductCode())
                            .and(rfidScanHistory.deviceCode.eq(deviceCode))
                            .and(rfidScanHistory.clientCode.eq(callProductDetail.getClientCode()))
                            .and(rfidScanHistory.supplierCode.eq(callProductDetail.getSupplierCode()))
                            //.and(rfidScanHistory.cycle.eq(callProductDetail.getCycle()))
                            .and(rfidScanHistory.status.eq("출고")))
                    .execute();

            entityManager.flush();
            entityManager.clear();


            RfidScanHistory redisScanHistory = jpaQueryFactory
                    .selectFrom(rfidScanHistory)
                    .where(rfidScanHistory.productCode.eq(callProductDetail.getProductCode())
                            .and(rfidScanHistory.deviceCode.eq(deviceCode))
                            .and(rfidScanHistory.clientCode.eq(callProductDetail.getClientCode()))
                            .and(rfidScanHistory.supplierCode.eq(callProductDetail.getSupplierCode()))
                            //.and(rfidScanHistory.cycle.eq(callProductDetail.getCycle()))
                            .and(rfidScanHistory.status.eq("출고")))
                    .fetchOne();


            assert redisScanHistory != null;
            //redisTemplate.opsForValue().set("rfidscanhistory:product:" + callProductDetail.getProductCode() + ":supplier:" + callProductDetail.getSupplierCode() + ":client:" + callProductDetail.getClientCode() + ":status:" + callProductDetail.getStatus() + ":cycle:" + callProductDetail.getCycle(), redisScanHistory);

            return true;
        }

        return false;
    }

    // 잘못된 입고 상태 처리로 인해 바로 다시 재차 스캔하여 상태 처리 했을 경우를 확인하기 위한 이력 조회
    @Transactional
    public boolean asapScanForPrevInHistory(String deviceCode, String productCode, String clientCode, String supplierCode, int cycle, int statusCount) {

        if (jpaQueryFactory
                .selectFrom(rfidScanHistory)
                .where(rfidScanHistory.productCode.eq(productCode)
                        .and(rfidScanHistory.deviceCode.eq(deviceCode))
                        .and(rfidScanHistory.clientCode.eq(clientCode))
                        .and(rfidScanHistory.supplierCode.eq(supplierCode))
                        //.and(rfidScanHistory.cycle.eq(cycle))
                        .and(rfidScanHistory.status.eq("회수")))
                .fetch() == null) {

            jpaQueryFactory
                    .update(rfidScanHistory)
                    .set(rfidScanHistory.statusCount, statusCount)
                    .where(rfidScanHistory.productCode.eq(productCode)
                            .and(rfidScanHistory.deviceCode.eq(deviceCode))
                            .and(rfidScanHistory.clientCode.eq(clientCode))
                            .and(rfidScanHistory.supplierCode.eq(supplierCode))
                            //.and(rfidScanHistory.cycle.eq(cycle))
                            .and(rfidScanHistory.status.eq("입고")))
                    .execute();

            entityManager.flush();
            entityManager.clear();

            return true;
        }

        return false;
    }

    // 잘못된 입고 상태 처리로 인해 바로 다시 재차 스캔하여 상태 처리 했을 경우를 확인하기 위한 이력 조회
    @Transactional
    public boolean testAsapScanForPrevInHistory(String deviceCode, ProductDetail callProductDetail, int statusCount) {

        if (jpaQueryFactory
                .selectFrom(rfidScanHistory)
                .where(rfidScanHistory.productCode.eq(callProductDetail.getProductCode())
                        .and(rfidScanHistory.deviceCode.eq(deviceCode))
                        .and(rfidScanHistory.clientCode.eq(callProductDetail.getClientCode()))
                        .and(rfidScanHistory.supplierCode.eq(callProductDetail.getSupplierCode()))
                        //.and(rfidScanHistory.cycle.eq(callProductDetail.getCycle()))
                        .and(rfidScanHistory.status.eq("회수")))
                .fetch() == null) {

            jpaQueryFactory
                    .update(rfidScanHistory)
                    .set(rfidScanHistory.statusCount, statusCount)
                    .where(rfidScanHistory.productCode.eq(callProductDetail.getProductCode())
                            .and(rfidScanHistory.deviceCode.eq(deviceCode))
                            .and(rfidScanHistory.clientCode.eq(callProductDetail.getClientCode()))
                            .and(rfidScanHistory.supplierCode.eq(callProductDetail.getSupplierCode()))
                            //.and(rfidScanHistory.cycle.eq(callProductDetail.getCycle()))
                            .and(rfidScanHistory.status.eq("입고")))
                    .execute();

            entityManager.flush();
            entityManager.clear();

            RfidScanHistory redisScanHistory = jpaQueryFactory
                    .selectFrom(rfidScanHistory)
                    .where(rfidScanHistory.productCode.eq(callProductDetail.getProductCode())
                            .and(rfidScanHistory.deviceCode.eq(deviceCode))
                            .and(rfidScanHistory.clientCode.eq(callProductDetail.getClientCode()))
                            .and(rfidScanHistory.supplierCode.eq(callProductDetail.getSupplierCode()))
                            //.and(rfidScanHistory.cycle.eq(callProductDetail.getCycle()))
                            .and(rfidScanHistory.status.eq("입고")))
                    .fetchOne();

            assert redisScanHistory != null;
            //redisTemplate.opsForValue().set("rfidscanhistory:product:" + callProductDetail.getProductCode() + ":supplier:" + callProductDetail.getSupplierCode() + ":client:" + callProductDetail.getClientCode() + ":status:" + callProductDetail.getStatus() + ":cycle:" + callProductDetail.getCycle(), redisScanHistory);

            return true;
        }

        return false;
    }


    // 잘못된 회수 상태 처리로 인해 바로 다시 재차 스캔하여 상태 처리 했을 경우를 확인하기 위한 이력 조회
    @Transactional
    public boolean asapScanForPrevTurnBackHistory(String deviceCode, String productCode, String clientCode, String supplierCode, int cycle, int statusCount) {

        if (jpaQueryFactory
                .selectFrom(rfidScanHistory)
                .where(rfidScanHistory.productCode.eq(productCode)
                        .and(rfidScanHistory.deviceCode.eq(deviceCode))
                        .and(rfidScanHistory.clientCode.eq(clientCode))
                        .and(rfidScanHistory.supplierCode.eq(supplierCode))
                        //.and(rfidScanHistory.cycle.eq(cycle))
                        .and(rfidScanHistory.status.eq("세척").or(rfidScanHistory.status.eq("출고"))))
                .fetch() == null) {

            jpaQueryFactory
                    .update(rfidScanHistory)
                    .set(rfidScanHistory.statusCount, statusCount)
                    .where(rfidScanHistory.productCode.eq(productCode)
                            .and(rfidScanHistory.deviceCode.eq(deviceCode))
                            .and(rfidScanHistory.clientCode.eq(clientCode))
                            .and(rfidScanHistory.supplierCode.eq(supplierCode))
                            //.and(rfidScanHistory.cycle.eq(cycle))
                            .and(rfidScanHistory.status.eq("회수")))
                    .execute();

            entityManager.flush();
            entityManager.clear();

            return true;
        }

        return false;
    }

    // 잘못된 회수 상태 처리로 인해 바로 다시 재차 스캔하여 상태 처리 했을 경우를 확인하기 위한 이력 조회
    @Transactional
    public boolean testAsapScanForPrevTurnBackHistory(String deviceCode, ProductDetail callProductDetail, int statusCount) {

        if (jpaQueryFactory
                .selectFrom(rfidScanHistory)
                .where(rfidScanHistory.productCode.eq(callProductDetail.getProductCode())
                        .and(rfidScanHistory.deviceCode.eq(deviceCode))
                        .and(rfidScanHistory.clientCode.eq(callProductDetail.getClientCode()))
                        .and(rfidScanHistory.supplierCode.eq(callProductDetail.getSupplierCode()))
                        //.and(rfidScanHistory.cycle.eq(callProductDetail.getCycle()))
                        .and(rfidScanHistory.status.eq("세척").or(rfidScanHistory.status.eq("출고"))))
                .fetch() == null) {

            jpaQueryFactory
                    .update(rfidScanHistory)
                    .set(rfidScanHistory.statusCount, statusCount)
                    .where(rfidScanHistory.productCode.eq(callProductDetail.getProductCode())
                            .and(rfidScanHistory.deviceCode.eq(deviceCode))
                            .and(rfidScanHistory.clientCode.eq(callProductDetail.getClientCode()))
                            .and(rfidScanHistory.supplierCode.eq(callProductDetail.getSupplierCode()))
                            //.and(rfidScanHistory.cycle.eq(callProductDetail.getCycle()))
                            .and(rfidScanHistory.status.eq("회수")))
                    .execute();

            entityManager.flush();
            entityManager.clear();

            RfidScanHistory redisScanHistory = jpaQueryFactory
                    .selectFrom(rfidScanHistory)
                    .where(rfidScanHistory.productCode.eq(callProductDetail.getProductCode())
                            .and(rfidScanHistory.deviceCode.eq(deviceCode))
                            .and(rfidScanHistory.clientCode.eq(callProductDetail.getClientCode()))
                            .and(rfidScanHistory.supplierCode.eq(callProductDetail.getSupplierCode()))
                            //.and(rfidScanHistory.cycle.eq(callProductDetail.getCycle()))
                            .and(rfidScanHistory.status.eq("회수")))
                    .fetchOne();

            assert redisScanHistory != null;
            //redisTemplate.opsForValue().set("rfidscanhistory:product:" + callProductDetail.getProductCode() + ":supplier:" + callProductDetail.getSupplierCode() + ":client:" + callProductDetail.getClientCode() + ":status:" + callProductDetail.getStatus() + ":cycle:" + callProductDetail.getCycle(), redisScanHistory);

            return true;
        }

        return false;
    }


    // 잘못된 세척 상태 처리로 인해 바로 다시 재차 스캔하여 상태 처리 했을 경우를 확인하기 위한 이력 조회
    @Transactional
    public boolean asapScanForPrevCleanHistory(String deviceCode, String productCode, String clientCode, String supplierCode, int cycle, int statusCount) {

        jpaQueryFactory
                .update(rfidScanHistory)
                .set(rfidScanHistory.statusCount, statusCount)
                .where(rfidScanHistory.productCode.eq(productCode)
                        .and(rfidScanHistory.deviceCode.eq(deviceCode))
                        .and(rfidScanHistory.clientCode.eq(clientCode))
                        .and(rfidScanHistory.supplierCode.eq(supplierCode))
                        //.and(rfidScanHistory.cycle.eq(cycle))
                        .and(rfidScanHistory.status.eq("세척")))
                .execute();

        entityManager.flush();
        entityManager.clear();

        return true;
    }

    // 테스트 제품 이력 조회

    public List<TestProductDetailResponseDto> testGetProductDetails(Long rfidHistoryId){

        RfidScanHistory rfidScanHistory1 = jpaQueryFactory
                .selectFrom(rfidScanHistory)
                .where(rfidScanHistory.rfidScanhistoryId.eq(rfidHistoryId))
                .fetchOne();

        List<TestProductDetailResponseDto> testResponseDto = new ArrayList<>();

        rfidScanHistory1.getProductDetailHistories().forEach(p ->
                testResponseDto.add(
                        TestProductDetailResponseDto.builder()
                                .productCode(p.getProductCode())
                                .productSerialCode(p.getProductSerialCode())
                                .build()
                )
        );

        return testResponseDto;
    }



}
