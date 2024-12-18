package com.rfid.circularlabs_rfid_backend.query2.scandata;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.rfid.circularlabs_rfid_backend.process.domain.SupplierOrder;
import com.rfid.circularlabs_rfid_backend.product.domain.Product;
import com.rfid.circularlabs_rfid_backend.product.domain.ProductDetail;
import com.rfid.circularlabs_rfid_backend.product.domain.ProductDetailHistory;
import com.rfid.circularlabs_rfid_backend.product.repository.ProductDetailRepository;
import com.rfid.circularlabs_rfid_backend.scan.domain.RfidScanHistory;
import com.rfid.circularlabs_rfid_backend.scan.request.SendProductCode;
import com.rfid.circularlabs_rfid_backend.scan.response.ProductDetailscanResponseDto;
import com.rfid.circularlabs_rfid_backend.scan.response.TestProductDetailResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.rfid.circularlabs_rfid_backend.process.domain.QSupplierOrder.supplierOrder;
import static com.rfid.circularlabs_rfid_backend.product.domain.QProductDetail.productDetail;
import static com.rfid.circularlabs_rfid_backend.product.domain.QProductDetailHistory.productDetailHistory;
import static com.rfid.circularlabs_rfid_backend.scan.domain.QRfidScanHistory.rfidScanHistory;
import static com.rfid.circularlabs_rfid_backend.product.domain.QProduct.product;

@Slf4j
@RequiredArgsConstructor
@Component
public class ScanDataQueryDataV3 {

    private final JPAQueryFactory jpaQueryFactory;
    private final EntityManager entityManager;
    //private final RedisTemplate<String, Object> redisTemplate;
    private final ProductDetailRepository productDetailRepository;


    public ProductDetailscanResponseDto updateProductDetail(SendProductCode productCodes, String supplierCode, String clientCode) {
        // 0 -- 초기 1 -- 업데이트 3 -- 재등록
        LocalDateTime now = LocalDateTime.now();
        ProductDetail result = jpaQueryFactory
                .select(productDetail)
                .from(productDetail)
                .where(productDetail.productSerialCode.eq(productCodes.getProductSerialCode()))
                .fetchOne();

        if (result == null) {
            ProductDetailscanResponseDto dto = ProductDetailscanResponseDto.builder()
                    .rfidChipCode(productCodes.getRfidChipCode())
                    .productSerialCode(productCodes.getProductSerialCode())
                    .productCode(productCodes.getProductCode())
                    .supplierCode(supplierCode)
                    .clientCode(clientCode)
                    .status("입고")
                    .cycle(0)
                    .latestReadingAt(LocalDateTime.now().toString())
                    .dataState(0)
                    .build();

            return dto;
        } else {

            if (result.getStatus().equals("입고") && now.isBefore(result.getLatestReadingAt().plusHours(1))) {
                ProductDetailscanResponseDto dto = ProductDetailscanResponseDto.builder()
                        .rfidChipCode(result.getRfidChipCode())
                        .productSerialCode(result.getProductSerialCode())
                        .productCode(result.getProductCode())
                        .supplierCode(supplierCode)
                        .clientCode(clientCode)
                        .status("입고")
                        .cycle(result.getCycle())
                        .latestReadingAt(result.getLatestReadingAt().toString())
                        .dataState(3)
                        .build();
                return dto;
            } else if (!result.getProductSerialCode().isEmpty()) {
                jpaQueryFactory
                        .update(productDetail)
                        .set(productDetail.status, "입고")
                        .set(productDetail.latestReadingAt, LocalDateTime.now())
                        .set(productDetail.clientCode, clientCode)
                        .set(productDetail.supplierCode, supplierCode)
                        .where(productDetail.productSerialCode.eq(productCodes.getProductSerialCode()))
                        .execute();


                entityManager.flush();
                entityManager.clear();
                ProductDetailscanResponseDto dto = ProductDetailscanResponseDto.builder()
                        .rfidChipCode(result.getRfidChipCode())
                        .productSerialCode(result.getProductSerialCode())
                        .productCode(result.getProductCode())
                        .supplierCode(supplierCode)
                        .clientCode(clientCode)
                        .status("입고")
                        .cycle(result.getCycle())
                        .latestReadingAt(result.getLatestReadingAt().toString())
                        .dataState(1)
                        .build();
                return dto;

            }
        }
        return null;
    }

    // 데이터 스캔 후 작업 처리 시 기존에 RFID 기기 스캔 이력 정보가 존재했는지 확인하기 위한 함수
    public Integer selectLastProductInfo(String productCode, String supplierCode) {

        return jpaQueryFactory
                .select(supplierOrder.orderMount.sum())
                .from(supplierOrder)
                .where(supplierOrder.classificationCode.eq(supplierCode).and(supplierOrder.productCode.eq(productCode)))
                .fetchOne();
    }

    public boolean correctProduct(String productCode, String supplierCode) {

        List<Product> existProducts = jpaQueryFactory
                .selectFrom(product)
                .where(product.productCode.eq(productCode))
                .fetch();

        if (existProducts != null && !existProducts.isEmpty()) {

            List<SupplierOrder> existSupplierOrderList = jpaQueryFactory
                    .selectFrom(supplierOrder)
                    .where(supplierOrder.classificationCode.eq(supplierCode)
                            .and(supplierOrder.productCode.eq(productCode)))
                    .fetch();

            if (existSupplierOrderList != null && !existSupplierOrderList.isEmpty()) {
                return true;
            } else {
                return false;
            }
        }

        return false;
    }


    @Transactional
    public List<ProductDetailscanResponseDto> updateTurnBackProductDetail(List<SendProductCode> scanTurnBackDataList, String supplierCode) {
        // 0 -- 초기 1 -- 업데이트 3 -- 재등록
        LocalDateTime now = LocalDateTime.now();

        List<ProductDetail> needToSaveProductDetail = new ArrayList<>();

        List<ProductDetailscanResponseDto> responseProductDetails = scanTurnBackDataList.stream()
                .map(eachScanData -> {

                    ProductDetail result = jpaQueryFactory
                            .select(productDetail)
                            .from(productDetail)
                            .where(productDetail.productSerialCode.eq(eachScanData.getProductSerialCode()))
                            .orderBy(productDetail.createdAt.desc())
                            .limit(1)
                            .fetchOne();

                    if (result == null) {

                        ProductDetail createProductDetail = ProductDetail.builder()
                                .rfidChipCode(eachScanData.getRfidChipCode())
                                .productSerialCode(eachScanData.getProductSerialCode())
                                .productCode(eachScanData.getProductCode())
                                .supplierCode(supplierCode)
                                .clientCode("null")
                                .status("회수")
                                .cycle(0)
                                .latestReadingAt(LocalDateTime.now())
                                .build();

                        needToSaveProductDetail.add(createProductDetail);

                        return ProductDetailscanResponseDto.builder()
                                .rfidChipCode(eachScanData.getRfidChipCode())
                                .productSerialCode(eachScanData.getProductSerialCode())
                                .productCode(eachScanData.getProductCode())
                                .supplierCode(supplierCode)
                                .clientCode("null")
                                .status("회수")
                                .cycle(0)
                                .latestReadingAt(createProductDetail.getLatestReadingAt().toString())
                                .dataState(0)
                                .build();

                    } else {
                        if (result.getStatus().equals("회수") && now.isBefore(result.getLatestReadingAt().plusHours(1))) {

                            return ProductDetailscanResponseDto.builder()
                                    .rfidChipCode(result.getRfidChipCode())
                                    .productSerialCode(result.getProductSerialCode())
                                    .productCode(result.getProductCode())
                                    .supplierCode(result.getSupplierCode())
                                    .clientCode(result.getClientCode())
                                    .status("회수")
                                    .cycle(result.getCycle())
                                    .latestReadingAt(result.getLatestReadingAt().toString())
                                    .dataState(3)
                                    .build();

                        } else {

                            jpaQueryFactory
                                    .update(productDetail)
                                    .set(productDetail.status, "회수")
                                    .set(productDetail.latestReadingAt, LocalDateTime.now())
                                    .set(productDetail.supplierCode, supplierCode)
                                    .set(productDetail.cycle, result.getCycle() + 1)
                                    .where(productDetail.productSerialCode.eq(result.getProductSerialCode()))
                                    .execute();

                            entityManager.flush();
                            entityManager.clear();

                            return ProductDetailscanResponseDto.builder()
                                    .rfidChipCode(result.getRfidChipCode())
                                    .productSerialCode(result.getProductSerialCode())
                                    .productCode(result.getProductCode())
                                    .supplierCode(result.getSupplierCode())
                                    .clientCode(result.getClientCode())
                                    .status("회수")
                                    .cycle(result.getCycle())
                                    .latestReadingAt(result.getLatestReadingAt().toString())
                                    .dataState(1)
                                    .build();

                        }
                    }

                })
                .collect(Collectors.toList());

        if (!needToSaveProductDetail.isEmpty()) {
            productDetailRepository.saveAll(needToSaveProductDetail);
        }

        return responseProductDetails;
    }


    @Transactional
    public void batchUpdateProductDetail(ProductDetail prevProductDetail) {
        jpaQueryFactory
                .update(productDetail)
                .set(productDetail.status, "회수")
                .set(productDetail.latestReadingAt, LocalDateTime.now())
                .set(productDetail.cycle, prevProductDetail.getCycle() + 1)
                .where(productDetail.productSerialCode.eq(prevProductDetail.getProductSerialCode()))
                .execute();

        entityManager.flush();
        entityManager.clear();
    }


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
    public Boolean checkSameCycleScanHistory(String productCode, String clientCode, String supplierCode, int productDetailCycle, String status) {

        ProductDetailHistory checkOneProductDetailHistory = jpaQueryFactory
                .selectFrom(productDetailHistory)
                .where(productDetailHistory.productCode.eq(productCode)
                        .and(productDetailHistory.clientCode.eq(clientCode))
                        .and(productDetailHistory.supplierCode.eq(supplierCode))
                        .and(productDetailHistory.status.eq(status))
                        .and(productDetailHistory.cycle.eq(productDetailCycle)))
                .limit(1)
                .fetchOne();

        if (checkOneProductDetailHistory == null) {
            return true;
        }

        return false;
    }


    // 잘못된 출고 상태 처리로 인해 바로 다시 재차 스캔하여 상태 처리 했을 경우를 확인하기 위한 이력 조회
    @Transactional
    public boolean asapScanForPrevHistory(String productCode, String clientCode, String supplierCode, int cycle, int statusCount) throws ParseException {

        RfidScanHistory updateScanHistory = jpaQueryFactory
                .selectFrom(rfidScanHistory)
                .where(rfidScanHistory.productCode.eq(productCode)
                        .and(rfidScanHistory.clientCode.eq(clientCode))
                        .and(rfidScanHistory.supplierCode.eq(supplierCode))
                        //.and(rfidScanHistory.cycle.eq(cycle))
                        .and(rfidScanHistory.status.eq("출고")))
                .orderBy(rfidScanHistory.latestReadingAt.desc())
                .limit(1)
                .fetchOne();

        assert updateScanHistory != null;

        if (updateScanHistory.getCreatedAt().isBefore(LocalDateTime.now()) &&
                updateScanHistory.getCreatedAt().plusHours(12L).isAfter(LocalDateTime.now())) {
            log.info("기존에 생성된 날짜 + 시각 : {}", updateScanHistory.getCreatedAt());
            log.info("12시간을 더한 날짜 + 시각 : {}", updateScanHistory.getCreatedAt().plusHours(12L));

            jpaQueryFactory
                    .update(rfidScanHistory)
                    .set(rfidScanHistory.statusCount, statusCount)
                    .where(rfidScanHistory.productCode.eq(productCode)
                            .and(rfidScanHistory.clientCode.eq(clientCode))
                            .and(rfidScanHistory.supplierCode.eq(supplierCode))
                            //.and(rfidScanHistory.cycle.eq(cycle))
                            .and(rfidScanHistory.status.eq("출고")))
                    .execute();

            entityManager.flush();
            entityManager.clear();

            return true;
        } else {
            return false;
        }

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

        RfidScanHistory updateScanHistory = jpaQueryFactory
                .selectFrom(rfidScanHistory)
                .where(rfidScanHistory.productCode.eq(productCode)
                        .and(rfidScanHistory.clientCode.eq(clientCode))
                        .and(rfidScanHistory.supplierCode.eq(supplierCode))
                        //.and(rfidScanHistory.cycle.eq(cycle))
                        .and(rfidScanHistory.status.eq("입고")))
                .orderBy(rfidScanHistory.latestReadingAt.desc())
                .limit(1)
                .fetchOne();

        assert updateScanHistory != null;

        if (updateScanHistory.getCreatedAt().isBefore(LocalDateTime.now()) &&
                updateScanHistory.getCreatedAt().plusHours(12L).isAfter(LocalDateTime.now())) {
            log.info("기존에 생성된 날짜 + 시각 : {}", updateScanHistory.getCreatedAt());
            log.info("12시간을 더한 날짜 + 시각 : {}", updateScanHistory.getCreatedAt().plusHours(12L));

            jpaQueryFactory
                    .update(rfidScanHistory)
                    .set(rfidScanHistory.statusCount, statusCount)
                    .where(rfidScanHistory.productCode.eq(productCode)
                            .and(rfidScanHistory.clientCode.eq(clientCode))
                            .and(rfidScanHistory.supplierCode.eq(supplierCode))
                            //.and(rfidScanHistory.cycle.eq(cycle))
                            .and(rfidScanHistory.status.eq("입고")))
                    .execute();

            entityManager.flush();
            entityManager.clear();

            return true;
        } else {
            return false;
        }
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
    public boolean asapScanForPrevTurnBackHistory(String productCode, String clientCode, String supplierCode, int cycle, int statusCount) {

        RfidScanHistory updateScanHistory = jpaQueryFactory
                .selectFrom(rfidScanHistory)
                .where(rfidScanHistory.productCode.eq(productCode)
                        .and(rfidScanHistory.clientCode.eq(clientCode))
                        .and(rfidScanHistory.supplierCode.eq(supplierCode))
                        //.and(rfidScanHistory.cycle.eq(cycle))
                        .and(rfidScanHistory.status.eq("회수")))
                .orderBy(rfidScanHistory.latestReadingAt.desc())
                .limit(1)
                .fetchOne();

        assert updateScanHistory != null;

        if (updateScanHistory.getCreatedAt().isBefore(LocalDateTime.now()) &&
                updateScanHistory.getCreatedAt().plusHours(12L).isAfter(LocalDateTime.now())) {
            log.info("기존에 생성된 날짜 + 시각 : {}", updateScanHistory.getCreatedAt());
            log.info("12시간을 더한 날짜 + 시각 : {}", updateScanHistory.getCreatedAt().plusHours(12L));

            jpaQueryFactory
                    .update(rfidScanHistory)
                    .set(rfidScanHistory.statusCount, statusCount)
                    .where(rfidScanHistory.productCode.eq(productCode)
                            .and(rfidScanHistory.clientCode.eq(clientCode))
                            .and(rfidScanHistory.supplierCode.eq(supplierCode))
                            //.and(rfidScanHistory.cycle.eq(cycle))
                            .and(rfidScanHistory.status.eq("회수")))
                    .execute();

            entityManager.flush();
            entityManager.clear();

            return true;
        } else {
            return false;
        }
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
    public boolean asapScanForPrevCleanHistory(String productCode, String clientCode, String supplierCode, int cycle, int statusCount) {

        RfidScanHistory updateScanHistory = jpaQueryFactory
                .selectFrom(rfidScanHistory)
                .where(rfidScanHistory.productCode.eq(productCode)
                        .and(rfidScanHistory.clientCode.eq(clientCode))
                        .and(rfidScanHistory.supplierCode.eq(supplierCode))
                        //.and(rfidScanHistory.cycle.eq(cycle))
                        .and(rfidScanHistory.status.eq("세척")))
                .orderBy(rfidScanHistory.latestReadingAt.desc())
                .limit(1)
                .fetchOne();

        assert updateScanHistory != null;

        if (updateScanHistory.getCreatedAt().isBefore(LocalDateTime.now()) &&
                updateScanHistory.getCreatedAt().plusHours(12L).isAfter(LocalDateTime.now())) {
            log.info("기존에 생성된 날짜 + 시각 : {}", updateScanHistory.getCreatedAt());
            log.info("12시간을 더한 날짜 + 시각 : {}", updateScanHistory.getCreatedAt().plusHours(12L));

            jpaQueryFactory
                    .update(rfidScanHistory)
                    .set(rfidScanHistory.statusCount, statusCount)
                    .where(rfidScanHistory.productCode.eq(productCode)
                            .and(rfidScanHistory.clientCode.eq(clientCode))
                            .and(rfidScanHistory.supplierCode.eq(supplierCode))
                            //.and(rfidScanHistory.cycle.eq(cycle))
                            .and(rfidScanHistory.status.eq("세척")))
                    .execute();

            entityManager.flush();
            entityManager.clear();

            return true;
        } else {
            return false;
        }
    }

    // 테스트 제품 이력 조회

    public List<TestProductDetailResponseDto> testGetProductDetails(Long rfidHistoryId) {

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
