import { Link } from 'react-router-dom';

const Navbar = () => {
  return (
    <nav className="bg-gray-800 p-4 text-white shadow-md">
      <div className="max-w-6xl mx-auto flex gap-6">
        <Link to="/products" className="hover:text-blue-300 font-bold">상품 목록</Link>
        <Link to="/my-orders" className="hover:text-blue-300 font-bold">주문 내역</Link>
      </div>
    </nav>
  );
};

export default Navbar;