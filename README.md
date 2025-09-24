# 🏷️ CircularLabs RFID 다회용기 관리 시스템

## 📋 프로젝트 개요

CircularLabs RFID BackEnd는 다회용기의 전체 생명주기를 RFID 기술을 활용하여 추적하고 관리하는 종합적인 백엔드 시스템입니다. 공급사에서 고객사로의 출고, 고객사에서 공급사로의 회수, 세척, 폐기까지의 전 과정을 실시간으로 모니터링하고 재고를 효율적으로 관리할 수 있습니다.

## 🛠️ 기술 스택

### Core Framework

- **Spring Boot 2.7.8** - 메인 프레임워크
- **Spring Data JPA** - 데이터베이스 ORM
- **QueryDSL 5.0.0** - 타입 안전한 쿼리 빌더
- **Spring Batch** - 대용량 데이터 배치 처리
- **Spring Security** - 보안 및 인증

### Database & Caching

- **MySQL** - 메인 데이터베이스
- **Redis** - 캐싱 및 세션 관리

### Architecture & Performance

- **Spring Boot Async** - 비동기 처리
- **Spring Batch Parallel Processing** - 병렬 배치 처리
- **JPA Auditing** - 엔티티 생성/수정 시간 자동 관리

### Development Tools

- **Lombok** - 보일러플레이트 코드 감소
- **Gradle** - 빌드 도구
- **JDK 17** - Java 런타임

## 🏗️ 시스템 아키텍처

### 핵심 도메인 모델

#### 1. RfidScanHistory (RFID 스캔 이력)

```java
@Entity
public class RfidScanHistory extends TimeStamped {
    private Long rfidScanhistoryId;        // 인덱스
    private String deviceCode;             // 기기 코드
    private String rfidChipCode;           // RFID 칩 코드
    private String productCode;            // 제품 분류 코드
    private String supplierCode;           // 공급사 코드
    private String clientCode;             // 고객사 코드
    private String status;                 // 상태 (출고/입고/회수/세척/폐기)
    private int statusCount;               // 작업 수량
    private int flowRemainQuantity;        // 유동 재고 수량
    private int noReturnQuantity;          // 미회수 수량
    private int totalRemainQuantity;       // 총 재고 수량
    private LocalDateTime latestReadingAt; // 마지막 리딩 시간
}
```

#### 2. ProductDetail (제품 상세 정보)

```java
@Entity
public class ProductDetail extends TimeStamped {
    private Long productDetailId;          // 인덱스
    private String rfidChipCode;           // RFID 칩 코드
    private String productSerialCode;      // 각 제품 고유 코드
    private String productCode;            // 제품 분류 코드
    private String supplierCode;           // 공급사 코드
    private String clientCode;             // 고객사 코드
    private String status;                 // 상태
    private int cycle;                     // 사이클 (재사용 횟수)
    private LocalDateTime latestReadingAt; // 마지막 리딩 시간
}
```

#### 3. ProductDetailHistory (제품 상세 이력)

```java
@Entity
public class ProductDetailHistory extends TimeStamped {
    private Long productDetailHistoryId;   // 인덱스
    private String rfidChipCode;           // RFID 칩 코드
    private String productSerialCode;      // 각 제품 고유 코드
    private String productCode;            // 제품 분류 코드
    private String supplierCode;           // 공급사 코드
    private String clientCode;             // 고객사 코드
    private String status;                 // 상태
    private int cycle;                     // 사이클
    private LocalDateTime latestReadingAt; // 마지막 리딩 시간

    @ManyToOne(fetch = FetchType.LAZY)
    private RfidScanHistory rfidScanHistory; // 연관된 스캔 이력
}
```

## 🎯 핵심 기능

### 1. RFID 스캔 데이터 처리 시스템 (RfidScanDataController_v3)

다회용기의 전체 생명주기를 관리하는 핵심 모듈로, RFID 스캔 데이터를 실시간으로 처리하여 재고 상태를 정확하게 추적합니다.

#### 📦 출고 처리 (Outbound) - 현재 주석 처리됨

**기능 설명**: 공급사에서 고객사로 다회용기를 출고하는 프로세스

- **목적**: 깨끗한 용기를 고객사에게 배송하고 재고에서 차감
- **처리 과정**:
  1. CCA2310 필터링 코드로 유효한 제품만 선별
  2. 폐기된 제품은 자동으로 제외 처리
  3. 신규 제품은 데이터베이스에 등록, 기존 제품은 상태 업데이트
  4. 총재고량에서 출고량을 차감하고 미회수량에 추가

```java
@PostMapping("/out")
public synchronized ResponseEntity<ResponseBody> sendOutData(
    @RequestBody RfidScanDataOutRequestDto sendOutDatas) {

    // 스캔 데이터 필터링 (CCA2310 코드 기준)
    // 폐기 여부 검증
    // ProductDetail 처리 (기존 데이터 업데이트 또는 신규 생성)
    // RfidScanHistory 생성 (재고량 계산 포함)
    // ProductDetailHistory 저장
}
```

#### 📥 입고 처리 (Inbound)

**기능 설명**: 고객사에서 사용된 다회용기가 공급사로 돌아오는 프로세스

- **목적**: 사용된 용기를 회수하여 세척 대기 상태로 전환
- **비즈니스 로직**:
  - 미회수량(noReturnQuantity) 증가로 고객사에서 받은 용기 수량 반영
  - 유동재고량(flowRemainQuantity) 감소로 세척 필요 상태임을 표시
  - 데이터 상태에 따라 신규(cycle=0) 또는 기존(기존 cycle 유지) 처리

```java
@PostMapping("/in")
public synchronized ResponseEntity<ResponseBody> sendInData(
    @RequestBody RfidScanDataInRequestDto sendInDatas) {

    return new ResponseEntity<>(
        new ResponseBody(StatusCode.OK, scanDataServiceV3.sendInData(sendInDatas)),
        HttpStatus.OK
    );
}
```

**입고 처리 핵심 로직:**

```java
public synchronized CompletableFuture<String> sendInData(RfidScanDataInRequestDto sendInDatas) {
    String deviceCode = sendInDatas.getMachineId();
    String clientCode = sendInDatas.getSelectClientCode();
    String supplierCode = sendInDatas.getSupplierCode();

    // 1. 올바른 제품 코드 필터링
    List<SendProductCode> productCodes = sendInDatas.getProductCodes().stream()
        .filter(eachCorrectProduct -> scanDataQueryDataV3.correctProduct(
            eachCorrectProduct.getProductCode(), supplierCode))
        .collect(Collectors.toList());

    // 2. 배치 서비스를 통한 ProductDetail 처리
    HashMap<String, Object> responseProductDetailsInfo =
        batchService.launchProductDetail("입고", productCodes, supplierCode, clientCode);

    // 3. ProductDetailHistory 생성 및 저장
    List<ProductDetailHistory> saveEachCategoryProductDetailHistories = new ArrayList<>();

    responseProductDetails.forEach(eachResponseProductDetail -> {
        if (eachResponseProductDetail.getDataState() == 0) {
            // 신규 입고 데이터 처리
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
                    .build()
            );
        } else if (eachResponseProductDetail.getDataState() == 1) {
            // 기존 데이터 업데이트 처리
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
                    .build()
            );
        }
    });

    // 4. 제품별 수량 그룹화 및 RfidScanHistory 생성
    final Map<String, Long> map = saveEachCategoryProductDetailHistories.stream()
        .collect(Collectors.groupingBy(ProductDetailHistory::getProductCode, counting()));

    for (Map.Entry<String, Long> m : map.entrySet()) {
        Integer totalRemainQuantity = scanDataQueryDataV3.selectLastProductInfo(m.getKey(), supplierCode);
        RfidScanHistory latestScanHistory = scanDataQueryDataV3.getLatestRfidScanHistory(m.getKey(), supplierCode);

        RfidScanHistory createRfidScanHistory = RfidScanHistory.builder()
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
    }
}
```

#### 🔄 회수 처리 (Return)

**기능 설명**: 고객사에서 사용 완료된 다회용기를 공급사로 회수하는 프로세스

- **목적**: 사용된 용기를 수거하여 세척 후 재사용 준비
- **핵심 특징**:
  - 고객사별/제품별로 데이터를 그룹화하여 정확한 회수량 계산
  - 사이클(cycle) 증가로 재사용 횟수 추적
  - 미회수량 감소 및 유동재고량 증가로 사용 가능한 재고 복원

```java
@PostMapping("/return")
public synchronized ResponseEntity<ResponseBody> sendReturnData(
    @RequestBody RfidScanDataReturnRequestDto sendReturnDatas) {

    return new ResponseEntity<>(
        new ResponseBody(StatusCode.OK, scanDataServiceV3.sendReturnData2(sendReturnDatas)),
        HttpStatus.OK
    );
}
```

**회수 처리 핵심 로직:**

```java
public synchronized CompletableFuture<String> sendReturnData2(RfidScanDataReturnRequestDto sendTurnBackDatas) {
    // 1. 올바른 제품 필터링
    List<SendProductCode> scanTurnBackDataList = sendTurnBackDatas.getProductCodes()
        .stream()
        .filter(eachCorrectProduct -> scanDataQueryDataV3.correctProduct(
            eachCorrectProduct.getProductCode(), supplierCode))
        .collect(Collectors.toList());

    // 2. 배치 서비스를 통한 ProductDetail 처리
    HashMap<String, Object> responseProductDetailsInfo =
        batchService.launchProductDetail2("회수", scanTurnBackDataList, supplierCode);

    // 3. 고객사별/제품별 그룹화
    final Map<String, List<ProductDetailHistory>> map2 =
        saveEachCategoryProductDetailHistories.stream()
            .collect(Collectors.groupingBy(ProductDetailHistory::getClientCode));

    // 4. RfidScanHistory 생성 (고객사별, 제품별)
    for (Map.Entry<String, List<ProductDetailHistory>> m2 : map2.entrySet()) {
        final Map<String, Long> map = m2.getValue()
            .stream()
            .collect(Collectors.groupingBy(ProductDetailHistory::getProductCode, counting()));

        for (Map.Entry<String, Long> m : map.entrySet()) {
            RfidScanHistory createRfidScanHistory = RfidScanHistory.builder()
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
        }
    }
}
```

#### 🗑️ 폐기 처리 (Discard)

**기능 설명**: 손상되거나 수명이 다한 다회용기를 폐기 처리하는 프로세스

- **목적**: 더 이상 사용할 수 없는 용기를 시스템에서 제거하고 폐기 이력 관리
- **폐기 기준**:
  - 물리적 손상으로 재사용 불가능한 용기
  - 일정 사이클 이상 사용하여 품질이 저하된 용기
  - 안전상 문제가 있는 용기
- **처리 결과**:
  - ProductDetail 상태를 '폐기'로 변경
  - ProductDetailHistory에 폐기 이력 기록
  - DiscardHistory 테이블에 폐기 사유와 함께 별도 관리

```java
@PostMapping("/discard")
public synchronized ResponseEntity<ResponseBody> sendDiscardData(
    @RequestBody RfidScanDataDiscardRequestDto senddiscardDatas) {

    return new ResponseEntity<>(
        new ResponseBody(StatusCode.OK, scanDataServiceV3.sendDiscardData(senddiscardDatas)),
        HttpStatus.OK
    );
}
```

**폐기 처리 핵심 로직:**

```java
public synchronized CompletableFuture<String> sendDiscardData(RfidScanDataDiscardRequestDto sendDiscardDatas) {
    // 1. 폐기 데이터 필터링 (CCA2310 코드 기준)
    List<SendProductCode> scanDiscardDatas = productCodes.stream()
        .filter(scanDiscardData -> scanDiscardData.getFilteringCode().contains("CCA2310"))
        .collect(Collectors.toList());

    // 2. ProductDetail 처리 (상태를 '폐기'로 업데이트)
    List<ProductDetail> productDetails = scanDiscardDetail(scanDiscardDatas, supplierCode, clientCode);

    // 3. ProductDetailHistory 및 DiscardHistory 처리
    for (ProductDetail eachDiscardProductDetail : useProductDetailList) {
        // ProductDetailHistory 생성
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

        // DiscardHistory 생성 (중복 체크 후)
        if (!discardHistoryQueryDataV2.checkProductDiscard(
                eachProductDetailHistory.getProductCode(),
                eachProductDetailHistory.getProductSerialCode())) {

            DiscardHistory discardHistory = DiscardHistory.builder()
                .supplierCode(eachProductDetailHistory.getSupplierCode())
                .clientCode(eachProductDetailHistory.getClientCode())
                .productCode(eachProductDetailHistory.getProductCode())
                .productSerialCode(eachProductDetailHistory.getProductSerialCode())
                .rfidChipCode(eachProductDetailHistory.getRfidChipCode())
                .discardAt(LocalDateTime.now())
                .reason("")
                .build();
        }
    }
}
```

### 2. 배치 처리 시스템 (BatchParallelConfig)

**기능 설명**: 대용량 RFID 스캔 데이터를 효율적으로 처리하기 위한 병렬 배치 시스템

- **목적**: 많은 수의 용기를 동시에 스캔했을 때 발생하는 성능 병목 해결
- **처리 방식**:
  - **소량 데이터 (50개 미만)**: 단일 플로우로 순차 처리
  - **대량 데이터 (50개 이상)**: 5개 플로우로 분할하여 병렬 처리
- **성능 향상**: 최대 5배의 처리 속도 향상 가능

#### 병렬 처리 최적화

```java
@Component
public class BatchParallelConfig {

    // 50개 미만: 단일 플로우 처리
    // 50개 이상: 5개 플로우로 분할하여 병렬 처리
    public List<ProductDetail> launchProductDetail(String status, List<SendProductCode> scanDatas,
                                                  String supplierCode, String clientCode) {

        if (status.equals("출고")) {
            jobLauncher.run(parallelOutJob(scanDatas, supplierCode, clientCode), new JobParameters(parameters));
        } else if (status.equals("입고")) {
            jobLauncher.run(parallelInJob(scanDatas, supplierCode, clientCode), new JobParameters(parameters));
        }
    }

    // 대용량 데이터 처리를 위한 5개 플로우 분할
    public Job parallelOutJob(List<SendProductCode> scanOutDatas, String supplierCode, String clientCode) {
        if(scanOutDatas.size() >= 50) {
            int scanOutDatas_separate_section = scanOutDatas.size() / 5;
            List<SendProductCode> outDatas1 = scanOutDatas.subList(0, scanOutDatas_separate_section);
            List<SendProductCode> outDatas2 = scanOutDatas.subList(scanOutDatas_separate_section, scanOutDatas_separate_section * 2);
            // ... 5개 그룹으로 분할

            Flow parallelStepFlow = new FlowBuilder<Flow>("parallelStepFlow")
                .split(new SimpleAsyncTaskExecutor())
                .add(flow1, flow2, flow3, flow4, flow5)
                .build();
        }
    }
}
```

### 3. 재고 관리 및 수량 계산

**기능 설명**: 다회용기의 복잡한 재고 상황을 실시간으로 추적하고 관리하는 시스템

- **목적**: 출고/입고/회수/폐기 과정에서 발생하는 다양한 재고 상태를 정확하게 계산
- **특징**: 일반적인 재고 관리와 달리 용기의 '순환' 특성을 고려한 3차원 재고 관리

#### 복합적인 재고 계산 로직

- **flowRemainQuantity (유동 재고)**: 현재 사용 가능한 실제 재고량
  - 공급사에서 즉시 출고 가능한 용기 수량
  - 세척 완료되어 다시 사용할 수 있는 상태의 용기
- **noReturnQuantity (미회수 재고)**: 고객사에서 아직 회수되지 않은 용기 수량
  - 출고되었지만 아직 돌아오지 않은 용기들
  - 실제 재고에서는 차감되지만 향후 회수 예정인 용기
- **totalRemainQuantity (총 재고)**: 공급사가 보유한 전체 용기 수량
  - 물리적으로 존재하는 모든 용기 (사용 가능 + 세척 대기 + 손상 등)

```java
// 입고 시 재고 계산
.flowRemainQuantity(totalRemainQuantity - latestScanHistory.getNoReturnQuantity() - m.getValue().intValue())
.noReturnQuantity(latestScanHistory.getNoReturnQuantity() + m.getValue().intValue())

// 회수 시 재고 계산
.flowRemainQuantity(totalRemainQuantity - latestScanHistory.getNoReturnQuantity() + m.getValue().intValue())
.noReturnQuantity(latestScanHistory.getNoReturnQuantity() - m.getValue().intValue())
```

## 🔄 데이터 플로우

다회용기의 생명주기에 따른 상세한 데이터 처리 과정을 설명합니다.

### 1. 출고 프로세스 (공급사 → 고객사)

```
📱 RFID 스캔
→ 🔍 데이터 필터링 (CCA2310 코드 검증)
→ ❌ 폐기 여부 검증 (폐기된 용기 제외)
→ 💾 ProductDetail 처리 (신규 등록 또는 상태 업데이트)
→ 📊 RfidScanHistory 생성 (재고량 계산)
→ 📋 ProductDetailHistory 저장 (이력 기록)
→ ✅ 출고 완료 (미회수량 증가, 유동재고 감소)
```

### 2. 입고 프로세스 (고객사 → 공급사, 사용 후 회수)

```
📱 RFID 스캔
→ ✅ 제품 유효성 검증 (공급사 제품 확인)
→ ⚡ 배치 처리 (ProductDetail 병렬 처리)
→ 📦 수량 그룹화 (제품별 입고량 집계)
→ 📊 RfidScanHistory 생성 (재고 상태 업데이트)
→ 📋 ProductDetailHistory 저장
→ ✅ 입고 완료 (미회수량 증가, 세척 대기 상태)
```

### 3. 회수 프로세스 (고객사 사용 완료 후 수거)

```
📱 RFID 스캔
→ ✅ 제품 유효성 검증
→ 🏢 고객사별/제품별 그룹화 (정확한 회수량 계산)
→ 🔄 사이클 증가 (재사용 횟수 +1)
→ 📊 RfidScanHistory 생성 (미회수량 감소, 유동재고 증가)
→ 📋 ProductDetailHistory 저장
→ ✅ 회수 완료 (세척 후 재사용 가능 상태)
```

### 4. 폐기 프로세스 (수명 완료 또는 손상)

```
📱 RFID 스캔
→ 🔍 데이터 필터링 (CCA2310 코드 검증)
→ 💾 ProductDetail 상태 업데이트 (상태 → '폐기')
→ 📋 ProductDetailHistory 생성 (폐기 이력)
→ 🗑️ DiscardHistory 생성 (폐기 사유 및 상세 정보)
→ ✅ 폐기 완료 (시스템에서 영구 제거)
```

## 📊 성능 최적화

### 1. 배치 처리

- **Spring Batch**를 활용한 대용량 데이터 처리
- **병렬 처리**: 50개 이상의 데이터는 5개 플로우로 분할 처리
- **SimpleAsyncTaskExecutor**를 통한 비동기 실행

### 2. 데이터베이스 최적화

- **QueryDSL**을 통한 타입 안전한 쿼리 작성
- **JPA Batch Insert**를 통한 벌크 처리
- **EntityManager flush/clear**를 통한 메모리 최적화

### 3. 캐싱 전략

- **Redis**를 활용한 세션 및 데이터 캐싱
- **Spring Cache**를 통한 메서드 레벨 캐싱

### 4. 동시성 제어

- **synchronized** 키워드를 통한 동시성 제어
- **@Transactional**을 통한 트랜잭션 일관성 보장

## 🛡️ 보안 및 검증

### 1. 데이터 검증

- 제품 코드 필터링 (CCA2310 기준)
- 폐기 제품 검증
- 중복 처리 방지

### 2. 보안

- **Spring Security**를 통한 인증/인가
- **CORS** 설정을 통한 교차 출처 요청 제어

## 📈 모니터링 및 로깅

### 상세한 로깅 시스템

```java
log.info("입고 스캔한 제품 코드들 : {}", productCodes.stream()
    .map(SendProductCode::getProductCode)
    .distinct()
    .collect(Collectors.toList()));

log.info("- 처음 들어온 데이터 수 : {}", scanTurnBackDataList.size());
log.info("- 배치 프로그램을 돌린 후 저장 및 정제된 ProductDetail 수 : {}", responseProductDetails.size());
log.info("- 정제된 ProductDetail 정보들을 기준으로 빌드된 ProductDetailHistory 수 : {}", saveEachCategoryProductDetailHistories.size());
```

## 🚀 주요 특징

### 1. 확장성

- 모듈화된 서비스 구조
- 버전별 컨트롤러 관리 (v1, v2, v3)
- 플러그인 가능한 배치 작업

### 2. 안정성

- 동기화된 API 엔드포인트
- 트랜잭션 롤백 지원
- 예외 처리 및 에러 복구

### 3. 성능

- 대용량 데이터 배치 처리
- 병렬 처리를 통한 성능 최적화
- 효율적인 메모리 관리

### 4. 추적성

- 완전한 제품 생명주기 추적
- 상세한 이력 관리
- 실시간 재고 계산

## 📦 배포 및 운영

### 환경 설정

```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://116.125.141.139:3306/circularlabs
spring.jpa.hibernate.ddl-auto=update

# Redis Configuration
spring.redis.host=127.0.0.1
spring.redis.port=6379

# Batch Configuration
spring.batch.job.enabled=false
spring.batch.jdbc.initialize-schema=always

# Server Configuration
server.port=8090
```

### 시스템 요구사항

- **JDK 17+**
- **MySQL 8.0+**
- **Redis 6.0+**
- **메모리**: 최소 4GB RAM (권장 8GB+)

## 🔮 향후 개발 계획

1. **실시간 알림 시스템** - WebSocket을 통한 실시간 재고 알림
2. **대시보드 강화** - 더 상세한 분석 및 리포팅 기능
3. **AI 기반 예측** - 재고 수요 예측 및 최적화
4. **모바일 앱 연동** - 현장 작업자용 모바일 인터페이스
5. **API 문서화** - Swagger/OpenAPI 3.0 통합

---

이 프로젝트는 RFID 기술을 활용한 다회용기 관리의 혁신적인 솔루션으로, 순환 경제와 지속가능한 포장재 사용을 촉진하는 데 기여합니다.
