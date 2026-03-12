package com.commercecore.order.service;

import com.commercecore.member.domain.Member;
import com.commercecore.member.repository.MemberRepository;
import com.commercecore.order.domain.OrderStatus;
import com.commercecore.order.dto.OrderCreateRequest;
import com.commercecore.order.repository.OrderRepository;
import com.commercecore.product.domain.Product;
import com.commercecore.product.repository.ProductRepository;
import com.commercecore.stock.service.RedisStockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class OrderConcurrencyTest {

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private RedisStockService redisStockService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired private MemberRepository memberRepository;
    @Autowired private ProductRepository productRepository;

    private Long savedMemberId;
    private Long savedProductId;

    @BeforeEach
    void setUp() {
        // 1. 테스트용 회원 생성 및 저장
        Member member = memberRepository.save(new Member("테스터", "test@test.com"));
        savedMemberId = member.getId();

        // 2. 테스트용 상품 생성 및 저장 (가격 10000원, 재고 100개)
        Product product = productRepository.save(new Product("테스트 상품", 10000L, 100));
        savedProductId = product.getId();

        // 3. Redis 재고 초기화
        redisStockService.setStock(savedProductId, 100);

        // 4. 기존 주문 데이터 삭제 (테스트 격리)
        orderRepository.deleteAll();
    }

    @Test
    void 동시_200명_주문시_100개만_성공해야한다() throws InterruptedException {
        // given
        int threadCount = 200;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 테스트 루프 직전
        System.out.println("🚩 [DEBUG] 테스트 시작 전 재고: " + redisStockService.getStock(savedProductId));
        // when
        for (int i = 0; i < threadCount; i++) {
            // 이제 모든 스레드가 동일한 savedMemberId와 savedProductId를 사용합니다.
            executorService.submit(() -> {
                try {
                    OrderCreateRequest request = OrderCreateRequest.of(1L, savedProductId, 1);
                    String idempotencyKey = UUID.randomUUID().toString(); // 각 스레드마다 고유 키
                    orderFacade.placeOrder(savedMemberId, request, idempotencyKey);
                } catch (Exception e) {
                    // 재고 부족으로 인한 에러는 정상!
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        Thread.sleep(2000); // DB 반영 대기

        // then
        long realSuccessCount = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.PAID)
                .count();

        System.out.println("최종 성공 개수: " + realSuccessCount);
        assertThat(realSuccessCount).isEqualTo(100);
        assertThat(redisStockService.getStock(savedProductId)).isEqualTo(0);
    }
}

