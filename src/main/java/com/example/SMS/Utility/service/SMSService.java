package com.example.SMS.Utility.service;

import com.example.SMS.Utility.model.SMSHistory;
import com.example.SMS.Utility.model.SMSRequest;
import com.example.SMS.Utility.repository.SMSHistoryRepository;
import com.example.SMS.Utility.repository.SMSRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class SMSService {
    private static final Logger logger = LoggerFactory.getLogger(SMSService.class);

    private static final String api_URL = "http://localhost:9090/api/v1/message";

    private final SMSRepository smsRepository;
    private final SMSHistoryRepository smsHistoryRepository;

    private final WebClient webClient;

    public CompletableFuture<Void> processedSMS(SMSRequest Body){
        try{
            if(Body.getBody() == null || Body.getRecipientID() == null || Body.getBody().equalsIgnoreCase("") || Body.getRecipientID().equalsIgnoreCase("")){
                failed(Body, "Body or recipient can not be null");
                smsRepository.save(Body);
                return CompletableFuture.completedFuture(null);
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("recipient", Body.getRecipientID());
            requestBody.put("smsBody", Body.getBody());
            requestBody.put("senderId", Body.getSenderID());
            long start = System.currentTimeMillis();
            return webClient.post()
                    .uri(api_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .toEntity(String.class)
                    .doOnSuccess(response -> onSuccess(response, Body, start))
                    .doOnError(err -> {
                        failed(Body, err.getMessage());
                        Body.setResponseJSON(err.getMessage());
                        smsRepository.save(Body);
                        logger.error("While processing the rows error throwing:- {}", err.getMessage());
                    }).then().toFuture();

        } catch (Exception e) {
            logger.error("Exception happened while calling the for :- {} and exception is:- {}", Body.getInsertionOrderId(), e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }
    private void onSuccess(ResponseEntity<?> response, SMSRequest Body, long start){
        long end = System.currentTimeMillis();
        logger.info("⏱ External API response time: {} ms", (end - start));
        if(response.getStatusCode().is2xxSuccessful()){
            if(Body.getRetryCount() == null){
                Body.setRetryCount(1);
            }
            else {
                Body.setRetryCount(Body.getRetryCount() + 1);
            }
            Body.setComments("Message Sent Successfully");
            Body.setDelieverdTime(LocalDateTime.now());
            Body.setResponseJSON(response.toString());
            Body.setStatus(4);
            smsRepository.save(Body);

            SMSHistory history = new SMSHistory();
            history.setComments("Message Sent Successfully");
            history.setStatus(4);
            history.setBody(Body.getBody());
            history.setRecipientID(Body.getRecipientID());
            history.setSenderID(Body.getSenderID());
            history.setDelieverdTime(LocalDateTime.now());
            history.setModule(Body.getModule());

            smsHistoryRepository.save(history);
        }
        else{
            failed(Body, response.toString());
            Body.setResponseJSON(response.toString());
            smsRepository.save(Body);
        }
        end = System.currentTimeMillis();
        logger.info("⏱ OverAll processing time: {} ms", (end - start));
    }
    private void failed(SMSRequest Body, String comments){
        if(Body.getRetryCount() == null){
            Body.setRetryCount(1);
        }
        else {
            Body.setRetryCount(Body.getRetryCount() + 1);
        }
        Body.setComments(comments);
        Body.setStatus(3);
    }
}
