package com.rfid.circularlabs_rfid_backend.share;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.rfid.circularlabs_rfid_backend.configuration.BatchParallelConfig;
import com.rfid.circularlabs_rfid_backend.process.repository.DiscardHistoryRepository;
import com.rfid.circularlabs_rfid_backend.product.domain.ProductDetail;
import com.rfid.circularlabs_rfid_backend.product.domain.ProductDetailHistory;
import com.rfid.circularlabs_rfid_backend.product.repository.ProductDetailHistoryRepository;
import com.rfid.circularlabs_rfid_backend.product.repository.ProductDetailRepository;
import com.rfid.circularlabs_rfid_backend.product.response.ProductDetailResponseDto;
import com.rfid.circularlabs_rfid_backend.query.clientorder.ClientOrderQueryData;
import com.rfid.circularlabs_rfid_backend.query.discardhistory.DiscardHistoryQueryData;
import com.rfid.circularlabs_rfid_backend.query.productdetail.ProductDetailQueryData;
import com.rfid.circularlabs_rfid_backend.query.productdetailhistory.ProductDetailHistoryQueryData;
import com.rfid.circularlabs_rfid_backend.query.scandata.ScanDataQueryData;
import com.rfid.circularlabs_rfid_backend.query.supplierorder.SupplierOrderQueryData;
import com.rfid.circularlabs_rfid_backend.query2.clientorder.ClientOrderQueryDataV2;
import com.rfid.circularlabs_rfid_backend.query2.discardhistory.DiscardHistoryQueryDataV2;
import com.rfid.circularlabs_rfid_backend.query2.productdetail.ProductDetailQueryDataV2;
import com.rfid.circularlabs_rfid_backend.query2.productdetailhistory.ProductDetailHistoryQueryDataV2;
import com.rfid.circularlabs_rfid_backend.query2.scandata.ScanDataQueryDataV2;
import com.rfid.circularlabs_rfid_backend.query2.supplierorder.SupplierOrderQueryDataV2;
import com.rfid.circularlabs_rfid_backend.scan.domain.RfidScanHistory;
import com.rfid.circularlabs_rfid_backend.scan.repository.RfidScanHistoryRepository;
import com.rfid.circularlabs_rfid_backend.scan.request.*;
import com.rfid.circularlabs_rfid_backend.scan.response.RfidScanDataInResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
//import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.rfid.circularlabs_rfid_backend.scan.domain.QRfidScanHistory.rfidScanHistory;

@Slf4j
@RequiredArgsConstructor
public class TestCode {

    private final RfidScanHistoryRepository rfidScanHistoryRepository;
    private final ProductDetailRepository productDetailRepository;
    private final ProductDetailHistoryRepository productDetailHistoryRepository;
    private final DiscardHistoryRepository discardHistoryRepository;
    private final ProductDetailQueryDataV2 productDetailQueryDataV2;
    private final EntityManager entityManager;
    private final ScanDataQueryDataV2 scanDataQueryDataV2;
    private final SupplierOrderQueryDataV2 supplierOrderQueryDataV2;
    private final ClientOrderQueryDataV2 clientOrderQueryDataV2;
    private final DiscardHistoryQueryDataV2 discardHistoryQueryDataV2;
    private final ProductDetailHistoryQueryDataV2 productDetailHistoryQueryDataV2;
    private final BatchParallelConfig batchParallelConfig;


    public synchronized CompletableFuture<String> sendOutData(RfidScanDataOutRequestDto sendOutDatas) throws InterruptedException, JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        log.info("제품 출고 처리 service v2");

        // 앱에서 넘겨받은 데이터들을 변수에 저장하여 고정
        String deviceCode = sendOutDatas.getMachineId(); // 기기 코드
        String clientCode = sendOutDatas.getSelectClientCode(); // 고객사 코드
        String supplierCode = sendOutDatas.getSupplierCode(); // 공급사 코드
        List<SendProductCode> receiveProductCodes = sendOutDatas.getProductCodes(); // 스캔한 제품 데이터들의 제품 분류 코드와 각 제품의 시리얼 코드 리스트
        List<SendOrderCount> eachProductCount = sendOutDatas.getEachProductCount(); // 각 제품의 출고 제품 수량

        // 실제 RfidScanHistory 엔티티에 같이 매핑되어 저장될 ProductDetail 엔티티 리스트 객체 생성
        List<ProductDetailHistory> rfidHistoryMappingProductDetailHistorties = new ArrayList<>();

        // 출고 스캔한 데이터들을 필터링 코드와 폐기 여부 조건을 거쳐 재정립 하여 리스트화
        List<SendProductCode> scanOutDatas = receiveProductCodes.stream()
                .filter(scanOutData -> scanOutData.getFilteringCode().contains("CCA2310") && !discardHistoryQueryDataV2.checkProductDiscard(scanOutData.getProductCode(), scanOutData.getProductSerialCode()))
                .collect(Collectors.toList());

        // 1. ProductDetail 처리
        // ProductDetail 처리 함수로 보내 정제된 ProductDetail 리스트 반환
        List<ProductDetail> productDetails = batchParallelConfig.launchProductDetail("출고", scanOutDatas, supplierCode, clientCode);
        //List<ProductDetail> productDetails = scanOutDetail(scanOutDatas, supplierCode, clientCode);

        // RfidScanHistory 처리 이전에 실제로 저장될 중복제거된 ProductDetail 일부 정보를 리스트화
        List<String> distinctProductDetailClientProduct = productDetails.stream()
                .map(eachCategoryProductDetail -> eachCategoryProductDetail.getClientCode() + ":" + eachCategoryProductDetail.getProductCode())
                .distinct()
                .collect(Collectors.toList());

        // RfidScanHistory 일괄 저장을 위한 리스트
        List<RfidScanHistory> saveRfidScanHistoryList = new ArrayList<>();

        // 2. RfidScanHistory 처리
        // 중복 제거된 RroductDetail 일부 정보 리스트 기준으로 RfidSCanHistory 처리
        distinctProductDetailClientProduct.forEach(eachClientProductProductDetail -> {

            // : 기호 기준으로 ClientCode 와 ProductCode 정보를 분리
            String[] separateClientCodeAndProductCode = eachClientProductProductDetail.split(":");
            String separateClientCode = separateClientCodeAndProductCode[0]; // ClientCode
            String separateProductCode = separateClientCodeAndProductCode[1]; // ProductCode

            long unexpectedDicardProductCount = receiveProductCodes.stream()
                    .filter(unExpectedDiscardData -> unExpectedDiscardData.getProductCode().equals(separateProductCode) && discardHistoryQueryDataV2.checkProductDiscard(unExpectedDiscardData.getProductCode(), unExpectedDiscardData.getProductSerialCode()))
                    .count();

            // 앞에서 처리된 ProductDetail들 중 분리한 ClientCode와 ProductCode와 일치한 스캔 수량 계산
            long dataStatusCount = productDetails.stream()
                    .filter(data -> data.getClientCode().equals(separateClientCode) && data.getProductCode().equals(separateProductCode))
                    .count();

            // SendOrderCount 객체에 저장
            SendOrderCount orderProduct = SendOrderCount.builder()
                    .product(separateProductCode)
                    .orderCount(Math.toIntExact(dataStatusCount))
                    .build();

            // RfidScanHIstory를 처리하기 위해 필요한 특정 ProductDetail 한 개 추출
            ProductDetail eachProductDetail = productDetails.stream()
                    .filter(findProductDetail -> findProductDetail.getProductCode().equals(separateProductCode) && findProductDetail.getClientCode().equals(separateClientCode))
                    .findFirst()
                    .get();

            try {
                // RfidScanHistory 처리 후 반환
                RfidScanHistory saveRfidScanHistory = scanOutRfidScanHistory(orderProduct, unexpectedDicardProductCount, eachProductDetail, deviceCode, rfidHistoryMappingProductDetailHistorties);

                // 일괄 처리를 위해 리스트에 저장
                if (saveRfidScanHistory != null) {
                    rfidScanHistoryRepository.save(saveRfidScanHistory);
                    saveRfidScanHistoryList.add(saveRfidScanHistory);
                }
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }

        });

        // 3. ProductDetailHistory 처리
        //scanOutProductDetailHistory(saveRfidScanHistoryList, productDetails);
        batchParallelConfig.launchProductDetailHistory("출고", saveRfidScanHistoryList, productDetails);

        log.info("출고 처리 완료");

        return new AsyncResult<>("N").completable();
    }

    // 스캔 데이터 출고 상세 처리 로직 함수
    @Async("threadPoolTaskExecutor")
    public List<ProductDetail> scanOutDetail(
            List<SendProductCode> scanOutDatas,
            String supplierCode,
            String clientCode) {

        List<ProductDetail> finalProductDetails = new ArrayList<>();

        // 기존에 존재하는 Product 데이터들
        List<ProductDetail> existPrevProductDetails = scanOutDatas.stream()
                .filter(eachExistPrevOutData -> productDetailQueryDataV2.checkRemainProductDetail(
                        eachExistPrevOutData.getProductSerialCode(),
                        eachExistPrevOutData.getProductCode(), supplierCode, clientCode) != null)
                .map(eachNullOutData -> productDetailQueryDataV2.checkRemainProductDetail(
                        eachNullOutData.getProductSerialCode(),
                        eachNullOutData.getProductCode(), supplierCode, clientCode))
                .collect(Collectors.toList());

        if (!existPrevProductDetails.isEmpty()) {
            existPrevProductDetails.forEach(eachUpdateScanData ->
                    finalProductDetails.add(productDetailQueryDataV2.scanOutUpdateStatusAndReadingAt(eachUpdateScanData)));

            entityManager.flush();
            entityManager.clear();
        }

        // 처음 출고 처리되어 지금 상태는 null인 데이터들
        List<ProductDetail> nullProductDetails = scanOutDatas.stream()
                .filter(eachNullOutData -> productDetailQueryDataV2.checkRemainProductDetail(
                        eachNullOutData.getProductSerialCode(),
                        eachNullOutData.getProductCode(), supplierCode, clientCode) == null)
                .map(mappingNullFieldData ->
                        ProductDetail.builder()
                                .rfidChipCode(mappingNullFieldData.getRfidChipCode())
                                .productSerialCode(mappingNullFieldData.getProductSerialCode())
                                .productCode(mappingNullFieldData.getProductCode())
                                .supplierCode(supplierCode)
                                .clientCode(clientCode)
                                .status("출고")
                                .cycle(0)
                                .latestReadingAt(LocalDateTime.now())
                                .build())
                .collect(Collectors.toList());

        // 만약 처음 들어오는 데이터들이라면 저장 처리
        if (!nullProductDetails.isEmpty()) {
            finalProductDetails.addAll(productDetailRepository.saveAll(nullProductDetails));
        }

        return finalProductDetails;
    }


    // 출고 처리 시 RfidScanHistory 생성 상세 로직
    @Async("threadPoolTaskExecutor")
    protected RfidScanHistory scanOutRfidScanHistory(
            SendOrderCount eachProductCount,
            long unexpectedDicardProductCount,
            ProductDetail productDetail,
            String deviceCode,
            List<ProductDetailHistory> rfidHistoryMappingProductDetailHistorties) throws ParseException {

        // 기존에 동일한 기기로 동일한 공급사가 동일한 고객사에 동일한 제품을 출고 시킨 RFID 스캔 이력이 존재하지 않으면 이력을 저장
        if (scanDataQueryDataV2.checkSameCycleScanHistory(productDetail.getProductCode(), productDetail.getClientCode(), productDetail.getSupplierCode(), productDetail.getCycle(), "출고")) {
            log.info("RfidScanHistory에 출고 이력을 저장하기 위한 로직 접근");

            // 상태 요청 수량 변수
            int statusCount = 0;
            int discardCallCount = (int) unexpectedDicardProductCount;

            // 요청 수량 추출 후 변수에 저장
            if (eachProductCount.getProduct().equals(productDetail.getProductCode())) {
                // 상태 요청 수량 변수에 해당 제품군의 총 요청 수량에 폐기 수량을 빼서 적용 후 탈출
                statusCount = eachProductCount.getOrderCount();
            }


            // 만약 폐기 제품이 있을 경우 폐기 수량 저장 변수
            int discardMount = discardHistoryQueryDataV2.getDiscardHistory(productDetail.getSupplierCode(), productDetail.getProductCode());
            // 직전 가장 최신 스캔 이력 조회
            RfidScanHistory latestScanHistory = scanDataQueryDataV2.getLatestRfidScanHistory(productDetail.getProductCode(), productDetail.getSupplierCode());
            // 총 재고량
            int totalRemainCount = supplierOrderQueryDataV2.getTotalRemainCount(productDetail.getSupplierCode(), productDetail.getProductCode()) - discardMount;

            // 만약 공급사 측에서 추가 물량을 주문했을 경우 재고 추가량 변수
            int plusMount = 0;

            // 유동 재고량을 위해 뺼셈 계산이 적용되어야할 유동 재고량 전용 폐기 수량
            int minusCount = 0;

            // 만약 해당 제품군의 총 폐기 수량과 현재 스캔한 제품 데이터들에 혹시라도 들어가있는 폐기 물품의 수량을 뺏을 때 0이 아닐 경우
            if (discardMount - discardCallCount != 0) {
                // 거기에 현재 스캔한 제품 데이터들에 혹시라도 들어가있는 폐기 물품의 수량이 하나라도 존재할 경우 유동 재고량 계산을 위해
                // 현재 빼야할 폐기 수량을 계산하여 minusCount 변수에 저장
                if (discardCallCount != 0) {
                    minusCount = discardMount - discardCallCount + discardCallCount;
                }
            }

            RfidScanHistory rfidScanHistory;

            // 첫 스캔 이력이 아닐 경우
            if (latestScanHistory != null) {
                log.info("기존 이력 존재 시 진입");

                // 추가 재고량 추출 후 변수 저장
                if (totalRemainCount - latestScanHistory.getTotalRemainQuantity() != 0) {
                    plusMount = totalRemainCount - latestScanHistory.getTotalRemainQuantity();
                }

                int flowRemainQuantity = 0;

                if (latestScanHistory.getStatus().equals("출고")) {
                    flowRemainQuantity = latestScanHistory.getFlowRemainQuantity() + plusMount;
                } else {
                    flowRemainQuantity = latestScanHistory.getFlowRemainQuantity() + plusMount - minusCount;
                }

                // RFID 기기 스캔 이력 정보
                rfidScanHistory = RfidScanHistory.builder()
                        .deviceCode(deviceCode)
                        .rfidChipCode(productDetail.getRfidChipCode())
                        .productCode(productDetail.getProductCode())
                        .supplierCode(productDetail.getSupplierCode())
                        .clientCode(productDetail.getClientCode())
                        .status(productDetail.getStatus())
                        .statusCount(statusCount) // 출고 처리 데이터 수량
                        .flowRemainQuantity(flowRemainQuantity) // 유동 재고 수량
                        .noReturnQuantity(latestScanHistory.getNoReturnQuantity()) // 미회수 분 수량
                        .totalRemainQuantity(totalRemainCount) // 해당 공급사의 총 재고량
                        .latestReadingAt(LocalDateTime.now())
                        .productDetailHistories(rfidHistoryMappingProductDetailHistorties)
                        .build();

            } else { // 첫 스캔 이력으로 들어오는 경우
                log.info("기존 이력이 존재하지 않는 첫 이력일 경우 진입");

                // RFID 기기 스캔 이력 정보
                rfidScanHistory = RfidScanHistory.builder()
                        .deviceCode(deviceCode)
                        .rfidChipCode(productDetail.getRfidChipCode())
                        .productCode(productDetail.getProductCode())
                        .supplierCode(productDetail.getSupplierCode())
                        .clientCode(productDetail.getClientCode())
                        .status(productDetail.getStatus())
                        .statusCount(statusCount)
                        .flowRemainQuantity(totalRemainCount)
                        .noReturnQuantity(0)
                        .totalRemainQuantity(totalRemainCount)
                        .latestReadingAt(LocalDateTime.now())
                        .productDetailHistories(rfidHistoryMappingProductDetailHistorties)
                        .build();
            }

            return rfidScanHistory;
        } else {
            log.info("RfidScanHistory에 이미 동일한 내용의 출고 이력이 존재할 경우 접근");

            // 상태 요청 수량 변수
            int statusCount = 0;

            // 요청 수량 추출 후 변수에 저장
            if (eachProductCount.getProduct().equals(productDetail.getProductCode())) {
                // 상태 요청 수량 변수에 해당 제품군의 총 요청 수량에 폐기 수량을 빼서 적용 후 탈출
                statusCount = eachProductCount.getOrderCount();
            }

            // 잘못된 상태 처리로 인해 바로 다시 재차 스캔하여 상태 처리 했을 경우를 확인하기 위한 이력 조회
            if (scanDataQueryDataV2.asapScanForPrevHistory(productDetail.getProductCode(), productDetail.getClientCode(), productDetail.getSupplierCode(), productDetail.getCycle(), statusCount)) {
                log.info("즉시 바로 재차 출고 처리 및 하루 지나기 이전에 재차 출고 처리 수정");
            } else {
                log.info("시간이 지나 재차 출고 처리 불가");
            }
        }

        log.info("RFICSCANHISTORY 처리 함수에서 확인 : PRODUCT_CODE - {} / COUNT - {}", eachProductCount.getProduct(), eachProductCount.getOrderCount());

        return null;
    }


    // 출고 처리 시 ProductDetailHistory 생성 상세 로직
    @Async("threadPoolTaskExecutor")
    public void scanOutProductDetailHistory(List<RfidScanHistory> rfidScanHistoryList, List<ProductDetail> useProductDetailList) {
        log.info("대조시킬 RFID 스캔 이력 저장 리스트 확인 : {}", rfidScanHistoryList.size());

        for (RfidScanHistory eachScanHistory : rfidScanHistoryList) {
            List<ProductDetail> eachCategoryProducts = useProductDetailList.stream()
                    .filter(info -> info.getProductCode().equals(eachScanHistory.getProductCode()) &&
                            info.getClientCode().equals(eachScanHistory.getClientCode()) &&
                            info.getSupplierCode().equals(eachScanHistory.getSupplierCode()) &&
                            productDetailHistoryQueryDataV2.checkPrevProductDetailHistoryCategory2(info))
                    .collect(Collectors.toList());

            if (!eachCategoryProducts.isEmpty()) {
                List<ProductDetailHistory> saveEachCategoryProductDetailHistories = new ArrayList<>();

                eachCategoryProducts.forEach(eachProductDetail -> {
                    // 각 제품 스캔 상세 이력
                    ProductDetailHistory productDetailHistory = ProductDetailHistory.builder()
                            .rfidChipCode(eachProductDetail.getRfidChipCode())
                            .productSerialCode(eachProductDetail.getProductSerialCode())
                            .productCode(eachProductDetail.getProductCode())
                            .supplierCode(eachProductDetail.getSupplierCode())
                            .clientCode(eachProductDetail.getClientCode())
                            .status("출고")
                            .cycle(eachProductDetail.getCycle())
                            .latestReadingAt(LocalDateTime.now())
                            .rfidScanHistory(eachScanHistory)
                            .build();

                    saveEachCategoryProductDetailHistories.add(productDetailHistory);
                });

                // ProductDetailHistory 일괄 저장
                productDetailHistoryRepository.saveAll(saveEachCategoryProductDetailHistories);
            }
        }
    }
}
