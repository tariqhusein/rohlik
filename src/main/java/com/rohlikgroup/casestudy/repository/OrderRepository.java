package com.rohlikgroup.casestudy.repository;

import com.rohlikgroup.casestudy.entity.Order;
import com.rohlikgroup.casestudy.entity.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    boolean existsByOrderItemsProductIdAndStatusNotIn(Long productId, Set<OrderStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT o FROM Order o 
            LEFT JOIN FETCH o.orderItems items 
            LEFT JOIN FETCH items.product 
            WHERE o.status = :status 
            AND o.createdAt <= :createdBefore
            """)
    List<Order> findUnpaidOrdersForCancellation(
            @Param("createdBefore") LocalDateTime createdBefore,
            @Param("status") OrderStatus status);
}
