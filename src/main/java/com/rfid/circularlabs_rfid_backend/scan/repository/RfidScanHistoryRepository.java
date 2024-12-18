package com.rfid.circularlabs_rfid_backend.scan.repository;

import com.rfid.circularlabs_rfid_backend.scan.domain.RfidScanHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RfidScanHistoryRepository extends JpaRepository<RfidScanHistory, Long> {
}
