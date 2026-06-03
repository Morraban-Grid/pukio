package com.pukio.appserver.dataaccess;

import com.pukio.appserver.domain.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    List<Sale> findByStore_StoreIdAndShiftId(String storeId, String shiftId);

    List<Sale> findBySaleDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT CASE WHEN COUNT(si) > 0 THEN true ELSE false END FROM SaleItem si WHERE si.product.sku = :sku")
    boolean existsBySku(@Param("sku") String sku);
}
