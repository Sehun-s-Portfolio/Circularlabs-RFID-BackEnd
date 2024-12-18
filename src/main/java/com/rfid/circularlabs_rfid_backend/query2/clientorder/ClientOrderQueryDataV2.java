package com.rfid.circularlabs_rfid_backend.query2.clientorder;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.rfid.circularlabs_rfid_backend.process.domain.ClientOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.rfid.circularlabs_rfid_backend.process.domain.QClientOrder.clientOrder;

@Slf4j
@RequiredArgsConstructor
@Component
public class ClientOrderQueryDataV2 {

    private final JPAQueryFactory jpaQueryFactory;
    private final EntityManager entityManager;

    // 출고 시 고객사 주문 상태 변경
    @Transactional
    public void updateClientOrder(int orderCompleteCount, String productCode, String supplierCode, String clientCode) {
        log.info("주문 완료 처리 Query 함수 진입");

        // 출고 수량과 비교 계산할 ClientOrder 수량
        int completeFlowCount = 0;

        // 주문 완료 처리할 가장 오래된 순의 ClientOrder 리스트
        List<ClientOrder> completeOrders = jpaQueryFactory
                .selectFrom(clientOrder)
                .where(clientOrder.productCode.eq(productCode)
                        .and(clientOrder.motherCode.eq(supplierCode))
                        .and(clientOrder.classificationCode.eq(clientCode))
                        .and(clientOrder.orderMount.loe(orderCompleteCount))
                        .and(clientOrder.status.eq("주문대기")))
                .orderBy(clientOrder.createdAt.asc())
                .fetch();

        // 주문 완료 처리할 ClientOrder의 아이디 리스트
        List<Long> clientOrderIdList = new ArrayList<>();

        log.info("스캔한 수량 : {}", orderCompleteCount);

        // 조회한 가장 오래된 순의 ClientOrder 리스트를 하나씩 조회
        for(ClientOrder eachClientOrder : completeOrders){
            log.info("요청 주문 수량 : {}", eachClientOrder.getOrderMount());

            // 만약 completeFlowCount와 ClientOrder의 수량 합이 요청 출고 수량 보다 작거나 같은 경우
            if(completeFlowCount + eachClientOrder.getOrderMount() <= orderCompleteCount){
                // 주문 완료 처리할 리스트에 해당 ClientOrder 아이디를 저장
                clientOrderIdList.add(eachClientOrder.getClientOrderId());
                // completeFlowCount에 해당 주문 수량을 축적
                completeFlowCount += eachClientOrder.getOrderMount();

                log.info("합친 요청 주문 수량 : {}", completeFlowCount);
            }

            // 만약 요청받은 출고 수량과 completeFLowCount가 동일하다면 주문 완료 리스트 저장 완료이므로 반복문 탈출
            if(completeFlowCount == orderCompleteCount){
                break;
            }
        }

        if(completeFlowCount != orderCompleteCount){
            log.info("요청 주문 수량과 스캔 데이터 수량이 불일치");
        }else{
            log.info("요청 주문 수량과 스캔 데이터 수량이 일치");
        }


        // 주문 완료 리스트에 저장된 아이디를 가진 ClientOrder 들을 주문 완료 처리
        for(Long eachClientOrderId : clientOrderIdList){
            jpaQueryFactory
                    .update(clientOrder)
                    .set(clientOrder.status, "주문완료")
                    .set(clientOrder.deliveryAt, LocalDateTime.now())
                    .where(clientOrder.productCode.eq(productCode)
                            .and(clientOrder.motherCode.eq(supplierCode))
                            .and(clientOrder.classificationCode.eq(clientCode))
                            .and(clientOrder.clientOrderId.eq(eachClientOrderId)))
                    .execute();
        }

        entityManager.flush();
        entityManager.clear();
    }
}
