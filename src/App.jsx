import { Routes, Route } from 'react-router-dom'
import LandingPage from './pages/LandingPage'
import Login from './pages/Login'
import Dashboard from './pages/Dashboards'
import Spaces from './pages/Spaces'
import Tenants from './pages/Tenants'
import Leases from './pages/Leases'
import Payments from './pages/Payments'

function App() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<Login />} />
      <Route path="/dashboard" element={<Dashboard />} />
      <Route path="/spaces" element={<Spaces />} />
      <Route path="/tenants" element={<Tenants />} />
      <Route path="/leases" element={<Leases />} />
      <Route path="/payments" element={<Payments />} />
    </Routes>
  )
}

export default App