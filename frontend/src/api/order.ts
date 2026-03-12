import api from './axios';
import { v4 as uuidv4 } from 'uuid';

export const placeOrder = async (productId: number, quantity: number) => {
  // 1. 멱등성 키 생성 (중복 주문 방지)
  const idempotencyKey = uuidv4();

  // 2. 백엔드 OrderController로 요청
  const response = await api.post('/orders', {
    memberId: 1, // 테스트용 고정 ID
    items: [
      {
        productId: productId,
        quantity: quantity
      }
    ]
  }, {
    headers: {
      'X-Idempotency-Key': idempotencyKey
    }
  });

  return response.data;
};