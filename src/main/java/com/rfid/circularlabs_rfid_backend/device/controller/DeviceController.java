package com.rfid.circularlabs_rfid_backend.device.controller;

import com.rfid.circularlabs_rfid_backend.device.domain.Device;
import com.rfid.circularlabs_rfid_backend.device.response.DeviceResponseDto;
import com.rfid.circularlabs_rfid_backend.device.service.DeviceService;
import com.rfid.circularlabs_rfid_backend.member.response.GetClientsResponseDto;
import com.rfid.circularlabs_rfid_backend.product.response.GetProductInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/cl/device")
@RestController
public class DeviceController {

    private final DeviceService deviceService;
    //private final JedisPool jedisPool;
    //private final RedisTemplate<String, Object> deviceRedisTemplate;

    // 기기에 매핑된 공급사 코드 추출 api
    @GetMapping("/supplier")
    public DeviceResponseDto getSupplierCode(HttpServletResponse response, @RequestParam String dc){
        log.info("공급사 코드를 추출할 기기 코드 : {}", dc);

        return useRedisTemplate(response, dc);
        //return useSpringCache(response, dc);
        //return useJedis(response, dc);
    }

    // RedisTemplate 이용
    private DeviceResponseDto useRedisTemplate(HttpServletResponse response, String dc){
        log.info("RedisTemplate 활용");

/**
        String key = "confirm:device:" + dc + ":code";
        // 1. cache에서 질의 요청 후 있으면 get
        Object deviceConfirmData = deviceRedisTemplate.opsForValue().get(key);

        if(deviceConfirmData != null){
            log.info("Redis에 캐시 데이터가 존재 시 해당 캐시 데이터를 빠르게 추출");

            // 만약, 고객사나 제품이 추가가 되거나 삭제되어 변동사항이 발생되었을 때 기존에
            // Redis에 저장된 데이터를 활용하기엔 무리이니 이것을 업데이트 시켜줄 방법을 구상해야한다.

            return (DeviceResponseDto)deviceConfirmData;
        }else{
            log.info("Redis에 캐시 데이터가 존재하지 않아 DB에서부터 데이터를 추출 및 세팅");
            // 기존에 기기 확정된 적이 없으면 초기 서비스 동작 로직 수행
            return deviceService.getSupplierCode(response, dc);
        }
         **/

        return deviceService.getSupplierCode(response, dc);
    }


    // Spring Cache 이용
    /**
    private DeviceResponseDto useSpringCache(HttpServletResponse response, String dc){
        log.info("Spring Cache 활용");

        return deviceService.useSpringCache(response, dc);
    }
     **/


    /**
    // Jedis 이용
    private DeviceResponseDto useJedis(HttpServletResponse response, String dc){
        // Redis Client 접속
        try(Jedis jedis = jedisPool.getResource()){

            // [ aside 패턴 ] - GET 과 같이 빠르게 데이터를 조회하고 반환할 때 주로 사용하는 패턴
            // 1. cache 질의 요청
            // 2. cache에 데이터 존재하지 않으면 DB에 질의 요청
            // 3. cache에 저장
            // end

            // Redis Client에 이미 확정된 기기 코드 정보를 조회
            String deviceCode = jedis.get("confirm:device:" + dc + ":code");

            // 만약 기존에 확정된 기기 코드 정보가 존재할 경우
            if(deviceCode != null){

                // 공급사에 속한 고객사들을 담을 반환 리스트 객체
                List<GetClientsResponseDto> responseClients = new ArrayList<>();
                // 모든 제품 정보들을 담을 반환 리스트 객체
                List<GetProductInfo> responseProducts = new ArrayList<>();

                // Redis Cache에 저장된 공급사에 속한 고객사들 정보 추출
                Set<String> clients = jedis.smembers("confirm:device:" + dc + ":clients");
                // 추출한 고객사들 정보들을 반환객체에 저장
                clients.forEach(client -> {
                    String[] clientDetailInfo = client.split(":");
                    String clientMemberId = clientDetailInfo[0];
                    String clientClassificationCode = clientDetailInfo[1];
                    String clientName = clientDetailInfo[2];

                    responseClients.add(
                            GetClientsResponseDto.builder()
                                    .memberId(Long.parseLong(clientMemberId))
                                    .classificationCode(clientClassificationCode)
                                    .companyName(clientName)
                                    .build()
                    );
                });

                // Redis Cache에 저장된 모든 제품들 정보 추출
                Set<String> products = jedis.smembers("confirm:device:" + dc + ":products");
                // 모든 제품 정보들을 하나씩 조회하여 반환 객체에 저장
                products.forEach(product -> {
                    String[] productDetailInfo = product.split(":");
                    String productCode = productDetailInfo[0];
                    String productName = productDetailInfo[1];

                    responseProducts.add(
                            GetProductInfo.builder()
                                    .productCode(productCode)
                                    .productName(productName)
                                    .build()
                    );
                });

                // 이미 Cache에 저장된 데이터들을 활용하여 빠르게 반환객체에 담아 반환
                return DeviceResponseDto.builder()
                        .supplierId(Long.parseLong(jedis.get("confirm:device:" + dc + ":supplier:id")))
                        .supplierCode(jedis.get("confirm:device:" + dc + ":supplier:code"))
                        .supplierName(jedis.get("confirm:device:" + dc + ":supplier:name"))
                        .clients(responseClients)
                        .productsInfo(responseProducts)
                        .build();
            }else{
                // 기존에 기기 확정된 적이 없으면 초기 서비스 동작 로직 수행
                return deviceService.getSupplierCode(response, dc);
            }

        }
    }
     **/



}
