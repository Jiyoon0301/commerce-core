package com.commercecore.order.service;

import com.commercecore.member.domain.Member;
import com.commercecore.member.repository.MemberRepository;
import com.commercecore.order.domain.Order;
import com.commercecore.order.domain.OrderItem;
import com.commercecore.order.domain.OrderStatus;
import com.commercecore.order.repository.OrderRepository;
import com.commercecore.product.repository.ProductRepository;
import com.commercecore.stock.service.RedisStockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@Transactional // 테스트 완료 후 DB 데이터를 롤백하기 위해 추가
class OrderCancelTest {

    @Autowired private OrderService orderService;
    @Autowired private RedisStockService redisStockService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private MemberRepository memberRepository; // 추가 필요

    @Autowired private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    private Long savedProductId = 1L;
    private Member testMember;

    @BeforeEach
    void setUp() {
        // 1. 멤버 생성
        testMember = memberRepository.save(new Member("test@test.com", "Jiyoon"));

        // 2. 찌꺼기 데이터 삭제 (Key 형식을 RedisStockService와 맞춰주세요)
        String stockKey = "product:stock:" + savedProductId;
        redisTemplate.delete(stockKey);

        // 3. 재고 초기화
        redisStockService.setStock(savedProductId, 10);
    }

    @Test
    @DisplayName("주문을 취소하면 Redis 재고가 복구되고 DB 상태가 CANCELLED로 변해야 한다")
    void 주문_취소시_재고_복구_테스트() {
        // given: 실제 주문을 하나 생성해서 DB에 넣습니다.
        // (이 부분은 기존에 만드신 Order.create 로직을 활용하세요)
        Order order = Order.create(testMember, List.of(
                OrderItem.create(savedProductId, 5000, 2) // 🚩 순서 변경: 가격 5000, 수량 2
        ));
        Order savedOrder = orderRepository.save(order);
        Long orderId = savedOrder.getId();

        // 주문 발생 시뮬레이션 (Redis 재고 차감)
        redisStockService.decrease(savedProductId, 2);
        int stockAfterOrder = redisStockService.getStock(savedProductId);


        String key = "product:stock:" + savedProductId;

        // when: 주문 취소 실행
        orderService.cancel(orderId);

        // then: 검증
        int finalStock = redisStockService.getStock(savedProductId);

        // 1. Redis 재고가 다시 10개로 돌아왔는지 확인
        assertThat(finalStock).isEqualTo(10);

        // 2. DB의 주문 상태가 CANCELLED인지 확인
        Order updatedOrder = orderRepository.findById(orderId).get();
        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
}