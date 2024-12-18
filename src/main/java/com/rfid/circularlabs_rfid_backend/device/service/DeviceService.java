package com.rfid.circularlabs_rfid_backend.device.service;

import com.querydsl.core.Tuple;
import com.rfid.circularlabs_rfid_backend.device.response.DeviceResponseDto;
import com.rfid.circularlabs_rfid_backend.member.domain.Member;
import com.rfid.circularlabs_rfid_backend.member.response.GetClientsResponseDto;
import com.rfid.circularlabs_rfid_backend.product.response.GetProductInfo;
import com.rfid.circularlabs_rfid_backend.query.device.DeviceQueryData;
import com.rfid.circularlabs_rfid_backend.query.member.MemberQueryData;
import com.rfid.circularlabs_rfid_backend.query.product.ProductQueryData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import static com.rfid.circularlabs_rfid_backend.device.domain.QDevice.device;
import static com.rfid.circularlabs_rfid_backend.member.domain.QMember.member;

@Slf4j
@RequiredArgsConstructor
@Service
public class DeviceService {

    private final DeviceQueryData deviceQueryData;
    private final MemberQueryData memberQueryData;
    private final ProductQueryData productQueryData;
    //private final JedisPool jedisPool;
    //private final RedisTemplate<String, Object> deviceRedisTemplate;
    //private final RedisHashConfirmDeviceDataRepository redisHashConfirmDeviceDataRepository;

    // 기기에 매핑된 공급사 코드 추출 service
    public DeviceResponseDto getSupplierCode(HttpServletResponse response, String dc) {
        log.info("service 단으로 넘어온 기기 코드 : {}", dc);

        return useRedisTemplate(response, dc);
    }


    // 곧바로 RedisTemplate만을 사용하여 활용
    private DeviceResponseDto useRedisTemplate(HttpServletResponse response, String dc) {
        log.info("RedisTemplate을 통한 Redis 데이터 추출");

        // 기기 정보 일부 추출
        Tuple deviceInfo = deviceQueryData.getSeveralDeviceInfo(dc);
        String supplierCode = deviceInfo.get(device.supplierCode); // 기기에 속한 공급사 코드

        // 공급사 정보 일부 추출
        Tuple supplierInfo = memberQueryData.getSupplierId(supplierCode);
        Long supplierId = supplierInfo.get(member.memberId); // 공급사 id
        String supplierName = supplierInfo.get(member.clientCompany); // 공급사 이름

        // 공급사에 해당되는 고객사 코드 리스트
        List<Member> clients = memberQueryData.getClients(supplierCode);
        List<GetClientsResponseDto> responseClients = new ArrayList<>();

        // 공급사에 해당되는 고객사들을 하나씩 조회하여 정보를 반환 객체에 저장
        for (Member client : clients) {
            responseClients.add(
                    GetClientsResponseDto.builder()
                            .memberId(client.getMemberId())
                            .companyName(client.getClientCompany())
                            .classificationCode(client.getClassificationCode())
                            .build()
            );
        }

        // 모든 제품 리스트
        List<GetProductInfo> productsInfo = productQueryData.getProductInfoList();

        // 반환 및 Redis에 저장될 Json 객체 데이터
        DeviceResponseDto deviceResponseDto = DeviceResponseDto.builder()
                .supplierId(supplierId)
                .supplierCode(supplierCode)
                .supplierName(supplierName)
                .clients(responseClients)
                .productsInfo(productsInfo)
                .build();

        /**
        // RedisTemplate를 활용하여 캐시 데이터 저장
        deviceRedisTemplate.opsForValue().set("confirm:device:" + dc + ":code", deviceResponseDto); // 기기 확정 후 기기 코드 및 공급사, 고객사들, 제품들 정보 Json 직렬화 후 저장

        responseClients.forEach(client -> {
            deviceRedisTemplate.opsForSet().add("confirm:device:" + dc + ":clients", client);
            //deviceRedisTemplate.opsForSet().members("confirm:device:" + dc + ":clients");
        });

        productsInfo.forEach(product -> {
            deviceRedisTemplate.opsForSet().add("confirm:device:" + dc + ":products", product);
            //deviceRedisTemplate.opsForSet().members("confirm:device:" + dc + ":products");
        });
         **/

        // 쿠키 설정
        Cookie cookie = new Cookie("supplier", supplierCode);
        cookie.setMaxAge(604800);

        // HTTP 응답 헤더 생성 및 데이터 저장
        response.addHeader("supplier", supplierCode);
        response.addHeader("device", dc);
        response.addHeader("Set-Cookie", supplierCode);
        response.addCookie(cookie);

        return deviceResponseDto;
    }


    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
    // Spring Cache 사용
    @Cacheable(cacheNames = CACHE1, key = "'confirm:device:' + #dc + ':code'")
    public DeviceResponseDto useSpringCache(HttpServletResponse response, String dc) {
        log.info("Spring Cache를 통한 데이터 추출");

        // 기기 정보 일부 추출
        Tuple deviceInfo = deviceQueryData.getSeveralDeviceInfo(dc);
        String supplierCode = deviceInfo.get(device.supplierCode); // 기기에 속한 공급사 코드

        // 공급사 정보 일부 추출
        Tuple supplierInfo = memberQueryData.getSupplierId(supplierCode);
        Long supplierId = supplierInfo.get(member.memberId); // 공급사 id
        String supplierName = supplierInfo.get(member.clientCompany); // 공급사 이름

        // 공급사에 해당되는 고객사 코드 리스트
        List<Member> clients = memberQueryData.getClients(supplierCode);
        List<GetClientsResponseDto> responseClients = new ArrayList<>();

        // 공급사에 해당되는 고객사들을 하나씩 조회하여 정보를 반환 객체에 저장
        for (Member client : clients) {
            responseClients.add(
                    GetClientsResponseDto.builder()
                            .memberId(client.getMemberId())
                            .companyName(client.getClientCompany())
                            .classificationCode(client.getClassificationCode())
                            .build()
            );
        }

        // 모든 제품 리스트
        List<GetProductInfo> productsInfo = productQueryData.getProductInfoList();

        // 반환 및 Redis에 저장될 Json 객체 데이터
        DeviceResponseDto deviceResponseDto = DeviceResponseDto.builder()
                .supplierId(supplierId)
                .supplierCode(supplierCode)
                .supplierName(supplierName)
                .clients(responseClients)
                .productsInfo(productsInfo)
                .build();

        // RedisTemplate를 활용하여 캐시 데이터 저장
        deviceRedisTemplate.opsForValue().set("confirm:device:" + dc + ":code", deviceResponseDto, Duration.ofSeconds(30)); // 기기 확정 후 기기 코드 및 공급사, 고객사들, 제품들 정보 Json 직렬화 후 저장

        // 쿠키 설정
        Cookie cookie = new Cookie("supplier", supplierCode);
        cookie.setMaxAge(604800);

        // HTTP 응답 헤더 생성 및 데이터 저장
        response.addHeader("supplier", supplierCode);
        response.addHeader("device", dc);
        response.addHeader("Set-Cookie", supplierCode);
        response.addCookie(cookie);

        return DeviceResponseDto.builder()
                .supplierId(supplierId)
                .supplierCode(supplierCode)
                .supplierName(supplierName)
                .clients(responseClients)
                .productsInfo(productsInfo)
                .build();
    }
**/

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
    // RedisHash를 활용
    private DeviceResponseDto useRedisHash(HttpServletResponse response, String dc) {
        // RedisHash에 데이터
        RedisHashConfirmDeviceData confirmDeviceData = redisHashConfirmDeviceDataRepository.findByDeviceCode(dc).orElseGet(() -> {
            // 기기 정보 일부 추출
            Tuple deviceInfo = deviceQueryData.getSeveralDeviceInfo(dc);
            String supplierCode = deviceInfo.get(device.supplierCode); // 기기에 속한 공급사 코드
            Long deviceId = deviceInfo.get(device.deviceId); // 기기 id

            // 공급사 정보 일부 추출
            Tuple supplierInfo = memberQueryData.getSupplierId(supplierCode);
            Long supplierId = supplierInfo.get(member.memberId); // 공급사 id
            String supplierName = supplierInfo.get(member.clientCompany); // 공급사 이름

            // 공급사에 해당되는 고객사 코드 리스트
            List<Member> clients = memberQueryData.getClients(supplierCode);
            List<GetClientsResponseDto> responseClients = new ArrayList<>();

            // 공급사에 해당되는 고객사들을 하나씩 조회하여 정보를 반환 객체에 저장
            for (Member client : clients) {
                responseClients.add(
                        GetClientsResponseDto.builder()
                                .memberId(client.getMemberId())
                                .companyName(client.getClientCompany())
                                .classificationCode(client.getClassificationCode())
                                .build()
                );
            }

            // 모든 제품 리스트
            List<GetProductInfo> productsInfo = productQueryData.getProductInfoList();

            // RedisHash에 캐시 데이터가 존재하지 않으므로 데이터 저장 (TTL 30초)
            RedisHashConfirmDeviceData redisHashConfirmDeviceData =
                    RedisHashConfirmDeviceData.builder()
                            .confirmDeviceDataId(deviceId)
                            .deviceCode(dc)
                            .supplierId(supplierId)
                            .supplierCode(supplierCode)
                            .supplierName(supplierName)
                            .clients(responseClients)
                            .productsInfo(productsInfo)
                            .build();

            redisHashConfirmDeviceDataRepository.save(redisHashConfirmDeviceData);

            return redisHashConfirmDeviceData;
        });

        // 쿠키 설정
        Cookie cookie = new Cookie("supplier", confirmDeviceData.getSupplierCode());
        cookie.setMaxAge(604800);

        // HTTP 응답 헤더 생성 및 데이터 저장
        response.addHeader("supplier", confirmDeviceData.getSupplierCode());
        response.addHeader("device", dc);
        response.addHeader("Set-Cookie", confirmDeviceData.getSupplierCode());
        response.addCookie(cookie);


        return DeviceResponseDto.builder()
                .supplierId(confirmDeviceData.getSupplierId())
                .supplierCode(confirmDeviceData.getSupplierCode())
                .supplierName(confirmDeviceData.getSupplierName())
                .clients(confirmDeviceData.getClients())
                .productsInfo(confirmDeviceData.getProductsInfo())
                .build();
    }
**/

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    /**
     // Jedis 사용
     private DeviceResponseDto useJedis(String dc, Long supplierId, String supplierCode, String supplierName, List<GetClientsResponseDto> responseClients, List<GetProductInfo> productsInfo){
     // Redis Client와 소통하여 빠르게 확정 데이터 저장
     try(Jedis jedis = jedisPool.getResource()){
     jedis.set("confirm:device:" + dc + ":code", dc); // 기기 확정 시 기기 코드
     jedis.set("confirm:device:" + dc + ":supplier:id", String.valueOf(supplierId)); // 기기 확정 시 공급사 id
     jedis.set("confirm:device:" + dc + ":supplier:code", supplierCode); // 기기 확정 시 공급사 코드
     jedis.set("confirm:device:" + dc + ":supplier:name", supplierName); // 기기 확정 시 공급사 명

     // 기기 확정 시 공급사에 속한 고객사들 정보
     responseClients.forEach(client -> {
     String clientInfo = client.getMemberId() + ":" + client.getClassificationCode() + ":" + client.getCompanyName();
     jedis.sadd("confirm:device:" + dc + ":clients", clientInfo);
     });

     // 기기 확정 후 모든 제품들 정보
     productsInfo.forEach(product -> {
     String productInfo = product.getProductCode() + ":" + product.getProductName();
     jedis.sadd("confirm:device:" + dc + ":products", productInfo);
     });
     }

     return DeviceResponseDto.builder()
     .supplierId(supplierId)
     .supplierCode(supplierCode)
     .supplierName(supplierName)
     .clients(responseClients)
     .productsInfo(productsInfo)
     .build();
     }
     **/


}


