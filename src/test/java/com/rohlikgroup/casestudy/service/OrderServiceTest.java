package com.rohlikgroup.casestudy.service;

import com.rohlikgroup.casestudy.dto.CreateOrderRequest;
import com.rohlikgroup.casestudy.dto.OrderDto;
import com.rohlikgroup.casestudy.dto.OrderItemRequest;
import com.rohlikgroup.casestudy.entity.Order;
import com.rohlikgroup.casestudy.entity.OrderItem;
import com.rohlikgroup.casestudy.entity.OrderStatus;
import com.rohlikgroup.casestudy.entity.Product;
import com.rohlikgroup.casestudy.mapper.OrderMapper;
import com.rohlikgroup.casestudy.repository.OrderRepository;
import com.rohlikgroup.casestudy.repository.ProductRepository;
import com.rohlikgroup.casestudy.service.impl.OrderServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderMapper orderMapper;

    private OrderService orderService;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderRepository, productRepository, orderMapper);
    }

    private Product createTestProduct(Long id, Integer stockAmount) {
        Product product = new Product();
        product.setId(id);
        product.setName("Test Product " + id);
        product.setStockAmount(stockAmount);
        product.setPrice(BigDecimal.TEN);
        return product;
    }

    private Order createTestOrder(Long id, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setStatus(status);
        
        Product product = createTestProduct(1L, 10);
        
        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(2);
        item.setOrder(order);
        
        order.setOrderItems(List.of(item));
        return order;
    }

    @Test
    void createOrder_Success() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest(List.of(
            new OrderItemRequest(1L, 2)
        ));
        
        Product product = createTestProduct(1L, 10);
        Order mappedOrder = createTestOrder(null, null);
        Order savedOrder = createTestOrder(1L, OrderStatus.PENDING);
        OrderDto expectedDto = new OrderDto(1L, OrderStatus.PENDING, null, null);
        
        when(orderMapper.map(request)).thenReturn(mappedOrder);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.hasAvailableStock(1L, 2)).thenReturn(true);
        when(productRepository.reserveStock(1L, 2)).thenReturn(1);
        when(orderRepository.save(any())).thenReturn(savedOrder);
        when(orderMapper.map(savedOrder)).thenReturn(expectedDto);

        // When
        OrderDto result = orderService.createOrder(request);

        // Then
        assertThat(result).isEqualTo(expectedDto);
        verify(orderRepository).save(orderCaptor.capture());
        Order capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void createOrder_ProductNotFound() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest(List.of(
            new OrderItemRequest(1L, 2)
        ));
        
        Order mappedOrder = createTestOrder(null, null);
        when(orderMapper.map(request)).thenReturn(mappedOrder);
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> orderService.createOrder(request))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Product not found");
    }

    @Test
    void createOrder_InsufficientStock() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest(List.of(
            new OrderItemRequest(1L, 2)
        ));
        
        Product product = createTestProduct(1L, 1);
        Order mappedOrder = createTestOrder(null, null);
        
        when(orderMapper.map(request)).thenReturn(mappedOrder);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.hasAvailableStock(1L, 2)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> orderService.createOrder(request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Insufficient stock");
    }

    @Test
    void cancelOrder_Success() {
        // Given
        Long orderId = 1L;
        Order order = createTestOrder(orderId, OrderStatus.PENDING);
        OrderDto expectedDto = new OrderDto(orderId, OrderStatus.CANCELED, null, null);
        
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderMapper.map(any(Order.class))).thenReturn(expectedDto);

        // When
        OrderDto result = orderService.cancelOrder(orderId);

        // Then
        assertThat(result).isEqualTo(expectedDto);
        verify(productRepository).releaseStock(eq(1L), eq(2));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    void cancelOrder_OrderNotFound() {
        // Given
        Long orderId = 1L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> orderService.cancelOrder(orderId))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Order not found");
    }

    @Test
    void cancelOrder_AlreadyCanceled() {
        // Given
        Long orderId = 1L;
        Order order = createTestOrder(orderId, OrderStatus.CANCELED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When/Then
        assertThatThrownBy(() -> orderService.cancelOrder(orderId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot cancel order in status CANCELED");
    }

    @Test
    void setOrderPaid_Success() {
        // Given
        Long orderId = 1L;
        Order order = createTestOrder(orderId, OrderStatus.PENDING);
        OrderDto expectedDto = new OrderDto(orderId, OrderStatus.PAID, null, LocalDateTime.now());
        
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderMapper.map(any(Order.class))).thenReturn(expectedDto);

        // When
        OrderDto result = orderService.setOrderPaid(orderId);

        // Then
        assertThat(result).isEqualTo(expectedDto);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getPaidAt()).isNotNull();
    }

    @Test
    void setOrderPaid_OrderNotFound() {
        // Given
        Long orderId = 1L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> orderService.setOrderPaid(orderId))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Order not found");
    }

    @Test
    void setOrderPaid_NotPendingOrder() {
        // Given
        Long orderId = 1L;
        Order order = createTestOrder(orderId, OrderStatus.CANCELED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When/Then
        assertThatThrownBy(() -> orderService.setOrderPaid(orderId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Only pending orders can be paid for");
    }
} 