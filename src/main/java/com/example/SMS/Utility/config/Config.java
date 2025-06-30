package com.example.SMS.Utility.config;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.Executor;

@Configuration
@Slf4j
public class  Config{

    private final static Logger logger = LoggerFactory.getLogger(Config.class);
    @Value("${sms.executor.core-pool-size:10}")
    private int corePoolSize;

    @Value("${sms.executor.max-pool-size:15}")
    private int maxPoolSize;

    @Value("${sms.executor.queue-capacity:20}")
    private int queueCapacity;

    @Bean
    public ThreadPoolTaskExecutor threadPool(){
        logger.info("corePoolSize:-{}, maxPoolSize:-{},queueCapacity:-{}", corePoolSize, maxPoolSize, queueCapacity);
        ThreadPoolTaskExecutor executer = new ThreadPoolTaskExecutor();
        executer.setCorePoolSize(corePoolSize == 0 ? 10 : corePoolSize);
        executer.setMaxPoolSize(maxPoolSize == 0 ? 15 : maxPoolSize);
        executer.setQueueCapacity(queueCapacity == 0 ? 20 : queueCapacity);
        executer.setThreadNamePrefix("SMS-Thread-");
        executer.initialize();
        return executer;
    }

//    @Bean
//    public RestTemplate restTemplate(RestTemplateBuilder builder){
//        return builder.build();
//    }

    @Bean
    public WebClient webClient(){
        return WebClient.builder().build();
    }
}
