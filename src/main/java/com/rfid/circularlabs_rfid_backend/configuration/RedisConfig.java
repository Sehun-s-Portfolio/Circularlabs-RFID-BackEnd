/**
package com.rfid.circularlabs_rfid_backend.configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rfid.circularlabs_rfid_backend.device.domain.Device;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.resps.Tuple;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

@Configuration
public class RedisConfig {

    // RedisTemplate
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator
                .builder()
                .allowIfSubType(Object.class)
                .build();

        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) // 혹시라도 의도치 않거나 알 수 없는 정보가 들어와 시리얼라이즈를 할 수 없게 될 경우를 대비한 설정값
                .registerModule(new JavaTimeModule())
                .activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL)
                .disable(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS); // redis를 활용할 객체들에 날짜 정보가 TimeStamp 형식으로 적용되어있을 경우 그대로 RedisTemplate을 사용하면 에러가 발생하므로 그것에 대비하기 위한 설정값

        GenericJackson2JsonRedisSerializer genericJackson2JsonRedisSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory); // Reids Connection 설정
        redisTemplate.setKeySerializer(new StringRedisSerializer()); // RedisTemplate 사용 시 key는 String 타입으로 직렬화
        redisTemplate.setValueSerializer(genericJackson2JsonRedisSerializer); // RedisTemplate에 들어갈 Value는 Generic 타입으로서 어떤 클래스든 Json형식으로 직렬화할 수 있도록 함.

        return redisTemplate;
    }


/**
 @Bean public JedisPool jedisPoolSetting() {
 return new JedisPool("127.0.0.1", 6379);
 }


 // Redis - String
 public void jedisOfString() {
 try (JedisPool pool = jedisPoolSetting()) {
 try (Jedis jedis = pool.getResource()) {
 // [ String ]
 // set을 통해 String 타입의 일반 문자열 데이터를 저장
 jedis.set("users:234:email", "sehun@naver.com");
 jedis.set("users:234:name", "sehun");
 jedis.set("users:234:age", "28");

 // get을 통해 String 타입의 데이터를 조회
 System.out.println(jedis.get("users:234:email"));

 // mget을 통한 다수의 특정 String 데이터를 조회
 List<String> mgetData = jedis.mget("users:234:email", "users:234:name", "users:234:age");
 mgetData.forEach(System.out::println);

 // incr를 통한 특정 단일 데이터 증량 (없다면 바로 데이터가 생성되면서 1로 초기화)
 long counter = jedis.incr("counter");
 System.out.println(counter);

 // incrBy를 통한 특정 단일 데이터의 1씩 증가가 아닌 많은 수를 증가
 counter = jedis.incrBy("counter", 10L);
 System.out.println(counter);

 // decr를 통한 특정 단일 데이터 1 감소
 counter = jedis.decr("counter");
 System.out.println(counter);

 // decrBy를 통한 특정 단일 데이터 많은 수 감소
 counter = jedis.decrBy("counter", 10L);
 System.out.println(counter);

 }
 }
 }


 // Redis - List
 public void jedisOfList() {
 try (JedisPool pool = jedisPoolSetting()) {
 try (Jedis jedis = pool.getResource()) {
 // [ list ]
 // 1. stack
 // rpush, lpush로 데이터를 List에 저장
 jedis.rpush("stack1", "aaa");
 jedis.rpush("stack1", "bbb");
 jedis.rpush("stack1", "ccc");

 // List의 특정 길이만큼의 데이터를 추출
 List<String> stack1 = jedis.lrange("stack1", 0, -1);
 stack1.forEach(System.out::println);

 System.out.println();
 // stack의 경우, 후입선출 로직으로서 가장 나중에 들어간 데이터가 가장 먼저 추출되므로 push와 동일한 방향으로 pop을 수행
 // lpop, rpop 데이터 추출
 System.out.println(jedis.rpop("stack1"));
 System.out.println();
 System.out.println("==================================");

 // 2. queue
 // rpush, lpush로 데이터를 List에 저장
 jedis.rpush("queue1", "zzz");
 jedis.rpush("queue1", "xxx");
 jedis.rpush("queue1", "sss");

 // List의 특정 길이만큼의 데이터를 추출
 List<String> queue1 = jedis.lrange("queue1", 0, -1);
 queue1.forEach(System.out::println);

 System.out.println();
 // queue의 경우, 선입선출 로직으로서 가장 먼저 들어간 데이터가 가장 먼저 추출되므로 push의 반대 방향으로 pop을 수행
 // lpop, rpop 데이터 추출
 System.out.println(jedis.lpop("queue1"));
 System.out.println();
 System.out.println("==================================");

 // 3. block brpop, blpop
 while (true) {
 // 시간 제한을 두고 List에서 데이터를 pop으로 추출 하는 blpop, brpop
 List<String> blpop = jedis.blpop(10, "queue:blocking");
 if (blpop != null) {
 blpop.forEach(System.out::println);
 }
 }

 }
 }
 }


 // Redis - Set
 public void jedisOfSets() {
 try (JedisPool pool = jedisPoolSetting()) {
 try (Jedis jedis = pool.getResource()) {
 // [ Set ]
 jedis.sadd("users:100:follow", "150", "회원");
 jedis.sadd("users:200:follow", "150", "300");

 // set 데이터 추가
 jedis.sadd("users:500:follow", "100", "200", "300", "회원");
 // set 데이터 삭제
 jedis.srem("users:500:follow", "100");

 // set에 들어있는 값들
 Set<String> smembers = jedis.smembers("users:500:follow");
 smembers.forEach(System.out::println);

 System.out.println(jedis.sismember("users:500:follow", "200"));
 System.out.println(jedis.sismember("users:500:follow", "400"));

 // s card - set의 현재 가지고 있는 데이터의 row 수
 System.out.println(jedis.scard("users:500:follow"));

 // s inter - set 끼리 동일한 값을 가지고 있는지 확인
 Set<String> sinter = jedis.sinter("users:500:follow", "users:100:follow");
 sinter.forEach(System.out::println);

 }
 }
 }


 // Redis - Hash
 public void jedisOfHash() {
 try (JedisPool pool = jedisPoolSetting()) {
 try (Jedis jedis = pool.getResource()) {
 // [ Hash ]
 // HashSet에 단일 데이터를 넣을 경우, 바로 필드값 + value를 넣는다
 jedis.hset("users:2:info", "name", "sehun");

 // HashSet에는 다중의 데이터나 다양한 데이터 형식을 넣을 수 있으므로 곧바로 HashMap을 넣을 수 있다.
 HashMap<String, String> map = new HashMap<>();
 map.put("email", "sehun@naver.com");
 map.put("phone", "010-2222-5555");
 jedis.hset("users:2:info", map);

 // hget을 통한 특정 필드의 값을 조회
 System.out.println(jedis.hget("users:2:info", "name"));
 System.out.println(jedis.hget("users:2:info", "phone"));

 // hgetall을 통한 키에 속한 전체 필드 데이터 조회
 System.out.println(jedis.hgetAll("users:2:info"));

 // hdel을 통한 특정 필드 삭제
 jedis.hdel("users:2:info", "phone");
 System.out.println(jedis.hget("users:2:info", "phone"));

 // hincrBy를 통한 특정 필드 값 증가
 jedis.hincrBy("users:2:info", "visits", 1);
 System.out.println(jedis.hget("users:2:info", "visits"));
 }
 }
 }


 // Redis - Sorted Set
 public void jedisOfSortedSet() {
 try (JedisPool pool = jedisPoolSetting()) {
 try (Jedis jedis = pool.getResource()) {
 // [ Sorted Set ]
 HashMap<String, Double> scores = new HashMap<>();
 scores.put("user1", 100d);
 scores.put("user2", 30d);
 scores.put("user3", 50d);
 scores.put("user4", 80d);
 scores.put("user5", 15d);

 // zadd를 통한 정렬된 set에 데이터 저장 (HashMap을 통해 다수의 데이터를 한번에 저장 가능)
 jedis.zadd("game2:scores", scores);

 // zrange를 통한 특정 set의 정렬된 값들 조회
 List<String> zrange = jedis.zrange("game2:scores", 0, Long.MAX_VALUE);
 zrange.forEach(System.out::println);

 // zrangeWithScores를 통한 특정 Set의 필드값과 스코어(값)들을 함께 조회
 List<Tuple> tuples = jedis.zrangeWithScores("game2:scores", 0, Long.MAX_VALUE);
 tuples.forEach(tuple -> System.out.println(tuple.getElement() + " : " + tuple.getScore()));

 // zcard를 통한 특정 set의 필드 데이터 수
 System.out.println(jedis.zcard("game2:scores"));

 // zincrby를 통한 특정 Set의 특정 필드의 데이터를 증량
 jedis.zincrby("game2:scores", 100d, "user5");
 List<Tuple> tuples2 = jedis.zrangeWithScores("game2:scores", 0, Long.MAX_VALUE);
 tuples2.forEach(tuple -> System.out.println(tuple.getElement() + " : " + tuple.getScore()));
 }
 }
 }


 // Reids - Bitmap (메모리를 적게 사용하여 대량의 데이터 저장 가능)
 public void jedisOfBitmap() {
 try (JedisPool pool = jedisPoolSetting()) {
 try (Jedis jedis = pool.getResource()) {
 // [ Bitmap ]
 // setbit를 통한 요청 비트 설정
 jedis.setbit("request-0403", 100, true);
 jedis.setbit("request-0403", 200, true);
 jedis.setbit("request-0403", 300, true);

 // getbit를 통한 특정 요청 비트 조회
 System.out.println(jedis.getbit("request-0403", 200));
 System.out.println(jedis.getbit("request-0403", 350));

 // bitcount를 통한 특정 요청 비트의 갯수
 System.out.println(jedis.bitcount("request-0403"));

 // bitmap VS set
 // Pipeline을 통해 다수의 동작을 한번에 처리
 Pipeline pipeline = jedis.pipelined();
 // IntStream에서 특정 max 수까지 돌려주는 rangeClosed함수를 통해 100000건의 데이터를 각각 set과 bitmap에 저장
 IntStream.rangeClosed(0, 100000).forEach(each -> {
 pipeline.sadd("request-set-0403", String.valueOf(each), "1");
 pipeline.setbit("request-bit-0403", each, true);

 // 1000일 때 Pipeline 동기화를 통해 모든 요청 수행
 if(each == 1000){
 pipeline.sync();
 }
 });
 pipeline.sync();

 // memoryUsage를 통해 특정 Redis 객체 데이터의 메모리 사용량을 조회
 System.out.println("set을 사용했을 때의 메모리 사용량 : " + jedis.memoryUsage("request-set-0403"));
 System.out.println("bitmap을 사용했을 때의 메모리 사용량 : " + jedis.memoryUsage("request-bit-0403"));

 // 비교
 if(jedis.memoryUsage("request-set-0403") > jedis.memoryUsage("request-bit-0403")){
 System.out.println("bitmap이 더욱 효율적");
 }else{
 System.out.println("set이 더욱 효율적");
 }

 }
 }
 }

 // Redis - Pipeline (다수의 동작 요청을 동기화(sync)를 통해 한번에 수행)
 public void jedisOfPipeLine() {
 try (JedisPool pool = jedisPoolSetting()) {
 try (Jedis jedis = pool.getResource()) {
 // piplined를 통해 여러 동작을 수행할 파이프라인을 호출
 Pipeline pipeline = jedis.pipelined();
 // 다수의 동작 요청 (String, Set Sorted Set, Hash, List, Bitmap)
 pipeline.set("users:400:name", "sehun");
 pipeline.set("users:400:email", "sehun@naver.com");
 pipeline.set("users:400:age", "15");

 // sync(), syncAndReturnAll()를 통해 pipeline에 걸친 요청을 한번에 동기화하면서 동시에 수행하고 결과를 반환
 List<Object> objectList = pipeline.syncAndReturnAll();
 objectList.forEach(o -> System.out.println(o.toString()));
 }
 }
 }


}
**/