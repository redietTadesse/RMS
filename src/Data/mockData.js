export const mockTenants = [
  { id: 1, name: "ABC Trading", type: "Business", contact: "John", phone: "0912345678", email: "john@abc.com" },
  { id: 2, name: "XYZ Cafe", type: "Business", contact: "Sarah", phone: "0923456789", email: "sarah@xyz.com" },
  { id: 3, name: "Tech Hub", type: "Organization", contact: "Mike", phone: "0934567890", email: "mike@tech.com" },
]

export const mockSpaces = [
  { id: 1, unitNumber: "A-101", floor: "1", category: "Office", size: 45, status: "Vacant" },
  { id: 2, unitNumber: "A-102", floor: "1", category: "Shop", size: 60, status: "Occupied" },
  { id: 3, unitNumber: "B-201", floor: "2", category: "Office", size: 30, status: "Vacant" },
  { id: 4, unitNumber: "B-202", floor: "2", category: "Cafe", size: 80, status: "Occupied" },
]

export const mockLeases = [
  { 
    id: 1, 
    tenant: "ABC Trading", 
    space: "A-102", 
    startDate: "2026-01-01", 
    endDate: "2026-12-31", 
    monthlyRent: 15000, 
    deposit: 30000,
    dueDay: 1,
    status: "Active" 
  },
  { 
    id: 2, 
    tenant: "XYZ Cafe", 
    space: "B-202", 
    startDate: "2026-03-01", 
    endDate: "2026-12-31", 
    monthlyRent: 8000, 
    deposit: 16000,
    dueDay: 5,
    status: "Active" 
  },
]

export const mockInvoices = [
  { 
    id: 1, 
    tenant: "ABC Trading", 
    amount: 15000, 
    dueDate: "2026-07-31", 
    status: "Pending" 
  },
  { 
    id: 2, 
    tenant: "XYZ Cafe", 
    amount: 8000, 
    dueDate: "2026-07-31", 
    status: "Paid" 
  },
  { 
    id: 3, 
    tenant: "Tech Hub", 
    amount: 12000, 
    dueDate: "2026-08-15", 
    status: "Pending" 
  },
  { 
    id: 4, 
    tenant: "ABC Trading", 
    amount: 15000, 
    dueDate: "2026-06-30", 
    status: "Paid" 
  },
]