package com.rfid.circularlabs_rfid_backend;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableBatchProcessing
@EnableCaching
//@EnableRedisRepositories
@EnableJpaAuditing
@SpringBootApplication
public class CircularLabsRfidBackEndApplication {

    public static void main(String[] args) {
        SpringApplication.run(CircularLabsRfidBackEndApplication.class, args);
        System.out.println("앱(APP) 어플리케이션 실행~~~~~~~~~~~~~");
        System.out.println("JDK 버전 17");

        //new RedisConfig().jedisOfString();
		//new RedisConfig().jedisOfList();
		//new RedisConfig().jedisOfSets();
		//new RedisConfig().jedisOfHash();
        //new RedisConfig().jedisOfSortedSet();
        //new RedisConfig().jedisOfBitmap();
        //new RedisConfig().jedisOfPipeLine();
    }

}
