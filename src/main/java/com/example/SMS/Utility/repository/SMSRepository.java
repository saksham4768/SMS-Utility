package com.example.SMS.Utility.repository;

import com.example.SMS.Utility.model.SMSRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SMSRepository extends JpaRepository<SMSRequest, Long> {

    @Query(value = """
            SELECT *
            FROM smsqueue
            WHERE (retry_count IS NULL OR retry_count != 5)
            AND (status IS NULL OR status = 3)
            ORDER BY priority
            LIMIT ?1;
            """, nativeQuery = true)
    List<SMSRequest> fetchpendingRows(int smsLimit);
}
