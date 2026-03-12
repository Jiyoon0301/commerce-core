import { Routes, Route, Navigate } from 'react-router-dom';
import ProductList from '../pages/ProductList';
import OrderHistory from '../pages/OrderHistory';

export const AppRoutes = () => {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/products" replace />} />
      <Route path="/products" element={<ProductList />} />
      <Route path="/my-orders" element={<OrderHistory />} />
    </Routes>
  );
};