import { useState, useEffect } from 'react';
import { Link as RouterLink } from 'react-router-dom';
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
  InputLabel,
  Select,
  MenuItem
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { AccountResponse, AccountRequest } from '../types';
import * as accountService from '../services/accountService';

const AccountsPage = () => {
  const [accounts, setAccounts] = useState<AccountResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [openDialog, setOpenDialog] = useState(false);
  const [openSnackbar, setOpenSnackbar] = useState(false);
  const [snackbarMessage, setSnackbarMessage] = useState('');
  const [snackbarSeverity, setSnackbarSeverity] = useState<'success' | 'error'>('success');
  const [newAccount, setNewAccount] = useState<AccountRequest>({ 
    type: '', 
    identifier: '', 
    initialBalance: { value: 0, currencyCode: 'EUR' } 
  });

  useEffect(() => {
    fetchAccounts();
  }, []);

  const fetchAccounts = async () => {
    try {
      setLoading(true);
      const data = await accountService.getAllAccounts();
      setAccounts(data);
      setError(null);
    } catch (err) {
      console.error('Failed to fetch accounts:', err);
      setError('Failed to fetch accounts. Please try again later.');
    } finally {
      setLoading(false);
    }
  };

  const handleOpenDialog = () => {
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setNewAccount({ 
      type: '', 
      identifier: '', 
      initialBalance: { value: 0, currencyCode: 'EUR' } 
    });
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | { name?: string; value: unknown }>) => {
    const { name, value } = e.target;

    if (name) {
      if (name === 'balanceValue') {
        setNewAccount(prev => ({
          ...prev,
          initialBalance: {
            ...prev.initialBalance!,
            value: parseFloat(value as string) || 0
          }
        }));
      } else if (name === 'balanceCurrencyCode') {
        setNewAccount(prev => ({
          ...prev,
          initialBalance: {
            ...prev.initialBalance!,
            currencyCode: value as string
          }
        }));
      } else {
        setNewAccount(prev => ({ ...prev, [name]: value }));
      }
    }
  };

  const handleCreateAccount = async () => {
    try {
      // We only need to call createAccount now, as the Account Twin Service
      // is responsible for monitoring accounts
      await accountService.createAccount(newAccount);

      setSnackbarMessage('Account created successfully!');
      setSnackbarSeverity('success');
      setOpenSnackbar(true);

      handleCloseDialog();
      fetchAccounts();
    } catch (err) {
      console.error('Failed to create account:', err);
      setSnackbarMessage('Failed to create account. Please try again.');
      setSnackbarSeverity('error');
      setOpenSnackbar(true);
    }
  };

  const handleDeleteAccount = async (accountId: string) => {
    try {
      await accountService.deleteAccount(accountId);

      setSnackbarMessage('Account deleted successfully!');
      setSnackbarSeverity('success');
      setOpenSnackbar(true);

      fetchAccounts();
    } catch (err) {
      console.error('Failed to delete account:', err);
      setSnackbarMessage('Failed to delete account. Please try again.');
      setSnackbarSeverity('error');
      setOpenSnackbar(true);
    }
  };

  const handleCloseSnackbar = () => {
    setOpenSnackbar(false);
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4 }}>
        <Typography variant="h4" component="h1">
          Accounts
        </Typography>
        <Button 
          variant="contained" 
          startIcon={<AddIcon />}
          onClick={handleOpenDialog}
        >
          Add Account
        </Button>
      </Box>

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', my: 4 }}>
          <CircularProgress />
        </Box>
      ) : error ? (
        <Alert severity="error" sx={{ my: 2 }}>{error}</Alert>
      ) : accounts.length === 0 ? (
        <Alert severity="info" sx={{ my: 2 }}>No accounts found. Add an account to get started.</Alert>
      ) : (
        <Grid container spacing={3}>
          {accounts.map(account => (
            <Grid item xs={12} sm={6} md={4} key={account.id}>
              <Card>
                <CardContent>
                  <Typography variant="h6" component="div">
                    {account.type}: {account.identifier}
                  </Typography>
                  <Typography variant="body1" color="text.secondary" sx={{ mt: 1 }}>
                    Balance: {account.balance.value} {account.balance.currencyCode}
                  </Typography>
                </CardContent>
                <CardActions>
                  <Button 
                    size="small" 
                    component={RouterLink} 
                    to={`/accounts/${account.id}`}
                  >
                    View Details
                  </Button>
                  <Button 
                    size="small" 
                    color="error"
                    onClick={() => handleDeleteAccount(account.id)}
                  >
                    Delete
                  </Button>
                </CardActions>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      {/* Add Account Dialog */}
      <Dialog open={openDialog} onClose={handleCloseDialog}>
        <DialogTitle>Add New Account</DialogTitle>
        <DialogContent>
          <FormControl fullWidth margin="dense" sx={{ mb: 2 }}>
            <InputLabel id="account-type-label">Account Type</InputLabel>
            <Select
              labelId="account-type-label"
              id="account-type"
              name="type"
              value={newAccount.type}
              label="Account Type"
              onChange={handleInputChange}
              autoFocus
            >
              <MenuItem value="IBAN">IBAN</MenuItem>
              <MenuItem value="CREDIT_CARD">Credit Card</MenuItem>
              <MenuItem value="PAYPAL">PayPal</MenuItem>
            </Select>
          </FormControl>
          <TextField
            margin="dense"
            name="identifier"
            label="Account Identifier"
            type="text"
            fullWidth
            variant="outlined"
            value={newAccount.identifier}
            onChange={handleInputChange}
            sx={{ mb: 2 }}
          />
          <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
            <TextField
              margin="dense"
              name="balanceValue"
              label="Initial Balance"
              type="number"
              fullWidth
              variant="outlined"
              value={newAccount.initialBalance?.value || 0}
              onChange={handleInputChange}
              inputProps={{ step: 0.01 }}
            />
            <TextField
              margin="dense"
              name="balanceCurrencyCode"
              label="Currency Code"
              type="text"
              fullWidth
              variant="outlined"
              value={newAccount.initialBalance?.currencyCode || 'EUR'}
              onChange={handleInputChange}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button 
            onClick={handleCreateAccount}
            disabled={!newAccount.type || !newAccount.identifier}
          >
            Create
          </Button>
        </DialogActions>
      </Dialog>

      {/* Snackbar for notifications */}
      <Snackbar open={openSnackbar} autoHideDuration={6000} onClose={handleCloseSnackbar}>
        <Alert onClose={handleCloseSnackbar} severity={snackbarSeverity} sx={{ width: '100%' }}>
          {snackbarMessage}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default AccountsPage;
