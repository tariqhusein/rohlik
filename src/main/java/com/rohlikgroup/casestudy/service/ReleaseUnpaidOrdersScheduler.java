package com.rohlikgroup.casestudy.scheduler;

import com.rohlikgroup.casestudy.entity.Order;
import com.rohlikgroup.casestudy.entity.OrderStatus;
import com.rohlikgroup.casestudy.repository.OrderRepository;
import com.rohlikgroup.casestudy.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReleaseUnpaidOrdersScheduler {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
    public void releaseUnpaidOrders() {
        LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(30);

        // Find orders that are PENDING and created more than 30 minutes ago
        // Using pessimistic lock to prevent race conditions with payment processing
        List<Order> unpaidOrders = orderRepository.findUnpaidOrdersForCancellation(thirtyMinutesAgo, OrderStatus.PENDING);

        if (unpaidOrders.isEmpty()) {
            return;
        }

        log.info("Found {} unpaid orders to cancel", unpaidOrders.size());

        for (Order order : unpaidOrders) {
            try {
                // Release stock for each order item
                order.getOrderItems().forEach(item ->
                        productRepository.releaseStock(item.getProduct().getId(), item.getQuantity())
                );

                // Mark order as cancelled
                order.setStatus(OrderStatus.CANCELED);
                orderRepository.save(order);

                log.info("Successfully cancelled order {} and released stock", order.getId());
            } catch (Exception e) {
                // Log error but continue processing other orders
                log.error("Failed to process unpaid order {}: {}", order.getId(), e.getMessage());
            }
        }
    }
}