import axios from 'axios';
import { Account, AccountRequest, AccountResponse } from '../types';

const API_URL = '/api/accounts';

export const getAllAccounts = async (): Promise<AccountResponse[]> => {
  const response = await axios.get<AccountResponse[]>(API_URL);
  return response.data;
};

export const getAccount = async (accountId: string, includeTransactions: boolean = false): Promise<AccountResponse> => {
  const response = await axios.get<AccountResponse>(`${API_URL}/${accountId}`, {
    params: { includeTransactions }
  });
  return response.data;
};

export const getAccountByTypeAndIdentifier = async (
  type: string, 
  identifier: string, 
  includeTransactions: boolean = false
): Promise<AccountResponse> => {
  const response = await axios.get<AccountResponse>(`${API_URL}/by-type-and-identifier`, {
    params: { type, identifier, includeTransactions }
  });
  return response.data;
};

export const createAccount = async (accountRequest: AccountRequest): Promise<AccountResponse> => {
  const response = await axios.post<AccountResponse>(API_URL, accountRequest);
  return response.data;
};

export const deleteAccount = async (accountId: string): Promise<void> => {
  await axios.delete(`${API_URL}/${accountId}`);
};

// Account Twin Service endpoints
const ACCOUNT_TWIN_SERVICE_API_URL = '/api/accounts';

export const getAllMonitoredAccounts = async (): Promise<Account[]> => {
  const response = await axios.get<Account[]>(ACCOUNT_TWIN_SERVICE_API_URL);
  return response.data;
};

export const addMonitoredAccount = async (accountRequest: AccountRequest): Promise<void> => {
  // The Account Twin Service is now responsible for monitoring accounts
  // This is the same endpoint as createAccount, but we keep the function separate
  // for clarity and potential future changes
  await axios.post(ACCOUNT_TWIN_SERVICE_API_URL, accountRequest);
};

export const removeMonitoredAccount = async (accountRequest: AccountRequest): Promise<void> => {
  await axios.delete(ACCOUNT_TWIN_SERVICE_API_URL, { data: accountRequest });
};
