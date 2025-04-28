package com.rohlikgroup.casestudy.service;

import com.rohlikgroup.casestudy.entity.Order;
import com.rohlikgroup.casestudy.entity.OrderItem;
import com.rohlikgroup.casestudy.entity.OrderStatus;
import com.rohlikgroup.casestudy.entity.Product;
import com.rohlikgroup.casestudy.repository.OrderRepository;
import com.rohlikgroup.casestudy.repository.ProductRepository;
import com.rohlikgroup.casestudy.scheduler.ReleaseUnpaidOrdersScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ReleaseUnpaidOrdersSchedulerTest {
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ReleaseUnpaidOrdersScheduler scheduler;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    private Order createTestOrder(Long id, LocalDateTime createdAt, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setStatus(status);
        order.setCreatedAt(createdAt);

        Product product = new Product();
        product.setId(1L);

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(2);
        item.setOrder(order);

        order.setOrderItems(List.of(item));
        return order;
    }


    @Test
    void shouldNotProcessAnyOrdersWhenNoUnpaidOrdersExist() {
        // Given
        when(orderRepository.findUnpaidOrdersForCancellation(any(), eq(OrderStatus.PENDING)))
                .thenReturn(Collections.emptyList());

        // When
        scheduler.releaseUnpaidOrders();

        // Then
        verify(productRepository, never()).releaseStock(any(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldCancelUnpaidOrdersAndReleaseStock() {
        // Given
        LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(30);
        Order unpaidOrder = createTestOrder(1L, thirtyMinutesAgo, OrderStatus.PENDING);

        when(orderRepository.findUnpaidOrdersForCancellation(any(), eq(OrderStatus.PENDING)))
                .thenReturn(List.of(unpaidOrder));

        // When
        scheduler.releaseUnpaidOrders();

        // Then
        verify(productRepository).releaseStock(eq(1L), eq(2));
        verify(orderRepository).save(orderCaptor.capture());

        Order savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    void shouldContinueProcessingWhenOneOrderFails() {
        // Given
        LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(30);
        Order order1 = createTestOrder(1L, thirtyMinutesAgo, OrderStatus.PENDING);
        Order order2 = createTestOrder(2L, thirtyMinutesAgo, OrderStatus.PENDING);

        when(orderRepository.findUnpaidOrdersForCancellation(any(), eq(OrderStatus.PENDING)))
                .thenReturn(List.of(order1, order2));

        doThrow(new RuntimeException("Stock release failed"))
                .when(productRepository)
                .releaseStock(eq(1L), eq(2));

        // When
        scheduler.releaseUnpaidOrders();

        // Then
        verify(productRepository, times(2)).releaseStock(eq(1L), eq(2));
        verify(orderRepository).save(orderCaptor.capture());

        Order savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getId()).isEqualTo(2L);
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    void shouldProcessMultipleOrdersSuccessfully() {
        // Given
        LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(30);
        List<Order> unpaidOrders = new ArrayList<>();
        for (long i = 1; i <= 3; i++) {
            unpaidOrders.add(createTestOrder(i, thirtyMinutesAgo, OrderStatus.PENDING));
        }

        when(orderRepository.findUnpaidOrdersForCancellation(any(), eq(OrderStatus.PENDING)))
                .thenReturn(unpaidOrders);

        // When
        scheduler.releaseUnpaidOrders();

        // Then
        verify(productRepository, times(3)).releaseStock(eq(1L), eq(2));
        verify(orderRepository, times(3)).save(any());

        verify(orderRepository).save(argThat(order ->
                order.getStatus() == OrderStatus.CANCELED &&
                        order.getId() != null &&
                        order.getId() >= 1 &&
                        order.getId() <= 3
        ));
    }
}
