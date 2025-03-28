import axios from 'axios';
import { TransactionRequest, TransactionResponse } from '../types';

const API_URL = '/api/transactions';

export const processTransaction = async (transactionRequest: TransactionRequest): Promise<TransactionResponse> => {
  const response = await axios.post<TransactionResponse>(API_URL, transactionRequest);
  return response.data;
};

export const getTransaction = async (transactionId: string): Promise<TransactionResponse> => {
  const response = await axios.get<TransactionResponse>(`${API_URL}/${transactionId}`);
  return response.data;
};

export const getTransactionsByAccount = async (
  accountId: string, 
  page: number = 0, 
  size: number = 10
): Promise<{ content: TransactionResponse[], totalElements: number, totalPages: number }> => {
  const response = await axios.get<{ content: TransactionResponse[], totalElements: number, totalPages: number }>(
    `${API_URL}/by-account/${accountId}`,
    {
      params: { page, size }
    }
  );
  return response.data;
};