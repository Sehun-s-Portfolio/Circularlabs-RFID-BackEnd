package com.rfid.circularlabs_rfid_backend.share;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.rfid.circularlabs_rfid_backend.device.domain.Device;
import com.rfid.circularlabs_rfid_backend.member.domain.Member;
import com.rfid.circularlabs_rfid_backend.product.domain.ProductDetail;
import com.rfid.circularlabs_rfid_backend.product.domain.ProductDetailHistory;
import com.rfid.circularlabs_rfid_backend.product.repository.ProductDetailHistoryRepository;
import com.rfid.circularlabs_rfid_backend.product.repository.ProductDetailRepository;
import com.rfid.circularlabs_rfid_backend.query2.productdetail.ProductDetailQueryDataV2;
import com.rfid.circularlabs_rfid_backend.query2.productdetailhistory.ProductDetailHistoryQueryDataV2;
import com.rfid.circularlabs_rfid_backend.query2.scandata.ScanDataQueryDataV3;
import com.rfid.circularlabs_rfid_backend.scan.domain.RfidScanHistory;
import com.rfid.circularlabs_rfid_backend.scan.request.SendProductCode;
import com.rfid.circularlabs_rfid_backend.scan.response.ProductDetailscanResponseDto;
import lombok.RequiredArgsConstructor;
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
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.rfid.circularlabs_rfid_backend.device.domain.QDevice.device;
import static com.rfid.circularlabs_rfid_backend.member.domain.QMember.member;
import static com.rfid.circularlabs_rfid_backend.product.domain.QProductDetail.productDetail;

@RequiredArgsConstructor
//@Configuration
@Component
public class BatchService {
    private static final Logger log = LoggerFactory.getLogger(BatchService.class);

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final JobLauncher jobLauncher;
    private final JPAQueryFactory jpaQueryFactory;
    private final ProductDetailRepository productDetailRepository;
    private final EntityManager entityManager;
    private final SecureRandom secureRandom = new SecureRandom();
    private List<ProductDetailscanResponseDto> totalResponseProductDetails = new ArrayList<>();
    private List<Long> needUpdateProductDetailIds = new ArrayList<>();

    // [ProductDetail] 배치 실행 (출고 / 입고)
    public HashMap<String, Object> launchProductDetail(String status, List<SendProductCode> scanDatas, String supplierCode, String clientCode) throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        Map<String, JobParameter> parameters = new HashMap<>();
        parameters.put("random", new JobParameter(this.secureRandom.nextLong()));

        this.totalResponseProductDetails = new ArrayList<>();
        this.needUpdateProductDetailIds = new ArrayList<>();

        HashMap<String, Object> responseProductDetailInfo = new HashMap<>();

        if (status.equals("출고")) {
            //jobLauncher.run(parallelOutJob(scanDatas, supplierCode, clientCode), new JobParameters(parameters));
        } else if (status.equals("입고")) {
            jobLauncher.run(parallelInJob(scanDatas, supplierCode, clientCode), new JobParameters(parameters));
        }

        if(!this.needUpdateProductDetailIds.isEmpty()){
            responseProductDetailInfo.put("updateProductDetailIds", this.needUpdateProductDetailIds);
        }

        responseProductDetailInfo.put("totalResponseProductDetails", this.totalResponseProductDetails);

        return responseProductDetailInfo;
    }

    // [ProductDetail] 입고 저장 Job
    public Job parallelInJob(List<SendProductCode> scanInDatas, String supplierCode, String clientCode) {

        if (scanInDatas.size() < 50) {
            Flow flow1 = new FlowBuilder<Flow>("inFlow")
                    .start(parallelInStep(scanInDatas, supplierCode, clientCode))
                    .build();

            Flow parallelStepFlow = new FlowBuilder<Flow>("parallelStepInFlow")
                    .split(new SimpleAsyncTaskExecutor())
                    .add(flow1)
                    .build();

            return jobBuilderFactory.get("parallelInJob")
                    .start(parallelStepFlow)
                    .build()
                    .build();

        } else if (scanInDatas.size() >= 50) {
            int scanInDatas_separate_section = scanInDatas.size() / 5; // 100개로 가정 했을 때 20개
            List<SendProductCode> inDatas1 = scanInDatas.subList(0, scanInDatas_separate_section);
            List<SendProductCode> inDatas2 = scanInDatas.subList(scanInDatas_separate_section, scanInDatas_separate_section * 2);
            List<SendProductCode> inDatas3 = scanInDatas.subList(scanInDatas_separate_section * 2, (scanInDatas_separate_section * 2) + scanInDatas_separate_section);
            List<SendProductCode> inDatas4 = scanInDatas.subList((scanInDatas_separate_section * 2) + scanInDatas_separate_section, (scanInDatas_separate_section * 2) + (scanInDatas_separate_section * 2));
            List<SendProductCode> inDatas5 = scanInDatas.subList((scanInDatas_separate_section * 2) + (scanInDatas_separate_section * 2), scanInDatas.size());

            Flow flow1 = new FlowBuilder<Flow>("inFlow1")
                    .start(parallelInStep(inDatas1, supplierCode, clientCode))
                    .build();

            Flow flow2 = new FlowBuilder<Flow>("inFlow2")
                    .start(parallelInStep(inDatas2, supplierCode, clientCode))
                    .build();

            Flow flow3 = new FlowBuilder<Flow>("inFlow3")
                    .start(parallelInStep(inDatas3, supplierCode, clientCode))
                    .build();

            Flow flow4 = new FlowBuilder<Flow>("inFlow4")
                    .start(parallelInStep(inDatas4, supplierCode, clientCode))
                    .build();

            Flow flow5 = new FlowBuilder<Flow>("inFlow5")
                    .start(parallelInStep(inDatas5, supplierCode, clientCode))
                    .build();

            Flow parallelStepFlow = new FlowBuilder<Flow>("parallelStepInFlow")
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
        return stepBuilderFactory.get("parallelInStep")
                .tasklet((contribution, chunkContext) -> {

                    // 0 -- 초기 1 -- 업데이트 3 -- 재등록
                    LocalDateTime now = LocalDateTime.now();

                    List<ProductDetail> needToSaveProductDetail = new ArrayList<>();
                    List<Long> updateProductDetailIds = new ArrayList<>();

                    List<ProductDetailscanResponseDto> responseProductDetails = scanInDatas.stream()
                            .map(eachScanData -> {
                                ProductDetail result = jpaQueryFactory
                                        .select(productDetail)
                                        .from(productDetail)
                                        .where(productDetail.productSerialCode.eq(eachScanData.getProductSerialCode()))
                                        .orderBy(productDetail.latestReadingAt.desc())
                                        .limit(1)
                                        .fetchOne();

                                if (result == null) {

                                    ProductDetail createProductDetail = ProductDetail.builder()
                                            .rfidChipCode(eachScanData.getRfidChipCode())
                                            .productSerialCode(eachScanData.getProductSerialCode())
                                            .productCode(eachScanData.getProductCode())
                                            .supplierCode(supplierCode)
                                            .clientCode(clientCode)
                                            .status("입고")
                                            .cycle(0)
                                            .latestReadingAt(LocalDateTime.now())
                                            .build();

                                    needToSaveProductDetail.add(createProductDetail);

                                    return ProductDetailscanResponseDto.builder()
                                            .rfidChipCode(eachScanData.getRfidChipCode())
                                            .productSerialCode(eachScanData.getProductSerialCode())
                                            .productCode(eachScanData.getProductCode())
                                            .supplierCode(supplierCode)
                                            .clientCode(clientCode)
                                            .status("입고")
                                            .cycle(0)
                                            .latestReadingAt(LocalDateTime.now().toString())
                                            .dataState(0)
                                            .build();

                                } else {
                                    if (result.getStatus().equals("입고") && now.isBefore(result.getLatestReadingAt().plusHours(1))) {

                                        return ProductDetailscanResponseDto.builder()
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

                                    } else if (!result.getProductSerialCode().isEmpty()) {

                                        updateProductDetailIds.add(result.getProductDetailId());

                                        return ProductDetailscanResponseDto.builder()
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
                                    }
                                }

                                return null;
                            })
                            .collect(Collectors.toList());


                    if (!updateProductDetailIds.isEmpty()) {
                        this.needUpdateProductDetailIds.addAll(updateProductDetailIds);
                    }

                    if (!needToSaveProductDetail.isEmpty()) {
                        productDetailRepository.saveAll(needToSaveProductDetail);
                    }

                    this.totalResponseProductDetails.addAll(responseProductDetails);

                    return RepeatStatus.FINISHED;
                })
                .build();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    // [ProductDetail] 배치 실행 (회수 / 세척)
    public HashMap<String, Object> launchProductDetail2(String status, List<SendProductCode> scanDatas, String supplierCode) throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        Map<String, JobParameter> parameters = new HashMap<>();
        parameters.put("random", new JobParameter(this.secureRandom.nextLong()));

        this.totalResponseProductDetails = new ArrayList<>();
        this.needUpdateProductDetailIds = new ArrayList<>();

        HashMap<String, Object> responseProductDetailInfo = new HashMap<>();

        if (status.equals("회수")) {
            log.info("배치 돌릴 스캔 데이터 수 : {}", scanDatas.size());
            jobLauncher.run(parallelTurnBackJob(scanDatas, supplierCode), new JobParameters(parameters));
        } else if (status.equals("세척")) {
            //jobLauncher.run(parallelCleanJob(), new JobParameters());
        }

        if(!this.needUpdateProductDetailIds.isEmpty()){
            responseProductDetailInfo.put("updateProductDetailIds", this.needUpdateProductDetailIds);
        }

        responseProductDetailInfo.put("totalResponseProductDetails", this.totalResponseProductDetails);

        return responseProductDetailInfo;
    }


    // [ProductDetail] 회수 저장 Job
    public Job parallelTurnBackJob(List<SendProductCode> scanTurnBackDatas, String supplierCode) {
        if (scanTurnBackDatas.size() < 50) {
            Flow flow1 = new FlowBuilder<Flow>("turnBackflow")
                    .start(parallelTurnBackStep(scanTurnBackDatas, supplierCode, "turnBackflow"))
                    .build();

            Flow parallelStepFlow = new FlowBuilder<Flow>("parallelStepTurnBackFlow")
                    .split(new SimpleAsyncTaskExecutor())
                    .add(flow1)
                    .build();

            return jobBuilderFactory.get("parallelTurnBackJob")
                    .start(parallelStepFlow)
                    .build()
                    .build();

        } else if (scanTurnBackDatas.size() >= 50) {
            int scanTurnBackDatas_separate_section = scanTurnBackDatas.size() / 5; // 100개로 가정 했을 때 20개
            List<SendProductCode> turnBackDatas1 = scanTurnBackDatas.subList(0, scanTurnBackDatas_separate_section);
            List<SendProductCode> turnBackDatas2 = scanTurnBackDatas.subList(scanTurnBackDatas_separate_section, scanTurnBackDatas_separate_section * 2);
            List<SendProductCode> turnBackDatas3 = scanTurnBackDatas.subList(scanTurnBackDatas_separate_section * 2, scanTurnBackDatas_separate_section * 3);
            List<SendProductCode> turnBackDatas4 = scanTurnBackDatas.subList(scanTurnBackDatas_separate_section * 3, scanTurnBackDatas_separate_section * 4);
            List<SendProductCode> turnBackDatas5 = scanTurnBackDatas.subList(scanTurnBackDatas_separate_section * 4, scanTurnBackDatas.size());


            Flow flow1 = new FlowBuilder<Flow>("turnBackflow1")
                    .start(parallelTurnBackStep(turnBackDatas1, supplierCode, "turnBackflow1"))
                    .build();

            Flow flow2 = new FlowBuilder<Flow>("turnBackflow2")
                    .start(parallelTurnBackStep(turnBackDatas2, supplierCode, "turnBackflow2"))
                    .build();

            Flow flow3 = new FlowBuilder<Flow>("turnBackflow3")
                    .start(parallelTurnBackStep(turnBackDatas3, supplierCode, "turnBackflow3"))
                    .build();

            Flow flow4 = new FlowBuilder<Flow>("turnBackflow4")
                    .start(parallelTurnBackStep(turnBackDatas4, supplierCode, "turnBackflow4"))
                    .build();

            Flow flow5 = new FlowBuilder<Flow>("turnBackflow5")
                    .start(parallelTurnBackStep(turnBackDatas5, supplierCode, "turnBackflow5"))
                    .build();


            Flow parallelStepFlow = new FlowBuilder<Flow>("parallelStepTurnBackFlow")
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
    public Step parallelTurnBackStep(List<SendProductCode> scanTurnBackDatas, String supplierCode, String workClassification) {

        return stepBuilderFactory.get(workClassification)
                .tasklet((contribution, chunkContext) -> {

                    // 0 -- 초기 1 -- 업데이트 3 -- 재등록
                    LocalDateTime now = LocalDateTime.now();

                    List<ProductDetail> needToSaveProductDetail = new ArrayList<>();
                    List<Long> updateProductDetailIds = new ArrayList<>();

                    List<ProductDetailscanResponseDto> responseProductDetails = scanTurnBackDatas.stream()
                            .map(eachScanData -> {

                                ProductDetail result = jpaQueryFactory
                                        .select(productDetail)
                                        .from(productDetail)
                                        .where(productDetail.productSerialCode.eq(eachScanData.getProductSerialCode()))
                                        .orderBy(productDetail.latestReadingAt.desc())
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

                                        updateProductDetailIds.add(result.getProductDetailId());

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


                    if (!updateProductDetailIds.isEmpty()) {
                        this.needUpdateProductDetailIds.addAll(updateProductDetailIds);
                    }


                    if (!needToSaveProductDetail.isEmpty()) {
                        productDetailRepository.saveAll(needToSaveProductDetail);
                    }

                    this.totalResponseProductDetails.addAll(responseProductDetails);

                    return RepeatStatus.FINISHED;
                })
                .build();
    }

}
