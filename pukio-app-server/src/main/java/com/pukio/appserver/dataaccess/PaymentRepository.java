package com.pukio.appserver.dataaccess;

import com.pukio.appserver.domain.Payment;
import com.pukio.appserver.domain.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findBySale(Sale sale);
}
