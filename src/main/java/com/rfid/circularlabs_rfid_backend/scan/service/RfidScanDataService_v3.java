package com.rfid.circularlabs_rfid_backend.scan.service;

import com.rfid.circularlabs_rfid_backend.configuration.BatchParallelConfig;
import com.rfid.circularlabs_rfid_backend.process.domain.DiscardHistory;
import com.rfid.circularlabs_rfid_backend.process.repository.DiscardHistoryRepository;
import com.rfid.circularlabs_rfid_backend.product.domain.ProductDetail;
import com.rfid.circularlabs_rfid_backend.product.domain.ProductDetailHistory;
import com.rfid.circularlabs_rfid_backend.product.repository.ProductDetailHistoryRepository;
import com.rfid.circularlabs_rfid_backend.product.repository.ProductDetailRepository;
import com.rfid.circularlabs_rfid_backend.query2.clientorder.ClientOrderQueryDataV2;
import com.rfid.circularlabs_rfid_backend.query2.discardhistory.DiscardHistoryQueryDataV2;
import com.rfid.circularlabs_rfid_backend.query2.productdetail.ProductDetailQueryDataV2;
import com.rfid.circularlabs_rfid_backend.query2.productdetailhistory.ProductDetailHistoryQueryDataV2;
import com.rfid.circularlabs_rfid_backend.query2.scandata.ScanDataQueryDataV2;
import com.rfid.circularlabs_rfid_backend.query2.scandata.ScanDataQueryDataV3;
import com.rfid.circularlabs_rfid_backend.query2.supplierorder.SupplierOrderQueryDataV2;
import com.rfid.circularlabs_rfid_backend.scan.domain.RfidScanHistory;
import com.rfid.circularlabs_rfid_backend.scan.repository.RfidScanHistoryRepository;
import com.rfid.circularlabs_rfid_backend.scan.request.*;
import com.rfid.circularlabs_rfid_backend.scan.response.ProductDetailscanResponseDto;
import com.rfid.circularlabs_rfid_backend.share.BatchService;
import com.rfid.circularlabs_rfid_backend.share.BatchServiceV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.rfid.circularlabs_rfid_backend.product.domain.QProductDetail.productDetail;
import static java.util.stream.Collectors.counting;

@Slf4j
@RequiredArgsConstructor
@Service
public class RfidScanDataService_v3 {

    private final RfidScanHistoryRepository rfidScanHistoryRepository;
    private final ProductDetailRepository productDetailRepository;
    private final ProductDetailHistoryRepository productDetailHistoryRepository;
    private final DiscardHistoryRepository discardHistoryRepository;
    private final ProductDetailQueryDataV2 productDetailQueryDataV2;
    private final EntityManager entityManager;
    private final ScanDataQueryDataV3 scanDataQueryDataV3;
    private final SupplierOrderQueryDataV2 supplierOrderQueryDataV2;
    private final DiscardHistoryQueryDataV2 discardHistoryQueryDataV2;
    private final ProductDetailHistoryQueryDataV2 productDetailHistoryQueryDataV2;
    private final BatchService batchService;
    private final BatchServiceV2 batchServiceV2;


    /**
     * // 스캔 데이터 출고 service v2
     * //@Transactional
     * public synchronized CompletableFuture<String> sendOutData(RfidScanDataOutRequestDto sendOutDatas) throws InterruptedException, JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
     * log.info("제품 출고 처리 service v2");
     * <p>
     * // 앱에서 넘겨받은 데이터들을 변수에 저장하여 고정
     * String deviceCode = sendOutDatas.getMachineId(); // 기기 코드
     * String clientCode = sendOutDatas.getSelectClientCode(); // 고객사 코드
     * String supplierCode = sendOutDatas.getSupplierCode(); // 공급사 코드
     * List<SendProductCode> receiveProductCodes = sendOutDatas.getProductCodes(); // 스캔한 제품 데이터들의 제품 분류 코드와 각 제품의 시리얼 코드 리스트
     * List<SendOrderCount> eachProductCount = sendOutDatas.getEachProductCount(); // 각 제품의 출고 제품 수량
     * <p>
     * // 실제 RfidScanHistory 엔티티에 같이 매핑되어 저장될 ProductDetail 엔티티 리스트 객체 생성
     * List<ProductDetailHistory> rfidHistoryMappingProductDetailHistorties = new ArrayList<>();
     * <p>
     * // 출고 스캔한 데이터들을 필터링 코드와 폐기 여부 조건을 거쳐 재정립 하여 리스트화
     * List<SendProductCode> scanOutDatas = receiveProductCodes.stream()
     * .filter(scanOutData -> scanOutData.getFilteringCode().contains("CCA2310") && !discardHistoryQueryDataV2.checkProductDiscard(scanOutData.getProductCode(), scanOutData.getProductSerialCode()))
     * .collect(Collectors.toList());
     * <p>
     * // 1. ProductDetail 처리
     * // ProductDetail 처리 함수로 보내 정제된 ProductDetail 리스트 반환
     * List<ProductDetail> productDetails = batchService.launchProductDetail("출고", scanOutDatas, supplierCode, clientCode);
     * //List<ProductDetail> productDetails = scanOutDetail(scanOutDatas, supplierCode, clientCode);
     * <p>
     * // RfidScanHistory 처리 이전에 실제로 저장될 중복제거된 ProductDetail 일부 정보를 리스트화
     * List<String> distinctProductDetailClientProduct = productDetails.stream()
     * .map(eachCategoryProductDetail -> eachCategoryProductDetail.getClientCode() + ":" + eachCategoryProductDetail.getProductCode())
     * .distinct()
     * .collect(Collectors.toList());
     * <p>
     * // RfidScanHistory 일괄 저장을 위한 리스트
     * List<RfidScanHistory> saveRfidScanHistoryList = new ArrayList<>();
     * <p>
     * // 2. RfidScanHistory 처리
     * // 중복 제거된 RroductDetail 일부 정보 리스트 기준으로 RfidSCanHistory 처리
     * distinctProductDetailClientProduct.forEach(eachClientProductProductDetail -> {
     * <p>
     * // : 기호 기준으로 ClientCode 와 ProductCode 정보를 분리
     * String[] separateClientCodeAndProductCode = eachClientProductProductDetail.split(":");
     * String separateClientCode = separateClientCodeAndProductCode[0]; // ClientCode
     * String separateProductCode = separateClientCodeAndProductCode[1]; // ProductCode
     * <p>
     * long unexpectedDicardProductCount = receiveProductCodes.stream()
     * .filter(unExpectedDiscardData -> unExpectedDiscardData.getProductCode().equals(separateProductCode) && discardHistoryQueryDataV2.checkProductDiscard(unExpectedDiscardData.getProductCode(), unExpectedDiscardData.getProductSerialCode()))
     * .count();
     * <p>
     * // 앞에서 처리된 ProductDetail들 중 분리한 ClientCode와 ProductCode와 일치한 스캔 수량 계산
     * long dataStatusCount = productDetails.stream()
     * .filter(data -> data.getClientCode().equals(separateClientCode) && data.getProductCode().equals(separateProductCode))
     * .count();
     * <p>
     * // SendOrderCount 객체에 저장
     * SendOrderCount orderProduct = SendOrderCount.builder()
     * .product(separateProductCode)
     * .orderCount(Math.toIntExact(dataStatusCount))
     * .build();
     * <p>
     * // RfidScanHIstory를 처리하기 위해 필요한 특정 ProductDetail 한 개 추출
     * ProductDetail eachProductDetail = productDetails.stream()
     * .filter(findProductDetail -> findProductDetail.getProductCode().equals(separateProductCode) && findProductDetail.getClientCode().equals(separateClientCode))
     * .findFirst()
     * .get();
     * <p>
     * try {
     * // RfidScanHistory 처리 후 반환
     * RfidScanHistory saveRfidScanHistory = scanOutRfidScanHistory(orderProduct, unexpectedDicardProductCount, eachProductDetail, deviceCode, rfidHistoryMappingProductDetailHistorties);
     * <p>
     * // 일괄 처리를 위해 리스트에 저장
     * if (saveRfidScanHistory != null) {
     * rfidScanHistoryRepository.save(saveRfidScanHistory);
     * saveRfidScanHistoryList.add(saveRfidScanHistory);
     * }
     * } catch (ParseException e) {
     * throw new RuntimeException(e);
     * }
     * <p>
     * });
     * <p>
     * // 3. ProductDetailHistory 처리
     * //scanOutProductDetailHistory(saveRfidScanHistoryList, productDetails);
     * batchService.launchProductDetailHistory("출고", saveRfidScanHistoryList, productDetails);
     * <p>
     * log.info("출고 처리 완료");
     * <p>
     * return new AsyncResult<>("N").completable();
     * }
     * <p>
     * // 스캔 데이터 출고 상세 처리 로직 함수
     *
     * @Async("threadPoolTaskExecutor") public List<ProductDetail> scanOutDetail(
     * List<SendProductCode> scanOutDatas,
     * String supplierCode,
     * String clientCode) {
     * <p>
     * List<ProductDetail> finalProductDetails = new ArrayList<>();
     * <p>
     * // 기존에 존재하는 Product 데이터들
     * List<ProductDetail> existPrevProductDetails = scanOutDatas.stream()
     * .filter(eachExistPrevOutData -> productDetailQueryDataV2.checkRemainProductDetail(
     * eachExistPrevOutData.getProductSerialCode(),
     * eachExistPrevOutData.getProductCode(), supplierCode, clientCode) != null)
     * .map(eachNullOutData -> productDetailQueryDataV2.checkRemainProductDetail(
     * eachNullOutData.getProductSerialCode(),
     * eachNullOutData.getProductCode(), supplierCode, clientCode))
     * .collect(Collectors.toList());
     * <p>
     * if (!existPrevProductDetails.isEmpty()) {
     * existPrevProductDetails.forEach(eachUpdateScanData ->
     * finalProductDetails.add(productDetailQueryDataV2.scanOutUpdateStatusAndReadingAt(eachUpdateScanData)));
     * <p>
     * entityManager.flush();
     * entityManager.clear();
     * }
     * <p>
     * // 처음 출고 처리되어 지금 상태는 null인 데이터들
     * List<ProductDetail> nullProductDetails = scanOutDatas.stream()
     * .filter(eachNullOutData -> productDetailQueryDataV2.checkRemainProductDetail(
     * eachNullOutData.getProductSerialCode(),
     * eachNullOutData.getProductCode(), supplierCode, clientCode) == null)
     * .map(mappingNullFieldData ->
     * ProductDetail.builder()
     * .rfidChipCode(mappingNullFieldData.getRfidChipCode())
     * .productSerialCode(mappingNullFieldData.getProductSerialCode())
     * .productCode(mappingNullFieldData.getProductCode())
     * .supplierCode(supplierCode)
     * .clientCode(clientCode)
     * .status("출고")
     * .cycle(0)
     * .latestReadingAt(LocalDateTime.now())
     * .build())
     * .collect(Collectors.toList());
     * <p>
     * // 만약 처음 들어오는 데이터들이라면 저장 처리
     * if (!nullProductDetails.isEmpty()) {
     * finalProductDetails.addAll(productDetailRepository.saveAll(nullProductDetails));
     * }
     * <p>
     * return finalProductDetails;
     * }
     * <p>
     * <p>
     * // 출고 처리 시 RfidScanHistory 생성 상세 로직
     * @Async("threadPoolTaskExecutor") protected RfidScanHistory scanOutRfidScanHistory(
     * SendOrderCount eachProductCount,
     * long unexpectedDicardProductCount,
     * ProductDetail productDetail,
     * String deviceCode,
     * List<ProductDetailHistory> rfidHistoryMappingProductDetailHistorties) throws ParseException {
     * <p>
     * // 기존에 동일한 기기로 동일한 공급사가 동일한 고객사에 동일한 제품을 출고 시킨 RFID 스캔 이력이 존재하지 않으면 이력을 저장
     * if (scanDataQueryDataV3.checkSameCycleScanHistory(productDetail.getProductCode(), productDetail.getClientCode(), productDetail.getSupplierCode(), productDetail.getCycle(), "출고")) {
     * log.info("RfidScanHistory에 출고 이력을 저장하기 위한 로직 접근");
     * <p>
     * // 상태 요청 수량 변수
     * int statusCount = 0;
     * int discardCallCount = (int) unexpectedDicardProductCount;
     * <p>
     * // 요청 수량 추출 후 변수에 저장
     * if (eachProductCount.getProduct().equals(productDetail.getProductCode())) {
     * // 상태 요청 수량 변수에 해당 제품군의 총 요청 수량에 폐기 수량을 빼서 적용 후 탈출
     * statusCount = eachProductCount.getOrderCount();
     * }
     * <p>
     * <p>
     * // 만약 폐기 제품이 있을 경우 폐기 수량 저장 변수
     * int discardMount = discardHistoryQueryDataV2.getDiscardHistory(productDetail.getSupplierCode(), productDetail.getProductCode());
     * // 직전 가장 최신 스캔 이력 조회
     * RfidScanHistory latestScanHistory = scanDataQueryDataV3.getLatestRfidScanHistory(productDetail.getProductCode(), productDetail.getSupplierCode());
     * // 총 재고량
     * int totalRemainCount = supplierOrderQueryDataV2.getTotalRemainCount(productDetail.getSupplierCode(), productDetail.getProductCode()) - discardMount;
     * <p>
     * // 만약 공급사 측에서 추가 물량을 주문했을 경우 재고 추가량 변수
     * int plusMount = 0;
     * <p>
     * // 유동 재고량을 위해 뺼셈 계산이 적용되어야할 유동 재고량 전용 폐기 수량
     * int minusCount = 0;
     * <p>
     * // 만약 해당 제품군의 총 폐기 수량과 현재 스캔한 제품 데이터들에 혹시라도 들어가있는 폐기 물품의 수량을 뺏을 때 0이 아닐 경우
     * if (discardMount - discardCallCount != 0) {
     * // 거기에 현재 스캔한 제품 데이터들에 혹시라도 들어가있는 폐기 물품의 수량이 하나라도 존재할 경우 유동 재고량 계산을 위해
     * // 현재 빼야할 폐기 수량을 계산하여 minusCount 변수에 저장
     * if (discardCallCount != 0) {
     * minusCount = discardMount - discardCallCount + discardCallCount;
     * }
     * }
     * <p>
     * RfidScanHistory rfidScanHistory;
     * <p>
     * // 첫 스캔 이력이 아닐 경우
     * if (latestScanHistory != null) {
     * log.info("기존 이력 존재 시 진입");
     * <p>
     * // 추가 재고량 추출 후 변수 저장
     * if (totalRemainCount - latestScanHistory.getTotalRemainQuantity() != 0) {
     * plusMount = totalRemainCount - latestScanHistory.getTotalRemainQuantity();
     * }
     * <p>
     * int flowRemainQuantity = 0;
     * <p>
     * if (latestScanHistory.getStatus().equals("출고")) {
     * flowRemainQuantity = latestScanHistory.getFlowRemainQuantity() + plusMount;
     * } else {
     * flowRemainQuantity = latestScanHistory.getFlowRemainQuantity() + plusMount - minusCount;
     * }
     * <p>
     * // RFID 기기 스캔 이력 정보
     * rfidScanHistory = RfidScanHistory.builder()
     * .deviceCode(deviceCode)
     * .rfidChipCode(productDetail.getRfidChipCode())
     * .productCode(productDetail.getProductCode())
     * .supplierCode(productDetail.getSupplierCode())
     * .clientCode(productDetail.getClientCode())
     * .status(productDetail.getStatus())
     * .statusCount(statusCount) // 출고 처리 데이터 수량
     * .flowRemainQuantity(flowRemainQuantity) // 유동 재고 수량
     * .noReturnQuantity(latestScanHistory.getNoReturnQuantity()) // 미회수 분 수량
     * .totalRemainQuantity(totalRemainCount) // 해당 공급사의 총 재고량
     * .latestReadingAt(LocalDateTime.now())
     * .productDetailHistories(rfidHistoryMappingProductDetailHistorties)
     * .build();
     * <p>
     * } else { // 첫 스캔 이력으로 들어오는 경우
     * log.info("기존 이력이 존재하지 않는 첫 이력일 경우 진입");
     * <p>
     * // RFID 기기 스캔 이력 정보
     * rfidScanHistory = RfidScanHistory.builder()
     * .deviceCode(deviceCode)
     * .rfidChipCode(productDetail.getRfidChipCode())
     * .productCode(productDetail.getProductCode())
     * .supplierCode(productDetail.getSupplierCode())
     * .clientCode(productDetail.getClientCode())
     * .status(productDetail.getStatus())
     * .statusCount(statusCount)
     * .flowRemainQuantity(totalRemainCount)
     * .noReturnQuantity(0)
     * .totalRemainQuantity(totalRemainCount)
     * .latestReadingAt(LocalDateTime.now())
     * .productDetailHistories(rfidHistoryMappingProductDetailHistorties)
     * .build();
     * }
     * <p>
     * return rfidScanHistory;
     * } else {
     * log.info("RfidScanHistory에 이미 동일한 내용의 출고 이력이 존재할 경우 접근");
     * <p>
     * // 상태 요청 수량 변수
     * int statusCount = 0;
     * <p>
     * // 요청 수량 추출 후 변수에 저장
     * if (eachProductCount.getProduct().equals(productDetail.getProductCode())) {
     * // 상태 요청 수량 변수에 해당 제품군의 총 요청 수량에 폐기 수량을 빼서 적용 후 탈출
     * statusCount = eachProductCount.getOrderCount();
     * }
     * <p>
     * // 잘못된 상태 처리로 인해 바로 다시 재차 스캔하여 상태 처리 했을 경우를 확인하기 위한 이력 조회
     * if (scanDataQueryDataV3.asapScanForPrevHistory(productDetail.getProductCode(), productDetail.getClientCode(), productDetail.getSupplierCode(), productDetail.getCycle(), statusCount)) {
     * log.info("즉시 바로 재차 출고 처리 및 하루 지나기 이전에 재차 출고 처리 수정");
     * } else {
     * log.info("시간이 지나 재차 출고 처리 불가");
     * }
     * }
     * <p>
     * log.info("RFICSCANHISTORY 처리 함수에서 확인 : PRODUCT_CODE - {} / COUNT - {}", eachProductCount.getProduct(), eachProductCount.getOrderCount());
     * <p>
     * return null;
     * }
     * <p>
     * <p>
     * // 출고 처리 시 ProductDetailHistory 생성 상세 로직
     * @Async("threadPoolTaskExecutor") public void scanOutProductDetailHistory(List<RfidScanHistory> rfidScanHistoryList, List<ProductDetail> useProductDetailList) {
     * log.info("대조시킬 RFID 스캔 이력 저장 리스트 확인 : {}", rfidScanHistoryList.size());
     * <p>
     * for (RfidScanHistory eachScanHistory : rfidScanHistoryList) {
     * List<ProductDetail> eachCategoryProducts = useProductDetailList.stream()
     * .filter(info -> info.getProductCode().equals(eachScanHistory.getProductCode()) &&
     * info.getClientCode().equals(eachScanHistory.getClientCode()) &&
     * info.getSupplierCode().equals(eachScanHistory.getSupplierCode()) &&
     * productDetailHistoryQueryDataV2.checkPrevProductDetailHistoryCategory2(info))
     * .collect(Collectors.toList());
     * <p>
     * if (!eachCategoryProducts.isEmpty()) {
     * List<ProductDetailHistory> saveEachCategoryProductDetailHistories = new ArrayList<>();
     * <p>
     * eachCategoryProducts.forEach(eachProductDetail -> {
     * // 각 제품 스캔 상세 이력
     * ProductDetailHistory productDetailHistory = ProductDetailHistory.builder()
     * .rfidChipCode(eachProductDetail.getRfidChipCode())
     * .productSerialCode(eachProductDetail.getProductSerialCode())
     * .productCode(eachProductDetail.getProductCode())
     * .supplierCode(eachProductDetail.getSupplierCode())
     * .clientCode(eachProductDetail.getClientCode())
     * .status("출고")
     * .cycle(eachProductDetail.getCycle())
     * .latestReadingAt(LocalDateTime.now())
     * .rfidScanHistory(eachScanHistory)
     * .build();
     * <p>
     * saveEachCategoryProductDetailHistories.add(productDetailHistory);
     * });
     * <p>
     * // ProductDetailHistory 일괄 저장
     * productDetailHistoryRepository.saveAll(saveEachCategoryProductDetailHistories);
     * }
     * }
     * }
     **/


///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    // 스캔 데이터 입고 service v3
    public synchronized CompletableFuture<String> sendInData(RfidScanDataInRequestDto sendInDatas)
            throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        log.info("제품 입고 처리 service v2");

        String deviceCode = sendInDatas.getMachineId();
        String clientCode = sendInDatas.getSelectClientCode();
        String supplierCode = sendInDatas.getSupplierCode();

        List<SendProductCode> productCodes = sendInDatas.getProductCodes().stream()
                .filter(eachCorrectProduct -> scanDataQueryDataV3.correctProduct(eachCorrectProduct.getProductCode(), supplierCode))
                .collect(Collectors.toList());

        log.info("입고 스캔한 제품 코드들 : {}", productCodes.stream().map(SendProductCode::getProductCode).distinct().collect(Collectors.toList()));

        List<ProductDetailHistory> saveEachCategoryProductDetailHistories = new ArrayList<>();

        HashMap<String, Object> responseProductDetailsInfo = batchService.launchProductDetail("입고", productCodes, supplierCode, clientCode);

        List<ProductDetailscanResponseDto> responseProductDetails = (List<ProductDetailscanResponseDto>) responseProductDetailsInfo.get("totalResponseProductDetails");
        List<Long> updateProductDetailIds = (List<Long>) responseProductDetailsInfo.get("updateProductDetailIds");

        if(updateProductDetailIds != null){
            productDetailQueryDataV2.needUpdateProductDetailStatusIn(updateProductDetailIds);
        }

        responseProductDetails.forEach(eachResponseProductDetail -> {
            if (eachResponseProductDetail.getDataState() == 0) {
                saveEachCategoryProductDetailHistories.add(
                        ProductDetailHistory.builder()
                                .rfidChipCode(eachResponseProductDetail.getRfidChipCode())
                                .productSerialCode(eachResponseProductDetail.getProductSerialCode())
                                .productCode(eachResponseProductDetail.getProductCode())
                                .supplierCode(supplierCode)
                                .clientCode(clientCode)
                                .status("입고")
                                .cycle(0)
                                .latestReadingAt(LocalDateTime.now())
                                .build());

            } else if (eachResponseProductDetail.getDataState() == 1) {

                saveEachCategoryProductDetailHistories.add(
                        ProductDetailHistory.builder()
                                .rfidChipCode(eachResponseProductDetail.getRfidChipCode())
                                .productSerialCode(eachResponseProductDetail.getProductSerialCode())
                                .productCode(eachResponseProductDetail.getProductCode())
                                .supplierCode(supplierCode)
                                .clientCode(clientCode)
                                .status("입고")
                                .cycle(eachResponseProductDetail.getCycle())
                                .latestReadingAt(LocalDateTime.now())
                                .build());
            }
        });


        //상품별 갯수 그룹화
        final Map<String, Long> map = saveEachCategoryProductDetailHistories.stream()
                .collect(Collectors.groupingBy(ProductDetailHistory::getProductCode, counting()));

        List<ProductDetailHistory> saveProductDetailList = new ArrayList<>();
        List<RfidScanHistory> saveRfidScanHistory = new ArrayList<>();

        // 상품별 rfid저장
        for (Map.Entry<String, Long> m : map.entrySet()) {

            Integer totalRemainQuantity = scanDataQueryDataV3.selectLastProductInfo(m.getKey(), supplierCode);

            RfidScanHistory latestScanHistory = scanDataQueryDataV3.getLatestRfidScanHistory(m.getKey(), supplierCode);
            RfidScanHistory createRfidScanHistory;

            if (latestScanHistory != null) {
                createRfidScanHistory = RfidScanHistory.builder()
                        .deviceCode(deviceCode)
                        .rfidChipCode("null")
                        .productCode(m.getKey())
                        .supplierCode(supplierCode)
                        .clientCode(clientCode)
                        .status("입고")
                        .statusCount(m.getValue().intValue())
                        .flowRemainQuantity(totalRemainQuantity - latestScanHistory.getNoReturnQuantity() - m.getValue().intValue())
                        .noReturnQuantity(latestScanHistory.getNoReturnQuantity() + m.getValue().intValue())
                        .totalRemainQuantity(totalRemainQuantity)
                        .latestReadingAt(LocalDateTime.now())
                        .build();

                saveRfidScanHistory.add(rfidScanHistoryRepository.save(createRfidScanHistory));

            } else {
                createRfidScanHistory = RfidScanHistory.builder()
                        .deviceCode(deviceCode)
                        .rfidChipCode("null")
                        .productCode(m.getKey())
                        .supplierCode(supplierCode)
                        .clientCode(clientCode)
                        .status("입고")
                        .statusCount(m.getValue().intValue())
                        .flowRemainQuantity(totalRemainQuantity - m.getValue().intValue())
                        .noReturnQuantity(m.getValue().intValue())
                        .totalRemainQuantity(totalRemainQuantity)
                        .latestReadingAt(LocalDateTime.now())
                        .build();

                saveRfidScanHistory.add(rfidScanHistoryRepository.save(createRfidScanHistory));
            }

        }

        saveRfidScanHistory.forEach(eachRfidScanHistory -> {
            List<ProductDetailHistory> realSaveProductDetailHistoryList = saveEachCategoryProductDetailHistories.stream()
                    .filter(eachProductDetailHistoryData ->
                            eachProductDetailHistoryData.getProductCode().equals(eachRfidScanHistory.getProductCode()) &&
                                    eachProductDetailHistoryData.getClientCode().equals(eachRfidScanHistory.getClientCode()) &&
                                    eachProductDetailHistoryData.getSupplierCode().equals(eachRfidScanHistory.getSupplierCode()))
                    .map(eachProductDetailHistoryData ->
                            ProductDetailHistory.builder()
                                    .rfidChipCode(eachProductDetailHistoryData.getRfidChipCode())
                                    .productSerialCode(eachProductDetailHistoryData.getProductSerialCode())
                                    .productCode(eachProductDetailHistoryData.getProductCode())
                                    .supplierCode(eachProductDetailHistoryData.getSupplierCode())
                                    .clientCode(eachProductDetailHistoryData.getClientCode())
                                    .status(eachProductDetailHistoryData.getStatus())
                                    .cycle(eachProductDetailHistoryData.getCycle())
                                    .latestReadingAt(eachProductDetailHistoryData.getLatestReadingAt())
                                    .rfidScanHistory(eachRfidScanHistory)
                                    .build()
                    ).collect(Collectors.toList());

            saveProductDetailList.addAll(realSaveProductDetailHistoryList);
        });

        productDetailHistoryRepository.saveAll(saveProductDetailList);
        log.info("입고 처리 완료");

        return new AsyncResult<>("스캔 제품들 입고 처리 완료").completable();


    }


    // 스캔 데이터 회수 service v3
    public synchronized CompletableFuture<String> sendReturnData2(RfidScanDataReturnRequestDto sendTurnBackDatas)
            throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        log.info("제품 회수 처리 service v2");

        String deviceCode = sendTurnBackDatas.getMachineId();
        String supplierCode = sendTurnBackDatas.getSupplierCode();

        List<SendProductCode> scanTurnBackDataList = sendTurnBackDatas.getProductCodes()
                .stream()
                .filter(eachCorrectProduct -> scanDataQueryDataV3.correctProduct(eachCorrectProduct.getProductCode(), supplierCode))
                .collect(Collectors.toList());

        log.info("회수 스캔한 제품 코드들 : {}", scanTurnBackDataList.stream().map(SendProductCode::getProductCode).distinct().collect(Collectors.toList()));

        List<ProductDetailHistory> saveEachCategoryProductDetailHistories = new ArrayList<>();

        HashMap<String, Object> responseProductDetailsInfo = batchService.launchProductDetail2("회수", scanTurnBackDataList, supplierCode);

        List<ProductDetailscanResponseDto> responseProductDetails = (List<ProductDetailscanResponseDto>) responseProductDetailsInfo.get("totalResponseProductDetails");
        List<Long> updateProductDetailIds = (List<Long>) responseProductDetailsInfo.get("updateProductDetailIds");

        if(updateProductDetailIds != null){
            productDetailQueryDataV2.needUpdateProductDetailStatusTurnBack(updateProductDetailIds);
        }

        responseProductDetails.forEach(eachResponseProductDetail -> {
            if (eachResponseProductDetail.getDataState() == 0) {
                saveEachCategoryProductDetailHistories.add(
                        ProductDetailHistory.builder()
                                .rfidChipCode(eachResponseProductDetail.getRfidChipCode())
                                .productSerialCode(eachResponseProductDetail.getProductSerialCode())
                                .productCode(eachResponseProductDetail.getProductCode())
                                .supplierCode(supplierCode)
                                .clientCode("null")
                                .status("회수")
                                .cycle(0)
                                .latestReadingAt(LocalDateTime.now())
                                .build());

            } else if (eachResponseProductDetail.getDataState() == 1) {

                saveEachCategoryProductDetailHistories.add(
                        ProductDetailHistory.builder()
                                .rfidChipCode(eachResponseProductDetail.getRfidChipCode())
                                .productSerialCode(eachResponseProductDetail.getProductSerialCode())
                                .productCode(eachResponseProductDetail.getProductCode())
                                .supplierCode(supplierCode)
                                .clientCode(eachResponseProductDetail.getClientCode())
                                .status("회수")
                                .cycle(eachResponseProductDetail.getCycle() + 1)
                                .latestReadingAt(LocalDateTime.now())
                                .build());
            }
        });


        final Map<String, List<ProductDetailHistory>> map2 = saveEachCategoryProductDetailHistories.stream().collect(Collectors.groupingBy(ProductDetailHistory::getClientCode));

        List<ProductDetailHistory> saveProductDetailList = new ArrayList<>();
        List<RfidScanHistory> saveRfidScanHistory = new ArrayList<>();

       //map2 {clientcodce: {}}
        for (Map.Entry<String, List<ProductDetailHistory>> m2 : map2.entrySet()) {

            final Map<String, Long> map = m2.getValue()
                    .stream()
                    .collect(Collectors.groupingBy(ProductDetailHistory::getProductCode, counting()));


            for (Map.Entry<String, Long> m : map.entrySet()) {

                log.info("스캔 제품 코드 : {}", m.getKey());
                log.info("공급사 코드 : {}", supplierCode);

                int totalRemainQuantity = scanDataQueryDataV3.selectLastProductInfo(m.getKey(), supplierCode);

                RfidScanHistory latestScanHistory = scanDataQueryDataV3.getLatestRfidScanHistory(m.getKey(), supplierCode);
                RfidScanHistory createRfidScanHistory;

                if (latestScanHistory != null) {
                    createRfidScanHistory = RfidScanHistory.builder()
                            .deviceCode(deviceCode)
                            .rfidChipCode("null")
                            .productCode(m.getKey())
                            .supplierCode(supplierCode)
                            .clientCode(m2.getKey())
                            .status("회수")
                            .statusCount(m.getValue().intValue())
                            .flowRemainQuantity(totalRemainQuantity - latestScanHistory.getNoReturnQuantity() + m.getValue().intValue())
                            .noReturnQuantity(latestScanHistory.getNoReturnQuantity() - m.getValue().intValue())
                            .totalRemainQuantity(totalRemainQuantity)
                            .latestReadingAt(LocalDateTime.now())
                            .build();

                    saveRfidScanHistory.add(rfidScanHistoryRepository.save(createRfidScanHistory));

                } else {
                    createRfidScanHistory = RfidScanHistory.builder()
                            .deviceCode(deviceCode)
                            .rfidChipCode("null")
                            .productCode(m.getKey())
                            .supplierCode(supplierCode)
                            .clientCode("null")
                            .status("회수")
                            .statusCount(m.getValue().intValue())
                            .flowRemainQuantity(totalRemainQuantity + m.getValue().intValue())
                            .noReturnQuantity(m.getValue().intValue())
                            .totalRemainQuantity(totalRemainQuantity)
                            .latestReadingAt(LocalDateTime.now())
                            .build();

                    saveRfidScanHistory.add(rfidScanHistoryRepository.save(createRfidScanHistory));
                }

            }
        }

        saveRfidScanHistory.forEach(eachRfidScanHistory -> {
            List<ProductDetailHistory> realSaveProductDetailHistoryList = saveEachCategoryProductDetailHistories.stream()
                    .filter(eachProductDetailHistoryData ->
                            eachProductDetailHistoryData.getProductCode().equals(eachRfidScanHistory.getProductCode()) &&
                                    eachProductDetailHistoryData.getClientCode().equals(eachRfidScanHistory.getClientCode()) &&
                                    eachProductDetailHistoryData.getSupplierCode().equals(eachRfidScanHistory.getSupplierCode()))
                    .map(eachProductDetailHistoryData ->
                            ProductDetailHistory.builder()
                                    .rfidChipCode(eachProductDetailHistoryData.getRfidChipCode())
                                    .productSerialCode(eachProductDetailHistoryData.getProductSerialCode())
                                    .productCode(eachProductDetailHistoryData.getProductCode())
                                    .supplierCode(eachProductDetailHistoryData.getSupplierCode())
                                    .clientCode(eachProductDetailHistoryData.getClientCode())
                                    .status(eachProductDetailHistoryData.getStatus())
                                    .cycle(eachProductDetailHistoryData.getCycle())
                                    .latestReadingAt(eachProductDetailHistoryData.getLatestReadingAt())
                                    .rfidScanHistory(eachRfidScanHistory)
                                    .build()
                    ).collect(Collectors.toList());

            saveProductDetailList.addAll(realSaveProductDetailHistoryList);
        });

        // 상품별 rfid저장
        productDetailHistoryRepository.saveAll(saveProductDetailList);
        log.info("회수 처리 완료");

        log.info("- 처음 들어온 데이터 수 : {}", scanTurnBackDataList.size());
        log.info("- 배치 프로그램을 돌린 후 저장 및 정제된 ProductDetail 수 : {}", responseProductDetails.size());
        log.info("- 정제된 ProductDetail 정보들을 기준으로 빌드된 ProductDetailHistory 수 : {}", saveEachCategoryProductDetailHistories.size());
        log.info("- RfidScanHistory와 ProductDetail 정보들을 기준으로 최종적으로 저장될 ProductDetailHistory  수 : {}", saveProductDetailList.size());

        return new AsyncResult<>("스캔 제품들 회수 처리 완료").completable();

    }

    /**
    // 스캔 데이터 상세 입고 처리 로직 함수
    @Async("threadPoolTaskExecutor")
    protected List<ProductDetail> scanInDetail(
            List<SendProductCode> scanInDatas,
            String supplierCode,
            String clientCode) {

        List<ProductDetail> finalProductDetails = new ArrayList<>();

        // 기존에 존재하는 Product 데이터들
        List<ProductDetail> existPrevProductDetails = scanInDatas.stream()
                .filter(eachExistPrevOutData -> productDetailQueryDataV2.checkRemainProductDetail(
                        eachExistPrevOutData.getProductSerialCode(),
                        eachExistPrevOutData.getProductCode(), supplierCode, clientCode) != null)
                .map(eachNullOutData -> productDetailQueryDataV2.checkRemainProductDetail(
                        eachNullOutData.getProductSerialCode(),
                        eachNullOutData.getProductCode(), supplierCode, clientCode))
                .collect(Collectors.toList());

        if (!existPrevProductDetails.isEmpty()) {
            existPrevProductDetails.forEach(eachUpdateScanData ->
                    finalProductDetails.add(productDetailQueryDataV2.scanInUpdateStatusAndReadingAt(eachUpdateScanData)));

            entityManager.flush();
            entityManager.clear();
        }

        // 처음 입고 처리되어 지금 상태는 null인 데이터들
        List<ProductDetail> nullProductDetails = scanInDatas.stream()
                .filter(eachNullInData -> productDetailQueryDataV2.checkRemainProductDetail(
                        eachNullInData.getProductSerialCode(),
                        eachNullInData.getProductCode(), supplierCode, clientCode) == null)
                .map(mappingNullFieldData ->
                        ProductDetail.builder()
                                .rfidChipCode(mappingNullFieldData.getRfidChipCode())
                                .productSerialCode(mappingNullFieldData.getProductSerialCode())
                                .productCode(mappingNullFieldData.getProductCode())
                                .supplierCode(supplierCode)
                                .clientCode(clientCode)
                                .status("입고")
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


    // 입고 처리 시 RfidScanHistory 생성 상세 로직
    @Async("threadPoolTaskExecutor")
    protected RfidScanHistory scanInRfidScanHistory(
            SendOrderCount eachProductCount,
            long unexpectedDicardProductCount,
            ProductDetail productDetail,
            String deviceCode,
            List<ProductDetailHistory> rfidHistoryMappingProductDetailHistorties) {

        // 기존에 동일한 기기로 동일한 공급사가 동일한 고객사에 동일한 제품을 출고 시킨 RFID 스캔 이력이 존재하지 않으면 이력을 저장
        if (scanDataQueryDataV3.checkSameCycleScanHistory(productDetail.getProductCode(), productDetail.getClientCode(), productDetail.getSupplierCode(), productDetail.getCycle(), "입고")) {
            log.info("RfidScanHistory에 입고 이력을 저장하기 위한 로직 접근");

            // 입고 수량
            int statusCount = 0;
            int discardCallCount = (int) unexpectedDicardProductCount;

            // 요청 받은 제품의 입고 수량을 추출하여 statusCount 변수에 저장
            // 상태 요청 수량 변수에 해당 제품군의 총 요청 수량에 폐기 수량을 빼서 적용 후 탈출
            if (eachProductCount.getProduct().equals(productDetail.getProductCode())) {
                // 상태 요청 수량 변수에 해당 제품군의 총 요청 수량에 폐기 수량을 빼서 적용 후 탈출
                statusCount = eachProductCount.getOrderCount();
            }

            // 만약 폐기 제품이 있을 경우 폐기 수량 저장 변수
            int discardMount = discardHistoryQueryDataV2.getDiscardHistory(productDetail.getSupplierCode(), productDetail.getProductCode());

            // 직전 가장 최신 스캔 이력 조회
            RfidScanHistory latestScanHistory = scanDataQueryDataV3.getLatestRfidScanHistory(productDetail.getProductCode(), productDetail.getSupplierCode());
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

            if (latestScanHistory != null) {
                // 추가 재고량 추출 후 변수 저장
                if (totalRemainCount - latestScanHistory.getTotalRemainQuantity() != 0) {
                    plusMount = totalRemainCount - latestScanHistory.getTotalRemainQuantity();
                }

                // RFID 기기 스캔 이력 정보
                rfidScanHistory = RfidScanHistory.builder()
                        .deviceCode(deviceCode)
                        .rfidChipCode(productDetail.getRfidChipCode())
                        .productCode(productDetail.getProductCode())
                        .supplierCode(productDetail.getSupplierCode())
                        .clientCode(productDetail.getClientCode())
                        .status(productDetail.getStatus())
                        .statusCount(statusCount)
                        .flowRemainQuantity(latestScanHistory.getFlowRemainQuantity() - statusCount + plusMount - minusCount)
                        .noReturnQuantity(latestScanHistory.getNoReturnQuantity() + statusCount)
                        .totalRemainQuantity(totalRemainCount)
                        .latestReadingAt(LocalDateTime.now())
                        .productDetailHistories(rfidHistoryMappingProductDetailHistorties)
                        .build();
            } else {
                // RFID 기기 스캔 이력 정보
                rfidScanHistory = RfidScanHistory.builder()
                        .deviceCode(deviceCode)
                        .rfidChipCode(productDetail.getRfidChipCode())
                        .productCode(productDetail.getProductCode())
                        .supplierCode(productDetail.getSupplierCode())
                        .clientCode(productDetail.getClientCode())
                        .status(productDetail.getStatus())
                        .statusCount(statusCount)
                        .flowRemainQuantity(totalRemainCount - statusCount + plusMount - minusCount)
                        .noReturnQuantity(statusCount)
                        .totalRemainQuantity(totalRemainCount)
                        .latestReadingAt(LocalDateTime.now())
                        .productDetailHistories(rfidHistoryMappingProductDetailHistorties)
                        .build();

            }

            return rfidScanHistory;
        } else {

            // 상태 요청 수량 변수
            int statusCount = 0;

            // 요청 수량 추출 후 변수에 저장
            if (eachProductCount.getProduct().equals(productDetail.getProductCode())) {
                // 상태 요청 수량 변수에 해당 제품군의 총 요청 수량에 폐기 수량을 빼서 적용 후 탈출
                statusCount = eachProductCount.getOrderCount();
            }

            // 잘못된 상태 처리로 인해 바로 다시 재차 스캔하여 상태 처리 했을 경우를 확인하기 위한 이력 조회
            if (scanDataQueryDataV3.asapScanForPrevInHistory(deviceCode, productDetail.getProductCode(), productDetail.getClientCode(), productDetail.getSupplierCode(), productDetail.getCycle(), statusCount)) {
                log.info("즉시 바로 재차 입고 처리 및 하루 지나기 이전에 재차 출고 처리 수정");
            } else {
                log.info("시간이 지나 재차 입고 처리 불가");
            }
        }

        return null;
    }


    // 입고 처리 시 ProductDetailHistory 생성 상세 로직
    @Async("threadPoolTaskExecutor")
    protected void scanInProductDetailHistory(List<RfidScanHistory> rfidScanHistoryList, List<ProductDetail> useProductDetailList) {

        for (RfidScanHistory eachScanHistory : rfidScanHistoryList) {
            log.info("대조시킬 RFID 리스트에 저장된 제품 코드 확인 : {}", eachScanHistory.getProductCode());
            log.info("이력 id : {}", eachScanHistory.getRfidScanhistoryId());

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
                            .status("입고")
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
    **/


///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    /**
    // 스캔 데이터 회수 service v2
    //@Transactional
    public synchronized CompletableFuture<String> sendReturnData(RfidScanDataReturnRequestDto sendReturnDatas)
            throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        log.info("제품 회수 처리 service v2");

        String deviceCode = sendReturnDatas.getMachineId();
        String supplierCode = sendReturnDatas.getSupplierCode();
        List<SendProductCode> productCodes = sendReturnDatas.getProductCodes();
        List<SendOrderCount> eachProductCount = sendReturnDatas.getEachProductCount();

        // 실제 RfidScanHistory 엔티티에 같이 매핑되어 저장될 ProductDetail 엔티티 리스트 객체 생성
        List<ProductDetailHistory> rfidHistoryMappingProductDetailHistorties = new ArrayList<>();

        // 회수 스캔한 데이터들을 필터링 코드와 폐기 여부 조건을 거쳐 재정립 하여 리스트화
        List<SendProductCode> scanTurnBackDatas = productCodes.stream()
                .filter(scanTurnBackData -> scanTurnBackData.getFilteringCode().contains("CCA2310") && !discardHistoryQueryDataV2.checkProductDiscard(scanTurnBackData.getProductCode(), scanTurnBackData.getProductSerialCode()))
                .collect(Collectors.toList());

        // 1. ProductDetail 처리
        // ProductDetail 처리 함수로 보내 정제된 ProductDetail 리스트 반환
        List<ProductDetail> productDetails = batchService.launchProductDetail2("회수", scanTurnBackDatas, supplierCode);
        //List<ProductDetail> productDetails = scanReturnDetail(scanTurnBackDatas, supplierCode);

        // RfidScanHistory 처리 이전에 실제로 저장될 중복제거된 ProductDetail 일부 정보를 리스트화
        List<String> distinctProductDetailClientProduct = productDetails.stream()
                .map(eachCategoryProductDetail -> eachCategoryProductDetail.getClientCode() + ":" + eachCategoryProductDetail.getProductCode())
                .distinct()
                .collect(Collectors.toList());

        // RfidScanHistory 한 번에 저장
        List<RfidScanHistory> saveRfidScanHistoryList = new ArrayList<>();

        // 2. RfidScanHistory 처리
        // 중복 제거된 RroductDetail 일부 정보 리스트 기준으로 RfidSCanHistory 처리
        distinctProductDetailClientProduct.forEach(eachClientProductProductDetail -> {

            // : 기호 기준으로 ClientCode 와 ProductCode 정보를 붑ㄴ리
            String[] separateClientCodeAndProductCode = eachClientProductProductDetail.split(":");
            String separateClientCode = separateClientCodeAndProductCode[0]; // ClientCode
            String separateProductCode = separateClientCodeAndProductCode[1]; // ProductCode

            long unexpectedDicardProductCount = productCodes.stream()
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


            // RfidScanHistory 처리 후 반환
            RfidScanHistory saveRfidScanHistory = scanReturnRfidScanHistory(orderProduct, unexpectedDicardProductCount, eachProductDetail, deviceCode, rfidHistoryMappingProductDetailHistorties);

            // 일괄 처리를 위해 리스트에 저장
            if (saveRfidScanHistory != null) {
                rfidScanHistoryRepository.save(saveRfidScanHistory);
                saveRfidScanHistoryList.add(saveRfidScanHistory);
            }

        });

        // 3. ProductDetailHistory 처리
        //scanReturnProductDetailHistory(saveRfidScanHistoryList, productDetails);
        batchService.launchProductDetailHistory2("회수", saveRfidScanHistoryList, productDetails);

        log.info("회수 처리 완료");

        return new AsyncResult<>("스캔 제품들 회수 처리 완료").completable();
    }

    // 스캔 데이터 회수 상세 로직 함수
    @Async("threadPoolTaskExecutor")
    protected List<ProductDetail> scanReturnDetail(
            List<SendProductCode> scanTurnBackDatas,
            String supplierCode) {

        List<ProductDetail> finalProductDetails = new ArrayList<>();

        // 기존에 존재하는 Product 데이터들
        List<ProductDetail> existPrevProductDetails = scanTurnBackDatas.stream()
                .filter(eachExistPrevTurnBackData -> productDetailQueryDataV2.checkBeforeStatusProductDetail(
                        eachExistPrevTurnBackData.getProductSerialCode(),
                        eachExistPrevTurnBackData.getProductCode(), supplierCode) != null)
                .map(eachNullTurnBackData -> productDetailQueryDataV2.checkBeforeStatusProductDetail(
                        eachNullTurnBackData.getProductSerialCode(),
                        eachNullTurnBackData.getProductCode(), supplierCode))
                .collect(Collectors.toList());

        if (!existPrevProductDetails.isEmpty()) {
            if (existPrevProductDetails.stream().map(ProductDetail::getStatus)
                    .distinct().anyMatch(status -> !status.equals("입고"))) {
                List<ProductDetail> notExistInDataProductDetails = existPrevProductDetails.stream()
                        .filter(eachProductDetail -> !eachProductDetail.getStatus().equals("입고"))
                        .map(mappingNotInDataProductDetail ->
                                ProductDetail.builder()
                                        .rfidChipCode(mappingNotInDataProductDetail.getRfidChipCode())
                                        .productSerialCode(mappingNotInDataProductDetail.getProductSerialCode())
                                        .productCode(mappingNotInDataProductDetail.getProductCode())
                                        .supplierCode(supplierCode)
                                        .clientCode("null")
                                        .status("회수")
                                        .cycle(mappingNotInDataProductDetail.getCycle() + 1)
                                        .latestReadingAt(LocalDateTime.now())
                                        .build())
                        .collect(Collectors.toList());

                if (!notExistInDataProductDetails.isEmpty()) {
                    finalProductDetails.addAll(productDetailRepository.saveAll(notExistInDataProductDetails));
                }
            }

            List<ProductDetail> existInDataProductDetails = existPrevProductDetails.stream()
                    .filter(eachProductDetail -> eachProductDetail.getStatus().equals("입고"))
                    .collect(Collectors.toList());

            if (!existInDataProductDetails.isEmpty()) {
                existInDataProductDetails.forEach(eachUpdateScanData ->
                        finalProductDetails.add(productDetailQueryDataV2.scanReturnUpdateStatusAndReadingAt(eachUpdateScanData)));

                entityManager.flush();
                entityManager.clear();
            }
        }


        List<ProductDetail> nullProductDetails = scanTurnBackDatas.stream()
                .filter(eachNullTurnBackData -> productDetailQueryDataV2.checkBeforeStatusProductDetail(
                        eachNullTurnBackData.getProductSerialCode(),
                        eachNullTurnBackData.getProductCode(), supplierCode) == null)
                .map(mappingNullFieldData ->
                        ProductDetail.builder()
                                .rfidChipCode(mappingNullFieldData.getRfidChipCode())
                                .productSerialCode(mappingNullFieldData.getProductSerialCode())
                                .productCode(mappingNullFieldData.getProductCode())
                                .supplierCode(supplierCode)
                                .clientCode("null")
                                .status("회수")
                                .cycle(1)
                                .latestReadingAt(LocalDateTime.now())
                                .build())
                .collect(Collectors.toList());

        if (!nullProductDetails.isEmpty()) {
            finalProductDetails.addAll(productDetailRepository.saveAll(nullProductDetails));
        }

        return finalProductDetails;
    }

    // 회수 처리 시 RfidScanHistory 생성 상세 로직
    @Async("threadPoolTaskExecutor")
    protected RfidScanHistory scanReturnRfidScanHistory(
            SendOrderCount eachProductCount,
            long unexpectedDicardProductCount,
            ProductDetail productDetail,
            String deviceCode,
            List<ProductDetailHistory> rfidHistoryMappingProductDetailHistorties) {

        // 기존에 동일한 기기로 동일한 공급사가 동일한 고객사에 동일한 제품을 출고 시킨 RFID 스캔 이력이 존재하지 않으면 이력을 저장
        if (scanDataQueryDataV3.checkSameCycleScanHistory(productDetail.getProductCode(), productDetail.getClientCode(), productDetail.getSupplierCode(), productDetail.getCycle(), "회수")) {

            // 회수 수량
            int statusCount = 0;
            int discardCallCount = (int) unexpectedDicardProductCount;

            // 요청 받은 제품의 회수 수량을 추출하여 statusCount 변수에 저장
            if (eachProductCount.getProduct().equals(productDetail.getProductCode())) {
                // 상태 요청 수량 변수에 해당 제품군의 총 요청 수량에 폐기 수량을 빼서 적용 후 탈출
                statusCount = eachProductCount.getOrderCount();
            }

            // 만약 폐기 제품이 있을 경우 폐기 수량 저장 변수
            int discardMount = discardHistoryQueryDataV2.getDiscardHistory(productDetail.getSupplierCode(), productDetail.getProductCode());

            // 직전 가장 최신 스캔 이력 조회
            RfidScanHistory latestScanHistory = scanDataQueryDataV3.getLatestRfidScanHistory(productDetail.getProductCode(), productDetail.getSupplierCode());

            // 총 재고량
            int totalRemainCount = supplierOrderQueryDataV2.getTotalRemainCount(productDetail.getSupplierCode(), productDetail.getProductCode()) - discardMount;

            // 만약 공급사 측에서 추가 물량을 주문했을 경우 재고 추가량 변수
            int plusMount = 0;
            int minusCount = 0;

            if (discardMount - discardCallCount != 0) {
                if (discardCallCount != 0) {
                    minusCount = discardMount - discardCallCount + discardCallCount;
                }
            }

            RfidScanHistory rfidScanHistory;

            if (latestScanHistory != null) {
                // 추가 재고량 추출 후 변수 저장
                if (totalRemainCount - latestScanHistory.getTotalRemainQuantity() != 0) {
                    plusMount = totalRemainCount - latestScanHistory.getTotalRemainQuantity();
                }

                // RFID 기기 스캔 이력 정보
                rfidScanHistory = RfidScanHistory.builder()
                        .deviceCode(deviceCode)
                        .rfidChipCode(productDetail.getRfidChipCode())
                        .productCode(productDetail.getProductCode())
                        .supplierCode(productDetail.getSupplierCode())
                        .clientCode(productDetail.getClientCode())
                        .status(productDetail.getStatus())
                        .statusCount(statusCount)
                        .flowRemainQuantity(latestScanHistory.getFlowRemainQuantity() + statusCount + plusMount - minusCount)
                        .noReturnQuantity(latestScanHistory.getNoReturnQuantity() - statusCount)
                        .totalRemainQuantity(totalRemainCount)
                        .latestReadingAt(LocalDateTime.now())
                        .productDetailHistories(rfidHistoryMappingProductDetailHistorties)
                        .build();
            } else {
                // RFID 기기 스캔 이력 정보
                rfidScanHistory = RfidScanHistory.builder()
                        .deviceCode(deviceCode)
                        .rfidChipCode(productDetail.getRfidChipCode())
                        .productCode(productDetail.getProductCode())
                        .supplierCode(productDetail.getSupplierCode())
                        .clientCode(productDetail.getClientCode())
                        .status(productDetail.getStatus())
                        .statusCount(statusCount)
                        .flowRemainQuantity(totalRemainCount + statusCount + plusMount - minusCount)
                        .noReturnQuantity(-statusCount)
                        .totalRemainQuantity(totalRemainCount)
                        .latestReadingAt(LocalDateTime.now())
                        .productDetailHistories(rfidHistoryMappingProductDetailHistorties)
                        .build();
            }

            return rfidScanHistory;

        } else {
            log.info("RfidScanHistory에 이미 동일한 내용의 회수 이력이 존재할 경우 접근");

            // 상태 요청 수량 변수
            int statusCount = 0;

            // 요청 수량 추출 후 변수에 저장
            if (eachProductCount.getProduct().equals(productDetail.getProductCode())) {
                // 상태 요청 수량 변수에 해당 제품군의 총 요청 수량에 폐기 수량을 빼서 적용 후 탈출
                statusCount = eachProductCount.getOrderCount();
            }

            // 잘못된 상태 처리로 인해 바로 다시 재차 스캔하여 상태 처리 했을 경우를 확인하기 위한 이력 조회
            if (scanDataQueryDataV3.asapScanForPrevTurnBackHistory(productDetail.getProductCode(), productDetail.getClientCode(), productDetail.getSupplierCode(), productDetail.getCycle(), statusCount)) {
                log.info("즉시 바로 재차 회수 처리 및 하루 지나기 이전에 재차 출고 처리 수정");
            } else {
                log.info("시간이 지나 재차 회수 처리 불가");
            }
        }

        return null;
    }


    // 회수 처리 시 ProductDetailHistory 생성 상세 로직
    @Async("threadPoolTaskExecutor")
    protected void scanReturnProductDetailHistory
    (List<RfidScanHistory> rfidScanHistoryList, List<ProductDetail> useProductDetailList) {

        for (RfidScanHistory eachScanHistory : rfidScanHistoryList) {
            log.info("대조시킬 RFID 리스트에 저장된 제품 코드 확인 : {}", eachScanHistory.getProductCode());
            log.info("이력 id : {}", eachScanHistory.getRfidScanhistoryId());

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
                            .status("회수")
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
    **/


///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // 스캔 데이터 세척 service v2
    @Transactional
    public synchronized CompletableFuture<String> sendCleaningData(RfidScanDataCleanRequestDto
                                                                           sendCleanDatas) throws InterruptedException {
        log.info("제품 세척 처리 service v2");

        String deviceCode = sendCleanDatas.getMachineId();
        String supplierCode = sendCleanDatas.getSupplierCode();
        List<SendProductCode> productCodes = sendCleanDatas.getProductCodes();
        List<SendOrderCount> eachProductCount = sendCleanDatas.getEachProductCount();

        // 실제 RfidScanHistory 엔티티에 같이 매핑되어 저장될 ProductDetail 엔티티 리스트 객체 생성
        List<ProductDetailHistory> rfidHistoryMappingProductDetailHistorties = new ArrayList<>();

        // 예기치 않은 폐기 제품 수 (RfidScanHIstory의 flowRemainCount를 계산하기 위한 용도)
        HashMap<String, Integer> unExpectedDiscardProductCountSet = new HashMap<>();
        Set<String> unExpectedDiscardProduct = new HashSet<>();

        eachProductCount.forEach(productCategory -> {
            if (discardHistoryQueryDataV2.firstCheckDiscardHistory(productCategory.getProduct())) {
                List<SendProductCode> checkDiscardProducts = discardHistoryQueryDataV2.getCategoryDiscardProducts(productCategory.getProduct());

                checkDiscardProducts.forEach(eachDiscardProduct -> {

                    if (productCodes.stream().anyMatch(data ->
                            data.getProductCode().equals(eachDiscardProduct.getProductCode()) && data.getProductSerialCode().equals(eachDiscardProduct.getProductSerialCode()))) {
                        unExpectedDiscardProduct.add(eachDiscardProduct.getProductCode() + ":" + eachDiscardProduct.getProductSerialCode());

                        if (!unExpectedDiscardProductCountSet.containsKey(eachDiscardProduct.getProductCode())) {
                            unExpectedDiscardProductCountSet.put(eachDiscardProduct.getProductCode(), 1);
                        } else {
                            unExpectedDiscardProductCountSet.put(eachDiscardProduct.getProductCode(), unExpectedDiscardProductCountSet.get(eachDiscardProduct.getProductCode()) + 1);
                        }
                    }
                });
            }
        });

        // 세척 스캔한 데이터들을 필터링 코드와 폐기 여부 조건을 거쳐 재정립 하여 리스트화
        List<SendProductCode> scanCleanDatas = productCodes.stream()
                .filter(scanCleanData -> scanCleanData.getFilteringCode().contains("CCA2310") && !unExpectedDiscardProduct.contains(scanCleanData.getProductCode() + ":" + scanCleanData.getProductSerialCode()))
                .collect(Collectors.toList());

        // 1. ProductDetail 처리
        // ProductDetail 처리 함수로 보내 정제된 ProductDetail 리스트 반환
        List<ProductDetail> productDetails = scanCleanDetail(scanCleanDatas, supplierCode);

        // RfidScanHistory 처리 이전에 실제로 저장될 중복제거된 ProductDetail 일부 정보를 리스트화
        List<String> distinctProductDetailClientProduct = productDetails.stream()
                .map(eachCategoryProductDetail -> eachCategoryProductDetail.getClientCode() + ":" + eachCategoryProductDetail.getProductCode())
                .distinct()
                .collect(Collectors.toList());

        // RfidScanHistory 한 번에 저장
        List<RfidScanHistory> saveRfidScanHistoryList = new ArrayList<>();

        // 2. RfidScanHistory 처리
        // 중복 제거된 RroductDetail 일부 정보 리스트 기준으로 RfidSCanHistory 처리
        distinctProductDetailClientProduct.forEach(eachClientProductProductDetail -> {

            // : 기호 기준으로 ClientCode 와 ProductCode 정보를 분리
            String[] separateClientCodeAndProductCode = eachClientProductProductDetail.split(":");
            String separateClientCode = separateClientCodeAndProductCode[0]; // ClientCode
            String separateProductCode = separateClientCodeAndProductCode[1]; // ProductCode

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


            // RfidScanHistory 처리 후 반환
            RfidScanHistory saveRfidScanHistory = scanCleanRfidScanHistory(orderProduct, eachProductDetail, unExpectedDiscardProductCountSet, deviceCode, rfidHistoryMappingProductDetailHistorties);

            // 일괄 처리를 위해 리스트에 저장
            if (saveRfidScanHistory != null) {
                rfidScanHistoryRepository.save(saveRfidScanHistory);
                saveRfidScanHistoryList.add(saveRfidScanHistory);
            }

        });

        // 3. ProductDetailHistory 처리
        scanCleanProductDetailHistory(saveRfidScanHistoryList, productDetails);

        log.info("세척 처리 완료");

        return new AsyncResult<>("스캔 제품들 세척 처리 완료").completable();
    }

    // 스캔 데이터 세척 상세 로직 함수
    @Async("threadPoolTaskExecutor")
    protected List<ProductDetail> scanCleanDetail(
            List<SendProductCode> scanCleanDatas,
            String supplierCode) {

        List<ProductDetail> finalProductDetails = new ArrayList<>();

        // 기존에 존재하는 Product 데이터들
        List<ProductDetail> existPrevProductDetails = scanCleanDatas.stream()
                .filter(eachExistPrevData -> productDetailQueryDataV2.checkBeforeStatusProductDetailAboutClean(
                        eachExistPrevData.getProductSerialCode(),
                        eachExistPrevData.getProductCode(), supplierCode) != null)
                .map(eachData -> productDetailQueryDataV2.checkBeforeStatusProductDetailAboutClean(
                        eachData.getProductSerialCode(),
                        eachData.getProductCode(), supplierCode))
                .collect(Collectors.toList());

        if (!existPrevProductDetails.isEmpty()) {
            existPrevProductDetails.forEach(eachUpdateScanData ->
                    finalProductDetails.add(productDetailQueryDataV2.scanCleanUpdateStatusAndReadingAt(eachUpdateScanData)));

            entityManager.flush();
            entityManager.clear();
        }

        List<ProductDetail> nullProductDetails = scanCleanDatas.stream()
                .filter(eachData -> productDetailQueryDataV2.checkBeforeStatusProductDetailAboutClean(
                        eachData.getProductSerialCode(),
                        eachData.getProductCode(), supplierCode) == null)
                .map(eachNullData -> ProductDetail.builder()
                        .rfidChipCode(eachNullData.getRfidChipCode())
                        .productSerialCode(eachNullData.getProductSerialCode())
                        .productCode(eachNullData.getProductCode())
                        .supplierCode(supplierCode)
                        .clientCode("null")
                        .status("세척")
                        .cycle(0)
                        .latestReadingAt(LocalDateTime.now())
                        .build())
                .collect(Collectors.toList());

        if (!nullProductDetails.isEmpty()) {
            finalProductDetails.addAll(productDetailRepository.saveAll(nullProductDetails));
        }

        return finalProductDetails;
    }

    // 세척 처리 시 RfidScanHistory 생성 상세 로직
    @Async("threadPoolTaskExecutor")
    protected RfidScanHistory scanCleanRfidScanHistory(
            SendOrderCount eachProductCount,
            ProductDetail productDetail,
            HashMap<String, Integer> unExpectedDiscardProductCountSet,
            String deviceCode,
            List<ProductDetailHistory> rfidHistoryMappingProductDetailHistorties
    ) {
        // 세척 수량
        int statusCount = 0;
        int discardCallCount = 0;

        // 요청 받은 제품의 세척 수량을 추출하여 statusCount 변수에 저장
        if (eachProductCount.getProduct().equals(productDetail.getProductCode())) {
            if (unExpectedDiscardProductCountSet.get(productDetail.getProductCode()) != null) {
                discardCallCount = unExpectedDiscardProductCountSet.get(productDetail.getProductCode());
                int calculDiscardCount = eachProductCount.getOrderCount() - discardCallCount;

                // 상태 요청 수량 변수에 해당 제품군의 총 요청 수량에 폐기 수량을 빼서 적용 후 탈출
                statusCount = calculDiscardCount;
            } else {
                // 상태 요청 수량 변수에 해당 제품군의 총 요청 수량에 폐기 수량을 빼서 적용 후 탈출
                statusCount = eachProductCount.getOrderCount();
            }
        }


        // 기존에 동일한 기기로 동일한 공급사가 동일한 고객사에 동일한 제품을 출고 시킨 RFID 스캔 이력이 존재하지 않으면 이력을 저장
        if (scanDataQueryDataV3.checkSameCycleScanHistory(productDetail.getProductCode(), productDetail.getClientCode(), productDetail.getSupplierCode(), productDetail.getCycle(), "세척")) {
            // 만약 폐기 제품이 있을 경우 폐기 수량 저장 변수
            int discardMount = discardHistoryQueryDataV2.getDiscardHistory(productDetail.getSupplierCode(), productDetail.getProductCode());

            // 직전 가장 최신 스캔 이력 조회
            RfidScanHistory latestScanHistory = scanDataQueryDataV3.getLatestRfidScanHistory(productDetail.getProductCode(), productDetail.getSupplierCode());
            // 총 재고량
            int totalRemainCount = supplierOrderQueryDataV2.getTotalRemainCount(productDetail.getSupplierCode(), productDetail.getProductCode()) - discardMount;

            // 만약 공급사 측에서 추가 물량을 주문했을 경우 재고 추가량 변수
            int plusMount = 0;
            int minusCount = 0;

            if (discardMount - discardCallCount != 0) {
                if (discardCallCount != 0) {
                    minusCount = discardMount - discardCallCount + discardCallCount;
                }
            }

            RfidScanHistory rfidScanHistory;

            if (latestScanHistory != null) {
                // 추가 재고량 추출 후 변수 저장
                if (totalRemainCount - latestScanHistory.getTotalRemainQuantity() != 0) {
                    plusMount = totalRemainCount - latestScanHistory.getTotalRemainQuantity();
                }

                // RFID 기기 스캔 이력 정보
                rfidScanHistory = RfidScanHistory.builder()
                        .deviceCode(deviceCode)
                        .rfidChipCode(productDetail.getRfidChipCode())
                        .productCode(productDetail.getProductCode())
                        .supplierCode(productDetail.getSupplierCode())
                        .clientCode(productDetail.getClientCode())
                        .status(productDetail.getStatus())
                        .statusCount(statusCount)
                        .flowRemainQuantity(latestScanHistory.getFlowRemainQuantity() + plusMount - minusCount)
                        .noReturnQuantity(latestScanHistory.getNoReturnQuantity())
                        .totalRemainQuantity(totalRemainCount)
                        .latestReadingAt(LocalDateTime.now())
                        .productDetailHistories(rfidHistoryMappingProductDetailHistorties)
                        .build();
            } else {
                // RFID 기기 스캔 이력 정보
                rfidScanHistory = RfidScanHistory.builder()
                        .deviceCode(deviceCode)
                        .rfidChipCode(productDetail.getRfidChipCode())
                        .productCode(productDetail.getProductCode())
                        .supplierCode(productDetail.getSupplierCode())
                        .clientCode(productDetail.getClientCode())
                        .status(productDetail.getStatus())
                        .statusCount(statusCount)
                        .flowRemainQuantity(totalRemainCount + plusMount - minusCount)
                        .noReturnQuantity(0)
                        .totalRemainQuantity(totalRemainCount)
                        .latestReadingAt(LocalDateTime.now())
                        .productDetailHistories(rfidHistoryMappingProductDetailHistorties)
                        .build();
            }

            return rfidScanHistory;

        } else {
            // 잘못된 상태 처리로 인해 바로 다시 재차 스캔하여 상태 처리 했을 경우를 확인하기 위한 이력 조회
            if (scanDataQueryDataV3.asapScanForPrevCleanHistory(productDetail.getProductCode(), productDetail.getClientCode(), productDetail.getSupplierCode(), productDetail.getCycle(), statusCount)) {
                log.info("즉시 바로 재차 세척 처리 및 하루 지나기 이전에 재차 출고 처리 수정");
            } else {
                log.info("시간이 지나 재차 세척 처리 불가");
            }
        }

        return null;
    }

    // 세척 처리 시 ProductDetailHistory 생성 상세 로직
    @Async("threadPoolTaskExecutor")
    protected void scanCleanProductDetailHistory
    (List<RfidScanHistory> rfidScanHistoryList, List<ProductDetail> useProductDetailList) {

        for (RfidScanHistory eachScanHistory : rfidScanHistoryList) {
            log.info("대조시킬 RFID 리스트에 저장된 제품 코드 확인 : {}", eachScanHistory.getProductCode());
            log.info("이력 id : {}", eachScanHistory.getRfidScanhistoryId());

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
                            .status("세척")
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


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    // 스캔 데이터 폐기 service v2
    @Transactional
    public synchronized CompletableFuture<String> sendDiscardData(RfidScanDataDiscardRequestDto sendDiscardDatas) throws
            InterruptedException {
        log.info("제품 폐기 처리 service v2");

        String clientCode = sendDiscardDatas.getSelectClientCode();
        String supplierCode = sendDiscardDatas.getSupplierCode();
        List<SendProductCode> productCodes = sendDiscardDatas.getProductCodes();

        // 폐기 스캔한 데이터들을 필터링 코드와 폐기 여부 조건을 거쳐 재정립 하여 리스트화
        List<SendProductCode> scanDiscardDatas = productCodes.stream()
                .filter(scanDiscardData -> scanDiscardData.getFilteringCode().contains("CCA2310"))
                .collect(Collectors.toList());

        // 1. ProductDetail 처리
        // ProductDetail 처리 함수로 보내 정제된 ProductDetail 리스트 반환
        List<ProductDetail> productDetails = scanDiscardDetail(scanDiscardDatas, supplierCode, clientCode);

        // 2. ProductDetailHistory 와 DiscardHistory 처리
        scanInProductDetailHistory(productDetails);

        log.info("폐기 처리 완료");

        return new AsyncResult<>("스캔 제품들 폐기 처리 완료").completable();
    }


    // 스캔 데이터 폐기 상세 로직 함수
    @Async("threadPoolTaskExecutor")
    protected List<ProductDetail> scanDiscardDetail(
            List<SendProductCode> scanDiscardDatas,
            String supplierCode,
            String clientCode) {

        List<ProductDetail> finalProductDetails = new ArrayList<>();

        // 기존에 존재하는 Product 데이터들
        List<ProductDetail> existPrevProductDetails = scanDiscardDatas.stream()
                .filter(eachExistPrevData -> productDetailQueryDataV2.checkRemainProductDetail(
                        eachExistPrevData.getProductSerialCode(),
                        eachExistPrevData.getProductCode(), supplierCode, clientCode) != null)
                .map(eachData -> productDetailQueryDataV2.checkRemainProductDetail(
                        eachData.getProductSerialCode(),
                        eachData.getProductCode(), supplierCode, clientCode))
                .collect(Collectors.toList());

        if (!existPrevProductDetails.isEmpty()) {
            existPrevProductDetails.forEach(eachUpdateScanData ->
                    finalProductDetails.add(productDetailQueryDataV2.scanDiscardUpdateStatusAndReadingAt(eachUpdateScanData)));
        }

        List<ProductDetail> nullProductDetails = scanDiscardDatas.stream()
                .filter(eachExistPrevData -> productDetailQueryDataV2.checkRemainProductDetail(
                        eachExistPrevData.getProductSerialCode(),
                        eachExistPrevData.getProductCode(), supplierCode, clientCode) == null)
                .map(eachData -> ProductDetail.builder()
                        .rfidChipCode(eachData.getRfidChipCode())
                        .productSerialCode(eachData.getProductSerialCode())
                        .productCode(eachData.getProductCode())
                        .supplierCode(supplierCode)
                        .clientCode(clientCode)
                        .status("폐기")
                        .cycle(0)
                        .latestReadingAt(LocalDateTime.now())
                        .build())
                .collect(Collectors.toList());

        if (!nullProductDetails.isEmpty()) {
            finalProductDetails.addAll(productDetailRepository.saveAll(nullProductDetails));
        }

        return finalProductDetails;
    }


    // 폐기 처리 시 ProductDetailHistory 생성 상세 로직
    @Async("threadPoolTaskExecutor")
    protected void scanInProductDetailHistory(List<ProductDetail> useProductDetailList) {

        List<ProductDetailHistory> saveProductDetailList = new ArrayList<>();

        for (ProductDetail eachDiscardProductDetail : useProductDetailList) {
            if (productDetailHistoryQueryDataV2.checkPreviewHistory(
                    eachDiscardProductDetail.getProductSerialCode(),
                    eachDiscardProductDetail.getProductCode(),
                    eachDiscardProductDetail.getSupplierCode(),
                    eachDiscardProductDetail.getClientCode(),
                    "폐기",
                    eachDiscardProductDetail.getCycle())) {

                // 각 제품 스캔 상세 이력
                ProductDetailHistory productDetailHistory = ProductDetailHistory.builder()
                        .rfidChipCode(eachDiscardProductDetail.getRfidChipCode())
                        .productSerialCode(eachDiscardProductDetail.getProductSerialCode())
                        .productCode(eachDiscardProductDetail.getProductCode())
                        .supplierCode(eachDiscardProductDetail.getSupplierCode())
                        .clientCode(eachDiscardProductDetail.getClientCode())
                        .status("폐기")
                        .cycle(eachDiscardProductDetail.getCycle())
                        .latestReadingAt(LocalDateTime.now())
                        .build();

                saveProductDetailList.add(productDetailHistory);
            }
        }

        List<ProductDetailHistory> resultSaveProductDetailList = productDetailHistoryRepository.saveAll(saveProductDetailList);
        List<DiscardHistory> resultDiscardHistoryList = new ArrayList<>();

        for (ProductDetailHistory eachProductDetailHistory : resultSaveProductDetailList) {
            // 기존에 동일한 기기로 동일한 공급사가 동일한 고객사에 동일한 제품을 출고 시킨 RFID 스캔 이력이 존재하지 않으면 이력을 저장
            if (!discardHistoryQueryDataV2.checkProductDiscard(eachProductDetailHistory.getProductCode(), eachProductDetailHistory.getProductSerialCode())) {

                // 폐기 이력 정보
                DiscardHistory discardHistory = DiscardHistory.builder()
                        .supplierCode(eachProductDetailHistory.getSupplierCode())
                        .clientCode(eachProductDetailHistory.getClientCode())
                        .productCode(eachProductDetailHistory.getProductCode())
                        .productSerialCode(eachProductDetailHistory.getProductSerialCode())
                        .rfidChipCode(eachProductDetailHistory.getRfidChipCode())
                        .discardAt(LocalDateTime.now())
                        .reason("")
                        .build();

                resultDiscardHistoryList.add(discardHistory);
            }
        }

        discardHistoryRepository.saveAll(resultDiscardHistoryList);
    }

}
