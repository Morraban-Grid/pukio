package com.pukio.appserver.dataaccess;

import com.pukio.appserver.domain.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for Promotion entity.
 * 
 * @author Pukio Team
 * @since Entregable 2
 */
@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    
    /**
     * Find all active promotions.
     * 
     * @return List of active promotions
     */
    List<Promotion> findByActiveTrue();
    
    /**
     * Find all active promotions within the valid date range.
     * REQ 2.4: THE Application_Server SHALL apply promotion if sale amount exceeds minimum threshold.
     */
    List<Promotion> findByActiveAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Boolean active, LocalDate startDate, LocalDate endDate);
}
