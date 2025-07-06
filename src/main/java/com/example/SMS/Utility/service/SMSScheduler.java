package com.example.SMS.Utility.service;

import com.example.SMS.Utility.model.SMSRequest;
import com.example.SMS.Utility.repository.SMSRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

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
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicLong startTime = new AtomicLong();

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setName("Custom-Scheduler");
        return thread;
    });

    //@PostConstruct :- Call init once, after all spring managed beans is created
    @PostConstruct
    public void init() {
        //this::scheduler same as () -> scheduler()
        scheduledExecutorService.schedule(this::scheduler, 0, TimeUnit.SECONDS);
    }

    public void scheduler(){
        try{
            if (!started.get()) {
                startTime.set(System.currentTimeMillis());
                started.set(true);
            }

            List<SMSRequest> pendingSMS = smsRepository.fetchpendingRows(smsLimit == 0 ? 10 : smsLimit);
            if(pendingSMS.isEmpty()){
                if (started.get()) {
                    long totalDuration = System.currentTimeMillis() - startTime.get();
                    logger.info("✅ Finished processing all SMS in {} ms", totalDuration);
                    started.set(false); // reset for next round
                }
                log.info("No SMS to process. Delaying for 5s.");
                scheduledExecutorService.schedule(this::scheduler,5,TimeUnit.SECONDS);
                return;
            }

            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for(SMSRequest sms : pendingSMS){
                //Completable future works asynchronously work as @Async annotation

                //::Method reference operator
                //serviceSMS::processedSMS is equivalent to (sms->serviceSMS.processedSMS(sms))
                CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
                    sms.setStatus(2);
                    sms.setComments("In Progress");
                    return sms;
                }, executor).thenCompose(serviceSMS::processedSMS)
                        .exceptionally(ex -> {
                            log.error("Error processing SMS id {}: {}", sms.getInsertionOrderId(), ex.getMessage());
                            return null;
                        });
                futures.add(future);
            }

            //.join() bocking the threads that runs scheduler()
            //scheduler run every fixedRate but if previous one is not completed the new scheduler run is skipped
            //The method won't be called again until the previous run finishes.
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .orTimeout(10, TimeUnit.SECONDS).join();

            scheduledExecutorService.schedule(this::scheduler, 0, TimeUnit.SECONDS);
        }
        catch (Exception e){
            log.error("Exception occured : ", e);
            scheduledExecutorService.schedule(this::scheduler, 5, TimeUnit.SECONDS);
        }
    }
}
