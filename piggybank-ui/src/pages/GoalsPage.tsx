import { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Button,
  Card,
  CardContent,
  CardActions,
  Grid,
  TextField,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Snackbar,
  Alert,
  CircularProgress,
  FormControl,
  FormControlLabel,
  InputLabel,
  Select,
  MenuItem,
  Tabs,
  Tab,
  LinearProgress,
  IconButton,
  Tooltip,
  FormHelperText,
  Divider,
  Switch
} from '@mui/material';
// Removed date picker imports to simplify implementation
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import CancelIcon from '@mui/icons-material/Cancel';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorIcon from '@mui/icons-material/Error';
import SavingsIcon from '@mui/icons-material/Savings';
import MoneyOffIcon from '@mui/icons-material/MoneyOff';
import NotificationsIcon from '@mui/icons-material/Notifications';
import NotificationsOffIcon from '@mui/icons-material/NotificationsOff';
import NotificationsActiveIcon from '@mui/icons-material/NotificationsActive';
import { 
  Goal, 
  GoalStatus, 
  GoalType, 
  SavingsGoal, 
  SpendingLimitGoal, 
  CreateSavingsGoalRequest, 
  CreateSpendingLimitGoalRequest,
  Amount,
  NotificationEventType,
  NotificationSubscriptionRequest,
  NotificationSubscription
} from '../types';
import * as goalService from '../services/goalService';
import * as accountService from '../services/accountService';
import * as notificationService from '../services/notificationService';

// Interface for the tab panel props
interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

// Tab panel component
const TabPanel = (props: TabPanelProps) => {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`goal-tabpanel-${index}`}
      aria-labelledby={`goal-tab-${index}`}
      {...other}
    >
      {value === index && (
        <Box sx={{ p: 3 }}>
          {children}
        </Box>
      )}
    </div>
  );
};

// Function to get props for the tabs
const a11yProps = (index: number) => {
  return {
    id: `goal-tab-${index}`,
    'aria-controls': `goal-tabpanel-${index}`,
  };
};

// Main component
const GoalsPage = () => {
  // State for goals
  const [activeGoals, setActiveGoals] = useState<Goal[]>([]);
  const [achievedGoals, setAchievedGoals] = useState<Goal[]>([]);
  const [failedGoals, setFailedGoals] = useState<Goal[]>([]);
  const [cancelledGoals, setCancelledGoals] = useState<Goal[]>([]);

  // State for UI
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [tabValue, setTabValue] = useState(0);

  // State for dialogs
  const [openCreateDialog, setOpenCreateDialog] = useState(false);
  const [goalType, setGoalType] = useState<GoalType | ''>('');
  const [openDeleteDialog, setOpenDeleteDialog] = useState(false);
  const [goalToDelete, setGoalToDelete] = useState<string | null>(null);
  const [openDetailsDialog, setOpenDetailsDialog] = useState(false);
  const [selectedGoal, setSelectedGoal] = useState<Goal | null>(null);
  const [selectedGoalDetails, setSelectedGoalDetails] = useState<SavingsGoal | SpendingLimitGoal | null>(null);

  // State for snackbar
  const [openSnackbar, setOpenSnackbar] = useState(false);
  const [snackbarMessage, setSnackbarMessage] = useState('');
  const [snackbarSeverity, setSnackbarSeverity] = useState<'success' | 'error'>('success');

  // State for accounts
  const [accounts, setAccounts] = useState<{ id: string, name: string }[]>([]);

  // State for new savings goal
  const [newSavingsGoal, setNewSavingsGoal] = useState<CreateSavingsGoalRequest>({
    name: '',
    description: '',
    startDate: new Date().toISOString(),
    endDate: new Date(new Date().setMonth(new Date().getMonth() + 3)).toISOString(),
    accountId: '',
    targetAmount: { value: 0, currencyCode: 'EUR' }
  });

  // State for new spending limit goal
  const [newSpendingLimitGoal, setNewSpendingLimitGoal] = useState<CreateSpendingLimitGoalRequest>({
    name: '',
    description: '',
    startDate: new Date().toISOString(),
    endDate: new Date(new Date().setMonth(new Date().getMonth() + 3)).toISOString(),
    accountId: '',
    limit: { value: 0, currencyCode: 'EUR' },
    category: ''
  });

  // State for form validation
  const [formErrors, setFormErrors] = useState<{
    name?: string;
    accountId?: string;
    targetAmount?: string;
    limit?: string;
    category?: string;
    dates?: string;
  }>({});

  // Fetch goals and accounts when component mounts
  useEffect(() => {
    fetchGoals();
    fetchAccounts();
  }, []);

  // Function to fetch goals
  const fetchGoals = async () => {
    try {
      setLoading(true);

      // Fetch goals for each status
      const active = await goalService.getGoalsByStatus(GoalStatus.ACTIVE);
      const achieved = await goalService.getGoalsByStatus(GoalStatus.ACHIEVED);
      const failed = await goalService.getGoalsByStatus(GoalStatus.FAILED);
      const cancelled = await goalService.getGoalsByStatus(GoalStatus.CANCELLED);

      // Ensure all goals arrays are properly initialized
      setActiveGoals(Array.isArray(active) ? active : []);
      setAchievedGoals(Array.isArray(achieved) ? achieved : []);
      setFailedGoals(Array.isArray(failed) ? failed : []);
      setCancelledGoals(Array.isArray(cancelled) ? cancelled : []);
      setError(null);
    } catch (err) {
      // Error handling for failed goal fetching
      setError('Failed to fetch goals. Please try again later.');
    } finally {
      setLoading(false);
    }
  };

  // Function to fetch accounts
  const fetchAccounts = async () => {
    try {
      const data = await accountService.getAllAccounts();
      setAccounts(data.map(account => ({
        id: account.id,
        name: `${account.type}: ${account.identifier}`
      })));
    } catch (err) {
      // Error handling for failed account fetching
      // Don't set error state here, as it would override the goals error
    }
  };

  // Function to handle tab change
  const handleTabChange = (_event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue);
  };

  // Function to handle snackbar close
  const handleCloseSnackbar = () => {
    setOpenSnackbar(false);
  };

  // Function to open create dialog
  const handleOpenCreateDialog = () => {
    setOpenCreateDialog(true);
    setGoalType('');
    resetFormErrors();
  };

  // Function to close create dialog
  const handleCloseCreateDialog = () => {
    setOpenCreateDialog(false);
    setGoalType('');
    resetFormErrors();
  };

  // Function to reset form errors
  const resetFormErrors = () => {
    setFormErrors({});
  };

  // Function to handle goal type change
  const handleGoalTypeChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setGoalType(event.target.value as GoalType);
  };

  // Function to handle savings goal input change
  const handleSavingsGoalInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement> | { target: { name?: string; value: unknown } }) => {
    const { name, value } = e.target;

    if (name) {
      if (name === 'targetAmountValue') {
        setNewSavingsGoal(prev => ({
          ...prev,
          targetAmount: {
            ...prev.targetAmount,
            value: parseFloat(value as string) || 0
          }
        }));
      } else if (name === 'targetAmountCurrencyCode') {
        setNewSavingsGoal(prev => ({
          ...prev,
          targetAmount: {
            ...prev.targetAmount,
            currencyCode: value as string
          }
        }));
      } else {
        setNewSavingsGoal(prev => ({ ...prev, [name]: value }));
      }
    }
  };

  // Function to handle spending limit goal input change
  const handleSpendingLimitGoalInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement> | { target: { name?: string; value: unknown } }) => {
    const { name, value } = e.target;

    if (name) {
      if (name === 'limitValue') {
        setNewSpendingLimitGoal(prev => ({
          ...prev,
          limit: {
            ...prev.limit,
            value: parseFloat(value as string) || 0
          }
        }));
      } else if (name === 'limitCurrencyCode') {
        setNewSpendingLimitGoal(prev => ({
          ...prev,
          limit: {
            ...prev.limit,
            currencyCode: value as string
          }
        }));
      } else {
        setNewSpendingLimitGoal(prev => ({ ...prev, [name]: value }));
      }
    }
  };

  // Function to handle date change for savings goal
  const handleSavingsGoalDateChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    if (value) {
      try {
        const date = new Date(value);
        setNewSavingsGoal(prev => ({ ...prev, [name]: date.toISOString() }));
      } catch (err) {
        // Error handling for invalid date format
      }
    }
  };

  // Function to handle date change for spending limit goal
  const handleSpendingLimitGoalDateChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    if (value) {
      try {
        const date = new Date(value);
        setNewSpendingLimitGoal(prev => ({ ...prev, [name]: date.toISOString() }));
      } catch (err) {
        // Error handling for invalid date format
      }
    }
  };

  // Function to validate form
  const validateForm = (): boolean => {
    const errors: {
      name?: string;
      accountId?: string;
      targetAmount?: string;
      limit?: string;
      category?: string;
      dates?: string;
    } = {};

    if (goalType === GoalType.SAVINGS) {
      if (!newSavingsGoal.name) {
        errors.name = 'Name is required';
      }
      if (!newSavingsGoal.accountId) {
        errors.accountId = 'Account is required';
      }
      if (newSavingsGoal.targetAmount.value <= 0) {
        errors.targetAmount = 'Target amount must be greater than 0';
      }

      const startDate = new Date(newSavingsGoal.startDate);
      const endDate = new Date(newSavingsGoal.endDate);
      if (startDate >= endDate) {
        errors.dates = 'End date must be after start date';
      }
    } else if (goalType === GoalType.SPENDING_LIMIT) {
      if (!newSpendingLimitGoal.name) {
        errors.name = 'Name is required';
      }
      if (!newSpendingLimitGoal.accountId) {
        errors.accountId = 'Account is required';
      }
      if (newSpendingLimitGoal.limit.value <= 0) {
        errors.limit = 'Limit must be greater than 0';
      }
      if (!newSpendingLimitGoal.category) {
        errors.category = 'Category is required';
      }

      const startDate = new Date(newSpendingLimitGoal.startDate);
      const endDate = new Date(newSpendingLimitGoal.endDate);
      if (startDate >= endDate) {
        errors.dates = 'End date must be after start date';
      }
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  // Function to create a goal
  const handleCreateGoal = async () => {
    if (!validateForm()) {
      return;
    }

    try {
      if (goalType === GoalType.SAVINGS) {
        await goalService.createSavingsGoal(newSavingsGoal);
        setSnackbarMessage('Savings goal created successfully!');
      } else if (goalType === GoalType.SPENDING_LIMIT) {
        await goalService.createSpendingLimitGoal(newSpendingLimitGoal);
        setSnackbarMessage('Spending limit goal created successfully!');
      }

      setSnackbarSeverity('success');
      setOpenSnackbar(true);
      handleCloseCreateDialog();
      fetchGoals();
    } catch (err) {
      // Error handling for failed goal creation
      setSnackbarMessage('Failed to create goal. Please try again.');
      setSnackbarSeverity('error');
      setOpenSnackbar(true);
    }
  };

  // Function to open delete dialog
  const handleOpenDeleteDialog = (goalId: string) => {
    setGoalToDelete(goalId);
    setOpenDeleteDialog(true);
  };

  // Function to close delete dialog
  const handleCloseDeleteDialog = () => {
    setOpenDeleteDialog(false);
    setGoalToDelete(null);
  };

  // State for detailed goal information
  const [detailedGoals, setDetailedGoals] = useState<{
    [key: string]: SavingsGoal | SpendingLimitGoal;
  }>({});

  // State for notification subscriptions
  const [goalUpdateSubscribed, setGoalUpdateSubscribed] = useState(false);
  const [goalAchievedSubscribed, setGoalAchievedSubscribed] = useState(false);
  const [goalFailedSubscribed, setGoalFailedSubscribed] = useState(false);
  const [subscriptionIds, setSubscriptionIds] = useState<{
    [key in NotificationEventType]?: string;
  }>({});
  const [subscriptionLoading, setSubscriptionLoading] = useState(false);

  // Function to fetch detailed goal information
  const fetchDetailedGoalInfo = async (goal: Goal) => {
    try {
      if (goal.type === GoalType.SAVINGS) {
        const savingsGoal = await goalService.getSavingsGoalById(goal.id);
        setDetailedGoals(prev => ({
          ...prev,
          [goal.id]: savingsGoal
        }));
      } else if (goal.type === GoalType.SPENDING_LIMIT) {
        const spendingLimitGoal = await goalService.getSpendingLimitGoalById(goal.id);
        setDetailedGoals(prev => ({
          ...prev,
          [goal.id]: spendingLimitGoal
        }));
      }
    } catch (err) {
      console.error('Failed to fetch detailed goal info:', err);
    }
  };

  // Fetch detailed goal information for all goals when they change
  useEffect(() => {
    const allGoals = [...activeGoals, ...achievedGoals, ...failedGoals, ...cancelledGoals];
    const goalsToFetch = allGoals.filter(goal => !detailedGoals[goal.id]);

    // Only fetch if there are goals that need detailed information
    if (goalsToFetch.length > 0) {
      goalsToFetch.forEach(goal => {
        fetchDetailedGoalInfo(goal);
      });
    }
  }, [activeGoals, achievedGoals, failedGoals, cancelledGoals, detailedGoals, fetchDetailedGoalInfo]);

  // Function to render goal cards
  const renderGoalCards = (goals: Goal[]) => {
    if (goals.length === 0) {
      return (
        <Alert severity="info" sx={{ my: 2 }}>No goals found in this category.</Alert>
      );
    }

    return (
      <Grid container spacing={3}>
        {goals.map((goal) => {
          const detailedGoal = detailedGoals[goal.id];

          return (
          <Grid item xs={12} sm={6} md={4} key={goal.id}>
            <Card>
              <CardContent>
                <Typography variant="h6" component="div">
                  {goal.name}
                </Typography>
                {goal.description && (
                  <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                    {goal.description}
                  </Typography>
                )}
                <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                  Type: {goal.type === GoalType.SAVINGS ? 'Savings' : 'Spending Limit'}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Status: {goal.status}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Start Date: {new Date(goal.startDate).toLocaleDateString()}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  End Date: {new Date(goal.endDate).toLocaleDateString()}
                </Typography>

                {/* Display progress information for savings goals */}
                {goal.type === GoalType.SAVINGS && detailedGoal && 'targetAmount' in detailedGoal && 'currentAmount' in detailedGoal && (
                  <Box sx={{ mt: 2 }}>
                    <Typography variant="body2" color="text.secondary">
                      Target: {detailedGoal.targetAmount.value} {detailedGoal.targetAmount.currencyCode}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Current: {detailedGoal.currentAmount.value} {detailedGoal.currentAmount.currencyCode}
                    </Typography>
                    <Box sx={{ mt: 1 }}>
                      <LinearProgress 
                        variant="determinate" 
                        value={Math.min(
                          (detailedGoal.currentAmount.value / detailedGoal.targetAmount.value) * 100, 
                          100
                        )} 
                        sx={{ height: 8, borderRadius: 4 }}
                      />
                      <Typography variant="body2" sx={{ mt: 0.5, fontSize: '0.75rem', textAlign: 'center' }}>
                        {Math.round((detailedGoal.currentAmount.value / detailedGoal.targetAmount.value) * 100)}% Complete
                      </Typography>
                    </Box>
                  </Box>
                )}

                {/* Display progress information for spending limit goals */}
                {goal.type === GoalType.SPENDING_LIMIT && detailedGoal && 'limit' in detailedGoal && 'currentSpending' in detailedGoal && (
                  <Box sx={{ mt: 2 }}>
                    <Typography variant="body2" color="text.secondary">
                      Category: {(detailedGoal as SpendingLimitGoal).category}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Limit: {detailedGoal.limit.value} {detailedGoal.limit.currencyCode}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Current Spending: {detailedGoal.currentSpending.value} {detailedGoal.currentSpending.currencyCode}
                    </Typography>
                    <Box sx={{ mt: 1 }}>
                      <LinearProgress 
                        variant="determinate" 
                        value={Math.min(
                          (detailedGoal.currentSpending.value / detailedGoal.limit.value) * 100, 
                          100
                        )} 
                        color={detailedGoal.currentSpending.value > detailedGoal.limit.value ? "error" : "primary"}
                        sx={{ height: 8, borderRadius: 4 }}
                      />
                      <Typography variant="body2" sx={{ mt: 0.5, fontSize: '0.75rem', textAlign: 'center' }}>
                        {Math.round((detailedGoal.currentSpending.value / detailedGoal.limit.value) * 100)}% of Limit Used
                      </Typography>
                    </Box>
                  </Box>
                )}
              </CardContent>
              <CardActions>
                <Button 
                  size="small" 
                  color="primary"
                  onClick={() => handleViewGoalDetails(goal.id)}
                >
                  View Details
                </Button>
                {goal.status === GoalStatus.ACTIVE && (
                  <Button 
                    size="small" 
                    color="warning"
                    onClick={() => handleCancelGoal(goal.id)}
                  >
                    Cancel
                  </Button>
                )}
                <Button 
                  size="small" 
                  color="error"
                  onClick={() => handleOpenDeleteDialog(goal.id)}
                >
                  Delete
                </Button>
              </CardActions>
            </Card>
          </Grid>
        )})}
      </Grid>
    );
  };

  // Function to fetch subscription status for a goal's account
  const fetchSubscriptionStatus = async (accountId: string) => {
    try {
      setSubscriptionLoading(true);
      const subscriptions = await notificationService.getAccountSubscriptions(accountId);

      // Reset subscription states
      setGoalUpdateSubscribed(false);
      setGoalAchievedSubscribed(false);
      setGoalFailedSubscribed(false);
      const newSubscriptionIds: { [key in NotificationEventType]?: string } = {};

      // Check for active subscriptions
      subscriptions.forEach(sub => {
        if (sub.active) {
          if (sub.eventType === NotificationEventType.GOAL_UPDATE) {
            setGoalUpdateSubscribed(true);
            newSubscriptionIds[NotificationEventType.GOAL_UPDATE] = sub.id;
          } else if (sub.eventType === NotificationEventType.GOAL_ACHIEVED) {
            setGoalAchievedSubscribed(true);
            newSubscriptionIds[NotificationEventType.GOAL_ACHIEVED] = sub.id;
          } else if (sub.eventType === NotificationEventType.GOAL_FAILED) {
            setGoalFailedSubscribed(true);
            newSubscriptionIds[NotificationEventType.GOAL_FAILED] = sub.id;
          }
        }
      });

      setSubscriptionIds(newSubscriptionIds);
    } catch (err) {
      console.error('Failed to fetch subscription status:', err);
      // Don't show an error message to the user, just assume they're not subscribed
    } finally {
      setSubscriptionLoading(false);
    }
  };

  // Function to toggle goal update subscription
  const handleToggleGoalUpdateSubscription = async (accountId: string) => {
    try {
      setSubscriptionLoading(true);

      if (goalUpdateSubscribed && subscriptionIds[NotificationEventType.GOAL_UPDATE]) {
        // Unsubscribe
        await notificationService.deactivateSubscription(subscriptionIds[NotificationEventType.GOAL_UPDATE]);
        setGoalUpdateSubscribed(false);
        setSubscriptionIds(prev => {
          const newIds = { ...prev };
          delete newIds[NotificationEventType.GOAL_UPDATE];
          return newIds;
        });
        setSnackbarMessage('Goal update notifications disabled');
      } else {
        // Subscribe
        const request: NotificationSubscriptionRequest = {
          accountId,
          eventType: NotificationEventType.GOAL_UPDATE
        };

        const response = await notificationService.createSubscription(request);
        setGoalUpdateSubscribed(true);
        setSubscriptionIds(prev => ({
          ...prev,
          [NotificationEventType.GOAL_UPDATE]: response.id
        }));
        setSnackbarMessage('Goal update notifications enabled');
      }

      setSnackbarSeverity('success');
      setOpenSnackbar(true);
    } catch (err) {
      console.error('Failed to toggle goal update subscription:', err);
      setSnackbarMessage('Failed to update notification settings');
      setSnackbarSeverity('error');
      setOpenSnackbar(true);
    } finally {
      setSubscriptionLoading(false);
    }
  };

  // Function to toggle goal achieved subscription
  const handleToggleGoalAchievedSubscription = async (accountId: string) => {
    try {
      setSubscriptionLoading(true);

      if (goalAchievedSubscribed && subscriptionIds[NotificationEventType.GOAL_ACHIEVED]) {
        // Unsubscribe
        await notificationService.deactivateSubscription(subscriptionIds[NotificationEventType.GOAL_ACHIEVED]);
        setGoalAchievedSubscribed(false);
        setSubscriptionIds(prev => {
          const newIds = { ...prev };
          delete newIds[NotificationEventType.GOAL_ACHIEVED];
          return newIds;
        });
        setSnackbarMessage('Goal achievement notifications disabled');
      } else {
        // Subscribe
        const request: NotificationSubscriptionRequest = {
          accountId,
          eventType: NotificationEventType.GOAL_ACHIEVED
        };

        const response = await notificationService.createSubscription(request);
        setGoalAchievedSubscribed(true);
        setSubscriptionIds(prev => ({
          ...prev,
          [NotificationEventType.GOAL_ACHIEVED]: response.id
        }));
        setSnackbarMessage('Goal achievement notifications enabled');
      }

      setSnackbarSeverity('success');
      setOpenSnackbar(true);
    } catch (err) {
      console.error('Failed to toggle goal achieved subscription:', err);
      setSnackbarMessage('Failed to update notification settings');
      setSnackbarSeverity('error');
      setOpenSnackbar(true);
    } finally {
      setSubscriptionLoading(false);
    }
  };

  // Function to toggle goal failed subscription
  const handleToggleGoalFailedSubscription = async (accountId: string) => {
    try {
      setSubscriptionLoading(true);

      if (goalFailedSubscribed && subscriptionIds[NotificationEventType.GOAL_FAILED]) {
        // Unsubscribe
        await notificationService.deactivateSubscription(subscriptionIds[NotificationEventType.GOAL_FAILED]);
        setGoalFailedSubscribed(false);
        setSubscriptionIds(prev => {
          const newIds = { ...prev };
          delete newIds[NotificationEventType.GOAL_FAILED];
          return newIds;
        });
        setSnackbarMessage('Goal failure notifications disabled');
      } else {
        // Subscribe
        const request: NotificationSubscriptionRequest = {
          accountId,
          eventType: NotificationEventType.GOAL_FAILED
        };

        const response = await notificationService.createSubscription(request);
        setGoalFailedSubscribed(true);
        setSubscriptionIds(prev => ({
          ...prev,
          [NotificationEventType.GOAL_FAILED]: response.id
        }));
        setSnackbarMessage('Goal failure notifications enabled');
      }

      setSnackbarSeverity('success');
      setOpenSnackbar(true);
    } catch (err) {
      console.error('Failed to toggle goal failed subscription:', err);
      setSnackbarMessage('Failed to update notification settings');
      setSnackbarSeverity('error');
      setOpenSnackbar(true);
    } finally {
      setSubscriptionLoading(false);
    }
  };

  // Function to view goal details
  const handleViewGoalDetails = async (goalId: string) => {
    try {
      // Find the goal in our existing lists
      const goal = [...activeGoals, ...achievedGoals, ...failedGoals, ...cancelledGoals]
        .find(g => g.id === goalId);

      if (!goal) {
        setSnackbarMessage('Goal not found.');
        setSnackbarSeverity('error');
        setOpenSnackbar(true);
        return;
      }

      setSelectedGoal(goal);

      // Fetch detailed goal information based on type
      if (goal.type === GoalType.SAVINGS) {
        const savingsGoal = await goalService.getSavingsGoalById(goalId);
        setSelectedGoalDetails(savingsGoal);
      } else if (goal.type === GoalType.SPENDING_LIMIT) {
        const spendingLimitGoal = await goalService.getSpendingLimitGoalById(goalId);
        setSelectedGoalDetails(spendingLimitGoal);
      }

      // Fetch subscription status for this goal's account
      await fetchSubscriptionStatus(goal.accountId);

      setOpenDetailsDialog(true);
    } catch (err) {
      console.error('Failed to fetch goal details:', err);
      setSnackbarMessage('Failed to fetch goal details. Please try again.');
      setSnackbarSeverity('error');
      setOpenSnackbar(true);
    }
  };

  // Function to close details dialog
  const handleCloseDetailsDialog = () => {
    setOpenDetailsDialog(false);
    setSelectedGoal(null);
    setSelectedGoalDetails(null);
  };

  const handleCancelGoal = async (goalId: string) => {
    try {
      await goalService.updateGoalStatus(goalId, GoalStatus.CANCELLED);
      setSnackbarMessage('Goal cancelled successfully!');
      setSnackbarSeverity('success');
      setOpenSnackbar(true);
      fetchGoals();
    } catch (err) {
      console.error('Failed to cancel goal:', err);
      setSnackbarMessage('Failed to cancel goal. Please try again.');
      setSnackbarSeverity('error');
      setOpenSnackbar(true);
    }
  };

  const handleDeleteGoal = async (goalId: string) => {
    try {
      await goalService.deleteGoal(goalId);
      setSnackbarMessage('Goal deleted successfully!');
      setSnackbarSeverity('success');
      setOpenSnackbar(true);
      fetchGoals();
    } catch (err) {
      console.error('Failed to delete goal:', err);
      setSnackbarMessage('Failed to delete goal. Please try again.');
      setSnackbarSeverity('error');
      setOpenSnackbar(true);
    }
  };

  // Render loading spinner if loading
  if (loading && activeGoals.length === 0 && achievedGoals.length === 0 && 
      failedGoals.length === 0 && cancelledGoals.length === 0) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', my: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  // Render error message if there's an error
  if (error) {
    return <Alert severity="error" sx={{ my: 2 }}>{error}</Alert>;
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4 }}>
        <Typography variant="h4" component="h1">
          Goals
        </Typography>
        <Button 
          variant="contained" 
          startIcon={<AddIcon />}
          onClick={handleOpenCreateDialog}
        >
          Create Goal
        </Button>
      </Box>

      <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Tabs value={tabValue} onChange={handleTabChange} aria-label="goal tabs">
          <Tab label={`Active (${activeGoals.length})`} {...a11yProps(0)} />
          <Tab label={`Achieved (${achievedGoals.length})`} {...a11yProps(1)} />
          <Tab label={`Failed (${failedGoals.length})`} {...a11yProps(2)} />
          <Tab label={`Cancelled (${cancelledGoals.length})`} {...a11yProps(3)} />
        </Tabs>
      </Box>

      <TabPanel value={tabValue} index={0}>
        {renderGoalCards(activeGoals)}
      </TabPanel>
      <TabPanel value={tabValue} index={1}>
        {renderGoalCards(achievedGoals)}
      </TabPanel>
      <TabPanel value={tabValue} index={2}>
        {renderGoalCards(failedGoals)}
      </TabPanel>
      <TabPanel value={tabValue} index={3}>
        {renderGoalCards(cancelledGoals)}
      </TabPanel>

      {/* Snackbar for notifications */}
      <Snackbar open={openSnackbar} autoHideDuration={6000} onClose={handleCloseSnackbar}>
        <Alert onClose={handleCloseSnackbar} severity={snackbarSeverity} sx={{ width: '100%' }}>
          {snackbarMessage}
        </Alert>
      </Snackbar>

      {/* Delete Goal Dialog */}
      <Dialog open={openDeleteDialog} onClose={handleCloseDeleteDialog}>
        <DialogTitle>Delete Goal</DialogTitle>
        <DialogContent>
          <Typography>Are you sure you want to delete this goal? This action cannot be undone.</Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDeleteDialog}>Cancel</Button>
          <Button onClick={handleDeleteGoal} color="error" variant="contained">Delete</Button>
        </DialogActions>
      </Dialog>

      {/* Goal Details Dialog */}
      <Dialog open={openDetailsDialog} onClose={handleCloseDetailsDialog} maxWidth="md">
        <DialogTitle>
          {selectedGoal?.name} - {selectedGoal?.type === GoalType.SAVINGS ? 'Savings Goal' : 'Spending Limit Goal'}
        </DialogTitle>
        <DialogContent>
          {selectedGoal && selectedGoalDetails && (
            <Box>
              {selectedGoal.description && (
                <Typography variant="body1" sx={{ mb: 2 }}>
                  {selectedGoal.description}
                </Typography>
              )}

              <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                Status: {selectedGoal.status}
              </Typography>

              <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                Account ID: {selectedGoal.accountId}
              </Typography>

              <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                Start Date: {new Date(selectedGoal.startDate).toLocaleDateString()}
              </Typography>

              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                End Date: {new Date(selectedGoal.endDate).toLocaleDateString()}
              </Typography>

              <Divider sx={{ my: 2 }} />

              {selectedGoal.type === GoalType.SAVINGS && 'targetAmount' in selectedGoalDetails && 'currentAmount' in selectedGoalDetails && (
                <Box>
                  <Typography variant="h6" sx={{ mb: 1 }}>
                    Savings Progress
                  </Typography>

                  <Typography variant="body1" sx={{ mb: 1 }}>
                    Target: {selectedGoalDetails.targetAmount.value} {selectedGoalDetails.targetAmount.currencyCode}
                  </Typography>

                  <Typography variant="body1" sx={{ mb: 2 }}>
                    Current: {selectedGoalDetails.currentAmount.value} {selectedGoalDetails.currentAmount.currencyCode}
                  </Typography>

                  <Box sx={{ mb: 2 }}>
                    <LinearProgress 
                      variant="determinate" 
                      value={Math.min(
                        (selectedGoalDetails.currentAmount.value / selectedGoalDetails.targetAmount.value) * 100, 
                        100
                      )} 
                      sx={{ height: 10, borderRadius: 5 }}
                    />
                    <Typography variant="body2" sx={{ mt: 1, textAlign: 'center' }}>
                      {Math.round((selectedGoalDetails.currentAmount.value / selectedGoalDetails.targetAmount.value) * 100)}% Complete
                    </Typography>
                  </Box>
                </Box>
              )}

              {selectedGoal.type === GoalType.SPENDING_LIMIT && 'limit' in selectedGoalDetails && 'currentSpending' in selectedGoalDetails && 'category' in selectedGoalDetails && (
                <Box>
                  <Typography variant="h6" sx={{ mb: 1 }}>
                    Spending Limit Progress
                  </Typography>

                  <Typography variant="body1" sx={{ mb: 1 }}>
                    Category: {selectedGoalDetails.category}
                  </Typography>

                  <Typography variant="body1" sx={{ mb: 1 }}>
                    Limit: {selectedGoalDetails.limit.value} {selectedGoalDetails.limit.currencyCode}
                  </Typography>

                  <Typography variant="body1" sx={{ mb: 2 }}>
                    Current Spending: {selectedGoalDetails.currentSpending.value} {selectedGoalDetails.currentSpending.currencyCode}
                  </Typography>

                  <Box sx={{ mb: 2 }}>
                    <LinearProgress 
                      variant="determinate" 
                      value={Math.min(
                        (selectedGoalDetails.currentSpending.value / selectedGoalDetails.limit.value) * 100, 
                        100
                      )} 
                      color={selectedGoalDetails.currentSpending.value > selectedGoalDetails.limit.value ? "error" : "primary"}
                      sx={{ height: 10, borderRadius: 5 }}
                    />
                    <Typography variant="body2" sx={{ mt: 1, textAlign: 'center' }}>
                      {Math.round((selectedGoalDetails.currentSpending.value / selectedGoalDetails.limit.value) * 100)}% of Limit Used
                    </Typography>
                  </Box>
                </Box>
              )}

              {/* Notification Settings Section */}
              <Divider sx={{ my: 3 }} />
              <Typography variant="h6" sx={{ mb: 2 }}>
                Notification Settings
              </Typography>

              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                {/* Goal Update Notifications */}
                <FormControlLabel
                  control={
                    <Switch
                      checked={goalUpdateSubscribed}
                      onChange={() => handleToggleGoalUpdateSubscription(selectedGoal.accountId)}
                      disabled={subscriptionLoading}
                      color="primary"
                    />
                  }
                  label={
                    <Box sx={{ display: 'flex', alignItems: 'center' }}>
                      {goalUpdateSubscribed ? (
                        <>
                          <NotificationsIcon color="primary" sx={{ mr: 1 }} />
                          <Typography>Goal update notifications enabled</Typography>
                        </>
                      ) : (
                        <>
                          <NotificationsOffIcon sx={{ mr: 1 }} />
                          <Typography>Goal update notifications disabled</Typography>
                        </>
                      )}
                    </Box>
                  }
                />

                {/* Goal Achievement Notifications */}
                <FormControlLabel
                  control={
                    <Switch
                      checked={goalAchievedSubscribed}
                      onChange={() => handleToggleGoalAchievedSubscription(selectedGoal.accountId)}
                      disabled={subscriptionLoading}
                      color="success"
                    />
                  }
                  label={
                    <Box sx={{ display: 'flex', alignItems: 'center' }}>
                      {goalAchievedSubscribed ? (
                        <>
                          <NotificationsActiveIcon color="success" sx={{ mr: 1 }} />
                          <Typography>Goal achievement notifications enabled</Typography>
                        </>
                      ) : (
                        <>
                          <NotificationsOffIcon sx={{ mr: 1 }} />
                          <Typography>Goal achievement notifications disabled</Typography>
                        </>
                      )}
                    </Box>
                  }
                />

                {/* Goal Failure Notifications */}
                <FormControlLabel
                  control={
                    <Switch
                      checked={goalFailedSubscribed}
                      onChange={() => handleToggleGoalFailedSubscription(selectedGoal.accountId)}
                      disabled={subscriptionLoading}
                      color="error"
                    />
                  }
                  label={
                    <Box sx={{ display: 'flex', alignItems: 'center' }}>
                      {goalFailedSubscribed ? (
                        <>
                          <NotificationsActiveIcon color="error" sx={{ mr: 1 }} />
                          <Typography>Goal failure notifications enabled</Typography>
                        </>
                      ) : (
                        <>
                          <NotificationsOffIcon sx={{ mr: 1 }} />
                          <Typography>Goal failure notifications disabled</Typography>
                        </>
                      )}
                    </Box>
                  }
                />

                {subscriptionLoading && (
                  <Box sx={{ display: 'flex', justifyContent: 'center', mt: 1 }}>
                    <CircularProgress size={24} />
                  </Box>
                )}
              </Box>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDetailsDialog}>Close</Button>
          {selectedGoal?.status === GoalStatus.ACTIVE && (
            <Button 
              onClick={() => {
                handleCancelGoal(selectedGoal.id);
                handleCloseDetailsDialog();
              }} 
              color="warning"
            >
              Cancel Goal
            </Button>
          )}
          <Button 
            onClick={() => {
              handleOpenDeleteDialog(selectedGoal?.id || '');
              handleCloseDetailsDialog();
            }} 
            color="error"
          >
            Delete
          </Button>
        </DialogActions>
      </Dialog>

      {/* Create Goal Dialog */}
      <Dialog open={openCreateDialog} onClose={handleCloseCreateDialog} maxWidth="md">
        <DialogTitle>Create New Goal</DialogTitle>
        <DialogContent>
          <FormControl fullWidth margin="dense" sx={{ mb: 2 }}>
            <InputLabel id="goal-type-label">Goal Type</InputLabel>
            <Select
              labelId="goal-type-label"
              id="goal-type"
              value={goalType}
              label="Goal Type"
              onChange={handleGoalTypeChange}
              autoFocus
            >
              <MenuItem value={GoalType.SAVINGS}>Savings Goal</MenuItem>
              <MenuItem value={GoalType.SPENDING_LIMIT}>Spending Limit Goal</MenuItem>
            </Select>
          </FormControl>

          {goalType === GoalType.SAVINGS && (
            <Box>
              <TextField
                margin="dense"
                name="name"
                label="Goal Name"
                type="text"
                fullWidth
                variant="outlined"
                value={newSavingsGoal.name}
                onChange={handleSavingsGoalInputChange}
                error={!!formErrors.name}
                helperText={formErrors.name}
                sx={{ mb: 2 }}
              />
              <TextField
                margin="dense"
                name="description"
                label="Description (Optional)"
                type="text"
                fullWidth
                variant="outlined"
                value={newSavingsGoal.description || ''}
                onChange={handleSavingsGoalInputChange}
                sx={{ mb: 2 }}
              />
              <FormControl fullWidth margin="dense" sx={{ mb: 2 }}>
                <InputLabel id="account-label">Account</InputLabel>
                <Select
                  labelId="account-label"
                  id="account"
                  name="accountId"
                  value={newSavingsGoal.accountId}
                  label="Account"
                  onChange={handleSavingsGoalInputChange}
                  error={!!formErrors.accountId}
                >
                  {accounts.map(account => (
                    <MenuItem key={account.id} value={account.id}>{account.name}</MenuItem>
                  ))}
                </Select>
                {formErrors.accountId && (
                  <FormHelperText error>{formErrors.accountId}</FormHelperText>
                )}
              </FormControl>
              <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
                <TextField
                  margin="dense"
                  name="targetAmountValue"
                  label="Target Amount"
                  type="number"
                  fullWidth
                  variant="outlined"
                  value={newSavingsGoal.targetAmount.value}
                  onChange={handleSavingsGoalInputChange}
                  error={!!formErrors.targetAmount}
                  helperText={formErrors.targetAmount}
                  inputProps={{ step: 0.01 }}
                />
                <TextField
                  margin="dense"
                  name="targetAmountCurrencyCode"
                  label="Currency Code"
                  type="text"
                  fullWidth
                  variant="outlined"
                  value={newSavingsGoal.targetAmount.currencyCode}
                  onChange={handleSavingsGoalInputChange}
                />
              </Box>
              <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
                <TextField
                  margin="dense"
                  name="startDate"
                  label="Start Date (YYYY-MM-DD)"
                  type="date"
                  fullWidth
                  variant="outlined"
                  value={new Date(newSavingsGoal.startDate).toISOString().split('T')[0]}
                  onChange={handleSavingsGoalDateChange}
                  error={!!formErrors.dates}
                  InputLabelProps={{ shrink: true }}
                />
                <TextField
                  margin="dense"
                  name="endDate"
                  label="End Date (YYYY-MM-DD)"
                  type="date"
                  fullWidth
                  variant="outlined"
                  value={new Date(newSavingsGoal.endDate).toISOString().split('T')[0]}
                  onChange={handleSavingsGoalDateChange}
                  error={!!formErrors.dates}
                  helperText={formErrors.dates}
                  InputLabelProps={{ shrink: true }}
                />
              </Box>
            </Box>
          )}

          {goalType === GoalType.SPENDING_LIMIT && (
            <Box>
              <TextField
                margin="dense"
                name="name"
                label="Goal Name"
                type="text"
                fullWidth
                variant="outlined"
                value={newSpendingLimitGoal.name}
                onChange={handleSpendingLimitGoalInputChange}
                error={!!formErrors.name}
                helperText={formErrors.name}
                sx={{ mb: 2 }}
              />
              <TextField
                margin="dense"
                name="description"
                label="Description (Optional)"
                type="text"
                fullWidth
                variant="outlined"
                value={newSpendingLimitGoal.description || ''}
                onChange={handleSpendingLimitGoalInputChange}
                sx={{ mb: 2 }}
              />
              <FormControl fullWidth margin="dense" sx={{ mb: 2 }}>
                <InputLabel id="account-label">Account</InputLabel>
                <Select
                  labelId="account-label"
                  id="account"
                  name="accountId"
                  value={newSpendingLimitGoal.accountId}
                  label="Account"
                  onChange={handleSpendingLimitGoalInputChange}
                  error={!!formErrors.accountId}
                >
                  {accounts.map(account => (
                    <MenuItem key={account.id} value={account.id}>{account.name}</MenuItem>
                  ))}
                </Select>
                {formErrors.accountId && (
                  <FormHelperText error>{formErrors.accountId}</FormHelperText>
                )}
              </FormControl>
              <TextField
                margin="dense"
                name="category"
                label="Spending Category"
                type="text"
                fullWidth
                variant="outlined"
                value={newSpendingLimitGoal.category}
                onChange={handleSpendingLimitGoalInputChange}
                error={!!formErrors.category}
                helperText={formErrors.category}
                sx={{ mb: 2 }}
              />
              <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
                <TextField
                  margin="dense"
                  name="limitValue"
                  label="Spending Limit"
                  type="number"
                  fullWidth
                  variant="outlined"
                  value={newSpendingLimitGoal.limit.value}
                  onChange={handleSpendingLimitGoalInputChange}
                  error={!!formErrors.limit}
                  helperText={formErrors.limit}
                  inputProps={{ step: 0.01 }}
                />
                <TextField
                  margin="dense"
                  name="limitCurrencyCode"
                  label="Currency Code"
                  type="text"
                  fullWidth
                  variant="outlined"
                  value={newSpendingLimitGoal.limit.currencyCode}
                  onChange={handleSpendingLimitGoalInputChange}
                />
              </Box>
              <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
                <TextField
                  margin="dense"
                  name="startDate"
                  label="Start Date (YYYY-MM-DD)"
                  type="date"
                  fullWidth
                  variant="outlined"
                  value={new Date(newSpendingLimitGoal.startDate).toISOString().split('T')[0]}
                  onChange={handleSpendingLimitGoalDateChange}
                  error={!!formErrors.dates}
                  InputLabelProps={{ shrink: true }}
                />
                <TextField
                  margin="dense"
                  name="endDate"
                  label="End Date (YYYY-MM-DD)"
                  type="date"
                  fullWidth
                  variant="outlined"
                  value={new Date(newSpendingLimitGoal.endDate).toISOString().split('T')[0]}
                  onChange={handleSpendingLimitGoalDateChange}
                  error={!!formErrors.dates}
                  helperText={formErrors.dates}
                  InputLabelProps={{ shrink: true }}
                />
              </Box>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseCreateDialog}>Cancel</Button>
          <Button 
            onClick={handleCreateGoal}
            disabled={!goalType}
            variant="contained"
          >
            Create
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default GoalsPage;
