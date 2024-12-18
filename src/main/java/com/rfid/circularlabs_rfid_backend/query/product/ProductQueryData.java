package com.rfid.circularlabs_rfid_backend.query.product;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.rfid.circularlabs_rfid_backend.product.response.GetProductInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.rfid.circularlabs_rfid_backend.product.domain.QProduct.product;

@Slf4j
@RequiredArgsConstructor
@Component
@Getter
public class ProductQueryData {

    private final JPAQueryFactory jpaQueryFactory;

    // 앱 상에서 제품 스캔 시 같이 추출될 제품 명들 조회
    public List<GetProductInfo> getProductInfoList(){

        List<GetProductInfo> productInfos = new ArrayList<>();

        List<Tuple> productsInfo = jpaQueryFactory
                .select(product.productCode, product.productName)
                .from(product)
                .fetch();

        for(Tuple eachProductInfo : productsInfo){
            productInfos.add(
                    GetProductInfo.builder()
                            .productCode(eachProductInfo.get(product.productCode))
                            .productName(eachProductInfo.get(product.productName))
                            .build()
            );
        }

        return productInfos;
    }
}
