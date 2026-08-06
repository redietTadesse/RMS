export const defaultSettings = {
  penaltyRate: 20,        // % applied to rent when paid after the payment window
  paymentWindowEnd: 10,   // rent must be paid between the 1st and 10th
}

export const mockTenants = [
  { id: 1, name: "ABC Trading", type: "Business", contact: "John", phone: "0912345678", email: "john@abc.com", tin: "0011223344", hasIdCard: true, hasBusinessLicense: true },
  { id: 2, name: "XYZ Cafe", type: "Business", contact: "Sarah", phone: "0923456789", email: "sarah@xyz.com", tin: "0055667788", hasIdCard: true, hasBusinessLicense: true },
  { id: 3, name: "Tech Hub", type: "Organization", contact: "Mike", phone: "0934567890", email: "mike@tech.com", tin: "0099887766", hasIdCard: true, hasBusinessLicense: false },
]

export const mockSpaces = [
  { id: 1, unitNumber: "A-101", floor: "1", category: "Office", size: 45, status: "Vacant" },
  { id: 2, unitNumber: "A-102", floor: "1", category: "Shop", size: 60, status: "Occupied" },
  { id: 3, unitNumber: "B-201", floor: "2", category: "Office", size: 30, status: "Vacant" },
  { id: 4, unitNumber: "B-202", floor: "2", category: "Cafe", size: 80, status: "Occupied" },
]

// SRS 4.3: leases track prepayment (minimum 3 months)
export const mockLeases = [
  { id: 1, tenant: "ABC Trading", space: "A-102", startDate: "2026-01-01", endDate: "2026-12-31", monthlyRent: 15000, deposit: 30000, prepaymentMonths: 3, dueDay: 1, status: "Active" },
  { id: 2, tenant: "XYZ Cafe", space: "B-202", startDate: "2026-03-01", endDate: "2026-12-31", monthlyRent: 8000, deposit: 16000, prepaymentMonths: 6, dueDay: 1, status: "Active" },
]

// SRS 4.4: invoice = number, lease link, rent, penalty (tracked separately),
// total, issue date, due date, status (Pending | Paid | Overdue)
export const mockInvoices = [
  {
    id: 1,
    invoiceNumber: "INV-2026-0001",
    leaseId: 1,
    tenant: "ABC Trading",
    rentAmount: 15000,
    penaltyAmount: 0,
    totalDue: 15000,
    issueDate: "2026-07-01",
    dueDate: "2026-07-10",
    status: "Pending",
    paymentMethod: null,
    paymentDate: null,
    receiptName: null,
  },
  {
    id: 2,
    invoiceNumber: "INV-2026-0002",
    leaseId: 2,
    tenant: "XYZ Cafe",
    rentAmount: 8000,
    penaltyAmount: 0,
    totalDue: 8000,
    issueDate: "2026-07-01",
    dueDate: "2026-07-10",
    status: "Paid",
    paymentMethod: "Bank Transfer",
    paymentDate: "2026-07-05",
    receiptName: "xyz-july-receipt.png",
  },
  {
    id: 3,
    invoiceNumber: "INV-2026-0003",
    leaseId: 1,
    tenant: "ABC Trading",
    rentAmount: 15000,
    penaltyAmount: 3000, // paid on the 14th → 20% penalty, tracked separately
    totalDue: 18000,
    issueDate: "2026-06-01",
    dueDate: "2026-06-10",
    status: "Paid",
    paymentMethod: "Cash",
    paymentDate: "2026-06-14",
    receiptName: "abc-june-receipt.jpg",
  },
  {
    id: 4,
    invoiceNumber: "INV-2026-0004",
    leaseId: 2,
    tenant: "XYZ Cafe",
    rentAmount: 8000,
    penaltyAmount: 1600,
    totalDue: 9600,
    issueDate: "2026-05-01",
    dueDate: "2026-05-10",
    status: "Overdue",
    paymentMethod: null,
    paymentDate: null,
    receiptName: null,
  },
]
