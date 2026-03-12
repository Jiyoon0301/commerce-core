package com.commercecore.stock.service;

import com.commercecore.member.domain.Member;
import com.commercecore.member.repository.MemberRepository;
import com.commercecore.order.service.OrderFacade;
import com.commercecore.order.dto.OrderCreateRequest;
import com.commercecore.product.domain.Product;
import com.commercecore.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.commercecore.stock.service.RedisStockService;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class StockConcurrencyTest {

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private RedisStockService redisStockService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    private Long testProductId;

    @BeforeEach
    public void setUp() {
        // 1. 회원 저장
        Member member = new Member("테스트유저", "test@email.com");
        memberRepository.save(member);

        // 2. 상품 저장 (빌더 패턴 사용)
        Product product = Product.builder()
                .name("테스트상품")
                .price(10000L)
                .stockQuantity(100) // Redis 동기화용 기본값
                .build();
        Product savedProduct = productRepository.save(product);

        // 저장된 상품의 실제 ID를 필드에 저장
        this.testProductId = savedProduct.getId();

        // 3. (선택사항) Redis 초기화: 테스트 직전 Redis의 재고도 100으로 동기화
        redisStockService.setStock(this.testProductId, 100);
    }

    @Test
    public void 동시에_100명이_주문하면_재고는_0이_된다() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    // 고유한 주문을 위해 루프 인덱스 대신 고유키 생성
                    // 테스트 시에는 각기 다른 사용자가 주문한다고 가정
                    OrderCreateRequest request = createMockRequest();
                    orderFacade.placeOrder(1L, request, "unique-key-" + Math.random());
                } catch (Exception e) {
                    // 에러가 나면 콘솔에 찍히도록 추가
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        // 2. 검증 로직 추가
        // redisStockService에 getStock(Long productId) 메서드가 있다고 가정합니다.
        int remainingStock = redisStockService.getStock(1L);

        System.out.println("📢 [TEST 결과] 남은 재고: " + remainingStock);
        assertThat(remainingStock).isEqualTo(0);
    }

    private OrderCreateRequest createMockRequest() {
        // 상품 ID 1번, 수량 1개로 주문 생성 (이미 정의해둔 of 메서드 활용)
        return OrderCreateRequest.of(1L, 1L, 1);
    }
}