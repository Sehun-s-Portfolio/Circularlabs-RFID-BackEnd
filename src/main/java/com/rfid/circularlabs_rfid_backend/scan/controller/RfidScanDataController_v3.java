package com.rfid.circularlabs_rfid_backend.scan.controller;

import com.rfid.circularlabs_rfid_backend.scan.request.*;
import com.rfid.circularlabs_rfid_backend.scan.service.RfidScanDataService_v2;
import com.rfid.circularlabs_rfid_backend.scan.service.RfidScanDataService_v3;
import com.rfid.circularlabs_rfid_backend.share.ResponseBody;
import com.rfid.circularlabs_rfid_backend.share.StatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/rfid")
@RestController
public class RfidScanDataController_v3 {

    private final RfidScanDataService_v3 scanDataServiceV3;

    /**
    // 스캔 데이터 출고 api v2
    @PostMapping("/out")
    public synchronized ResponseEntity<ResponseBody> sendOutData(@RequestBody RfidScanDataOutRequestDto sendOutDatas)
            throws InterruptedException, JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        log.info("제품 출고 처리 api v2 - 출고 데이터 확인");

        return new ResponseEntity<>(new ResponseBody(StatusCode.OK, scanDataServiceV3.sendOutData(sendOutDatas)), HttpStatus.OK);
    }
    **/


    // 스캔 데이터 입고 api v2
    @PostMapping("/in")
    public synchronized ResponseEntity<ResponseBody> sendInData(@RequestBody RfidScanDataInRequestDto sendInDatas)
            throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        log.info("제품 입고 처리 api v2 - 입고 데이터 확인");

        return new ResponseEntity<>(new ResponseBody(StatusCode.OK, scanDataServiceV3.sendInData(sendInDatas)), HttpStatus.OK);
    }


    // 스캔 데이터 회수 api v2
    @PostMapping("/return")
    public synchronized ResponseEntity<ResponseBody> sendReturnData(@RequestBody RfidScanDataReturnRequestDto sendReturnDatas)
            throws InterruptedException, JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        log.info("제품 회수 처리 api v2 - 회수 데이터 확인");

        return new ResponseEntity<>(new ResponseBody(StatusCode.OK, scanDataServiceV3.sendReturnData2(sendReturnDatas)), HttpStatus.OK);
    }


    /**
    // 스캔 데이터 세척 api v2
    @PostMapping("/clean")
    public synchronized ResponseEntity<ResponseBody> sendCleaningData(@RequestBody RfidScanDataCleanRequestDto sendCleanDatas) throws InterruptedException {
        log.info("제품 세척 처리 api v2 - 세척 데이터 확인");

        return new ResponseEntity<>(new ResponseBody(StatusCode.OK, scanDataServiceV3.sendCleaningData(sendCleanDatas)), HttpStatus.OK);
    }
    **/


    // 스캔 데이터 폐기 api v2
    @PostMapping("/discard")
    public synchronized ResponseEntity<ResponseBody> sendDiscardData(@RequestBody RfidScanDataDiscardRequestDto senddiscardDatas) throws InterruptedException {
        log.info("제품 폐기 처리 api v2 - 폐기 데이터 확인");

        return new ResponseEntity<>(new ResponseBody(StatusCode.OK, scanDataServiceV3.sendDiscardData(senddiscardDatas)), HttpStatus.OK);
    }

}
