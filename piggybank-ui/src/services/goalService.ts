import axios from 'axios';
import { 
  Goal, 
  SavingsGoal, 
  SpendingLimitGoal, 
  CreateSavingsGoalRequest, 
  CreateSpendingLimitGoalRequest, 
  GoalStatus 
} from '../types';

const GOALS_API_URL = '/api/goals';

// Goal functions
export const getGoalById = async (id: string): Promise<Goal> => {
  const response = await axios.get<Goal>(`${GOALS_API_URL}/${id}`);
  return response.data;
};

export const getGoalsByAccountId = async (accountId: string): Promise<Goal[]> => {
  const response = await axios.get<Goal[]>(`${GOALS_API_URL}/account/${accountId}`);
  return response.data;
};

export const getGoalsByStatus = async (status: GoalStatus): Promise<Goal[]> => {
  const response = await axios.get<Goal[]>(`${GOALS_API_URL}/status/${status}`);
  return response.data;
};

// Savings goal functions
export const createSavingsGoal = async (
  request: CreateSavingsGoalRequest
): Promise<SavingsGoal> => {
  // Transform the request to match the backend's expected format
  const transformedRequest = {
    name: request.name,
    description: request.description,
    startDate: request.startDate,
    endDate: request.endDate,
    accountId: request.accountId,
    targetAmount: request.targetAmount.value,
    currencyCode: request.targetAmount.currencyCode
  };

  const response = await axios.post<SavingsGoal>(`${GOALS_API_URL}/savings`, transformedRequest);
  return response.data;
};

export const getSavingsGoalById = async (id: string): Promise<SavingsGoal> => {
  const response = await axios.get<any>(`${GOALS_API_URL}/savings/${id}`);
  // Transform the response to match the frontend's expected format
  const data = response.data;
  return {
    ...data,
    targetAmount: {
      value: data.targetAmount,
      currencyCode: data.currencyCode
    },
    currentAmount: {
      value: data.currentAmount,
      currencyCode: data.currencyCode
    }
  };
};

export const getSavingsGoalsByAccountId = async (accountId: string): Promise<SavingsGoal[]> => {
  const response = await axios.get<any[]>(`${GOALS_API_URL}/savings/account/${accountId}`);
  // Transform the response to match the frontend's expected format
  return response.data.map(data => ({
    ...data,
    targetAmount: {
      value: data.targetAmount,
      currencyCode: data.currencyCode
    },
    currentAmount: {
      value: data.currentAmount,
      currencyCode: data.currencyCode
    }
  }));
};

// Spending limit goal functions
export const createSpendingLimitGoal = async (
  request: CreateSpendingLimitGoalRequest
): Promise<SpendingLimitGoal> => {
  // Transform the request to match the backend's expected format
  const transformedRequest = {
    name: request.name,
    description: request.description,
    startDate: request.startDate,
    endDate: request.endDate,
    accountId: request.accountId,
    limit: request.limit.value,
    currencyCode: request.limit.currencyCode,
    category: request.category
  };

  const response = await axios.post<SpendingLimitGoal>(`${GOALS_API_URL}/spending-limit`, transformedRequest);
  return response.data;
};

export const getSpendingLimitGoalById = async (id: string): Promise<SpendingLimitGoal> => {
  const response = await axios.get<any>(`${GOALS_API_URL}/spending-limit/${id}`);
  // Transform the response to match the frontend's expected format
  const data = response.data;
  return {
    ...data,
    limit: {
      value: data.limit,
      currencyCode: data.currencyCode
    },
    currentSpending: {
      value: data.currentSpending,
      currencyCode: data.currencyCode
    }
  };
};

export const getSpendingLimitGoalsByAccountId = async (accountId: string): Promise<SpendingLimitGoal[]> => {
  const response = await axios.get<any[]>(`${GOALS_API_URL}/spending-limit/account/${accountId}`);
  // Transform the response to match the frontend's expected format
  return response.data.map(data => ({
    ...data,
    limit: {
      value: data.limit,
      currencyCode: data.currencyCode
    },
    currentSpending: {
      value: data.currentSpending,
      currencyCode: data.currencyCode
    }
  }));
};

export const getSpendingLimitGoalsByCategory = async (category: string): Promise<SpendingLimitGoal[]> => {
  const response = await axios.get<any[]>(`${GOALS_API_URL}/spending-limit/category/${category}`);
  // Transform the response to match the frontend's expected format
  return response.data.map(data => ({
    ...data,
    limit: {
      value: data.limit,
      currencyCode: data.currencyCode
    },
    currentSpending: {
      value: data.currentSpending,
      currencyCode: data.currencyCode
    }
  }));
};

// Common goal functions
export const updateGoalStatus = async (id: string, status: GoalStatus): Promise<Goal> => {
  const response = await axios.put<Goal>(`${GOALS_API_URL}/${id}/status/${status}`);
  return response.data;
};

export const deleteGoal = async (id: string): Promise<void> => {
  await axios.delete(`${GOALS_API_URL}/${id}`);
};
