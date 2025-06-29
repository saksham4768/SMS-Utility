package com.example.SMS.Utility.repository;

import com.example.SMS.Utility.model.SMSHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SMSHistoryRepository extends JpaRepository<SMSHistory, Long> {
}
