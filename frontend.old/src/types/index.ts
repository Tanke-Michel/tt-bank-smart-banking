// ============================================================
// All TypeScript types derived from backend DTO shapes
// ============================================================

// --- Auth ---
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  userId: number;
  fullName: string;
  email: string;
  role: 'USER' | 'MERCHANT' | 'ADMIN';
  expiresIn: number;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  phoneNumber: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

// --- Wallet ---
export interface WalletResponse {
  id: number;
  userId: number;
  walletNumber: string;
  ownerName: string;
  email: string;
  phoneNumber: string;
  balance: number;
  currency: 'XAF' | 'USD' | 'EUR' | 'GBP';
  status: 'ACTIVE' | 'SUSPENDED' | 'CLOSED';
  createdAt: string;
}

export interface WalletTransactionResponse {
  id: number;
  type: 'DEPOSIT' | 'WITHDRAWAL' | 'CREDIT' | 'DEBIT';
  amount: number;
  balanceBefore: number;
  balanceAfter: number;
  currency: string;
  referenceCode: string;
  description: string;
  createdAt: string;
}

// --- Transaction ---
export interface TransactionResponse {
  id: number;
  referenceCode: string;
  type: 'TRANSFER' | 'PAYMENT';
  status: 'PENDING' | 'COMPLETED' | 'FAILED' | 'REVERSED';
  senderWalletNumber: string;
  senderEmail: string;
  receiverWalletNumber: string;
  receiverEmail: string;
  amount: number;
  currency: string;
  description: string;
  createdAt: string;
  completedAt: string;
}

// --- Merchant ---
export interface MerchantResponse {
  id: number;
  merchantCode: string;
  ownerUserId: number;
  ownerEmail: string;
  businessName: string;
  businessEmail: string;
  businessPhone: string;
  businessAddress: string;
  businessCategory: string;
  description: string;
  walletNumber: string;
  qrCodeBase64: string;
  status: 'PENDING' | 'ACTIVE' | 'SUSPENDED' | 'REJECTED';
  statusReason: string;
  createdAt: string;
  approvedAt: string;
}

export interface MerchantPaymentResponse {
  id: number;
  referenceCode: string;
  merchantId: number;
  merchantCode: string;
  businessName: string;
  customerUserId: number;
  customerEmail: string;
  customerWalletNumber: string;
  merchantWalletNumber: string;
  amount: number;
  currency: string;
  description: string;
  status: 'PENDING' | 'COMPLETED' | 'FAILED';
  failureReason: string;
  createdAt: string;
  completedAt: string;
}

export interface MerchantDashboardResponse {
  merchantCode: string;
  businessName: string;
  walletNumber: string;
  qrCodeBase64: string;
  todayRevenue: number;
  monthRevenue: number;
  totalRevenue: number;
  todayTransactionCount: number;
  monthTransactionCount: number;
  totalTransactionCount: number;
}

// --- Savings ---
export interface GroupResponse {
  id: number;
  name: string;
  description: string;
  creatorUserId: number;
  creatorEmail: string;
  contributionAmount: number;
  currency: string;
  payoutCycle: 'WEEKLY' | 'BIWEEKLY' | 'MONTHLY';
  maxMembers: number;
  currentMemberCount: number;
  currentRound: number;
  totalRounds: number;
  status: 'FORMING' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
  startDate: string;
  createdAt: string;
}

export interface MemberResponse {
  id: number;
  groupId: number;
  userId: number;
  userEmail: string;
  fullName: string;
  walletNumber: string;
  payoutOrder: number;
  status: 'ACTIVE' | 'DEFAULTED' | 'REMOVED';
  hasReceivedPayout: boolean;
  joinedAt: string;
}

export interface ContributionResponse {
  id: number;
  groupId: number;
  groupName: string;
  memberId: number;
  memberEmail: string;
  roundNumber: number;
  amount: number;
  currency: string;
  walletNumber: string;
  referenceCode: string;
  status: 'PENDING' | 'PAID' | 'FAILED' | 'WAIVED';
  failureReason: string;
  createdAt: string;
  paidAt: string;
}

export interface PayoutResponse {
  id: number;
  groupId: number;
  groupName: string;
  recipientMemberId: number;
  recipientEmail: string;
  roundNumber: number;
  amount: number;
  currency: string;
  recipientWalletNumber: string;
  referenceCode: string;
  status: 'SCHEDULED' | 'COMPLETED' | 'FAILED';
  failureReason: string;
  createdAt: string;
  completedAt: string;
}

// --- Audit ---
export interface AuditEventResponse {
  id: number;
  eventDomain: 'WALLET' | 'TRANSACTION' | 'MERCHANT' | 'SAVINGS';
  eventType: string;
  referenceCode: string;
  actorEmail: string;
  summary: string;
  rawPayload: string;
  eventTimestamp: string;
  receivedAt: string;
}

export interface AuditStatsResponse {
  totalEvents: number;
  todayEvents: number;
  byDomain: Record<string, number>;
  todayByType: Record<string, number>;
}

// --- Common ---
export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface ErrorResponse {
  status: number;
  error: string;
  message: string;
  timestamp: string;
  fieldErrors?: Record<string, string>;
}
