package com.rfid.circularlabs_rfid_backend.scan.controller;

import com.rfid.circularlabs_rfid_backend.device.response.DeviceResponseDto;
import com.rfid.circularlabs_rfid_backend.scan.request.*;
import com.rfid.circularlabs_rfid_backend.scan.service.RfidScanDataService;
import com.rfid.circularlabs_rfid_backend.share.ResponseBody;
import com.rfid.circularlabs_rfid_backend.share.StatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/rfid/prev")
@RestController
public class RfidScanDataController {

    private final RfidScanDataService scanDataService;

    // 스캔 데이터 출고 api
    @PostMapping("/out")
    public synchronized ResponseEntity<ResponseBody> sendOutData(@RequestBody RfidScanDataOutRequestDto sendOutDatas) throws InterruptedException {
        log.info("제품 출고 처리 api - 출고 데이터 확인");

        return new ResponseEntity<>(new ResponseBody(StatusCode.OK, scanDataService.sendOutData(sendOutDatas)), HttpStatus.OK);
    }

    // 스캔 데이터 입고 api
    @PostMapping("/in")
    public synchronized ResponseEntity<ResponseBody> sendInData(@RequestBody RfidScanDataInRequestDto sendInDatas) throws InterruptedException {
        log.info("제품 입고 처리 api - 입고 데이터 확인");

        return new ResponseEntity<>(new ResponseBody(StatusCode.OK, scanDataService.sendInData(sendInDatas)), HttpStatus.OK);
    }

    // 스캔 데이터 회수 api
    @PostMapping("/return")
    public synchronized ResponseEntity<ResponseBody> sendReturnData(@RequestBody RfidScanDataReturnRequestDto sendReturnDatas) throws InterruptedException {
        log.info("제품 회수 처리 api - 회수 데이터 확인");

        return new ResponseEntity<>(new ResponseBody(StatusCode.OK, scanDataService.sendReturnData(sendReturnDatas)), HttpStatus.OK);
    }

    // 스캔 데이터 세척 api
    @PostMapping("/clean")
    public synchronized ResponseEntity<ResponseBody> sendCleaningData(@RequestBody RfidScanDataCleanRequestDto sendCleanDatas) throws InterruptedException {
        log.info("제품 세척 처리 api - 세척 데이터 확인");

        return new ResponseEntity<>(new ResponseBody(StatusCode.OK, scanDataService.sendCleaningData(sendCleanDatas)), HttpStatus.OK);
    }

    // 스캔 데이터 폐기 api
    @PostMapping("/discard")
    public synchronized ResponseEntity<ResponseBody> sendDiscardData(@RequestBody RfidScanDataDiscardRequestDto senddiscardDatas) throws InterruptedException {
        log.info("제품 폐기 처리 api - 폐기 데이터 확인");

        return new ResponseEntity<>(new ResponseBody(StatusCode.OK, scanDataService.sendDiscardData(senddiscardDatas)), HttpStatus.OK);
    }

}
