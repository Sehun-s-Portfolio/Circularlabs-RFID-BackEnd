package com.rfid.circularlabs_rfid_backend.scan.service;

import com.rfid.circularlabs_rfid_backend.device.response.DeviceResponseDto;
import com.rfid.circularlabs_rfid_backend.exception.ScanExceptionInterface;
import com.rfid.circularlabs_rfid_backend.process.domain.DiscardHistory;
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
import com.rfid.circularlabs_rfid_backend.scan.domain.RfidScanHistory;
import com.rfid.circularlabs_rfid_backend.scan.repository.RfidScanHistoryRepository;
import com.rfid.circularlabs_rfid_backend.scan.request.*;
import com.rfid.circularlabs_rfid_backend.scan.response.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
@Service
public class RfidScanDataService {

    private final RfidScanHistoryRepository rfidScanHistoryRepository;
    private final ProductDetailRepository productDetailRepository;
    private final ProductDetailHistoryRepository productDetailHistoryRepository;
    private final DiscardHistoryRepository discardHistoryRepository;
    private final ProductDetailQueryData productDetailQueryData;
    private final EntityManager entityManager;
    private final ScanDataQueryData scanDataQueryData;
    private final SupplierOrderQueryData supplierOrderQueryData;
    private final ClientOrderQueryData clientOrderQueryData;
    private final DiscardHistoryQueryData discardHistoryQueryData;
    private final ProductDetailHistoryQueryData productDetailHistoryQueryData;


    // 스캔 데이터 출고 service
    //@Async("threadPoolTaskExecutor")
    @Transactional
    public synchronized CompletableFuture<String> sendOutData(RfidScanDataOutRequestDto sendOutDatas) throws InterruptedException {
        log.info("제품 출고 처리 service");

        // 앱에서 넘겨받은 데이터들을 변수에 저장하여 고정
        String deviceCode = sendOutDatas.getMachineId(); // 기기 코드
        String tag = sendOutDatas.getTag(); // 출고 태그 값
        String clientCode = sendOutDatas.getSelectClientCode(); // 고객사 코드
        String supplierCode = sendOutDatas.getSupplierCode(); // 공급사 코드
        List<SendProductCode> receiveProductCodes = sendOutDatas.getProductCodes(); // 스캔한 제품 데이터들의 제품 분류 코드와 각 제품의 시리얼 코드 리스트
        List<SendOrderCount> eachProductCount = sendOutDatas.getEachProductCount(); // 각 제품의 출고 제품 수량

        // 작업 처리를 수행하기 이전에 방금 탈퇴 처리된 고객사인지 선행 확인
        /**
         if(scanExceptionInterface.checkWithDrawalMember(clientCode)){
         log.info("반환값 확인 == {}", new AsyncResult<>(false).completable());
         }
         **/

        // 최종 반환 DTO 객체에 넣어줄 리스트객체 생성
        List<ProductDetailResponseDto> productDetailResponseDtos = new ArrayList<>();
        // 실제 RfidScanHistory 엔티티에 같이 매핑되어 저장될 ProductDetail 엔티티 리스트 객체 생성
        List<ProductDetailHistory> rfidHistoryMappingProductDetailHistorties = new ArrayList<>();
        List<RfidScanHistory> rfidScanHistoryList = new ArrayList<>();

        // 예기치 않은 폐기 제품 수 (RfidScanHIstory의 flowRemainCount를 계산하기 위한 용도)
        HashMap<String, Integer> unExpectedDiscardProductCountSet = new HashMap<>();
        Set<String> unExpectedDiscardProduct = new HashSet<>();

        eachProductCount.forEach(productCategory -> {
            if (discardHistoryQueryData.firstCheckDiscardHistory(productCategory.getProduct())) {
                List<SendProductCode> checkDiscardProducts = discardHistoryQueryData.getCategoryDiscardProducts(productCategory.getProduct());

                checkDiscardProducts.forEach(eachDiscardProduct -> {

                    if(receiveProductCodes.stream().anyMatch(data ->
                            data.getProductCode().equals(eachDiscardProduct.getProductCode()) && data.getProductSerialCode().equals(eachDiscardProduct.getProductSerialCode()))){
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

        /**
        int threadCount = receiveProductCodes.size();
        CountDownLatch latch = new CountDownLatch(threadCount);
         **/

        // 앱으로부터 넘겨 받은 스캔 데이터들의 제품 분류 코드와 각 제품 시리얼 코드 리스트를 하나씩 조회하여 출고 이력 처리
        for (int i = 0; i < receiveProductCodes.size(); i++) {
            if (receiveProductCodes.get(i).getFilteringCode().contains("CCA2310") && !unExpectedDiscardProduct.contains(receiveProductCodes.get(i).getProductCode() + ":" + receiveProductCodes.get(i).getProductSerialCode())) {

                scanOutDetail(
                        receiveProductCodes.get(i),
                        supplierCode,
                        clientCode,
                        unExpectedDiscardProductCountSet,
                        deviceCode,
                        tag,
                        rfidScanHistoryList,
                        productDetailResponseDtos,
                        eachProductCount,
                        rfidHistoryMappingProductDetailHistorties);

            }
        }

        log.info("데이터 저장 완료 후 확인 로그");

        //latch.await();
        //Thread.sleep(1500);

        log.info("필터링 코드 확인 : {}", receiveProductCodes.get(0).getFilteringCode());

        return new AsyncResult<>("N").completable();
    }

    // 스캔 데이터 출고 상세 처리 로직 함수
    @Async("threadPoolTaskExecutor")
    protected void scanOutDetail(
            SendProductCode eachProductCode,
            String supplierCode,
            String clientCode,
            HashMap<String, Integer> unExpectedDiscardProductCountSet,
            String deviceCode,
            String tag,
            List<RfidScanHistory> rfidScanHistoryList,
            List<ProductDetailResponseDto> productDetailResponseDtos,
            List<SendOrderCount> eachProductCount,
            List<ProductDetailHistory> rfidHistoryMappingProductDetailHistorties) {

        // 기존에 저장된 대분류 제품 스캔 이력(ProductDetail)이 존재하는지 확인
        ProductDetail productDetail = productDetailQueryData.checkRemainProductDetail(
                eachProductCode.getRfidChipCode(), eachProductCode.getProductSerialCode(),
                eachProductCode.getProductCode(), supplierCode, clientCode);

        // 만약 대분류 제품 스캔 이력(ProductDetail)이 기존에 존재하지 않았을 경우에 각 제품 스캔 이력을 하나씩 저장
        if (productDetail == null) {
            // 제품 스캔 이력
            ProductDetail newInputProductDetail = ProductDetail.builder()
                    .rfidChipCode(eachProductCode.getRfidChipCode())
                    .productSerialCode(eachProductCode.getProductSerialCode())
                    .productCode(eachProductCode.getProductCode())
                    .supplierCode(supplierCode)
                    .clientCode(clientCode)
                    .status("출고")
                    .cycle(0)
                    .latestReadingAt(LocalDateTime.now())
                    .build();

            // 저장
            productDetailRepository.save(newInputProductDetail);

            // 새로 저장된 대분류 제품 스캔 이력(ProductDetail)을 공유하기 위해 if문 바깥으로 공유 처리
            productDetail = newInputProductDetail;

            log.info("productDetail이 저장됨을 확인 : {}", productDetail.getProductDetailId());

        } else { // 만약 대분류 제품 스캔 이력(ProductDetail)이 기존에 존재하는데, 상태가 한 번 사이클이 완료된 상태(대기?) 이면 상태와 최신 리딩 시간을 출고로 변경
            if (!productDetail.getStatus().equals("출고") && !productDetail.getStatus().equals("입고") && !productDetail.getStatus().equals("폐기")) {
                productDetail = productDetailQueryData.scanOutUpdateStatusAndReadingAt(productDetail);

                entityManager.flush();
                entityManager.clear();
            }
        }

        // 최종 확인 용 DTO 객체의 productDetails 속성에 들어갈 ProductDetail 정보 list 데이터 저장
        productDetailResponseDtos.add(
                ProductDetailResponseDto.builder()
                        .productDetailId(productDetail.getProductDetailId())
                        .rfidChipCode(productDetail.getRfidChipCode())
                        .productSerialCode(productDetail.getProductSerialCode())
                        .productCode(productDetail.getProductCode())
                        .supplierCode(productDetail.getSupplierCode())
                        .clientCode(productDetail.getClientCode())
                        .status(productDetail.getStatus())
                        .cycle(productDetail.getCycle())
                        .latestReadingAt(productDetail.getLatestReadingAt().toString())
                        .build()
        );

        if (productDetail.getStatus().equals("출고")) {

            // 상태 요청 수량 변수
            int statusCount = 0;
            int discardCallCount = 0;

            // 요청 수량 추출 후 변수에 저장
            for (SendOrderCount eachOrder : eachProductCount) {
                if (eachOrder.getProduct().equals(productDetail.getProductCode())) {

                    if (unExpectedDiscardProductCountSet.get(productDetail.getProductCode()) != null) {

                        discardCallCount = unExpectedDiscardProductCountSet.get(productDetail.getProductCode());
                        int calculDiscardCount = eachOrder.getOrderCount() - discardCallCount;

                        // 상태 요청 수량 변수에 해당 제품군의 총 요청 수량에 폐기 수량을 빼서 적용 후 탈출
                        statusCount = calculDiscardCount;
                        break;
                    } else {
                        // 상태 요청 수량 변수에 해당 제품군의 총 요청 수량에 폐기 수량을 빼서 적용 후 탈출
                        statusCount = eachOrder.getOrderCount();
                        break;
                    }
                }
            }

            // 기존에 동일한 기기로 동일한 공급사가 동일한 고객사에 동일한 제품을 출고 시킨 RFID 스캔 이력이 존재하지 않으면 이력을 저장
            if (scanDataQueryData.checkSameCycleScanHistory(productDetail.getProductCode(), productDetail.getClientCode(), productDetail.getSupplierCode(), productDetail.getCycle(), "출고")) {
                log.info("RfidScanHistory에 출고 이력을 저장하기 위한 로직 접근");

                // 만약 폐기 제품이 있을 경우 폐기 수량 저장 변수
                int discardMount = discardHistoryQueryData.getDiscardHistory(productDetail.getSupplierCode(), productDetail.getProductCode());
                // 직전 가장 최신 스캔 이력 조회
                RfidScanHistory latestScanHistory = scanDataQueryData.getLatestRfidScanHistory(productDetail.getProductCode(), productDetail.getSupplierCode());
                // 총 재고량
                int totalRemainCount = supplierOrderQueryData.getTotalRemainCount(productDetail.getSupplierCode(), productDetail.getProductCode()) - discardMount;

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
                    RfidScanHistory rfidScanHistory = RfidScanHistory.builder()
                            .deviceCode(deviceCode)
                            .rfidChipCode(productDetail.getRfidChipCode())
                            .productCode(productDetail.getProductCode())
                            .supplierCode(productDetail.getSupplierCode())
                            .clientCode(productDetail.getClientCode())
                            .status(productDetail.getStatus())
                            .statusCount(statusCount) //
                            .flowRemainQuantity(flowRemainQuantity)
                            .noReturnQuantity(latestScanHistory.getNoReturnQuantity())
                            .totalRemainQuantity(totalRemainCount)
                            .latestReadingAt(LocalDateTime.now())
                            .productDetailHistories(rfidHistoryMappingProductDetailHistorties)
                            .build();

                    // 저장
                    rfidScanHistoryRepository.save(rfidScanHistory);
                    rfidScanHistoryList.add(rfidScanHistory);

                } else { // 첫 스캔 이력으로 들어오는 경우
                    log.info("기존 이력이 존재하지 않는 첫 이력일 경우 진입");

                    // RFID 기기 스캔 이력 정보
                    RfidScanHistory rfidScanHistory = RfidScanHistory.builder()
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

                    // 저장
                    rfidScanHistoryRepository.save(rfidScanHistory);
                    rfidScanHistoryList.add(rfidScanHistory);
                }

            } else {
                log.info("RfidScanHistory에 이미 동일한 내용의 출고 이력이 존재할 경우 접근");

                // 잘못된 상태 처리로 인해 바로 다시 재차 스캔하여 상태 처리 했을 경우를 확인하기 위한 이력 조회
                if (scanDataQueryData.asapScanForPrevHistory(deviceCode, productDetail.getProductCode(), productDetail.getClientCode(), productDetail.getSupplierCode(), productDetail.getCycle(), statusCount)) {
                    log.info("즉시 바로 재차 출고 처리");
                }
            }

            log.info("대조시킬 RFID 스캔 이력 저장 리스트 확인 : {}", rfidScanHistoryList.size());

            // 기존에 이미 동일한 내용의 ProductDetailHistory 데이터가 존재하는 지 확인 후, 존재하지 않을 경우 해당 데이터 생성
            if (productDetailHistoryQueryData.checkPreviewHistory(
                    eachProductCode.getRfidChipCode(),
                    eachProductCode.getProductSerialCode(),
                    eachProductCode.getProductCode(),
                    supplierCode, clientCode, "출고", productDetail.getCycle())) {

                for (RfidScanHistory eachScanHistory : rfidScanHistoryList) {

                    log.info("대조시킬 RFID 리스트에 저장된 제품 코드 확인 : {}", eachScanHistory.getProductCode());
                    log.info("이력 id : {}", eachScanHistory.getRfidScanhistoryId());

                    if (eachScanHistory.getProductCode().equals(eachProductCode.getProductCode())) {
                        // 각 제품 스캔 상세 이력
                        ProductDetailHistory productDetailHistory = ProductDetailHistory.builder()
                                .rfidChipCode(eachProductCode.getRfidChipCode())
                                .productSerialCode(eachProductCode.getProductSerialCode())
                                .productCode(eachProductCode.getProductCode())
                                .supplierCode(supplierCode)
                                .clientCode(clientCode)
                                .status("출고")
                                .cycle(productDetail.getCycle())
                                .latestReadingAt(LocalDateTime.now())
                                .rfidScanHistory(eachScanHistory)
                                .build();

                        // 저장
                        productDetailHistoryRepository.save(productDetailHistory);

                        log.info("productDetailHistory이 저장됨을 확인 : {}", productDetailHistory.getProductDetailHistoryId());
                    }

                }
            }
        }

        log.info("현재 들어간 예기치 못한 폐기 제품들 정보 : {}", unExpectedDiscardProductCountSet.toString());
        log.info("요청 총 제품 수량 : {}", eachProductCount.size());

        for (int j = 0; j < eachProductCount.size(); j++) {
            log.info("요청 제품 : {}", eachProductCount.get(j).getProduct());
            log.info("요청 제품 수량 : {}", eachProductCount.get(j).getOrderCount());
        }

    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    // 스캔 데이터 입고 service
    //@Async("threadPoolTaskExecutor")
    @Transactional
    public synchronized CompletableFuture<String> sendInData(RfidScanDataInRequestDto sendInDatas) throws
            InterruptedException {
        log.info("제품 입고 처리 service");

        String deviceCode = sendInDatas.getMachineId();
        String tag = sendInDatas.getTag();
        String clientCode = sendInDatas.getSelectClientCode();
        String supplierCode = sendInDatas.getSupplierCode();
        List<SendProductCode> productCodes = sendInDatas.getProductCodes();
        List<SendOrderCount> eachProductCount = sendInDatas.getEachProductCount();

        // 최종 반환 DTO 객체에 넣어줄 리스트객체 생성
        List<ProductDetailResponseDto> productDetailResponseDtos = new ArrayList<>();
        // 실제 RfidScanHistory 엔티티에 같이 매핑되어 저장될 ProductDetail 엔티티 리스트 객체 생성
        List<ProductDetailHistory> rfidHistoryMappingProductDetailHistorties = new ArrayList<>();
        List<RfidScanHistory> rfidScanHistoryList = new ArrayList<>();
        // 최종 반환 DTO 객체 생성
        RfidScanDataInResponseDto responseDto = null;
        // RFID 기기 스캔 이력 정보
        RfidScanHistory rfidScanHistory = null;

        // 예기치 않은 폐기 제품 수 (RfidScanHIstory의 flowRemainCount를 계산하기 위한 용도)
        //AtomicInteger unExpectedDiscardProductCount = new AtomicInteger();
        HashMap<String, Integer> unExpectedDiscardProductCountSet = new HashMap<>();
        Set<String> unExpectedDiscardProduct = new HashSet<>();

        eachProductCount.forEach(productCategory -> {
            if (discardHistoryQueryData.firstCheckDiscardHistory(productCategory.getProduct())) {
                List<SendProductCode> checkDiscardProducts = discardHistoryQueryData.getCategoryDiscardProducts(productCategory.getProduct());

                checkDiscardProducts.forEach(eachDiscardProduct -> {

                    if(productCodes.stream().anyMatch(data ->
                            data.getProductCode().equals(eachDiscardProduct.getProductCode()) && data.getProductSerialCode().equals(eachDiscardProduct.getProductSerialCode()))){
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

        int threadCount = productCodes.size();

        //ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 앱으로부터 넘겨 받은 스캔 데이터들의 제품 분류 코드와 각 제품 시리얼 코드 리스트를 하나씩 조회하여 출고 이력 처리
        for (int i = 0; i < productCodes.size(); i++) {
            if (productCodes.get(i).getFilteringCode().contains("CCA2310") && !unExpectedDiscardProduct.contains(productCodes.get(i).getProductCode() + ":" + productCodes.get(i).getProductSerialCode())) {

                scanInDetail(
                        productCodes.get(i),
                        supplierCode,
                        clientCode,
                        productDetailResponseDtos,
                        eachProductCount,
                        unExpectedDiscardProductCountSet,
                        deviceCode,
                        rfidHistoryMappingProductDetailHistorties,
                        rfidScanHistoryList);

                latch.countDown();
            }
        }

        latch.await();
        Thread.sleep(1500);

        return new AsyncResult<>("스캔 제품들 입고 처리 완료").completable();
    }


    // 스캔 데이터 상세 입고 처리 로직 함수
    @Async("threadPoolTaskExecutor")
    protected void scanInDetail(
            SendProductCode eachProductCode,
            String supplierCode,
            String clientCode,
            List<ProductDetailResponseDto> productDetailResponseDtos,
            List<SendOrderCount> eachProductCount,
            HashMap<String, Integer> unExpectedDiscardProductCountSet,
            String deviceCode,
            List<ProductDetailHistory> rfidHistoryMappingProductDetailHistorties,
            List<RfidScanHistory> rfidScanHistoryList) {

        // 기존에 저장된 대분류 제품 스캔 이력(ProductDetail)이 존재하는지 확인
        ProductDetail productDetail = productDetailQueryData.checkRemainProductDetail(
                eachProductCode.getRfidChipCode(), eachProductCode.getProductSerialCode(),
                eachProductCode.getProductCode(), supplierCode, clientCode);

        // 만약 대분류 제품 스캔 출고 이력(ProductDetail)이 기존에 존재하지 않았을 경우, 입고 처리할 데이터가 없음을 예외 처리
        if (productDetail == null) {
            log.info("");
            log.info("[ 출고 이력이 없는 제품 ]");
            log.info("----------------------------------------------------------");
            log.info("제품 분류 코드 : {} / 제품 시리얼 코드 : {}", eachProductCode.getProductCode(), eachProductCode.getProductSerialCode());
            log.info("고객사 코드 : {} / 공급사 코드 : {}", clientCode, supplierCode);
            log.info("RFID 칩 코드 : {}", eachProductCode.getRfidChipCode());
            log.info("");

        } else { // 만약 대분류 제품 스캔 이력(ProductDetail)이 기존에 존재하면, 상태를 입고 상태로, 최신 리딩 시간을 업데이트
            if (productDetail.getStatus().equals("출고")) {
                productDetail = productDetailQueryData.scanInUpdateStatusAndReadingAt(productDetail);

                // 최종 확인 용 DTO 객체의 productDetails 속성에 들어갈 ProductDetail 정보 list 데이터 저장
                productDetailResponseDtos.add(
                        ProductDetailResponseDto.builder()
                                .productDetailId(productDetail.getProductDetailId())
                                .rfidChipCode(productDetail.getRfidChipCode())
                                .productSerialCode(productDetail.getProductSerialCode())
                                .productCode(productDetail.getProductCode())
                                .supplierCode(productDetail.getSupplierCode())
                                .clientCode(productDetail.getClientCode())
                                .status(productDetail.getStatus())
                                .cycle(productDetail.getCycle())
                                .latestReadingAt(productDetail.getLatestReadingAt().toString())
                                .build()
                );

                // 입고 수량
                int statusCount = 0;

                int discardCallCount = 0;

                // 요청 받은 제품의 입고 수량을 추출하여 statusCount 변수에 저장
                for (SendOrderCount eachOrder : eachProductCount) {
                    // 상태 요청 수량 변수에 해당 제품군의 총 요청 수량에 폐기 수량을 빼서 적용 후 탈출
                    if (eachOrder.getProduct().equals(productDetail.getProductCode())) {
                        if (unExpectedDiscardProductCountSet.get(productDetail.getProductCode()) != null) {
                            discardCallCount = unExpectedDiscardProductCountSet.get(productDetail.getProductCode());
                            int calculDiscardCount = eachOrder.getOrderCount() - discardCallCount;

                            // 상태 요청 수량 변수에 해당 제품군의 총 요청 수량에 폐기 수량을 빼서 적용 후 탈출
                            statusCount = calculDiscardCount;
                            break;
                        } else {
                            // 상태 요청 수량 변수에 해당 제품군의 총 요청 수량에 폐기 수량을 빼서 적용 후 탈출
                            statusCount = eachOrder.getOrderCount();
                            break;
                        }

                    }
                }

                // 기존에 동일한 기기로 동일한 공급사가 동일한 고객사에 동일한 제품을 출고 시킨 RFID 스캔 이력이 존재하지 않으면 이력을 저장
                if (scanDataQueryData.checkSameCycleScanHistory(productDetail.getProductCode(), productDetail.getClientCode(), productDetail.getSupplierCode(), productDetail.getCycle(), "입고")) {

                    // 만약 폐기 제품이 있을 경우 폐기 수량 저장 변수
                    int discardMount = discardHistoryQueryData.getDiscardHistory(productDetail.getSupplierCode(), productDetail.getProductCode());

                    // 직전 가장 최신 스캔 이력 조회
                    RfidScanHistory latestScanHistory = scanDataQueryData.getLatestRfidScanHistory(productDetail.getProductCode(), productDetail.getSupplierCode());
                    // 총 재고량
                    int totalRemainCount = supplierOrderQueryData.getTotalRemainCount(productDetail.getSupplierCode(), productDetail.getProductCode()) - discardMount;

                    // 만약 공급사 측에서 추가 물량을 주문했을 경우 재고 추가량 변수
                    int plusMount = 0;

                    // 추가 재고량 추출 후 변수 저장
                    if (totalRemainCount - latestScanHistory.getTotalRemainQuantity() != 0) {
                        plusMount = totalRemainCount - latestScanHistory.getTotalRemainQuantity();
                    }

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

                    // RFID 기기 스캔 이력 정보
                    RfidScanHistory rfidScanHistory = RfidScanHistory.builder()
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


                    // 저장
                    rfidScanHistoryRepository.save(rfidScanHistory);
                    rfidScanHistoryList.add(rfidScanHistory);

                    // 주문 완료 처리할 출고 수량 정보 변수
                    int orderCompleteCount = 0;

                    // 각 제품의 출고 수량 정보를 조회하여 변수에 저장
                    for (SendOrderCount productOrder : eachProductCount) {
                        if (productOrder.getProduct().equals(rfidScanHistory.getProductCode())) {
                            orderCompleteCount = productOrder.getOrderCount();
                            break;
                        }
                    }

                    // 고객사 주문 완료 처리
                    clientOrderQueryData.updateClientOrder(orderCompleteCount, rfidScanHistory.getProductCode(), rfidScanHistory.getSupplierCode(), rfidScanHistory.getClientCode());
                } else {
                    // 잘못된 상태 처리로 인해 바로 다시 재차 스캔하여 상태 처리 했을 경우를 확인하기 위한 이력 조회
                    if (scanDataQueryData.asapScanForPrevInHistory(deviceCode, productDetail.getProductCode(), productDetail.getClientCode(), productDetail.getSupplierCode(), productDetail.getCycle(), statusCount)) {
                        log.info("즉시 바로 재차 입고 처리");
                    }
                }

                if (productDetailHistoryQueryData.checkPreviewHistory(
                        eachProductCode.getRfidChipCode(),
                        eachProductCode.getProductSerialCode(),
                        eachProductCode.getProductCode(),
                        supplierCode, clientCode, "입고", productDetail.getCycle())) {

                    for (RfidScanHistory eachScanHistory : rfidScanHistoryList) {

                        log.info("대조시킬 RFID 리스트에 저장된 제품 코드 확인 : {}", eachScanHistory.getProductCode());
                        log.info("이력 id : {}", eachScanHistory.getRfidScanhistoryId());

                        if (eachScanHistory.getProductCode().equals(eachProductCode.getProductCode())) {
                            // 각 제품 스캔 상세 이력
                            ProductDetailHistory productDetailHistory = ProductDetailHistory.builder()
                                    .rfidChipCode(eachProductCode.getRfidChipCode())
                                    .productSerialCode(eachProductCode.getProductSerialCode())
                                    .productCode(eachProductCode.getProductCode())
                                    .supplierCode(supplierCode)
                                    .clientCode(clientCode)
                                    .status("입고")
                                    .cycle(productDetail.getCycle())
                                    .latestReadingAt(LocalDateTime.now())
                                    .rfidScanHistory(eachScanHistory)
                                    .build();

                            // 저장
                            productDetailHistoryRepository.save(productDetailHistory);
                        }

                    }
                }

            } else {
                log.info("");
                log.info("[ 출고 이력이 없는 제품 ]");
                log.info("----------------------------------------------------------");
                log.info("제품 분류 코드 : {} / 제품 시리얼 코드 : {}", eachProductCode.getProductCode(), eachProductCode.getProductSerialCode());
                log.info("고객사 코드 : {} / 공급사 코드 : {}", clientCode, supplierCode);
                log.info("RFID 칩 코드 : {}", eachProductCode.getRfidChipCode());
                log.info("");
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    // 스캔 데이터 회수 service
    //@Async("threadPoolTaskExecutor")
    @Transactional
    public synchronized CompletableFuture<String> sendReturnData(RfidScanDataReturnRequestDto sendReturnDatas) throws InterruptedException {
        log.info("제품 회수 처리 service");

        String deviceCode = sendReturnDatas.getMachineId();
        String tag = sendReturnDatas.getTag();
        //String clientCode = sendReturnDatas.getSelectClientCode();
        String supplierCode = sendReturnDatas.getSupplierCode();
        List<SendProductCode> productCodes = sendReturnDatas.getProductCodes();
        List<SendOrderCount> eachProductCount = sendReturnDatas.getEachProductCount();

        // 최종 반환 DTO 객체에 넣어줄 리스트객체 생성
        List<ProductDetailResponseDto> productDetailResponseDtos = new ArrayList<>();
        // 실제 RfidScanHistory 엔티티에 같이 매핑되어 저장될 ProductDetail 엔티티 리스트 객체 생성
        List<ProductDetailHistory> rfidHistoryMappingProductDetailHistorties = new ArrayList<>();
        List<RfidScanHistory> rfidScanHistoryList = new ArrayList<>();
        // 최종 반환 DTO 객체 생성
        RfidScanDataReturnResponseDto responseDto = null;
        // RFID 기기 스캔 이력 정보
        RfidScanHistory rfidScanHistory = null;

        // 예기치 않은 폐기 제품 수 (RfidScanHIstory의 flowRemainCount를 계산하기 위한 용도)
        //AtomicInteger unExpectedDiscardProductCount = new AtomicInteger();
        HashMap<String, Integer> unExpectedDiscardProductCountSet = new HashMap<>();
        Set<String> unExpectedDiscardProduct = new HashSet<>();

        eachProductCount.forEach(productCategory -> {
            if (discardHistoryQueryData.firstCheckDiscardHistory(productCategory.getProduct())) {
                List<SendProductCode> checkDiscardProducts = discardHistoryQueryData.getCategoryDiscardProducts(productCategory.getProduct());

                checkDiscardProducts.forEach(eachDiscardProduct -> {

                    if(productCodes.stream().anyMatch(data ->
                            data.getProductCode().equals(eachDiscardProduct.getProductCode()) && data.getProductSerialCode().equals(eachDiscardProduct.getProductSerialCode()))){
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

        int threadCount = productCodes.size();

        //ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 앱으로부터 넘겨 받은 스캔 데이터들의 제품 분류 코드와 각 제품 시리얼 코드 리스트를 하나씩 조회하여 출고 이력 처리
        for (int i = 0; i < productCodes.size(); i++) {
            if (productCodes.get(i).getFilteringCode().contains("CCA2310") && !unExpectedDiscardProduct.contains(productCodes.get(i).getProductCode() + ":" + productCodes.get(i).getProductSerialCode())) {

                scanReturnDetail(
                        productCodes.get(i),
                        supplierCode,
                        productDetailResponseDtos,
                        eachProductCount,
                        unExpectedDiscardProductCountSet,
                        deviceCode,
                        rfidHistoryMappingProductDetailHistorties,
                        rfidScanHistoryList);

                latch.countDown();
            }
        }

        latch.await();
        Thread.sleep(1500);

        return new AsyncResult<>("스캔 제품들 회수 처리 완료").completable();
    }


    // 스캔 데이터 회수 상세 로직 함수
    @Async("threadPoolTaskExecutor")
    protected void scanReturnDetail(
            SendProductCode eachProductCode,
            String supplierCode,
            List<ProductDetailResponseDto> productDetailResponseDtos,
            List<SendOrderCount> eachProductCount,
            HashMap<String, Integer> unExpectedDiscardProductCountSet,
            String deviceCode,
            List<ProductDetailHistory> rfidHistoryMappingProductDetailHistorties,
            List<RfidScanHistory> rfidScanHistoryList) {

        // 기존에 저장된 대분류 제품 스캔 이력(ProductDetail)이 존재하는지 확인
        ProductDetail productDetail = productDetailQueryData.checkBeforeStatusProductDetail(
                eachProductCode.getRfidChipCode(), eachProductCode.getProductSerialCode(),
                eachProductCode.getProductCode(), supplierCode);

        // 만약 대분류 제품 스캔 입고 이력(ProductDetail)이 기존에 존재하지 않았을 경우, 회수 처리할 데이터가 없음을 예외 처리
        if (productDetail == null) {
            log.info("");
            log.info("[ 입고 이력이 없는 제품 ]");
            log.info("----------------------------------------------------------");
            log.info("제품 분류 코드 : {} / 제품 시리얼 코드 : {}", eachProductCode.getProductCode(), eachProductCode.getProductSerialCode());
            log.info("RFID 칩 코드 : {}", eachProductCode.getRfidChipCode());
            log.info("");

        } else { // 만약 대분류 제품 스캔 이력(ProductDetail)이 기존에 존재하면, 상태를 회수 상태로, 최신 리딩 시간을 업데이트
            if (productDetail.getStatus().equals("입고")) {
                productDetail = productDetailQueryData.scanReturnUpdateStatusAndReadingAt(productDetail);

                // 최종 확인 용 DTO 객체의 productDetails 속성에 들어갈 ProductDetail 정보 list 데이터 저장
                productDetailResponseDtos.add(
                        ProductDetailResponseDto.builder()
                                .productDetailId(productDetail.getProductDetailId())
                                .rfidChipCode(productDetail.getRfidChipCode())
                                .productSerialCode(productDetail.getProductSerialCode())
                                .productCode(productDetail.getProductCode())
                                .supplierCode(productDetail.getSupplierCode())
                                .clientCode(productDetail.getClientCode())
                                .status(productDetail.getStatus())
                                .cycle(productDetail.getCycle())
                                .latestReadingAt(productDetail.getLatestReadingAt().toString())
                                .build()
                );

                // 회수 수량
                int statusCount = 0;

                int discardCallCount = 0;

                // 요청 받은 제품의 회수 수량을 추출하여 statusCount 변수에 저장
                for (SendOrderCount eachOrder : eachProductCount) {
                    if (eachOrder.getProduct().equals(productDetail.getProductCode())) {
                        if (unExpectedDiscardProductCountSet.get(productDetail.getProductCode()) != null) {
                            discardCallCount = unExpectedDiscardProductCountSet.get(productDetail.getProductCode());
                            int calculDiscardCount = eachOrder.getOrderCount() - discardCallCount;

                            // 상태 요청 수량 변수에 해당 제품군의 총 요청 수량에 폐기 수량을 빼서 적용 후 탈출
                            statusCount = calculDiscardCount;
                            break;
                        } else {
                            // 상태 요청 수량 변수에 해당 제품군의 총 요청 수량에 폐기 수량을 빼서 적용 후 탈출
                            statusCount = eachOrder.getOrderCount();
                            break;
                        }

                    }
                }

                // 기존에 동일한 기기로 동일한 공급사가 동일한 고객사에 동일한 제품을 출고 시킨 RFID 스캔 이력이 존재하지 않으면 이력을 저장
                if (scanDataQueryData.checkSameCycleScanHistory(productDetail.getProductCode(), productDetail.getClientCode(), productDetail.getSupplierCode(), productDetail.getCycle(), "회수")) {

                    // 만약 폐기 제품이 있을 경우 폐기 수량 저장 변수
                    int discardMount = discardHistoryQueryData.getDiscardHistory(productDetail.getSupplierCode(), productDetail.getProductCode());

                    // 직전 가장 최신 스캔 이력 조회
                    RfidScanHistory latestScanHistory = scanDataQueryData.getLatestRfidScanHistory(productDetail.getProductCode(), productDetail.getSupplierCode());
                    // 총 재고량
                    int totalRemainCount = supplierOrderQueryData.getTotalRemainCount(productDetail.getSupplierCode(), productDetail.getProductCode()) - discardMount;

                    // 만약 공급사 측에서 추가 물량을 주문했을 경우 재고 추가량 변수
                    int plusMount = 0;

                    // 추가 재고량 추출 후 변수 저장
                    if (totalRemainCount - latestScanHistory.getTotalRemainQuantity() != 0) {
                        plusMount = totalRemainCount - latestScanHistory.getTotalRemainQuantity();
                    }

                    int minusCount = 0;

                    if (discardMount - discardCallCount != 0) {
                        if (discardCallCount != 0) {
                            minusCount = discardMount - discardCallCount + discardCallCount;
                        }
                    }

                    // RFID 기기 스캔 이력 정보
                    RfidScanHistory rfidScanHistory = RfidScanHistory.builder()
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

                    // 저장
                    rfidScanHistoryRepository.save(rfidScanHistory);
                    rfidScanHistoryList.add(rfidScanHistory);

                } else {
                    // 잘못된 상태 처리로 인해 바로 다시 재차 스캔하여 상태 처리 했을 경우를 확인하기 위한 이력 조회
                    if (scanDataQueryData.asapScanForPrevTurnBackHistory(deviceCode, productDetail.getProductCode(), productDetail.getClientCode(), productDetail.getSupplierCode(), productDetail.getCycle(), statusCount)) {
                        log.info("즉시 바로 재차 회수 처리");
                    }
                }

                if (productDetailHistoryQueryData.checkPreviewHistory(
                        eachProductCode.getRfidChipCode(),
                        eachProductCode.getProductSerialCode(),
                        eachProductCode.getProductCode(),
                        supplierCode, productDetail.getClientCode(), "회수", productDetail.getCycle())) {

                    for (RfidScanHistory eachScanHistory : rfidScanHistoryList) {

                        log.info("대조시킬 RFID 리스트에 저장된 제품 코드 확인 : {}", eachScanHistory.getProductCode());
                        log.info("이력 id : {}", eachScanHistory.getRfidScanhistoryId());

                        if (eachScanHistory.getProductCode().equals(eachProductCode.getProductCode())) {
                            // 각 제품 스캔 상세 이력
                            ProductDetailHistory productDetailHistory = ProductDetailHistory.builder()
                                    .rfidChipCode(eachProductCode.getRfidChipCode())
                                    .productSerialCode(eachProductCode.getProductSerialCode())
                                    .productCode(eachProductCode.getProductCode())
                                    .supplierCode(supplierCode)
                                    .clientCode(productDetail.getClientCode())
                                    .status("회수")
                                    .cycle(productDetail.getCycle())
                                    .latestReadingAt(LocalDateTime.now())
                                    .rfidScanHistory(eachScanHistory)
                                    .build();

                            // 저장
                            productDetailHistoryRepository.save(productDetailHistory);
                        }

                    }
                }

            } else {
                log.info("");
                log.info("[ 입고 이력이 없는 제품 ]");
                log.info("----------------------------------------------------------");
                log.info("제품 분류 코드 : {} / 제품 시리얼 코드 : {}", eachProductCode.getProductCode(), eachProductCode.getProductSerialCode());
                log.info("RFID 칩 코드 : {}", eachProductCode.getRfidChipCode());
                log.info("");
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    // 스캔 데이터 세척 service
    //@Async("threadPoolTaskExecutor")
    @Transactional
    public synchronized CompletableFuture<String> sendCleaningData(RfidScanDataCleanRequestDto
                                                                           sendCleanDatas) throws InterruptedException {
        log.info("제품 세척 처리 service");

        String deviceCode = sendCleanDatas.getMachineId();
        String tag = sendCleanDatas.getTag();
        String supplierCode = sendCleanDatas.getSupplierCode();
        List<SendProductCode> productCodes = sendCleanDatas.getProductCodes();
        List<SendOrderCount> eachProductCount = sendCleanDatas.getEachProductCount();

        // 최종 반환 DTO 객체에 넣어줄 리스트객체 생성
        List<ProductDetailResponseDto> productDetailResponseDtos = new ArrayList<>();
        // 실제 RfidScanHistory 엔티티에 같이 매핑되어 저장될 ProductDetail 엔티티 리스트 객체 생성
        List<ProductDetailHistory> rfidHistoryMappingProductDetailHistorties = new ArrayList<>();
        List<RfidScanHistory> rfidScanHistoryList = new ArrayList<>();
        // 최종 반환 DTO 객체 생성
        RfidScanDataCleanResponseDto responseDto = null;
        // RFID 기기 스캔 이력 정보
        RfidScanHistory rfidScanHistory = null;

        // 예기치 않은 폐기 제품 수 (RfidScanHIstory의 flowRemainCount를 계산하기 위한 용도)
        //AtomicInteger unExpectedDiscardProductCount = new AtomicInteger();
        HashMap<String, Integer> unExpectedDiscardProductCountSet = new HashMap<>();
        Set<String> unExpectedDiscardProduct = new HashSet<>();

        eachProductCount.forEach(productCategory -> {
            if (discardHistoryQueryData.firstCheckDiscardHistory(productCategory.getProduct())) {
                List<SendProductCode> checkDiscardProducts = discardHistoryQueryData.getCategoryDiscardProducts(productCategory.getProduct());

                checkDiscardProducts.forEach(eachDiscardProduct -> {

                    if(productCodes.stream().anyMatch(data ->
                            data.getProductCode().equals(eachDiscardProduct.getProductCode()) && data.getProductSerialCode().equals(eachDiscardProduct.getProductSerialCode()))){
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


        int threadCount = productCodes.size();

        //ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 앱으로부터 넘겨 받은 스캔 데이터들의 제품 분류 코드와 각 제품 시리얼 코드 리스트를 하나씩 조회하여 출고 이력 처리
        for (int i = 0; i < productCodes.size(); i++) {
            if (productCodes.get(i).getFilteringCode().contains("CCA2310") && !unExpectedDiscardProduct.contains(productCodes.get(i).getProductCode() + ":" + productCodes.get(i).getProductSerialCode())) {

                scanCleanDetail(
                        productCodes.get(i),
                        supplierCode,
                        productDetailResponseDtos,
                        eachProductCount,
                        unExpectedDiscardProductCountSet,
                        deviceCode,
                        rfidHistoryMappingProductDetailHistorties,
                        rfidScanHistoryList);

                latch.countDown();
            }
        }

        latch.await();
        Thread.sleep(1500);

        return new AsyncResult<>("스캔 제품들 세척 처리 완료").completable();
    }

    // 스캔 데이터 세척 상세 로직 함수
    @Async("threadPoolTaskExecutor")
    protected void scanCleanDetail(
            SendProductCode eachProductCode,
            String supplierCode,
            List<ProductDetailResponseDto> productDetailResponseDtos,
            List<SendOrderCount> eachProductCount,
            HashMap<String, Integer> unExpectedDiscardProductCountSet,
            String deviceCode,
            List<ProductDetailHistory> rfidHistoryMappingProductDetailHistorties,
            List<RfidScanHistory> rfidScanHistoryList) {

        // 기존에 저장된 대분류 제품 스캔 이력(ProductDetail)이 존재하는지 확인
        ProductDetail productDetail = productDetailQueryData.checkBeforeStatusProductDetail(
                eachProductCode.getRfidChipCode(), eachProductCode.getProductSerialCode(),
                eachProductCode.getProductCode(), supplierCode);

        // 만약 대분류 제품 스캔 입고 이력(ProductDetail)이 기존에 존재하지 않았을 경우, 회수 처리할 데이터가 없음을 예외 처리
        if (productDetail == null) {
            log.info("");
            log.info("[ 회수 이력이 없는 제품 ]");
            log.info("----------------------------------------------------------");
            log.info("제품 분류 코드 : {} / 제품 시리얼 코드 : {}", eachProductCode.getProductCode(), eachProductCode.getProductSerialCode());
            log.info("RFID 칩 코드 : {}", eachProductCode.getRfidChipCode());
            log.info("");

        } else { // 만약 대분류 제품 스캔 이력(ProductDetail)이 기존에 존재하면, 상태를 회수 상태로, 최신 리딩 시간을 업데이트
            if (productDetail.getStatus().equals("회수")) {
                productDetail = productDetailQueryData.scanCleanUpdateStatusAndReadingAt(productDetail);

                // 최종 확인 용 DTO 객체의 productDetails 속성에 들어갈 ProductDetail 정보 list 데이터 저장
                productDetailResponseDtos.add(
                        ProductDetailResponseDto.builder()
                                .productDetailId(productDetail.getProductDetailId())
                                .rfidChipCode(productDetail.getRfidChipCode())
                                .productSerialCode(productDetail.getProductSerialCode())
                                .productCode(productDetail.getProductCode())
                                .supplierCode(productDetail.getSupplierCode())
                                .clientCode(productDetail.getClientCode())
                                .status(productDetail.getStatus())
                                .cycle(productDetail.getCycle())
                                .latestReadingAt(productDetail.getLatestReadingAt().toString())
                                .build()
                );

                // 회수 수량
                int statusCount = 0;

                int discardCallCount = 0;

                // 요청 받은 제품의 회수 수량을 추출하여 statusCount 변수에 저장
                for (SendOrderCount eachOrder : eachProductCount) {
                    if (eachOrder.getProduct().equals(productDetail.getProductCode())) {
                        if (unExpectedDiscardProductCountSet.get(productDetail.getProductCode()) != null) {
                            discardCallCount = unExpectedDiscardProductCountSet.get(productDetail.getProductCode());
                            int calculDiscardCount = eachOrder.getOrderCount() - discardCallCount;

                            // 상태 요청 수량 변수에 해당 제품군의 총 요청 수량에 폐기 수량을 빼서 적용 후 탈출
                            statusCount = calculDiscardCount;
                            break;
                        } else {
                            // 상태 요청 수량 변수에 해당 제품군의 총 요청 수량에 폐기 수량을 빼서 적용 후 탈출
                            statusCount = eachOrder.getOrderCount();
                            break;
                        }

                    }
                }

                // 기존에 동일한 기기로 동일한 공급사가 동일한 고객사에 동일한 제품을 출고 시킨 RFID 스캔 이력이 존재하지 않으면 이력을 저장
                if (scanDataQueryData.checkSameCycleScanHistory(productDetail.getProductCode(), productDetail.getClientCode(), productDetail.getSupplierCode(), productDetail.getCycle(), "세척")) {

                    // 만약 폐기 제품이 있을 경우 폐기 수량 저장 변수
                    int discardMount = discardHistoryQueryData.getDiscardHistory(productDetail.getSupplierCode(), productDetail.getProductCode());

                    // 직전 가장 최신 스캔 이력 조회
                    RfidScanHistory latestScanHistory = scanDataQueryData.getLatestRfidScanHistory(productDetail.getProductCode(), productDetail.getSupplierCode());
                    // 총 재고량
                    int totalRemainCount = supplierOrderQueryData.getTotalRemainCount(productDetail.getSupplierCode(), productDetail.getProductCode()) - discardMount;

                    // 만약 공급사 측에서 추가 물량을 주문했을 경우 재고 추가량 변수
                    int plusMount = 0;

                    // 추가 재고량 추출 후 변수 저장
                    if (totalRemainCount - latestScanHistory.getTotalRemainQuantity() != 0) {
                        plusMount = totalRemainCount - latestScanHistory.getTotalRemainQuantity();
                    }

                    int minusCount = 0;

                    if (discardMount - discardCallCount != 0) {
                        if (discardCallCount != 0) {
                            minusCount = discardMount - discardCallCount + discardCallCount;
                        }
                    }

                    // RFID 기기 스캔 이력 정보
                    RfidScanHistory rfidScanHistory = RfidScanHistory.builder()
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

                    // 저장
                    rfidScanHistoryRepository.save(rfidScanHistory);
                    rfidScanHistoryList.add(rfidScanHistory);

                } else {
                    // 잘못된 상태 처리로 인해 바로 다시 재차 스캔하여 상태 처리 했을 경우를 확인하기 위한 이력 조회
                    if (scanDataQueryData.asapScanForPrevCleanHistory(deviceCode, productDetail.getProductCode(), productDetail.getClientCode(), productDetail.getSupplierCode(), productDetail.getCycle(), statusCount)) {
                        log.info("즉시 바로 재차 세척 처리");
                    }
                }

                if (productDetailHistoryQueryData.checkPreviewHistory(
                        eachProductCode.getRfidChipCode(),
                        eachProductCode.getProductSerialCode(),
                        eachProductCode.getProductCode(),
                        supplierCode, productDetail.getClientCode(), "세척", productDetail.getCycle())) {
                    for (RfidScanHistory eachScanHistory : rfidScanHistoryList) {

                        log.info("대조시킬 RFID 리스트에 저장된 제품 코드 확인 : {}", eachScanHistory.getProductCode());
                        log.info("이력 id : {}", eachScanHistory.getRfidScanhistoryId());

                        if (eachScanHistory.getProductCode().equals(eachProductCode.getProductCode())) {
                            // 각 제품 스캔 상세 이력
                            ProductDetailHistory productDetailHistory = ProductDetailHistory.builder()
                                    .rfidChipCode(eachProductCode.getRfidChipCode())
                                    .productSerialCode(eachProductCode.getProductSerialCode())
                                    .productCode(eachProductCode.getProductCode())
                                    .supplierCode(supplierCode)
                                    .clientCode(productDetail.getClientCode())
                                    .status("세척")
                                    .cycle(productDetail.getCycle())
                                    .latestReadingAt(LocalDateTime.now())
                                    .rfidScanHistory(eachScanHistory)
                                    .build();

                            // 저장
                            productDetailHistoryRepository.save(productDetailHistory);
                        }

                    }
                }
            } else {
                log.info("");
                log.info("[ 회수 이력이 없는 제품 ]");
                log.info("----------------------------------------------------------");
                log.info("제품 분류 코드 : {} / 제품 시리얼 코드 : {}", eachProductCode.getProductCode(), eachProductCode.getProductSerialCode());
                log.info("RFID 칩 코드 : {}", eachProductCode.getRfidChipCode());
                log.info("");
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    // 스캔 데이터 폐기 service
    @Transactional
    public synchronized CompletableFuture<String> sendDiscardData(RfidScanDataDiscardRequestDto sendDiscardDatas) throws InterruptedException {
        log.info("제품 폐기 처리 service");

        String deviceCode = sendDiscardDatas.getMachineId();
        String tag = sendDiscardDatas.getTag();
        String clientCode = sendDiscardDatas.getSelectClientCode();
        String supplierCode = sendDiscardDatas.getSupplierCode();
        List<SendProductCode> productCodes = sendDiscardDatas.getProductCodes();

        // 최종 반환 DTO 객체에 넣어줄 리스트객체 생성
        List<ProductDetailResponseDto> productDetailResponseDtos = new ArrayList<>();
        // 실제 RfidScanHistory 엔티티에 같이 매핑되어 저장될 ProductDetail 엔티티 리스트 객체 생성
        List<ProductDetail> rfidHistoryMappingProductDetail = new ArrayList<>();

        int threadCount = productCodes.size();

        //ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 앱으로부터 넘겨 받은 스캔 데이터들의 제품 분류 코드와 각 제품 시리얼 코드 리스트를 하나씩 조회하여 출고 이력 처리
        for (int i = 0; i < productCodes.size(); i++) {

            scanDiscardDetail(
                    productCodes.get(i),
                    supplierCode,
                    clientCode,
                    productDetailResponseDtos,
                    rfidHistoryMappingProductDetail);

            latch.countDown();

        }

        latch.await();
        Thread.sleep(1500);

        return new AsyncResult<>("스캔 제품들 폐기 처리 완료").completable();
    }


    // 스캔 데이터 폐기 상세 로직 함수
    @Async("threadPoolTaskExecutor")
    protected void scanDiscardDetail(
            SendProductCode eachProductCode,
            String supplierCode,
            String clientCode,
            List<ProductDetailResponseDto> productDetailResponseDtos,
            List<ProductDetail> rfidHistoryMappingProductDetail) {

        // 기존에 저장된 대분류 제품 스캔 이력(ProductDetail)이 존재하는지 확인
        ProductDetail productDetail = productDetailQueryData.checkRemainProductDetail(
                eachProductCode.getRfidChipCode(), eachProductCode.getProductSerialCode(),
                eachProductCode.getProductCode(), supplierCode, clientCode);

        // 만약 대분류 제품 스캔 입고 이력(ProductDetail)이 기존에 존재하지 않았을 경우, 회수 처리할 데이터가 없음을 예외 처리
        if (productDetail == null) {
            log.info("");
            log.info("[ 이력이 없는 제품 ]");
            log.info("----------------------------------------------------------");
            log.info("제품 분류 코드 : {} / 제품 시리얼 코드 : {}", eachProductCode.getProductCode(), eachProductCode.getProductSerialCode());
            log.info("고객사 코드 : {} / 공급사 코드 : {}", clientCode, supplierCode);
            log.info("RFID 칩 코드 : {}", eachProductCode.getRfidChipCode());
            log.info("");

        } else { // 스캔한 데이터들을 폐기 처리
            productDetail = productDetailQueryData.scanDiscardUpdateStatusAndReadingAt(productDetail);

            // 최종 확인 용 DTO 객체의 productDetails 속성에 들어갈 ProductDetail 정보 list 데이터 저장
            productDetailResponseDtos.add(
                    ProductDetailResponseDto.builder()
                            .productDetailId(productDetail.getProductDetailId())
                            .rfidChipCode(productDetail.getRfidChipCode())
                            .productSerialCode(productDetail.getProductSerialCode())
                            .productCode(productDetail.getProductCode())
                            .supplierCode(productDetail.getSupplierCode())
                            .clientCode(productDetail.getClientCode())
                            .status(productDetail.getStatus())
                            .cycle(productDetail.getCycle())
                            .latestReadingAt(productDetail.getLatestReadingAt().toString())
                            .build()
            );

            // RfidScanHistory 엔티티에 같이 매핑되어 저장하게할 ProductDetail 엔티티를 리스트에 저장
            rfidHistoryMappingProductDetail.add(productDetail);

            if (productDetailHistoryQueryData.checkPreviewHistory(
                    eachProductCode.getRfidChipCode(),
                    eachProductCode.getProductSerialCode(),
                    eachProductCode.getProductCode(),
                    supplierCode, clientCode, "폐기", productDetail.getCycle())) {

                // 각 제품 스캔 상세 이력
                ProductDetailHistory productDetailHistory = ProductDetailHistory.builder()
                        .rfidChipCode(eachProductCode.getRfidChipCode())
                        .productSerialCode(eachProductCode.getProductSerialCode())
                        .productCode(eachProductCode.getProductCode())
                        .supplierCode(supplierCode)
                        .clientCode(clientCode)
                        .status("폐기")
                        .cycle(productDetail.getCycle())
                        .latestReadingAt(LocalDateTime.now())
                        .build();

                // 저장
                productDetailHistoryRepository.save(productDetailHistory);

                // 기존에 동일한 기기로 동일한 공급사가 동일한 고객사에 동일한 제품을 출고 시킨 RFID 스캔 이력이 존재하지 않으면 이력을 저장
                if (!discardHistoryQueryData.checkProductDiscard(productDetail.getProductCode(), productDetail.getProductSerialCode())) {

                    // 폐기 이력 정보
                    DiscardHistory discardHistory = DiscardHistory.builder()
                            .supplierCode(productDetailHistory.getSupplierCode())
                            .clientCode(productDetailHistory.getClientCode())
                            .productCode(productDetailHistory.getProductCode())
                            .productSerialCode(productDetailHistory.getProductSerialCode())
                            .rfidChipCode(productDetailHistory.getRfidChipCode())
                            .discardAt(LocalDateTime.now())
                            .reason("")
                            .build();

                    // 저장
                    discardHistoryRepository.save(discardHistory);

                    /**
                     // 확인용 반환 DTO 객체에 저장
                     responseDto = RfidScanDataDiscardResponseDto.builder()
                     .machineId(deviceCode)
                     .tag(tag)
                     .selectClientCode(discardHistory.getClientCode())
                     .supplierCode(discardHistory.getSupplierCode())
                     .status(productDetailHistory.getStatus())
                     .discardAt(discardHistory.getDiscardAt().toString())
                     .productDetails(productDetailResponseDtos)
                     .build();
                     **/
                }
            }
        }

    }

}
