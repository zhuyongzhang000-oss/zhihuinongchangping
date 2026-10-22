package com.agritrace.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** 时间统一用 yyyy-MM-dd HH:mm:ss，日期用 yyyy-MM-dd（默认） */
@Configuration
public class JacksonConfig {
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer dateTimeCustomizer() {
        return b -> b.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME))
                .deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(DATE_TIME));
    }
}
