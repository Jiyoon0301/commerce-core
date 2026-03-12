package com.commercecore.stock.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class RedisStockService {
    // 스프링에서 레디스에 데이터를 읽고 쓰기 위한 도구
    private final StringRedisTemplate redisTemplate;

    // 1. 매번 생성하지 않도록 필드로 관리 (성능 최적화), 레디스에서 실행할 Lua 스크립트를 담는 그릇
    private DefaultRedisScript<Long> decreaseScript;

    // 레디스에 저장될 키의 접두사
    private static final String STOCK_KEY = "product:stock:";

    // Lua 스크립트
    private static final String DECREASE_SCRIPT =
            "local stock = redis.call('get', KEYS[1]) " +
                    "if not stock then return -2 end " + // 키가 없으면 -2 반환
                    "stock = tonumber(stock) " +
                    "if stock >= tonumber(ARGV[1]) then " +
                    "   return redis.call('decrby', KEYS[1], ARGV[1]) " +
                    "else " +
                    "   return -1 " + // 재고 부족이면 -1 반환
                    "end";

    // 재고 감소 메서드
    public void decrease(Long productId, int quantity) {
        String key = STOCK_KEY + productId;
        Long result = redisTemplate.execute(decreaseScript, Collections.singletonList(key), String.valueOf(quantity));

        if (result == -2) {
            throw new IllegalStateException("Redis에 키가 없음: " + key);
        }
        if (result == -1) {
            throw new IllegalStateException("재고 부족: " + key);
        }
    }

    // 재고 증가 (주문 취소 시 사용)
    public void increase(Long productId, int quantity) {
        String key = STOCK_KEY + productId;
        redisTemplate.opsForValue().increment(key, quantity);
    }

    // 초기화
    @PostConstruct
    public void init() {
        // 객체 생성 시 스크립트를 미리 로드
        decreaseScript = new DefaultRedisScript<>();
        decreaseScript.setScriptText(DECREASE_SCRIPT);
        decreaseScript.setResultType(Long.class);
    }

    public void setStock(Long productId, int quantity) {
        redisTemplate.opsForValue().set(STOCK_KEY + productId, String.valueOf(quantity));    }

    public int getStock(Long productId) {
        String val = redisTemplate.opsForValue().get(STOCK_KEY + productId);
        return val == null ? 0 : Integer.parseInt(val);
    }
}