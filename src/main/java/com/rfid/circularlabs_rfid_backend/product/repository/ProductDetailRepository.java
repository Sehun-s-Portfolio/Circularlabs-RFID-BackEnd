package com.rfid.circularlabs_rfid_backend.product.repository;

import com.rfid.circularlabs_rfid_backend.product.domain.ProductDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductDetailRepository extends JpaRepository<ProductDetail, Long> {
}
