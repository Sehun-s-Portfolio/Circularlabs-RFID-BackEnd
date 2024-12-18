package com.rfid.circularlabs_rfid_backend.configuration;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManager;

@Slf4j
@Configuration
public class QueryDslConfig {

    @Bean
    public JPAQueryFactory jpaQueryFactory(EntityManager entityManager){
        log.info("QueryDSL 설정 - EntityManager : {})", entityManager);
        return new JPAQueryFactory(entityManager);
    }
}