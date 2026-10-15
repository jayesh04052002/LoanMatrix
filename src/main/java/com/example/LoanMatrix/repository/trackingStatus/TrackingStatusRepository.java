
package com.example.LoanMatrix.repository.trackingStatus;

import com.example.LoanMatrix.entity.LoanDeal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrackingStatusRepository
        extends JpaRepository<LoanDeal, Long> {

    List<LoanDeal> findByCustomer_Id(Long customerId);

    long countByCustomer_Id(Long customerId);
}
