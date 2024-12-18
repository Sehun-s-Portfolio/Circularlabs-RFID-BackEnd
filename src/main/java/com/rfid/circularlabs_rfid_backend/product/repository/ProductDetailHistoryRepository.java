package com.rfid.circularlabs_rfid_backend.product.repository;

import com.rfid.circularlabs_rfid_backend.product.domain.ProductDetailHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductDetailHistoryRepository extends JpaRepository<ProductDetailHistory, Long> {
}
