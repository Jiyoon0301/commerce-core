import { useEffect, useState } from 'react';
import api from '../api/axios';
import { placeOrder } from '../api/order';
import { AxiosError } from 'axios';

interface Product {
  id: number;
  name: string;
  price: number;
}

const ProductList = () => {
  const [products, setProducts] = useState<Product[]>([]);

  useEffect(() => {
    api.get('/products')
      .then(res => setProducts(res.data))
      .catch(err => console.error("상품 로드 실패:", err));
  }, []);

const handleOrder = async (productId: number, productName: string) => {
  try {
    const result = await placeOrder(productId, 1);
    alert(`${productName} 주문 성공! ${result}`);
  } catch (error: unknown) { // any 대신 unknown 사용
    // 에러 타입 가드
    if (error instanceof AxiosError) {
      const errorMsg = error.response?.data || "주문 중 오류가 발생했습니다.";
      alert(`${productName} 주문 실패: ${errorMsg}`);
    } else {
      alert("알 수 없는 에러가 발생했습니다.");
    }
  }
};

  return (
    <div className="min-h-screen bg-gray-50 p-8">
      <div className="max-w-6xl mx-auto">
        <h1 className="text-3xl font-bold text-gray-800 mb-8">상품 목록</h1>
        
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {products.map(product => (
          <div key={product.id} className="bg-white p-6 rounded-lg shadow-md">
            <h3 className="text-xl font-semibold">{product.name}</h3>
            <p className="text-blue-600 font-bold mt-2">{product.price.toLocaleString()}원</p>
            {/* 버튼을 주문하기로 변경 */}
            <button 
              onClick={() => handleOrder(product.id, product.name)}
              className="mt-4 w-full bg-green-500 text-white py-2 rounded-md hover:bg-green-600 transition-colors"
            >
              주문하기
            </button>
          </div>
        ))}
          {/* {products.map(product => (
            <div key={product.id} className="bg-white p-6 rounded-lg shadow-md hover:shadow-lg transition-shadow">
              <h3 className="text-xl font-semibold text-gray-700">{product.name}</h3>
              <p className="text-blue-600 font-bold mt-2">{product.price.toLocaleString()}원</p>
              <button className="mt-4 w-full bg-blue-500 text-white py-2 rounded-md hover:bg-blue-600 transition-colors">
                상세보기
              </button>
            </div>
          ))} */}
        </div>

        {products.length === 0 && (
          <p className="text-gray-500 text-center mt-10">등록된 상품이 없습니다. 백엔드 데이터를 확인해주세요!</p>
        )}
      </div>
    </div>
  );
};

export default ProductList;