package com.rohlikgroup.casestudy.controller;

import com.rohlikgroup.casestudy.dto.CreateOrderRequest;
import com.rohlikgroup.casestudy.dto.OrderDto;
import com.rohlikgroup.casestudy.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderDto> cancelOrder(@PathVariable Long id) {
        var canceledOrder = orderService.cancelOrder(id);
        return ResponseEntity.ok(canceledOrder);
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<OrderDto> payOrder(@PathVariable Long id) {
        var paidOrder = orderService.setOrderPaid(id);
        return ResponseEntity.ok(paidOrder);
    }

    @PostMapping
    public  ResponseEntity<OrderDto> createOrder(@RequestBody CreateOrderRequest request){
        return orderService.createOrder(createOrder()
    }
}
