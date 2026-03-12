import { useEffect, useState } from 'react';
import api from '../api/axios';

// 1. 타입 명확히 정의
interface Order {
  id: number;
  status: string;
  totalPrice: number;
  orderDate: string;
}

const OrderHistory = () => {
  const [orders, setOrders] = useState<Order[]>([]);

  useEffect(() => {
    // API 요청 시 쿼리 파라미터로 memberId 전달
    api.get('/orders/my-orders?memberId=1')
      .then((res) => {
        setOrders(res.data);
      })
      .catch((err) => console.error("주문 내역 조회 실패:", err));
  }, []);

  return (
    <div className="p-8 max-w-4xl mx-auto">
      <h2 className="text-2xl font-bold mb-6 text-gray-800">내 주문 내역</h2>
      <div className="bg-white shadow rounded-lg overflow-hidden">
        <table className="w-full text-left">
          <thead className="bg-gray-100">
            <tr>
              <th className="p-4">주문번호</th>
              <th className="p-4">주문일자</th>
              <th className="p-4">주문상태</th>
              <th className="p-4">총 금액</th>
            </tr>
          </thead>
          <tbody>
            {/* 2. any 대신 명시된 Order 타입 사용 */}
            {orders.map((order: Order) => (
              <tr key={order.id} className="border-t hover:bg-gray-50">
                <td className="p-4">{order.id}</td>
                <td className="p-4">{new Date(order.orderDate).toLocaleDateString()}</td>
                <td className="p-4">{order.status}</td>
                <td className="p-4">{order.totalPrice.toLocaleString()}원</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default OrderHistory;