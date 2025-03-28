import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Button,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  Chip,
  Alert,
  CircularProgress,
  Divider,
  Switch,
  FormControlLabel,
  Snackbar
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import NotificationsIcon from '@mui/icons-material/Notifications';
import NotificationsOffIcon from '@mui/icons-material/NotificationsOff';
import { 
  AccountResponse, 
  TransactionResponse, 
  TransactionType, 
  NotificationEventType,
  NotificationSubscriptionRequest,
  NotificationSubscriptionResponse
} from '../types';
import * as accountService from '../services/accountService';
import * as transactionService from '../services/transactionService';
import * as notificationService from '../services/notificationService';

const AccountDetailPage = () => {
  const { accountId } = useParams<{ accountId: string }>();
  const navigate = useNavigate();

  const [account, setAccount] = useState<AccountResponse | null>(null);
  const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalElements, setTotalElements] = useState(0);

  // Notification subscription state
  const [subscribed, setSubscribed] = useState(false);
  const [subscriptionId, setSubscriptionId] = useState<string | null>(null);
  const [subscriptionLoading, setSubscriptionLoading] = useState(false);
  const [snackbarOpen, setSnackbarOpen] = useState(false);
  const [snackbarMessage, setSnackbarMessage] = useState('');

  useEffect(() => {
    if (accountId) {
      fetchAccountDetails();
      fetchSubscriptionStatus();
    }
  }, [accountId]);

  useEffect(() => {
    if (accountId) {
      fetchTransactions();
    }
  }, [accountId, page, rowsPerPage]);

  // Fetch the subscription status for this account
  const fetchSubscriptionStatus = async () => {
    if (!accountId) return;

    try {
      setSubscriptionLoading(true);
      const subscriptions = await notificationService.getAccountSubscriptions(accountId);

      // Check if there's an active subscription for this account for balance updates
      const subscription = subscriptions.find(
        sub => sub.eventType === NotificationEventType.BALANCE_UPDATE &&
               sub.active
      );

      if (subscription) {
        setSubscribed(true);
        setSubscriptionId(subscription.id);
      } else {
        setSubscribed(false);
        setSubscriptionId(null);
      }
    } catch (err) {
      console.error('Failed to fetch subscription status:', err);
      // Don't show an error message to the user, just assume they're not subscribed
      setSubscribed(false);
      setSubscriptionId(null);
    } finally {
      setSubscriptionLoading(false);
    }
  };

  // Toggle subscription status
  const handleToggleSubscription = async () => {
    if (!accountId) return;

    try {
      setSubscriptionLoading(true);

      if (subscribed && subscriptionId) {
        // Unsubscribe
        await notificationService.deactivateSubscription(subscriptionId);
        setSubscribed(false);
        setSubscriptionId(null);
        setSnackbarMessage('Notifications disabled for this account');
      } else {
        // Subscribe
        const request: NotificationSubscriptionRequest = {
          accountId,
          eventType: NotificationEventType.BALANCE_UPDATE
        };

        const response = await notificationService.createSubscription(request);
        setSubscribed(true);
        setSubscriptionId(response.id);
        setSnackbarMessage('Notifications enabled for this account');
      }

      setSnackbarOpen(true);
    } catch (err) {
      console.error('Failed to toggle subscription:', err);
      setSnackbarMessage('Failed to update notification settings');
      setSnackbarOpen(true);
    } finally {
      setSubscriptionLoading(false);
    }
  };

  // Close the snackbar
  const handleCloseSnackbar = () => {
    setSnackbarOpen(false);
  };

  const fetchAccountDetails = async () => {
    try {
      setLoading(true);
      if (!accountId) return;

      const data = await accountService.getAccount(accountId);
      setAccount(data);
      setError(null);
    } catch (err) {
      console.error('Failed to fetch account details:', err);
      setError('Failed to fetch account details. Please try again later.');
    } finally {
      setLoading(false);
    }
  };

  const fetchTransactions = async () => {
    try {
      if (!accountId) return;

      const data = await transactionService.getTransactionsByAccount(accountId, page, rowsPerPage);
      setTransactions(data.content);
      setTotalElements(data.totalElements);
      setError(null);
    } catch (err) {
      console.error('Failed to fetch transactions:', err);
      setError('Failed to fetch transactions. Please try again later.');
    }
  };

  const handleChangePage = (_event: unknown, newPage: number) => {
    setPage(newPage);
  };

  const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0);
  };

  const getTransactionTypeColor = (type: TransactionType) => {
    switch (type) {
      case TransactionType.CREDIT:
        return 'success';
      case TransactionType.DEBIT:
        return 'error';
      case TransactionType.DUMMY:
        return 'default';
      default:
        return 'default';
    }
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleString();
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', my: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return <Alert severity="error" sx={{ my: 2 }}>{error}</Alert>;
  }

  if (!account) {
    return <Alert severity="error" sx={{ my: 2 }}>Account not found</Alert>;
  }

  return (
    <Box>
      <Button 
        startIcon={<ArrowBackIcon />} 
        onClick={() => navigate('/accounts')}
        sx={{ mb: 3 }}
      >
        Back to Accounts
      </Button>

      <Paper sx={{ p: 3, mb: 4 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Account Details
        </Typography>
        <Divider sx={{ my: 2 }} />
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
          <Typography variant="body1">
            <strong>ID:</strong> {account.id}
          </Typography>
          <Typography variant="body1">
            <strong>Type:</strong> {account.type}
          </Typography>
          <Typography variant="body1">
            <strong>Identifier:</strong> {account.identifier}
          </Typography>
          <Typography variant="h6" sx={{ mt: 2 }}>
            <strong>Balance:</strong> {account.balance.value} {account.balance.currencyCode}
          </Typography>

          <Box sx={{ mt: 3, display: 'flex', alignItems: 'center' }}>
            <FormControlLabel
              control={
                <Switch
                  checked={subscribed}
                  onChange={handleToggleSubscription}
                  disabled={subscriptionLoading}
                  color="primary"
                />
              }
              label={
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  {subscribed ? (
                    <>
                      <NotificationsIcon color="primary" sx={{ mr: 1 }} />
                      <Typography>Notifications enabled</Typography>
                    </>
                  ) : (
                    <>
                      <NotificationsOffIcon sx={{ mr: 1 }} />
                      <Typography>Notifications disabled</Typography>
                    </>
                  )}
                </Box>
              }
            />
            {subscriptionLoading && <CircularProgress size={20} sx={{ ml: 2 }} />}
          </Box>
        </Box>
      </Paper>

      <Typography variant="h5" component="h2" gutterBottom>
        Transaction History
      </Typography>

      {transactions.length === 0 ? (
        <Alert severity="info" sx={{ my: 2 }}>No transactions found for this account.</Alert>
      ) : (
        <>
          <TableContainer component={Paper}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Type</TableCell>
                  <TableCell>Amount</TableCell>
                  <TableCell>Purpose</TableCell>
                  <TableCell>From Account</TableCell>
                  <TableCell>To Account</TableCell>
                  <TableCell>Timestamp</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {transactions.map((transaction) => (
                  <TableRow key={transaction.id}>
                    <TableCell>
                      <Chip 
                        label={transaction.type} 
                        color={getTransactionTypeColor(transaction.type) as any}
                        size="small"
                      />
                    </TableCell>
                    <TableCell>
                      {transaction.amount.value} {transaction.amount.currencyCode}
                    </TableCell>
                    <TableCell>{transaction.purpose}</TableCell>
                    <TableCell>{transaction.sourceAccount || '-'}</TableCell>
                    <TableCell>{transaction.destinationAccount || '-'}</TableCell>
                    <TableCell>{formatDate(transaction.valuationTimestamp)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
          <TablePagination
            component="div"
            count={totalElements}
            page={page}
            onPageChange={handleChangePage}
            rowsPerPage={rowsPerPage}
            onRowsPerPageChange={handleChangeRowsPerPage}
            rowsPerPageOptions={[5, 10, 25]}
          />
        </>
      )}

      {/* Snackbar for feedback */}
      <Snackbar
        open={snackbarOpen}
        autoHideDuration={6000}
        onClose={handleCloseSnackbar}
        message={snackbarMessage}
      />
    </Box>
  );
};

export default AccountDetailPage;
