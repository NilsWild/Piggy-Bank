import axios from 'axios';
import { TransferRequest } from '../types';

const API_URL = '/api/transfers';

export const processTransfer = async (transferRequest: TransferRequest): Promise<void> => {
  await axios.post(API_URL, transferRequest);
};