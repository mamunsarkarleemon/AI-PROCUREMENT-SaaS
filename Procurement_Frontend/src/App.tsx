import { BrowserRouter, Link, Navigate, Route, Routes } from 'react-router'
import { AuthProvider } from './auth/AuthProvider'
import { GuestOnly, RequireAuth, RequireMenu } from './auth/RouteGuards'
import { AppLayout } from './layouts/AppLayout'
import { PublicLayout } from './layouts/PublicLayout'
import { CategoriesPage } from './pages/admin/CategoriesPage'
import { MenuAccessPage } from './pages/admin/MenuAccessPage'
import { MessagesPage } from './pages/admin/MessagesPage'
import { UsersPage } from './pages/admin/UsersPage'
import { DashboardPage } from './pages/app/DashboardPage'
import { MarketplacePage } from './pages/app/MarketplacePage'
import { MyProductsPage } from './pages/app/MyProductsPage'
import { MyQuotesPage } from './pages/app/MyQuotesPage'
import { MyRequestsPage } from './pages/app/MyRequestsPage'
import { NegotiationDetailPage } from './pages/app/NegotiationDetailPage'
import { NegotiationsPage } from './pages/app/NegotiationsPage'
import { OrderDetailPage } from './pages/app/OrderDetailPage'
import { OrdersPage } from './pages/app/OrdersPage'
import { ProductDetailPage } from './pages/app/ProductDetailPage'
import { ProfilePage } from './pages/app/ProfilePage'
import { ReportsPage } from './pages/app/ReportsPage'
import { RequestDetailPage } from './pages/app/RequestDetailPage'
import { LandingPage } from './pages/public/LandingPage'
import { LoginPage } from './pages/public/LoginPage'
import { RegisterPage } from './pages/public/RegisterPage'

function NotFound() {
  return (
    <div className="page-center">
      <h1>Page not found</h1>
      <Link className="btn btn-primary" to="/">Go to home page</Link>
    </div>
  )
}

/**
 * Public:  /  /login  /register
 * App:     /app/...  (needs login; each page also needs its menu to be enabled for the role)
 */
export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route element={<PublicLayout />}>
            <Route path="/" element={<LandingPage />} />
            <Route element={<GuestOnly />}>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
            </Route>
          </Route>

          <Route element={<RequireAuth />}>
            <Route path="/app" element={<AppLayout />}>
              <Route index element={<Navigate to="dashboard" replace />} />
              <Route path="dashboard" element={<DashboardPage />} />
              <Route path="profile" element={<ProfilePage />} />

              <Route path="marketplace" element={<RequireMenu menu="marketplace"><MarketplacePage /></RequireMenu>} />
              <Route path="products/:id" element={<ProductDetailPage />} />
              <Route path="my-products" element={<RequireMenu menu="my_products"><MyProductsPage /></RequireMenu>} />
              <Route path="my-requests" element={<RequireMenu menu="my_requests"><MyRequestsPage /></RequireMenu>} />
              <Route path="requests/:id" element={<RequestDetailPage />} />
              <Route path="my-quotes" element={<RequireMenu menu="my_quotes"><MyQuotesPage /></RequireMenu>} />
              <Route path="negotiations" element={<RequireMenu menu="negotiations"><NegotiationsPage /></RequireMenu>} />
              <Route path="negotiations/:id" element={<NegotiationDetailPage />} />
              <Route path="orders" element={<RequireMenu menu="orders"><OrdersPage /></RequireMenu>} />
              <Route path="orders/:id" element={<OrderDetailPage />} />
              <Route path="reports" element={<RequireMenu menu="reports"><ReportsPage /></RequireMenu>} />

              <Route path="admin/users" element={<RequireMenu menu="admin_users"><UsersPage /></RequireMenu>} />
              <Route path="admin/products" element={<RequireMenu menu="admin_products"><MyProductsPage admin /></RequireMenu>} />
              <Route path="admin/requests" element={<RequireMenu menu="admin_requests"><MyRequestsPage admin /></RequireMenu>} />
              <Route path="admin/categories" element={<RequireMenu menu="admin_categories"><CategoriesPage /></RequireMenu>} />
              <Route path="admin/menu-access" element={<RequireMenu menu="admin_menus"><MenuAccessPage /></RequireMenu>} />
              <Route path="admin/messages" element={<RequireMenu menu="admin_messages"><MessagesPage /></RequireMenu>} />
              <Route path="*" element={<NotFound />} />
            </Route>
          </Route>

          <Route path="*" element={<NotFound />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
