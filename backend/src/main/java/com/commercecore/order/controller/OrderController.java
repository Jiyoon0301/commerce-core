package com.commercecore.order.controller;

import com.commercecore.order.dto.OrderCreateRequest;
import com.commercecore.order.dto.OrderResponse;
import com.commercecore.order.service.OrderFacade;
import com.commercecore.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderFacade orderFacade;
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<String> placeOrder(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody OrderCreateRequest request
    ) {
        // 멱등키가 없으면 새로 생성 (테스트 편의성)
        String key = (idempotencyKey != null) ? idempotencyKey : UUID.randomUUID().toString();

        // request에 memberId가 있다면 request.getMemberId() 사용, 없다면 1L 사용
        Long memberId = (request.getMemberId() != null) ? request.getMemberId() : 1L;

        Long orderId = orderFacade.placeOrder(memberId, request, key);

        return ResponseEntity.ok("주문 완료! 주문번호: " + orderId + ", 멱등키: " + key);
    }

    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderResponse>> getMyOrders(@RequestParam Long memberId) {
        return ResponseEntity.ok(orderService.findAllByMemberId(memberId));
    }
}