// Account types
export interface Account {
  id: string;
  type: string;
  identifier: string;
  balance: Amount;
  transactions?: Transaction[];
}

export interface Amount {
  value: number;
  currencyCode: string;
}

export interface AccountRequest {
  type: string;
  identifier: string;
  initialBalance: Amount;
}

// Simple account model for transfer gateway
export interface TransferAccount {
  type: string;
  identifier: string;
}

export interface AccountResponse {
  id: string;
  type: string;
  identifier: string;
  balance: Amount;
  createdAt: string;
  transactions?: TransactionResponse[];
}

// Transaction types
export enum TransactionType {
  CREDIT = 'CREDIT',
  DEBIT = 'DEBIT',
  DUMMY = 'DUMMY'
}

export interface Transaction {
  id: string;
  transferId: string;
  accountId: string;
  amount: Amount;
  valuationTimestamp: string;
  purpose: string;
  type: TransactionType;
  createdAt: string;
}

export interface TransactionRequest {
  id?: string;
  accountId: string;
  transferId: string;
  amount: Amount;
  valuationTimestamp: string;
  purpose: string;
  type: TransactionType;
}

export interface TransactionResponse {
  id: string;
  transferId: string;
  accountId: string;
  amount: Amount;
  valuationTimestamp: string;
  purpose: string;
  type: TransactionType;
  sourceAccount?: string;
  destinationAccount?: string;
  createdAt: string;
}

// Transfer types
export interface Transfer {
  id: string;
  sourceAccount: TransferAccount;
  targetAccount: TransferAccount;
  amount: Amount;
  purpose: string;
}

export interface TransferRequest {
  sourceAccount: TransferAccount;
  targetAccount: TransferAccount;
  amount: Amount;
  valuationTimestamp?: string;
  purpose: string;
}

// Notification types
export enum NotificationEventType {
  BALANCE_UPDATE = 'BALANCE_UPDATE',
  ACCOUNT_CREATED = 'ACCOUNT_CREATED',
  ACCOUNT_DELETED = 'ACCOUNT_DELETED'
}

export interface NotificationSubscription {
  id: string;
  accountId: string;
  eventType: NotificationEventType;
  active: boolean;
  createdAt: string;
}

export interface NotificationSubscriptionRequest {
  accountId: string;
  eventType: NotificationEventType;
}

export interface NotificationSubscriptionResponse {
  id: string;
  accountId: string;
  eventType: NotificationEventType;
  active: boolean;
  createdAt: string;
}

export interface Notification {
  id: string;
  accountId: string;
  eventType: NotificationEventType;
  message: string;
  read: boolean;
  createdAt: string;
}

export interface NotificationResponse {
  id: string;
  accountId: string;
  eventType: NotificationEventType;
  message: string;
  read: boolean;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface UnreadNotificationCount {
  count: number;
}
