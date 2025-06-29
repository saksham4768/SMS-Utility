package com.example.SMS.Utility.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "SMSQUEUE")
public class SMSRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long insertionOrderId;

    @Column(columnDefinition = "TEXT")
    private String body;
    private String recipientID;
    private String senderID;
    private Integer retryCount;
    private String module;
    private LocalDateTime entryTime;
    private LocalDateTime delieverdTime;
    private String comments;
    private Integer priority;
    private Integer status;

    @Column(columnDefinition = "TEXT")
    private String responseJSON;
}
