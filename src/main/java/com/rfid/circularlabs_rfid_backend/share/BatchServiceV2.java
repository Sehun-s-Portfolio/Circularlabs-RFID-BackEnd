package com.rfid.circularlabs_rfid_backend.share;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.rfid.circularlabs_rfid_backend.product.domain.ProductDetail;
import com.rfid.circularlabs_rfid_backend.product.repository.ProductDetailRepository;
import com.rfid.circularlabs_rfid_backend.query2.scandata.ScanDataQueryDataV3;
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
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.batch.item.database.support.ListPreparedStatementSetter;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterUtils;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.rfid.circularlabs_rfid_backend.product.domain.QProductDetail.productDetail;

@RequiredArgsConstructor
//@Configuration
@Component
public class BatchServiceV2 {
    private static final Logger log = LoggerFactory.getLogger(BatchServiceV2.class);

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final JobLauncher jobLauncher;
    private final JPAQueryFactory jpaQueryFactory;
    private final ProductDetailRepository productDetailRepository;
    private final EntityManagerFactory entityManagerFactory;
    private final EntityManager entityManager;
    private final DataSource dataSource;
    private final SecureRandom secureRandom = new SecureRandom();
    private List<ProductDetailscanResponseDto> totalResponseProductDetails = new ArrayList<>();
    private final ScanDataQueryDataV3 scanDataQueryDataV3;

    // [ProductDetail] 배치 실행 (출고 / 입고)
    public List<ProductDetailscanResponseDto> launchProductDetail(String status, List<SendProductCode> scanDatas, String supplierCode, String clientCode) throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        Map<String, JobParameter> parameters = new HashMap<>();
        parameters.put("random", new JobParameter(this.secureRandom.nextLong()));

        this.totalResponseProductDetails = new ArrayList<>();

        if (status.equals("출고")) {
            //jobLauncher.run(parallelOutJob(scanDatas, supplierCode, clientCode), new JobParameters(parameters));
        } else if (status.equals("입고")) {
            jobLauncher.run(parallelInJob(scanDatas, supplierCode, clientCode), new JobParameters(parameters));
        }

        return this.totalResponseProductDetails;
    }

    // [ProductDetail] 입고 저장 Job
    public Job parallelInJob(List<SendProductCode> scanInDatas, String supplierCode, String clientCode) {

        if (scanInDatas.size() < 50) {
            Flow flow1 = new FlowBuilder<Flow>("inFlow")
                    .start(parallelInStep(scanInDatas, supplierCode, clientCode, scanInDatas.size()))
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

            List<SendProductCode> group1 = new ArrayList<>();
            List<SendProductCode> group2 = new ArrayList<>();

            group1.addAll(inDatas1);
            group1.addAll(inDatas2);
            group1.addAll(inDatas3);

            group2.addAll(inDatas4);
            group2.addAll(inDatas5);

            Flow flow1 = new FlowBuilder<Flow>("inFlow1")
                    .start(parallelInStep(group1, supplierCode, clientCode, group1.size()))
                    .build();

            Flow flow2 = new FlowBuilder<Flow>("inFlow2")
                    .start(parallelInStep(group2, supplierCode, clientCode, group2.size()))
                    .build();

            /**
             Flow flow3 = new FlowBuilder<Flow>("inFlow3")
             .start(parallelInStep(inDatas3, supplierCode, clientCode))
             .build();

             Flow flow4 = new FlowBuilder<Flow>("inFlow4")
             .start(parallelInStep(inDatas4, supplierCode, clientCode))
             .build();

             Flow flow5 = new FlowBuilder<Flow>("inFlow5")
             .start(parallelInStep(inDatas5, supplierCode, clientCode))
             .build();
             **/

            Flow parallelStepFlow = new FlowBuilder<Flow>("parallelStepInFlow")
                    .split(new SimpleAsyncTaskExecutor())
                    .add(flow1)
                    .next(flow2)
                    .build();

            return jobBuilderFactory.get("parallelInJob")
                    .start(parallelStepFlow)
                    .build()
                    .build();
        }

        return null;
    }


    // [ProductDetail] 입고 저장 Step
    public Step parallelInStep(List<SendProductCode> scanInDatas, String supplierCode, String clientCode, int chunkSize) {
        return stepBuilderFactory.get("parallelInStep")
                .<ProductDetail, ProductDetail>chunk(chunkSize)
                .reader(jdbcCursorItemReader(scanInDatas, chunkSize))
                .writer(jdbcCursorItemWriter(supplierCode, clientCode))
                .build();
    }


    private ItemReader<ProductDetail> jdbcCursorItemReader(List<SendProductCode> scanInDatas, int chunkSize) {

        String sql = "select product_detail_id, created_at, modified_at, client_code, cycle, latest_reading_at, product_code, product_serial_code, rfid_chip_code, status, supplier_code from product_detail";

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("product_code", scanInDatas.get(0).getProductCode());


        return new JdbcCursorItemReaderBuilder<ProductDetail>()
                .fetchSize(chunkSize)
                .dataSource(dataSource)
                .rowMapper(new BeanPropertyRowMapper<>(ProductDetail.class))
                .sql(sql)
                //.queryArguments(parameters)
                //.preparedStatementSetter(new ListPreparedStatementSetter(Arrays.asList(NamedParameterUtils.buildValueArray(sql, testArgs))))
                .name("ScanInDataCursorItemReader")
                .build();


        /**
         HashMap<String, Object> parameters = new HashMap<>();
         parameters.put("product_code", scanInDatas.get(0).getProductCode());

         return new JpaCursorItemReaderBuilder<ProductDetail>()
         .name("jpaCursorItemReader")
         .entityManagerFactory(entityManagerFactory) // EntityManager 설정
         .queryString(sql) // 실행할 jpql문
         .parameterValues(parameters) // jpql문 내 인자
         .build();
         **/
    }


    private ItemWriter<ProductDetail> jdbcCursorItemWriter(String supplierCode, String clientCode) {
        return list -> {
            log.info("커서 아이템을 활용한 데이터 조회 확인");

            for (ProductDetail eachProductDetail : list) {
                log.info("ProductDetail 데이터 " + list.indexOf(eachProductDetail) + " - " + eachProductDetail.getProductSerialCode());
            }
        };
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    // [ProductDetail] 배치 실행 (회수 / 세척)
    public List<ProductDetailscanResponseDto> launchProductDetail2(String status, List<SendProductCode> scanDatas, String supplierCode) throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        Map<String, JobParameter> parameters = new HashMap<>();
        parameters.put("random", new JobParameter(this.secureRandom.nextLong()));

        this.totalResponseProductDetails = new ArrayList<>();

        if (status.equals("회수")) {
            log.info("배치 돌릴 스캔 데이터 수 : {}", scanDatas.size());
            jobLauncher.run(parallelTurnBackJob(scanDatas, supplierCode), new JobParameters(parameters));
        } else if (status.equals("세척")) {
            //jobLauncher.run(parallelCleanJob(), new JobParameters());
        }

        return this.totalResponseProductDetails;
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

            List<SendProductCode> group1 = new ArrayList<>();
            List<SendProductCode> group2 = new ArrayList<>();

            group1.addAll(turnBackDatas1);
            group1.addAll(turnBackDatas2);
            group1.addAll(turnBackDatas3);

            group2.addAll(turnBackDatas4);
            group2.addAll(turnBackDatas5);

            /**
             Flow flow1 = new FlowBuilder<Flow>("turnBackflow1")
             .start(parallelTurnBackStep(turnBackDatas1, supplierCode))
             .build();

             Flow flow2 = new FlowBuilder<Flow>("turnBackflow2")
             .start(parallelTurnBackStep(turnBackDatas2, supplierCode))
             .build();

             Flow flow3 = new FlowBuilder<Flow>("turnBackflow3")
             .start(parallelTurnBackStep(turnBackDatas3, supplierCode))
             .build();

             Flow flow4 = new FlowBuilder<Flow>("turnBackflow4")
             .start(parallelTurnBackStep(turnBackDatas4, supplierCode))
             .build();

             Flow flow5 = new FlowBuilder<Flow>("turnBackflow5")
             .start(parallelTurnBackStep(turnBackDatas5, supplierCode))
             .build();
             **/

            Flow flow1 = new FlowBuilder<Flow>("turnBackflow1")
                    .start(parallelTurnBackStep(group1, supplierCode, "turnBackflow1"))
                    .build();

            Flow flow2 = new FlowBuilder<Flow>("turnBackflow2")
                    .start(parallelTurnBackStep(group2, supplierCode, "turnBackflow2"))
                    .build();

            Flow parallelStepFlow = new FlowBuilder<Flow>("parallelStepTurnBackFlow")
                    .split(new SimpleAsyncTaskExecutor())
                    .add(flow1)
                    .next(flow2)
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

                    List<ProductDetailscanResponseDto> responseProductDetails = scanTurnBackDatas.stream()
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

                    this.totalResponseProductDetails.addAll(responseProductDetails);

                    return RepeatStatus.FINISHED;
                })
                .build();
    }

}
