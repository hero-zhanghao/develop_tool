package com.easypay.service.user.configuration;

import com.easypay.service.user.repository.cache.JedisRepository;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.ser.std.DateSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Configuration
public class RedisConfiguration {


    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private Integer redisPort;



    @Bean("reactiveRedisTemplate")
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(ReactiveRedisConnectionFactory connectionFactory) {
        Jackson2JsonRedisSerializer jacksonSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.EVERYTHING);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.EVERYTHING, JsonTypeInfo.As.PROPERTY);
        JavaTimeModule timeModule = new JavaTimeModule();
        timeModule.addSerializer(Date.class, new DateSerializer(Boolean.FALSE, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")));
        timeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        timeModule.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        timeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern("HH:mm:ss")));
        timeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        timeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        timeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern("HH:mm:ss")));
        objectMapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.registerModule(timeModule).registerModule(new ParameterNamesModule()).registerModules(ObjectMapper.findModules());
        jacksonSerializer.setObjectMapper(objectMapper);

        final StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        RedisSerializationContext<String , Object> serializationContext = RedisSerializationContext
                .<String, Object>newSerializationContext()
                .key(stringRedisSerializer)
                .value(jacksonSerializer)
                .hashKey(stringRedisSerializer)
                .hashValue(jacksonSerializer)
                .build();

        return new ReactiveRedisTemplate(connectionFactory, serializationContext);
    }

    @Bean
    public ReactiveValueOperations<String,Object> valueOperations( ReactiveRedisTemplate<String,Object> reactiveRedisTemplate){
        return reactiveRedisTemplate.opsForValue();
    }

    @Bean
    public ReactiveSetOperations<String,Object> setOperations(ReactiveRedisTemplate<String,Object> reactiveRedisTemplate){
        return reactiveRedisTemplate.opsForSet();
    }

    @Bean
    public ReactiveListOperations<String,Object> listOperations(ReactiveRedisTemplate<String,Object> reactiveRedisTemplate){
        return reactiveRedisTemplate.opsForList();
    }

    @Bean
    public ReactiveHashOperations<String,String,Object> hashOperations(ReactiveRedisTemplate<String,Object> reactiveRedisTemplate){
        return reactiveRedisTemplate.opsForHash();
    }

    @Bean
    public ReactiveZSetOperations<String,Object> zSetOperations(ReactiveRedisTemplate<String,Object> reactiveRedisTemplate){
        return reactiveRedisTemplate.opsForZSet();
    }


    @Bean
    public JedisRepository jedisRepository() {
        return new JedisRepository(redisHost, redisPort);
    }


}
