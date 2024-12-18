package com.rfid.circularlabs_rfid_backend.process.repository;

import com.rfid.circularlabs_rfid_backend.process.domain.DiscardHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscardHistoryRepository extends JpaRepository<DiscardHistory, Long> {
}
