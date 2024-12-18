package com.rfid.circularlabs_rfid_backend.configuration;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.rfid.circularlabs_rfid_backend.device.domain.Device;
import com.rfid.circularlabs_rfid_backend.member.domain.Member;
import com.rfid.circularlabs_rfid_backend.process.repository.DiscardHistoryRepository;
import com.rfid.circularlabs_rfid_backend.product.domain.ProductDetail;
import com.rfid.circularlabs_rfid_backend.product.domain.ProductDetailHistory;
import com.rfid.circularlabs_rfid_backend.product.repository.ProductDetailHistoryRepository;
import com.rfid.circularlabs_rfid_backend.product.repository.ProductDetailRepository;
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
import com.rfid.circularlabs_rfid_backend.scan.request.SendOrderCount;
import com.rfid.circularlabs_rfid_backend.scan.request.SendProductCode;
import com.rfid.circularlabs_rfid_backend.scan.service.RfidScanDataService_v2;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.repeat.RepeatStatus;

import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.rfid.circularlabs_rfid_backend.member.domain.QMember.member;
import static com.rfid.circularlabs_rfid_backend.device.domain.QDevice.device;

@RequiredArgsConstructor
//@Configuration
@Component
public class BatchParallelConfig {
    private static final Logger log = LoggerFactory.getLogger(BatchParallelConfig.class);

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final JobLauncher jobLauncher;
    private final JPAQueryFactory jpaQueryFactory;
    private final ProductDetailRepository productDetailRepository;
    private final ProductDetailHistoryRepository productDetailHistoryRepository;
    private final ProductDetailQueryDataV2 productDetailQueryDataV2;
    private final EntityManager entityManager;
    private final ProductDetailHistoryQueryDataV2 productDetailHistoryQueryDataV2;
    private final SecureRandom secureRandom = new SecureRandom();
    private List<ProductDetail> scanProductDetails = new ArrayList<>();

    // [ProductDetail] 배치 실행 (출고 / 입고)
    public List<ProductDetail> launchProductDetail(String status, List<SendProductCode> scanDatas, String supplierCode, String clientCode) throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        Map<String, JobParameter> parameters = new HashMap<>();
        parameters.put("random", new JobParameter(this.secureRandom.nextLong()));

        this.scanProductDetails = new ArrayList<>();

        if (status.equals("출고")) {
            jobLauncher.run(parallelOutJob(scanDatas, supplierCode, clientCode), new JobParameters(parameters));
        } else if (status.equals("입고")) {
            jobLauncher.run(parallelInJob(scanDatas, supplierCode, clientCode), new JobParameters(parameters));
        }

        List<ProductDetail> newScanProductDetails = this.scanProductDetails;
        this.scanProductDetails = new ArrayList<>();

        return newScanProductDetails;
    }

    // [ProductDetailHistory] 배치 실행
    public void launchProductDetailHistory(String status, List<RfidScanHistory> rfidScanHistoryList, List<ProductDetail> useProductDetailList)
            throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        Map<String, JobParameter> parameters = new HashMap<>();
        parameters.put("random", new JobParameter(this.secureRandom.nextLong()));

        if (status.equals("출고")) {
            jobLauncher.run(parallelOutProductDetailHistoryJob(rfidScanHistoryList, useProductDetailList), new JobParameters(parameters));
        } else if (status.equals("입고")) {
            jobLauncher.run(parallelInProductDetailHistoryJob(rfidScanHistoryList, useProductDetailList), new JobParameters(parameters));
        }

    }

    // [ProductDetail] 출고 저장 Job
    public Job parallelOutJob(List<SendProductCode> scanOutDatas, String supplierCode, String clientCode) {

        if(scanOutDatas.size() < 50){
            Flow flow1 = new FlowBuilder<Flow>("flow1")
                    .start(parallelOutStep(scanOutDatas, supplierCode, clientCode))
                    .build();

            Flow parallelStepFlow = new FlowBuilder<Flow>("parallelStepFlow")
                    .split(new SimpleAsyncTaskExecutor())
                    .add(flow1)
                    .build();

            return jobBuilderFactory.get("parallelOutJob")
                    .start(parallelStepFlow)
                    .build()
                    .build();

        }else if(scanOutDatas.size() >= 50){
            int scanOutDatas_separate_section = scanOutDatas.size() / 5; // 100개로 가정 했을 때 20개
            List<SendProductCode> outDatas1 = scanOutDatas.subList(0, scanOutDatas_separate_section);
            List<SendProductCode> outDatas2 = scanOutDatas.subList(scanOutDatas_separate_section, scanOutDatas_separate_section * 2);
            List<SendProductCode> outDatas3 = scanOutDatas.subList(scanOutDatas_separate_section * 2, (scanOutDatas_separate_section * 2) + scanOutDatas_separate_section);
            List<SendProductCode> outDatas4 = scanOutDatas.subList((scanOutDatas_separate_section * 2) + scanOutDatas_separate_section, (scanOutDatas_separate_section * 2) + (scanOutDatas_separate_section * 2));
            List<SendProductCode> outDatas5 = scanOutDatas.subList((scanOutDatas_separate_section * 2) + (scanOutDatas_separate_section * 2), scanOutDatas.size());

            Flow flow1 = new FlowBuilder<Flow>("flow1")
                    .start(parallelOutStep(outDatas1, supplierCode, clientCode))
                    .build();

            Flow flow2 = new FlowBuilder<Flow>("flow2")
                    .start(parallelOutStep(outDatas2, supplierCode, clientCode))
                    .build();

            Flow flow3 = new FlowBuilder<Flow>("flow3")
                    .start(parallelOutStep(outDatas3, supplierCode, clientCode))
                    .build();

            Flow flow4 = new FlowBuilder<Flow>("flow4")
                    .start(parallelOutStep(outDatas4, supplierCode, clientCode))
                    .build();

            Flow flow5 = new FlowBuilder<Flow>("flow5")
                    .start(parallelOutStep(outDatas5, supplierCode, clientCode))
                    .build();

            Flow parallelStepFlow = new FlowBuilder<Flow>("parallelStepFlow")
                    .split(new SimpleAsyncTaskExecutor())
                    .add(flow1, flow2, flow3, flow4, flow5)
                    .build();

            return jobBuilderFactory.get("parallelOutJob")
                    .start(parallelStepFlow)
                    .build()
                    .build();
        }

        return null;
    }


    // [ProductDetail] 출고 저장 Step
    public Step parallelOutStep(List<SendProductCode> scanOutDatas, String supplierCode, String clientCode) {
        return stepBuilderFactory.get("parallelOutStep1")
                .tasklet((contribution, chunkContext) -> {

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

                    scanProductDetails.addAll(finalProductDetails);

                    return RepeatStatus.FINISHED;
                })
                .build();
    }


    // [ProductDetailHistory] 출고 저장 Job
    public Job parallelOutProductDetailHistoryJob(List<RfidScanHistory> rfidScanHistoryList, List<ProductDetail> useProductDetailList) {

        if(useProductDetailList.size() < 50){
            Flow flow1 = new FlowBuilder<Flow>("flow1")
                    .start(parallelOutProductDetailHistoryStep(rfidScanHistoryList, useProductDetailList))
                    .build();

            Flow parallelStepFlow = new FlowBuilder<Flow>("parallelStepFlow")
                    .split(new SimpleAsyncTaskExecutor())
                    .add(flow1)
                    .build();

            return jobBuilderFactory.get("parallelOutJob")
                    .start(parallelStepFlow)
                    .build()
                    .build();

        }else if(useProductDetailList.size() >= 50){
            int scanOutDatas_separate_section = useProductDetailList.size() / 5; // 100개로 가정 했을 때 20개
            List<ProductDetail> outDatas1 = useProductDetailList.subList(0, scanOutDatas_separate_section);
            List<ProductDetail> outDatas2 = useProductDetailList.subList(scanOutDatas_separate_section, scanOutDatas_separate_section * 2);
            List<ProductDetail> outDatas3 = useProductDetailList.subList(scanOutDatas_separate_section * 2, (scanOutDatas_separate_section * 2) + scanOutDatas_separate_section);
            List<ProductDetail> outDatas4 = useProductDetailList.subList((scanOutDatas_separate_section * 2) + scanOutDatas_separate_section, (scanOutDatas_separate_section * 2) + (scanOutDatas_separate_section * 2));
            List<ProductDetail> outDatas5 = useProductDetailList.subList((scanOutDatas_separate_section * 2) + (scanOutDatas_separate_section * 2), useProductDetailList.size());

            Flow flow1 = new FlowBuilder<Flow>("flow1")
                    .start(parallelOutProductDetailHistoryStep(rfidScanHistoryList, outDatas1))
                    .build();

            Flow flow2 = new FlowBuilder<Flow>("flow2")
                    .start(parallelOutProductDetailHistoryStep(rfidScanHistoryList, outDatas2))
                    .build();

            Flow flow3 = new FlowBuilder<Flow>("flow3")
                    .start(parallelOutProductDetailHistoryStep(rfidScanHistoryList, outDatas3))
                    .build();

            Flow flow4 = new FlowBuilder<Flow>("flow4")
                    .start(parallelOutProductDetailHistoryStep(rfidScanHistoryList, outDatas4))
                    .build();

            Flow flow5 = new FlowBuilder<Flow>("flow5")
                    .start(parallelOutProductDetailHistoryStep(rfidScanHistoryList, outDatas5))
                    .build();

            Flow parallelStepFlow = new FlowBuilder<Flow>("parallelStepFlow")
                    .split(new SimpleAsyncTaskExecutor())
                    .add(flow1, flow2, flow3, flow4, flow5)
                    .build();

            return jobBuilderFactory.get("parallelOutJob")
                    .start(parallelStepFlow)
                    .build()
                    .build();
        }

        return null;
    }


    // [ProductDetailHistory] 출고 저장 Step
    public Step parallelOutProductDetailHistoryStep(List<RfidScanHistory> rfidScanHistoryList, List<ProductDetail> useProductDetailList) {
        return stepBuilderFactory.get("parallelOutStep2")
                .tasklet((contribution, chunkContext) -> {

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

                    return RepeatStatus.FINISHED;
                })
                .build();
    }


    // [ProductDetail] 입고 저장 Job
    public Job parallelInJob(List<SendProductCode> scanInDatas, String supplierCode, String clientCode) {

        if(scanInDatas.size() < 50){
            Flow flow1 = new FlowBuilder<Flow>("flow1")
                    .start(parallelInStep(scanInDatas, supplierCode, clientCode))
                    .build();

            Flow parallelStepFlow = new FlowBuilder<Flow>("parallelStepFlow")
                    .split(new SimpleAsyncTaskExecutor())
                    .add(flow1)
                    .build();

            return jobBuilderFactory.get("parallelInJob")
                    .start(parallelStepFlow)
                    .build()
                    .build();

        }else if(scanInDatas.size() >= 50){
            int scanInDatas_separate_section = scanInDatas.size() / 5; // 100개로 가정 했을 때 20개
            List<SendProductCode> inDatas1 = scanInDatas.subList(0, scanInDatas_separate_section);
            List<SendProductCode> inDatas2 = scanInDatas.subList(scanInDatas_separate_section, scanInDatas_separate_section * 2);
            List<SendProductCode> inDatas3 = scanInDatas.subList(scanInDatas_separate_section * 2, (scanInDatas_separate_section * 2) + scanInDatas_separate_section);
            List<SendProductCode> inDatas4 = scanInDatas.subList((scanInDatas_separate_section * 2) + scanInDatas_separate_section, (scanInDatas_separate_section * 2) + (scanInDatas_separate_section * 2));
            List<SendProductCode> inDatas5 = scanInDatas.subList((scanInDatas_separate_section * 2) + (scanInDatas_separate_section * 2), scanInDatas.size());

            Flow flow1 = new FlowBuilder<Flow>("flow1")
                    .start(parallelInStep(inDatas1, supplierCode, clientCode))
                    .build();

            Flow flow2 = new FlowBuilder<Flow>("flow2")
                    .start(parallelInStep(inDatas2, supplierCode, clientCode))
                    .build();

            Flow flow3 = new FlowBuilder<Flow>("flow3")
                    .start(parallelInStep(inDatas3, supplierCode, clientCode))
                    .build();

            Flow flow4 = new FlowBuilder<Flow>("flow4")
                    .start(parallelInStep(inDatas4, supplierCode, clientCode))
                    .build();

            Flow flow5 = new FlowBuilder<Flow>("flow5")
                    .start(parallelInStep(inDatas5, supplierCode, clientCode))
                    .build();

            Flow parallelStepFlow = new FlowBuilder<Flow>("parallelStepFlow")
                    .split(new SimpleAsyncTaskExecutor())
                    .add(flow1, flow2, flow3, flow4, flow5)
                    .build();

            return jobBuilderFactory.get("parallelInJob")
                    .start(parallelStepFlow)
                    .build()
                    .build();
        }

        return null;
    }


    // [ProductDetail] 입고 저장 Step
    public Step parallelInStep(List<SendProductCode> scanInDatas, String supplierCode, String clientCode) {
        return stepBuilderFactory.get("parallelOutStep1")
                .tasklet((contribution, chunkContext) -> {

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

                    scanProductDetails.addAll(finalProductDetails);

                    return RepeatStatus.FINISHED;
                })
                .build();
    }


    // [ProductDetailHistory] 입고 저장 Job
    public Job parallelInProductDetailHistoryJob(List<RfidScanHistory> rfidScanHistoryList, List<ProductDetail> useProductDetailList) {

        if(useProductDetailList.size() < 50){
            Flow flow1 = new FlowBuilder<Flow>("flow1")
                    .start(parallelInProductDetailHistoryStep(rfidScanHistoryList, useProductDetailList))
                    .build();

            Flow parallelStepFlow = new FlowBuilder<Flow>("parallelStepFlow")
                    .split(new SimpleAsyncTaskExecutor())
                    .add(flow1)
                    .build();

            return jobBuilderFactory.get("parallelInJob")
                    .start(parallelStepFlow)
                    .build()
                    .build();

        }else if(useProductDetailList.size() >= 50){
            int scanInDatas_separate_section = useProductDetailList.size() / 5; // 100개로 가정 했을 때 20개
            List<ProductDetail> inDatas1 = useProductDetailList.subList(0, scanInDatas_separate_section);
            List<ProductDetail> inDatas2 = useProductDetailList.subList(scanInDatas_separate_section, scanInDatas_separate_section * 2);
            List<ProductDetail> inDatas3 = useProductDetailList.subList(scanInDatas_separate_section * 2, (scanInDatas_separate_section * 2) + scanInDatas_separate_section);
            List<ProductDetail> inDatas4 = useProductDetailList.subList((scanInDatas_separate_section * 2) + scanInDatas_separate_section, (scanInDatas_separate_section * 2) + (scanInDatas_separate_section * 2));
            List<ProductDetail> inDatas5 = useProductDetailList.subList((scanInDatas_separate_section * 2) + (scanInDatas_separate_section * 2), useProductDetailList.size());

            Flow flow1 = new FlowBuilder<Flow>("flow1")
                    .start(parallelInProductDetailHistoryStep(rfidScanHistoryList, inDatas1))
                    .build();

            Flow flow2 = new FlowBuilder<Flow>("flow2")
                    .start(parallelInProductDetailHistoryStep(rfidScanHistoryList, inDatas2))
                    .build();

            Flow flow3 = new FlowBuilder<Flow>("flow3")
                    .start(parallelInProductDetailHistoryStep(rfidScanHistoryList, inDatas3))
                    .build();

            Flow flow4 = new FlowBuilder<Flow>("flow4")
                    .start(parallelInProductDetailHistoryStep(rfidScanHistoryList, inDatas4))
                    .build();

            Flow flow5 = new FlowBuilder<Flow>("flow5")
                    .start(parallelInProductDetailHistoryStep(rfidScanHistoryList, inDatas5))
                    .build();

            Flow parallelStepFlow = new FlowBuilder<Flow>("parallelStepFlow")
                    .split(new SimpleAsyncTaskExecutor())
                    .add(flow1, flow2, flow3, flow4, flow5)
                    .build();

            return jobBuilderFactory.get("parallelInJob")
                    .start(parallelStepFlow)
                    .build()
                    .build();
        }

        return null;
    }


    // [ProductDetailHistory] 입고 저장 Step
    public Step parallelInProductDetailHistoryStep(List<RfidScanHistory> rfidScanHistoryList, List<ProductDetail> useProductDetailList) {
        return stepBuilderFactory.get("parallelOutStep2")
                .tasklet((contribution, chunkContext) -> {

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

                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // [ProductDetail] 배치 실행 (회수 / 세척)
    public List<ProductDetail> launchProductDetail2(String status, List<SendProductCode> scanDatas, String supplierCode) throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        Map<String, JobParameter> parameters = new HashMap<>();
        parameters.put("random", new JobParameter(this.secureRandom.nextLong()));

        this.scanProductDetails = new ArrayList<>();

        if (status.equals("회수")) {
            jobLauncher.run(parallelTurnBackJob(scanDatas, supplierCode), new JobParameters(parameters));
        } else if (status.equals("세척")) {
            jobLauncher.run(parallelCleanJob(), new JobParameters());
        }

        List<ProductDetail> newScanProductDetails = this.scanProductDetails;
        this.scanProductDetails = new ArrayList<>();

        return newScanProductDetails;
    }

    // [ProductDetailHistory] 배치 실행
    public void launchProductDetailHistory2(String status, List<RfidScanHistory> rfidScanHistoryList, List<ProductDetail> useProductDetailList)
            throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        Map<String, JobParameter> parameters = new HashMap<>();
        parameters.put("random", new JobParameter(this.secureRandom.nextLong()));

        if (status.equals("회수")) {
            jobLauncher.run(parallelTurnBackProductDetailHistoryJob(rfidScanHistoryList, useProductDetailList), new JobParameters(parameters));
        } else if (status.equals("세척")) {
            jobLauncher.run(parallelCleanJob(), new JobParameters());
        }

    }

    // [ProductDetail] 회수 저장 Job
    public Job parallelTurnBackJob(List<SendProductCode> scanTurnBackDatas, String supplierCode) {
        if(scanTurnBackDatas.size() < 50){
            Flow flow1 = new FlowBuilder<Flow>("flow1")
                    .start(parallelTurnBackStep(scanTurnBackDatas, supplierCode))
                    .build();

            Flow parallelStepFlow = new FlowBuilder<Flow>("parallelStepFlow")
                    .split(new SimpleAsyncTaskExecutor())
                    .add(flow1)
                    .build();

            return jobBuilderFactory.get("parallelTurnBackJob")
                    .start(parallelStepFlow)
                    .build()
                    .build();

        }else if(scanTurnBackDatas.size() >= 50){
            int scanTurnBackDatas_separate_section = scanTurnBackDatas.size() / 5; // 100개로 가정 했을 때 20개
            List<SendProductCode> turnBackDatas1 = scanTurnBackDatas.subList(0, scanTurnBackDatas_separate_section);
            List<SendProductCode> turnBackDatas2 = scanTurnBackDatas.subList(scanTurnBackDatas_separate_section, scanTurnBackDatas_separate_section * 2);
            List<SendProductCode> turnBackDatas3 = scanTurnBackDatas.subList(scanTurnBackDatas_separate_section * 2, (scanTurnBackDatas_separate_section * 2) + scanTurnBackDatas_separate_section);
            List<SendProductCode> turnBackDatas4 = scanTurnBackDatas.subList((scanTurnBackDatas_separate_section * 2) + scanTurnBackDatas_separate_section, (scanTurnBackDatas_separate_section * 2) + (scanTurnBackDatas_separate_section * 2));
            List<SendProductCode> turnBackDatas5 = scanTurnBackDatas.subList((scanTurnBackDatas_separate_section * 2) + (scanTurnBackDatas_separate_section * 2), scanTurnBackDatas.size());

            Flow flow1 = new FlowBuilder<Flow>("flow1")
                    .start(parallelTurnBackStep(turnBackDatas1, supplierCode))
                    .build();

            Flow flow2 = new FlowBuilder<Flow>("flow2")
                    .start(parallelTurnBackStep(turnBackDatas2, supplierCode))
                    .build();

            Flow flow3 = new FlowBuilder<Flow>("flow3")
                    .start(parallelTurnBackStep(turnBackDatas3, supplierCode))
                    .build();

            Flow flow4 = new FlowBuilder<Flow>("flow4")
                    .start(parallelTurnBackStep(turnBackDatas4, supplierCode))
                    .build();

            Flow flow5 = new FlowBuilder<Flow>("flow5")
                    .start(parallelTurnBackStep(turnBackDatas5, supplierCode))
                    .build();

            Flow parallelStepFlow = new FlowBuilder<Flow>("parallelStepFlow")
                    .split(new SimpleAsyncTaskExecutor())
                    .add(flow1, flow2, flow3, flow4, flow5)
                    .build();

            return jobBuilderFactory.get("parallelTurnBackJob")
                    .start(parallelStepFlow)
                    .build()
                    .build();
        }

        return null;
    }


    // [ProductDetail] 회수 저장 Step
    public Step parallelTurnBackStep(List<SendProductCode> scanTurnBackDatas, String supplierCode) {
        return stepBuilderFactory.get("parallelTurnBackStep")
                .tasklet((contribution, chunkContext) -> {
                    List<ProductDetail> finalProductDetails = new ArrayList<>();

                    // 기존에 존재하는 Product 데이터들
                    List<ProductDetail> existPrevProductDetails = scanTurnBackDatas.stream()
                            .filter(eachExistPrevTurnBackData -> productDetailQueryDataV2.checkBeforeStatusProductDetail(
                                    eachExistPrevTurnBackData.getProductSerialCode(),
                                    eachExistPrevTurnBackData.getProductCode(), supplierCode) != null )
                            .map(eachNullTurnBackData -> productDetailQueryDataV2.checkBeforeStatusProductDetail(
                                    eachNullTurnBackData.getProductSerialCode(),
                                    eachNullTurnBackData.getProductCode(), supplierCode))
                            .collect(Collectors.toList());

                    if (!existPrevProductDetails.isEmpty()) {
                        if(existPrevProductDetails.stream().map(ProductDetail::getStatus)
                                .distinct().anyMatch(status -> !status.equals("입고"))){
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

                    scanProductDetails.addAll(finalProductDetails);

                    return RepeatStatus.FINISHED;
                })
                .build();
    }


    // [ProductDetailHistory] 회수 저장 Job
    public Job parallelTurnBackProductDetailHistoryJob(List<RfidScanHistory> rfidScanHistoryList, List<ProductDetail> useProductDetailList) {

        if(useProductDetailList.size() < 50){
            Flow flow1 = new FlowBuilder<Flow>("flow1")
                    .start(parallelTurnBackProductDetailHistoryStep(rfidScanHistoryList, useProductDetailList))
                    .build();

            Flow parallelStepFlow = new FlowBuilder<Flow>("parallelStepFlow")
                    .split(new SimpleAsyncTaskExecutor())
                    .add(flow1)
                    .build();

            return jobBuilderFactory.get("parallelTurnBackJob")
                    .start(parallelStepFlow)
                    .build()
                    .build();

        }else if(useProductDetailList.size() >= 50){
            int scanTurnBackDatas_separate_section = useProductDetailList.size() / 5; // 100개로 가정 했을 때 20개
            List<ProductDetail> turnBackDatas1 = useProductDetailList.subList(0, scanTurnBackDatas_separate_section);
            List<ProductDetail> turnBackDatas2 = useProductDetailList.subList(scanTurnBackDatas_separate_section, scanTurnBackDatas_separate_section * 2);
            List<ProductDetail> turnBackDatas3 = useProductDetailList.subList(scanTurnBackDatas_separate_section * 2, (scanTurnBackDatas_separate_section * 2) + scanTurnBackDatas_separate_section);
            List<ProductDetail> turnBackDatas4 = useProductDetailList.subList((scanTurnBackDatas_separate_section * 2) + scanTurnBackDatas_separate_section, (scanTurnBackDatas_separate_section * 2) + (scanTurnBackDatas_separate_section * 2));
            List<ProductDetail> turnBackDatas5 = useProductDetailList.subList((scanTurnBackDatas_separate_section * 2) + (scanTurnBackDatas_separate_section * 2), useProductDetailList.size());

            Flow flow1 = new FlowBuilder<Flow>("flow1")
                    .start(parallelTurnBackProductDetailHistoryStep(rfidScanHistoryList, turnBackDatas1))
                    .build();

            Flow flow2 = new FlowBuilder<Flow>("flow2")
                    .start(parallelTurnBackProductDetailHistoryStep(rfidScanHistoryList, turnBackDatas2))
                    .build();

            Flow flow3 = new FlowBuilder<Flow>("flow3")
                    .start(parallelTurnBackProductDetailHistoryStep(rfidScanHistoryList, turnBackDatas3))
                    .build();

            Flow flow4 = new FlowBuilder<Flow>("flow4")
                    .start(parallelTurnBackProductDetailHistoryStep(rfidScanHistoryList, turnBackDatas4))
                    .build();

            Flow flow5 = new FlowBuilder<Flow>("flow5")
                    .start(parallelTurnBackProductDetailHistoryStep(rfidScanHistoryList, turnBackDatas5))
                    .build();

            Flow parallelStepFlow = new FlowBuilder<Flow>("parallelStepFlow")
                    .split(new SimpleAsyncTaskExecutor())
                    .add(flow1, flow2, flow3, flow4, flow5)
                    .build();

            return jobBuilderFactory.get("parallelTurnBackJob")
                    .start(parallelStepFlow)
                    .build()
                    .build();
        }

        return null;
    }


    // [ProductDetailHistory] 회수 저장 Step
    public Step parallelTurnBackProductDetailHistoryStep(List<RfidScanHistory> rfidScanHistoryList, List<ProductDetail> useProductDetailList) {
        return stepBuilderFactory.get("parallelOutStep2")
                .tasklet((contribution, chunkContext) -> {

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

                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    // [세척 Batch]

    public Job parallelCleanJob() {

        Flow flow1 = new FlowBuilder<Flow>("flow1")
                .start(parallelCleanStep1())
                .build();

        Flow flow2 = new FlowBuilder<Flow>("flow2")
                .start(parallelCleanStep2())
                .build();

        Flow parallelStepFlow = new FlowBuilder<Flow>("parallelStepFlow")
                .split(new SimpleAsyncTaskExecutor())
                .add(flow1, flow2)
                .build();

        return jobBuilderFactory.get("parallelCleanJob")
                .start(parallelStepFlow)
                .build()
                .build();
    }


    public Step parallelCleanStep1() {
        return stepBuilderFactory.get("parallelCleanStep1")
                .tasklet((contribution, chunkContext) -> {
                    List<Member> memberList = jpaQueryFactory
                            .selectFrom(member)
                            .fetch();

                    for (Member eachMember : memberList) {
                        log.info("[회원] 정보 : {}", eachMember.getClientCompany());
                    }

                    return RepeatStatus.FINISHED;
                })
                .build();
    }


    public Step parallelCleanStep2() {
        return stepBuilderFactory.get("parallelCleanStep2")
                .tasklet((contribution, chunkContext) -> {
                    List<Device> deviceList = jpaQueryFactory
                            .selectFrom(device)
                            .fetch();

                    for (Device eachDevice : deviceList) {
                        log.info("[기기] 정보 : {}", eachDevice.getDeviceCode());
                    }

                    return RepeatStatus.FINISHED;
                })
                .build();
    }
}
