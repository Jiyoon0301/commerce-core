package com.commercecore;

import com.commercecore.stock.service.RedisStockService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing // 시간 자동으로 기록
public class CommerceCoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(CommerceCoreApplication.class, args);
	}

	// 서버 시작 시 자동으로 실행되는 로직 추가
	@Bean
	CommandLineRunner initStock(RedisStockService redisStockService) {
		return args -> {
			// 상품 ID 1번에 재고 100개 강제 주입!
			redisStockService.setStock(1L, 100);
			System.out.println("✅ Redis 재고 세팅 완료: product:stock:1 -> 100");
		};
	}
}