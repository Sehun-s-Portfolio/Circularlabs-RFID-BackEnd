# 🎯 CircularLabs RFID 시스템 시퀀스 다이어그램

## 📋 개요

이 문서는 CircularLabs RFID 다회용기 관리 시스템의 핵심 기능들에 대한 시스템 시퀀스 다이어그램을 포함합니다. README.md에서 정의된 핵심 기능들을 실제 구현된 코드 구조를 바탕으로 상세하게 표현합니다.

## 🏗️ 시스템 아키텍처 개요

```mermaid
graph TB
    subgraph "RFID 스캔 시스템"
        RF[RFID 스캐너] --> API[API Gateway]
    end

    subgraph "컨트롤러 레이어"
        API --> C1[RfidScanDataController_v3]
        API --> C2[RfidScanDataController_v2]
        API --> C3[RfidScanDataController]
    end

    subgraph "서비스 레이어"
        C1 --> S1[RfidScanDataService_v3]
        C2 --> S2[RfidScanDataService_v2]
        C3 --> S3[RfidScanDataService]
    end

    subgraph "배치 처리 시스템"
        S1 --> BS[BatchService]
        S2 --> BPC[BatchParallelConfig]
        BS --> JL[JobLauncher]
        BPC --> JL
    end

    subgraph "데이터베이스"
        S1 --> PD[ProductDetail]
        S1 --> RSH[RfidScanHistory]
        S1 --> PDH[ProductDetailHistory]
        S1 --> DH[DiscardHistory]
    end
```

## 1. 📥 입고 처리 (Inbound) 시퀀스 다이어그램

```mermaid
sequenceDiagram
    participant Client as RFID 스캐너
    participant Controller as RfidScanDataController_v3
    participant Service as RfidScanDataService_v3
    participant BatchService as BatchService
    participant JobLauncher as Spring Batch JobLauncher
    participant Query as ScanDataQueryDataV3
    participant DB as Database

    Note over Client, DB: 입고 처리 프로세스 시작

    Client->>+Controller: POST /rfid/in<br/>RfidScanDataInRequestDto
    Note right of Controller: synchronized로 동시성 제어

    Controller->>+Service: sendInData(sendInDatas)

    Service->>Service: 1. 데이터 추출<br/>- deviceCode<br/>- clientCode<br/>- supplierCode<br/>- productCodes

    Service->>Service: 2. 올바른 제품 필터링<br/>correctProduct() 검증

    Service->>+BatchService: launchProductDetail("입고", productCodes, supplierCode, clientCode)

    alt 데이터 수량 >= 50개
        BatchService->>+JobLauncher: 병렬 처리 Job 실행<br/>(5개 플로우로 분할)
        JobLauncher->>JobLauncher: SimpleAsyncTaskExecutor<br/>비동기 병렬 처리
    else 데이터 수량 < 50개
        BatchService->>+JobLauncher: 단일 플로우 Job 실행
    end

    JobLauncher->>DB: ProductDetail 처리<br/>(신규 생성 또는 업데이트)
    JobLauncher-->>-BatchService: 처리 완료된 ProductDetail 리스트

    BatchService-->>-Service: responseProductDetailsInfo

    Service->>Service: 3. ProductDetailHistory 생성
    loop 각 responseProductDetail
        alt dataState == 0 (신규)
            Service->>Service: cycle = 0으로 설정
        else dataState == 1 (기존)
            Service->>Service: 기존 cycle 유지
        end
        Service->>Service: ProductDetailHistory 빌드<br/>- status: "입고"<br/>- latestReadingAt: now()
    end

    Service->>Service: 4. 제품별 수량 그룹화<br/>Collectors.groupingBy(productCode)

    loop 각 제품 코드별
        Service->>+Query: selectLastProductInfo(productCode, supplierCode)
        Query-->>-Service: totalRemainQuantity

        Service->>+Query: getLatestRfidScanHistory(productCode, supplierCode)
        Query-->>-Service: latestScanHistory

        Service->>Service: RfidScanHistory 빌드<br/>- status: "입고"<br/>- statusCount: 입고 수량<br/>- flowRemainQuantity: 계산<br/>- noReturnQuantity: 증가<br/>- totalRemainQuantity: 총 재고

        Service->>DB: RfidScanHistory 저장
    end

    Service->>DB: ProductDetailHistory 벌크 저장

    Service-->>-Controller: CompletableFuture<String>
    Controller-->>-Client: ResponseEntity<ResponseBody><br/>StatusCode.OK

    Note over Client, DB: 입고 처리 완료<br/>미회수량 증가, 세척 대기 상태
```

## 2. 🔄 회수 처리 (Return) 시퀀스 다이어그램

```mermaid
sequenceDiagram
    participant Client as RFID 스캐너
    participant Controller as RfidScanDataController_v3
    participant Service as RfidScanDataService_v3
    participant BatchService as BatchService
    participant JobLauncher as Spring Batch JobLauncher
    participant Query as ScanDataQueryDataV3
    participant DB as Database

    Note over Client, DB: 회수 처리 프로세스 시작

    Client->>+Controller: POST /rfid/return<br/>RfidScanDataReturnRequestDto
    Note right of Controller: synchronized로 동시성 제어

    Controller->>+Service: sendReturnData2(sendReturnDatas)

    Service->>Service: 1. 데이터 추출 및 필터링<br/>correctProduct() 검증

    Service->>+BatchService: launchProductDetail2("회수", scanTurnBackDataList, supplierCode)

    alt 데이터 수량 >= 50개
        BatchService->>+JobLauncher: parallelTurnBackJob<br/>(5개 플로우 병렬 처리)
    else 데이터 수량 < 50개
        BatchService->>+JobLauncher: 단일 플로우 처리
    end

    JobLauncher->>DB: ProductDetail 업데이트<br/>(cycle 증가)
    JobLauncher-->>-BatchService: 처리된 ProductDetail 리스트

    BatchService-->>-Service: responseProductDetailsInfo

    Service->>Service: 2. ProductDetailHistory 생성<br/>- status: "회수"<br/>- cycle: 증가된 값<br/>- latestReadingAt: now()

    Service->>Service: 3. 고객사별/제품별 그룹화<br/>groupingBy(clientCode)<br/>→ groupingBy(productCode)

    loop 각 고객사별
        loop 각 제품별
            Service->>+Query: selectLastProductInfo(productCode, supplierCode)
            Query-->>-Service: totalRemainQuantity

            Service->>+Query: getLatestRfidScanHistory(productCode, supplierCode)
            Query-->>-Service: latestScanHistory

            Service->>Service: RfidScanHistory 빌드<br/>- status: "회수"<br/>- statusCount: 회수 수량<br/>- flowRemainQuantity: 증가<br/>- noReturnQuantity: 감소<br/>- totalRemainQuantity: 총 재고

            Service->>DB: RfidScanHistory 저장
        end
    end

    Service->>DB: ProductDetailHistory 벌크 저장

    Service-->>-Controller: CompletableFuture<String>
    Controller-->>-Client: ResponseEntity<ResponseBody><br/>StatusCode.OK

    Note over Client, DB: 회수 처리 완료<br/>미회수량 감소, 유동재고 증가
```

## 3. 🗑️ 폐기 처리 (Discard) 시퀀스 다이어그램

```mermaid
sequenceDiagram
    participant Client as RFID 스캐너
    participant Controller as RfidScanDataController_v3
    participant Service as RfidScanDataService_v3
    participant Query as ScanDataQueryDataV3
    participant DiscardQuery as DiscardHistoryQueryDataV2
    participant DB as Database

    Note over Client, DB: 폐기 처리 프로세스 시작

    Client->>+Controller: POST /rfid/discard<br/>RfidScanDataDiscardRequestDto
    Note right of Controller: synchronized로 동시성 제어

    Controller->>+Service: sendDiscardData(senddiscardDatas)

    Service->>Service: 1. 데이터 추출<br/>- deviceCode<br/>- clientCode<br/>- supplierCode<br/>- productCodes

    Service->>Service: 2. 폐기 데이터 필터링<br/>CCA2310 코드 기준

    Service->>+Query: scanDiscardDetail(scanDiscardDatas, supplierCode, clientCode)
    Note right of Query: ProductDetail 상태를 '폐기'로 업데이트
    Query->>DB: ProductDetail 업데이트<br/>status = "폐기"
    Query-->>-Service: 폐기 처리된 ProductDetail 리스트

    Service->>Service: 3. ProductDetailHistory 및 DiscardHistory 처리

    loop 각 폐기 ProductDetail
        Service->>Service: ProductDetailHistory 빌드<br/>- status: "폐기"<br/>- cycle: 기존 cycle<br/>- latestReadingAt: now()

        Service->>+DiscardQuery: checkProductDiscard(productCode, productSerialCode)
        DiscardQuery-->>-Service: 중복 여부 확인

        alt 중복되지 않은 경우
            Service->>Service: DiscardHistory 빌드<br/>- discardAt: now()<br/>- reason: ""<br/>- 폐기 상세 정보
            Service->>DB: DiscardHistory 저장
        end

        Service->>DB: ProductDetailHistory 저장
    end

    Service->>Service: 4. 제품별 수량 그룹화 및 RfidScanHistory 생성

    loop 각 제품 코드별
        Service->>+Query: selectLastProductInfo(productCode, supplierCode)
        Query-->>-Service: totalRemainQuantity

        Service->>Service: RfidScanHistory 빌드<br/>- status: "폐기"<br/>- statusCount: 폐기 수량<br/>- totalRemainQuantity: 감소<br/>- latestReadingAt: now()

        Service->>DB: RfidScanHistory 저장
    end

    Service-->>-Controller: CompletableFuture<String>
    Controller-->>-Client: ResponseEntity<ResponseBody><br/>StatusCode.OK

    Note over Client, DB: 폐기 처리 완료<br/>시스템에서 영구 제거
```

## 4. 🔄 배치 처리 시스템 상세 시퀀스

```mermaid
sequenceDiagram
    participant Service as RfidScanDataService_v3
    participant BatchService as BatchService
    participant JobBuilder as JobBuilderFactory
    participant StepBuilder as StepBuilderFactory
    participant JobLauncher as JobLauncher
    participant Executor as SimpleAsyncTaskExecutor
    participant DB as Database

    Note over Service, DB: 배치 처리 시스템 동작

    Service->>+BatchService: launchProductDetail(status, scanDatas, supplierCode, clientCode)

    BatchService->>BatchService: JobParameter 생성<br/>SecureRandom.nextLong()

    alt 데이터 수량 >= 50개
        BatchService->>+JobBuilder: parallelInJob() 생성
        JobBuilder->>JobBuilder: 5개 플로우로 데이터 분할<br/>- outDatas1 (0 ~ size/5)<br/>- outDatas2 (size/5 ~ size*2/5)<br/>- ... (5개 그룹)

        loop 5개 플로우
            JobBuilder->>+StepBuilder: Step 생성
            StepBuilder->>StepBuilder: Tasklet 정의<br/>RepeatStatus.FINISHED
            StepBuilder-->>-JobBuilder: Step 완성
        end

        JobBuilder->>JobBuilder: FlowBuilder로 병렬 플로우 구성<br/>SimpleAsyncTaskExecutor 사용
        JobBuilder-->>-BatchService: 병렬 Job 완성

        BatchService->>+JobLauncher: Job 실행 (병렬)
        JobLauncher->>+Executor: 5개 플로우 동시 실행

        par 플로우 1
            Executor->>DB: ProductDetail 처리 (1/5)
        and 플로우 2
            Executor->>DB: ProductDetail 처리 (2/5)
        and 플로우 3
            Executor->>DB: ProductDetail 처리 (3/5)
        and 플로우 4
            Executor->>DB: ProductDetail 처리 (4/5)
        and 플로우 5
            Executor->>DB: ProductDetail 처리 (5/5)
        end

        Executor-->>-JobLauncher: 모든 플로우 완료

    else 데이터 수량 < 50개
        BatchService->>+JobBuilder: 단일 Job 생성
        JobBuilder->>+StepBuilder: 단일 Step 생성
        StepBuilder-->>-JobBuilder: Step 완성
        JobBuilder-->>-BatchService: 단일 Job 완성

        BatchService->>+JobLauncher: Job 실행 (단일)
        JobLauncher->>DB: ProductDetail 순차 처리
    end

    JobLauncher-->>-BatchService: Job 실행 완료

    BatchService->>BatchService: 결과 수집<br/>totalResponseProductDetails
    BatchService-->>-Service: HashMap<String, Object><br/>처리 결과 반환

    Note over Service, DB: 최대 5배 성능 향상 달성
```

## 5. 📊 재고 계산 로직 상세 다이어그램

```mermaid
sequenceDiagram
    participant Service as RfidScanDataService_v3
    participant Query as ScanDataQueryDataV3
    participant DB as Database

    Note over Service, DB: 복합적인 재고 계산 프로세스

    Service->>+Query: selectLastProductInfo(productCode, supplierCode)
    Query->>DB: SELECT 총 재고량 조회
    DB-->>Query: totalRemainQuantity
    Query-->>-Service: totalRemainQuantity

    Service->>+Query: getLatestRfidScanHistory(productCode, supplierCode)
    Query->>DB: SELECT 최신 스캔 이력 조회
    DB-->>Query: latestScanHistory
    Query-->>-Service: latestScanHistory

    Note over Service: 상태별 재고 계산 로직

    alt 입고 처리
        Service->>Service: flowRemainQuantity 계산<br/>= totalRemainQuantity<br/>  - latestScanHistory.noReturnQuantity<br/>  - 입고수량
        Service->>Service: noReturnQuantity 계산<br/>= latestScanHistory.noReturnQuantity<br/>  + 입고수량
        Note right of Service: 미회수량 증가<br/>유동재고 감소

    else 회수 처리
        Service->>Service: flowRemainQuantity 계산<br/>= totalRemainQuantity<br/>  - latestScanHistory.noReturnQuantity<br/>  + 회수수량
        Service->>Service: noReturnQuantity 계산<br/>= latestScanHistory.noReturnQuantity<br/>  - 회수수량
        Note right of Service: 미회수량 감소<br/>유동재고 증가

    else 출고 처리
        Service->>Service: flowRemainQuantity 계산<br/>= totalRemainQuantity<br/>  - 출고수량
        Service->>Service: noReturnQuantity 계산<br/>= latestScanHistory.noReturnQuantity<br/>  + 출고수량
        Note right of Service: 출고량만큼 재고 감소<br/>미회수량 증가

    else 폐기 처리
        Service->>Service: totalRemainQuantity 계산<br/>= 기존값 - 폐기수량
        Service->>Service: flowRemainQuantity 조정<br/>= 폐기된 만큼 감소
        Note right of Service: 총 재고에서 영구 제거
    end

    Service->>Service: RfidScanHistory 빌드<br/>- flowRemainQuantity<br/>- noReturnQuantity<br/>- totalRemainQuantity<br/>- statusCount

    Service->>DB: RfidScanHistory 저장<br/>재고 상태 업데이트

    Note over Service, DB: 3차원 재고 관리 완료<br/>유동재고 + 미회수재고 + 총재고
```

## 📈 성능 최적화 포인트

### 1. 동시성 제어

- 모든 API 엔드포인트에 `synchronized` 키워드 적용
- 데이터 일관성 보장

### 2. 배치 처리 최적화

- 50개 이상 데이터: 5개 플로우 병렬 처리 (최대 5배 성능 향상)
- 50개 미만 데이터: 단일 플로우 순차 처리
- `SimpleAsyncTaskExecutor` 활용한 비동기 실행

### 3. 데이터베이스 최적화

- JPA Batch Insert로 벌크 처리
- EntityManager flush/clear로 메모리 관리
- QueryDSL 활용한 타입 안전 쿼리

### 4. 트랜잭션 관리

- `@Transactional` 어노테이션으로 일관성 보장
- 예외 발생 시 자동 롤백

## 🔍 주요 특징

1. **확장성**: 버전별 컨트롤러 관리 (v1, v2, v3)
2. **안정성**: 동기화된 API와 예외 처리
3. **성능**: 대용량 데이터 병렬 배치 처리
4. **추적성**: 완전한 제품 생명주기 추적
5. **일관성**: 복합적인 재고 계산 시스템

---

이 시퀀스 다이어그램들은 CircularLabs RFID 시스템의 실제 구현된 코드를 바탕으로 작성되었으며, 다회용기의 전체 생명주기 관리 프로세스를 상세하게 표현합니다.
