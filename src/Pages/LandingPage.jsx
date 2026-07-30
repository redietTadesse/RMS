import { Link } from 'react-router-dom'
import { FaBuilding, FaUsers, FaFileContract, FaMoneyBill, FaArrowRight, FaCheckCircle, FaShieldAlt, FaHeadset } from 'react-icons/fa'

function LandingPage() {
  return (
    <div className="min-h-screen bg-white">
      {/* Navigation */}
      <nav className="fixed top-0 left-0 right-0 bg-white/80 backdrop-blur-md border-b border-gray-100 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center gap-2">
              <span className="text-2xl">🏢</span>
              <span className="text-xl font-bold text-gray-800">RMS</span>
            </div>
            <div className="flex items-center gap-4">
              <Link 
                to="/login" 
                className="text-gray-600 hover:text-gray-800 transition-colors"
              >
                Sign In
              </Link>
              <Link 
                to="/dashboard" 
                className="px-4 py-2 bg-blue-500 hover:bg-blue-600 text-white font-medium rounded-lg transition-colors"
              >
                Get Started
              </Link>
            </div>
          </div>
        </div>
      </nav>

      {/* Hero Section */}
      <section className="pt-32 pb-20 px-4">
        <div className="max-w-7xl mx-auto">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
            <div>
              <span className="inline-block px-4 py-1 bg-blue-100 text-blue-700 text-sm font-semibold rounded-full mb-4">
                🚀 Coming Soon
              </span>
              <h1 className="text-4xl md:text-5xl lg:text-6xl font-bold text-gray-800 leading-tight">
                Simplify Your{' '}
                <span className="text-blue-500">Rental Management</span>
              </h1>
              <p className="mt-4 text-lg text-gray-500 max-w-lg">
                The all-in-one platform for managing commercial spaces, 
                tenants, leases, and payments. Built for building managers 
                who want to save time and reduce manual work.
              </p>
              <div className="mt-8 flex flex-wrap gap-4">
                <Link 
                  to="/dashboard" 
                  className="flex items-center gap-2 px-6 py-3 bg-blue-500 hover:bg-blue-600 text-white font-medium rounded-lg transition-colors"
                >
                  Get Started <FaArrowRight />
                </Link>
                <button className="px-6 py-3 border border-gray-300 hover:border-gray-400 text-gray-700 font-medium rounded-lg transition-colors">
                  Learn More
                </button>
              </div>
              <div className="mt-6 flex items-center gap-4 text-sm text-gray-500">
                <span className="flex items-center gap-1">✅ No credit card</span>
                <span className="flex items-center gap-1">✅ Free trial</span>
                <span className="flex items-center gap-1">✅ Setup in 5 minutes</span>
              </div>
            </div>
            <div className="relative">
              <div className="bg-gradient-to-br from-blue-500 to-purple-600 rounded-3xl p-8 text-white">
                <div className="bg-white/10 backdrop-blur-sm rounded-2xl p-6">
                  <h3 className="text-xl font-semibold mb-4">📊 Dashboard Preview</h3>
                  <div className="grid grid-cols-2 gap-4 mb-6">
                    <div className="bg-white/10 rounded-xl p-4">
                      <p className="text-2xl font-bold">24</p>
                      <p className="text-sm opacity-80">Total Spaces</p>
                    </div>
                    <div className="bg-white/10 rounded-xl p-4">
                      <p className="text-2xl font-bold">18</p>
                      <p className="text-sm opacity-80">Active Leases</p>
                    </div>
                  </div>
                  <div className="bg-white/10 rounded-xl p-4">
                    <p className="text-sm opacity-80 mb-2">Recent Payment</p>
                    <p className="font-semibold">ABC Trading • ETB 15,000</p>
                    <p className="text-sm opacity-80">Paid on July 25, 2026</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-20 bg-gray-50 px-4">
        <div className="max-w-7xl mx-auto">
          <div className="text-center mb-12">
            <h2 className="text-3xl md:text-4xl font-bold text-gray-800">
              Everything You Need to Manage Rentals
            </h2>
            <p className="mt-2 text-gray-500 max-w-2xl mx-auto">
              All the tools you need to run your rental operations smoothly
            </p>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            <div className="bg-white rounded-2xl p-6 shadow-sm border border-gray-100 hover:shadow-md transition-shadow">
              <div className="w-12 h-12 bg-blue-100 text-blue-600 rounded-xl flex items-center justify-center mb-4">
                <FaBuilding className="text-2xl" />
              </div>
              <h3 className="text-lg font-semibold text-gray-800">Space Management</h3>
              <p className="mt-2 text-gray-500 text-sm">
                Track all commercial spaces, their status, and availability. 
                Know at a glance which spaces are vacant or occupied.
              </p>
            </div>
            <div className="bg-white rounded-2xl p-6 shadow-sm border border-gray-100 hover:shadow-md transition-shadow">
              <div className="w-12 h-12 bg-green-100 text-green-600 rounded-xl flex items-center justify-center mb-4">
                <FaUsers className="text-2xl" />
              </div>
              <h3 className="text-lg font-semibold text-gray-800">Tenant Management</h3>
              <p className="mt-2 text-gray-500 text-sm">
                Keep all tenant information in one place. Contact details, 
                business info, and lease history all easily accessible.
              </p>
            </div>
            <div className="bg-white rounded-2xl p-6 shadow-sm border border-gray-100 hover:shadow-md transition-shadow">
              <div className="w-12 h-12 bg-purple-100 text-purple-600 rounded-xl flex items-center justify-center mb-4">
                <FaFileContract className="text-2xl" />
              </div>
              <h3 className="text-lg font-semibold text-gray-800">Lease Management</h3>
              <p className="mt-2 text-gray-500 text-sm">
                Create and manage lease agreements. Automatically update 
                space status when leases are created or terminated.
              </p>
            </div>
            <div className="bg-white rounded-2xl p-6 shadow-sm border border-gray-100 hover:shadow-md transition-shadow">
              <div className="w-12 h-12 bg-yellow-100 text-yellow-600 rounded-xl flex items-center justify-center mb-4">
                <FaMoneyBill className="text-2xl" />
              </div>
              <h3 className="text-lg font-semibold text-gray-800">Payment Tracking</h3>
              <p className="mt-2 text-gray-500 text-sm">
                Track all payments, generate invoices, and know exactly 
                who has paid and who is overdue at a glance.
              </p>
            </div>
            <div className="bg-white rounded-2xl p-6 shadow-sm border border-gray-100 hover:shadow-md transition-shadow">
              <div className="w-12 h-12 bg-red-100 text-red-600 rounded-xl flex items-center justify-center mb-4">
                <FaShieldAlt className="text-2xl" />
              </div>
              <h3 className="text-lg font-semibold text-gray-800">Secure & Reliable</h3>
              <p className="mt-2 text-gray-500 text-sm">
                Your data is safe with us. Role-based access ensures only 
                authorized users can access sensitive information.
              </p>
            </div>
            <div className="bg-white rounded-2xl p-6 shadow-sm border border-gray-100 hover:shadow-md transition-shadow">
              <div className="w-12 h-12 bg-indigo-100 text-indigo-600 rounded-xl flex items-center justify-center mb-4">
                <FaHeadset className="text-2xl" />
              </div>
              <h3 className="text-lg font-semibold text-gray-800">24/7 Support</h3>
              <p className="mt-2 text-gray-500 text-sm">
                We're here to help. Our team is available to assist you 
                with any questions or issues you might encounter.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Stats Section */}
      <section className="py-20 px-4">
        <div className="max-w-7xl mx-auto">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-8 text-center">
            <div>
              <p className="text-4xl font-bold text-blue-500">100+</p>
              <p className="text-gray-500 mt-1">Spaces Managed</p>
            </div>
            <div>
              <p className="text-4xl font-bold text-green-500">50+</p>
              <p className="text-gray-500 mt-1">Active Tenants</p>
            </div>
            <div>
              <p className="text-4xl font-bold text-purple-500">200+</p>
              <p className="text-gray-500 mt-1">Leases Created</p>
            </div>
            <div>
              <p className="text-4xl font-bold text-yellow-500">95%</p>
              <p className="text-gray-500 mt-1">Satisfaction Rate</p>
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-20 bg-blue-500 px-4">
        <div className="max-w-4xl mx-auto text-center text-white">
          <h2 className="text-3xl md:text-4xl font-bold">
            Ready to Simplify Your Rental Management?
          </h2>
          <p className="mt-4 text-blue-100 text-lg">
            Join hundreds of building managers who are saving time and 
            reducing manual work with RMS.
          </p>
          <Link 
            to="/dashboard" 
            className="inline-flex items-center gap-2 mt-8 px-8 py-3 bg-white text-blue-500 font-semibold rounded-lg hover:bg-gray-50 transition-colors"
          >
            Get Started Now <FaArrowRight />
          </Link>
        </div>
      </section>

      {/* Footer */}
      <footer className="py-12 bg-gray-900 text-gray-400 px-4">
        <div className="max-w-7xl mx-auto">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
            <div>
              <span className="text-xl font-bold text-white">🏢 RMS</span>
              <p className="mt-2 text-sm">
                The all-in-one platform for commercial building rental management.
              </p>
            </div>
            <div>
              <h4 className="text-white font-semibold mb-4">Product</h4>
              <ul className="space-y-2 text-sm">
                <li><Link to="/dashboard" className="hover:text-white transition-colors">Dashboard</Link></li>
                <li><Link to="/spaces" className="hover:text-white transition-colors">Spaces</Link></li>
                <li><Link to="/tenants" className="hover:text-white transition-colors">Tenants</Link></li>
                <li><Link to="/leases" className="hover:text-white transition-colors">Leases</Link></li>
              </ul>
            </div>
            <div>
              <h4 className="text-white font-semibold mb-4">Resources</h4>
              <ul className="space-y-2 text-sm">
                <li><a href="#" className="hover:text-white transition-colors">Documentation</a></li>
                <li><a href="#" className="hover:text-white transition-colors">API</a></li>
                <li><a href="#" className="hover:text-white transition-colors">Support</a></li>
              </ul>
            </div>
            <div>
              <h4 className="text-white font-semibold mb-4">Company</h4>
              <ul className="space-y-2 text-sm">
                <li><a href="#" className="hover:text-white transition-colors">About</a></li>
                <li><a href="#" className="hover:text-white transition-colors">Blog</a></li>
                <li><a href="#" className="hover:text-white transition-colors">Contact</a></li>
              </ul>
            </div>
          </div>
          <div className="border-t border-gray-800 mt-8 pt-6 text-sm text-center">
            © 2026 RMS. All rights reserved.
          </div>
        </div>
      </footer>
    </div>
  )
}

export default LandingPage