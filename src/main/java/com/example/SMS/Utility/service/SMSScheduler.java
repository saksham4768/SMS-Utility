package com.example.SMS.Utility.service;

import com.example.SMS.Utility.model.SMSRequest;
import com.example.SMS.Utility.repository.SMSRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class SMSScheduler {

    @Value("${sms.limit:10}")
    private int smsLimit;

    private static final Logger logger = LoggerFactory.getLogger(SMSScheduler.class);

    private final SMSRepository smsRepository;
    private final SMSService serviceSMS;
    private final ThreadPoolTaskExecutor executor;


    @Scheduled(fixedRate = 6000)
    public void scheduler(){
        List<SMSRequest> pendingSMS = smsRepository.fetchpendingRows(smsLimit == 0 ? 10 : smsLimit);
        if(pendingSMS.isEmpty()){
            logger.info("No pending SMS to send to the user.");
            return;
        }
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for(SMSRequest sms : pendingSMS){
            //Completeable future works asynchronously work as @Async annotation

            //::Method reference operator
            //serviceSMS::processedSMS is equivalent to (sms->serviceSMS.processedSMS(sms))
            CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
                sms.setStatus(2);
                sms.setComments("In Progress");
                return sms;
            }, executor).thenCompose(serviceSMS::processedSMS);
            futures.add(future);
        }

        //.join() bocking the threads that runs scheduler()
        //scheduler run every fixedRate but if previous one is not completed the new scheduler run is skipped
        //The method won't be called again until the previous run finishes.
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .orTimeout(6, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    logger.error("Time out occurred while waiting for future complete: {}", ex.getMessage());
                    return null;
                }).join();
    }
}
