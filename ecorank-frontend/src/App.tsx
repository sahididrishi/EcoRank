import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AuthProvider } from '@/context/AuthContext'
import ErrorBoundary from '@/components/common/ErrorBoundary'
import ProtectedRoute from '@/components/auth/ProtectedRoute'
import AdminLayout from '@/components/layout/AdminLayout'
import StoreLayout from '@/components/layout/StoreLayout'
import LoginPage from '@/pages/LoginPage'
import DashboardPage from '@/pages/DashboardPage'
import ProductsPage from '@/pages/ProductsPage'
import ProductFormPage from '@/pages/ProductFormPage'
import OrdersPage from '@/pages/OrdersPage'
import OrderDetailPage from '@/pages/OrderDetailPage'
import PlayerPage from '@/pages/PlayerPage'
import PlayerDetailPage from '@/pages/PlayerDetailPage'
import StorefrontPage from '@/pages/store/StorefrontPage'
import CheckoutPage from '@/pages/store/CheckoutPage'
import CheckoutSuccessPage from '@/pages/store/CheckoutSuccessPage'
import CheckoutCancelPage from '@/pages/store/CheckoutCancelPage'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
      staleTime: 30_000,
    },
  },
})

export default function App() {
  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <BrowserRouter>
            <Routes>
              <Route path="/login" element={<LoginPage />} />

              <Route element={<ProtectedRoute />}>
                <Route element={<AdminLayout />}>
                  <Route path="/dashboard" element={<DashboardPage />} />
                  <Route path="/products" element={<ProductsPage />} />
                  <Route path="/products/new" element={<ProductFormPage />} />
                  <Route path="/products/:id/edit" element={<ProductFormPage />} />
                  <Route path="/orders" element={<OrdersPage />} />
                  <Route path="/orders/:id" element={<OrderDetailPage />} />
                  <Route path="/players" element={<PlayerPage />} />
                  <Route path="/players/:uuid" element={<PlayerDetailPage />} />
                </Route>
              </Route>

              <Route element={<StoreLayout />}>
                <Route path="/store" element={<StorefrontPage />} />
                <Route path="/store/checkout/success" element={<CheckoutSuccessPage />} />
                <Route path="/store/checkout/cancel" element={<CheckoutCancelPage />} />
                <Route path="/store/checkout/:slug" element={<CheckoutPage />} />
              </Route>

              <Route path="/" element={<Navigate to="/store" replace />} />
              <Route path="*" element={<Navigate to="/store" replace />} />
            </Routes>
          </BrowserRouter>
        </AuthProvider>
      </QueryClientProvider>
    </ErrorBoundary>
  )
}
