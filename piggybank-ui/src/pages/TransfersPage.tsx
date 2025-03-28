import { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Button,
  Paper,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  FormHelperText,
  Snackbar,
  Alert,
  CircularProgress,
  Grid,
  Switch,
  FormControlLabel
} from '@mui/material';
import SendIcon from '@mui/icons-material/Send';
import { AccountResponse, TransferRequest, Amount, AccountRequest, TransferAccount } from '../types';
import * as accountService from '../services/accountService';
import * as transferService from '../services/transferService';

const TransfersPage = () => {
  const [accounts, setAccounts] = useState<AccountResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [openSnackbar, setOpenSnackbar] = useState(false);
  const [snackbarMessage, setSnackbarMessage] = useState('');
  const [snackbarSeverity, setSnackbarSeverity] = useState<'success' | 'error'>('success');

  const [transferForm, setTransferForm] = useState<{
    sourceAccountId: string;
    targetAccountId: string;
    amount: string;
    currencyCode: string;
    purpose: string;
    valuationTimestamp: string;
    useExternalSource: boolean;
    useExternalTarget: boolean;
    externalSourceType: string;
    externalSourceIdentifier: string;
    externalTargetType: string;
    externalTargetIdentifier: string;
  }>({
    sourceAccountId: '',
    targetAccountId: '',
    amount: '',
    currencyCode: 'EUR',
    purpose: '',
    valuationTimestamp: new Date().toISOString().slice(0, 16),
    useExternalSource: false,
    useExternalTarget: false,
    externalSourceType: 'IBAN',
    externalSourceIdentifier: '',
    externalTargetType: 'IBAN',
    externalTargetIdentifier: ''
  });

  const [formErrors, setFormErrors] = useState<{
    sourceAccountId?: string;
    targetAccountId?: string;
    externalSourceType?: string;
    externalSourceIdentifier?: string;
    externalTargetType?: string;
    externalTargetIdentifier?: string;
    amount?: string;
    purpose?: string;
  }>({});

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

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | { name?: string; value: unknown }>) => {
    const { name, value } = e.target;
    if (name) {
      setTransferForm(prev => ({ ...prev, [name]: value }));

      // Clear error when field is changed
      if (formErrors[name as keyof typeof formErrors]) {
        setFormErrors(prev => ({ ...prev, [name]: undefined }));
      }
    }
  };

  const handleSwitchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, checked } = e.target;
    setTransferForm(prev => ({ ...prev, [name]: checked }));
  };

  const validateForm = (): boolean => {
    const errors: {
      sourceAccountId?: string;
      targetAccountId?: string;
      externalSourceType?: string;
      externalSourceIdentifier?: string;
      externalTargetType?: string;
      externalTargetIdentifier?: string;
      amount?: string;
      purpose?: string;
    } = {};

    // Validate source account
    if (transferForm.useExternalSource) {
      if (!transferForm.externalSourceType) {
        errors.externalSourceType = 'Source account type is required';
      }
      if (!transferForm.externalSourceIdentifier) {
        errors.externalSourceIdentifier = 'Source account identifier is required';
      }
    } else if (!transferForm.sourceAccountId) {
      errors.sourceAccountId = 'Source account is required';
    }

    // Validate target account
    if (transferForm.useExternalTarget) {
      if (!transferForm.externalTargetType) {
        errors.externalTargetType = 'Target account type is required';
      }
      if (!transferForm.externalTargetIdentifier) {
        errors.externalTargetIdentifier = 'Target account identifier is required';
      }
    } else if (!transferForm.targetAccountId) {
      errors.targetAccountId = 'Target account is required';
    }

    // Check if source and target are the same
    if (!transferForm.useExternalSource && !transferForm.useExternalTarget && 
        transferForm.sourceAccountId === transferForm.targetAccountId && 
        transferForm.sourceAccountId !== '') {
      errors.targetAccountId = 'Source and target accounts must be different';
    }

    // Validate amount
    if (!transferForm.amount) {
      errors.amount = 'Amount is required';
    } else {
      const amountValue = parseFloat(transferForm.amount);
      if (isNaN(amountValue) || amountValue <= 0) {
        errors.amount = 'Amount must be a positive number';
      }
    }

    // Validate purpose
    if (!transferForm.purpose) {
      errors.purpose = 'Purpose is required';
    }

    // Ensure at least one account is monitored
    if (transferForm.useExternalSource && transferForm.useExternalTarget) {
      errors.externalTargetIdentifier = 'At least one account must be a monitored account';
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    try {
      setSubmitting(true);

      // Prepare source account
      let sourceAccount: TransferAccount;
      if (transferForm.useExternalSource) {
        sourceAccount = {
          type: transferForm.externalSourceType,
          identifier: transferForm.externalSourceIdentifier
        };
      } else {
        const account = accounts.find(a => a.id === transferForm.sourceAccountId);
        if (!account) {
          throw new Error('Source account not found');
        }
        sourceAccount = {
          type: account.type,
          identifier: account.identifier
        };
      }

      // Prepare target account
      let targetAccount: TransferAccount;
      if (transferForm.useExternalTarget) {
        targetAccount = {
          type: transferForm.externalTargetType,
          identifier: transferForm.externalTargetIdentifier
        };
      } else {
        const account = accounts.find(a => a.id === transferForm.targetAccountId);
        if (!account) {
          throw new Error('Target account not found');
        }
        targetAccount = {
          type: account.type,
          identifier: account.identifier
        };
      }

      // Ensure the valuationTimestamp is in full ISO format with seconds and timezone
      // The datetime-local input returns a format like "2025-03-27T23:34"
      // We need to add seconds and timezone for a valid Instant in Java
      let fullTimestamp = transferForm.valuationTimestamp;
      if (fullTimestamp.length === 16) { // Format is YYYY-MM-DDThh:mm
        fullTimestamp = `${fullTimestamp}:00Z`; // Add seconds and UTC timezone
      } else if (!fullTimestamp.includes('Z') && !fullTimestamp.includes('+')) {
        // If there's no timezone indicator, add Z for UTC
        fullTimestamp = `${fullTimestamp}Z`;
      }

      const transferRequest: TransferRequest = {
        sourceAccount,
        targetAccount,
        amount: {
          value: parseFloat(transferForm.amount),
          currencyCode: transferForm.currencyCode
        },
        valuationTimestamp: fullTimestamp,
        purpose: transferForm.purpose
      };

      await transferService.processTransfer(transferRequest);

      setSnackbarMessage('Transfer processed successfully!');
      setSnackbarSeverity('success');
      setOpenSnackbar(true);

      // Reset form
      setTransferForm({
        sourceAccountId: '',
        targetAccountId: '',
        amount: '',
        currencyCode: 'EUR',
        purpose: '',
        valuationTimestamp: new Date().toISOString().slice(0, 16),
        useExternalSource: false,
        useExternalTarget: false,
        externalSourceType: 'IBAN',
        externalSourceIdentifier: '',
        externalTargetType: 'IBAN',
        externalTargetIdentifier: ''
      });

    } catch (err) {
      console.error('Failed to process transfer:', err);
      setSnackbarMessage('Failed to process transfer. Please try again.');
      setSnackbarSeverity('error');
      setOpenSnackbar(true);
    } finally {
      setSubmitting(false);
    }
  };

  const handleCloseSnackbar = () => {
    setOpenSnackbar(false);
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

  return (
    <Box>
      <Typography variant="h4" component="h1" gutterBottom>
        Transfer Money
      </Typography>

      {accounts.length === 0 ? (
        <Alert severity="info" sx={{ my: 2 }}>
          You need at least one monitored account to make a transfer. Please add an account first.
        </Alert>
      ) : (
        <Paper component="form" onSubmit={handleSubmit} sx={{ p: 3, mt: 3 }}>
          <Grid container spacing={3}>
            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={transferForm.useExternalSource}
                    onChange={handleSwitchChange}
                    name="useExternalSource"
                  />
                }
                label="Use External Source Account"
              />
            </Grid>

            {transferForm.useExternalSource ? (
              <>
                <Grid item xs={12} md={6}>
                  <FormControl fullWidth error={!!formErrors.externalSourceType}>
                    <InputLabel id="external-source-type-label">Source Account Type</InputLabel>
                    <Select
                      labelId="external-source-type-label"
                      id="external-source-type"
                      name="externalSourceType"
                      value={transferForm.externalSourceType}
                      label="Source Account Type"
                      onChange={handleInputChange}
                    >
                      <MenuItem value="IBAN">IBAN</MenuItem>
                      <MenuItem value="CREDIT_CARD">Credit Card</MenuItem>
                      <MenuItem value="PAYPAL">PayPal</MenuItem>
                    </Select>
                    {formErrors.externalSourceType && (
                      <FormHelperText>{formErrors.externalSourceType}</FormHelperText>
                    )}
                  </FormControl>
                </Grid>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="Source Account Identifier"
                    name="externalSourceIdentifier"
                    value={transferForm.externalSourceIdentifier}
                    onChange={handleInputChange}
                    error={!!formErrors.externalSourceIdentifier}
                    helperText={formErrors.externalSourceIdentifier}
                  />
                </Grid>
              </>
            ) : (
              <Grid item xs={12} md={6}>
                <FormControl fullWidth error={!!formErrors.sourceAccountId}>
                  <InputLabel id="source-account-label">Source Account</InputLabel>
                  <Select
                    labelId="source-account-label"
                    id="source-account"
                    name="sourceAccountId"
                    value={transferForm.sourceAccountId}
                    label="Source Account"
                    onChange={handleInputChange}
                  >
                    {accounts.map(account => (
                      <MenuItem key={account.id} value={account.id}>
                        {account.type}: {account.identifier} ({account.balance.value} {account.balance.currencyCode})
                      </MenuItem>
                    ))}
                  </Select>
                  {formErrors.sourceAccountId && (
                    <FormHelperText>{formErrors.sourceAccountId}</FormHelperText>
                  )}
                </FormControl>
              </Grid>
            )}

            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={transferForm.useExternalTarget}
                    onChange={handleSwitchChange}
                    name="useExternalTarget"
                  />
                }
                label="Use External Target Account"
              />
            </Grid>

            {transferForm.useExternalTarget ? (
              <>
                <Grid item xs={12} md={6}>
                  <FormControl fullWidth error={!!formErrors.externalTargetType}>
                    <InputLabel id="external-target-type-label">Target Account Type</InputLabel>
                    <Select
                      labelId="external-target-type-label"
                      id="external-target-type"
                      name="externalTargetType"
                      value={transferForm.externalTargetType}
                      label="Target Account Type"
                      onChange={handleInputChange}
                    >
                      <MenuItem value="IBAN">IBAN</MenuItem>
                      <MenuItem value="CREDIT_CARD">Credit Card</MenuItem>
                      <MenuItem value="PAYPAL">PayPal</MenuItem>
                    </Select>
                    {formErrors.externalTargetType && (
                      <FormHelperText>{formErrors.externalTargetType}</FormHelperText>
                    )}
                  </FormControl>
                </Grid>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="Target Account Identifier"
                    name="externalTargetIdentifier"
                    value={transferForm.externalTargetIdentifier}
                    onChange={handleInputChange}
                    error={!!formErrors.externalTargetIdentifier}
                    helperText={formErrors.externalTargetIdentifier}
                  />
                </Grid>
              </>
            ) : (
              <Grid item xs={12} md={6}>
                <FormControl fullWidth error={!!formErrors.targetAccountId}>
                  <InputLabel id="target-account-label">Target Account</InputLabel>
                  <Select
                    labelId="target-account-label"
                    id="target-account"
                    name="targetAccountId"
                    value={transferForm.targetAccountId}
                    label="Target Account"
                    onChange={handleInputChange}
                  >
                    {accounts.map(account => (
                      <MenuItem key={account.id} value={account.id}>
                        {account.type}: {account.identifier} ({account.balance.value} {account.balance.currencyCode})
                      </MenuItem>
                    ))}
                  </Select>
                  {formErrors.targetAccountId && (
                    <FormHelperText>{formErrors.targetAccountId}</FormHelperText>
                  )}
                </FormControl>
              </Grid>
            )}

            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Amount"
                name="amount"
                type="number"
                value={transferForm.amount}
                onChange={handleInputChange}
                error={!!formErrors.amount}
                helperText={formErrors.amount}
                InputProps={{
                  endAdornment: (
                    <FormControl variant="standard" sx={{ minWidth: 80 }}>
                      <Select
                        name="currencyCode"
                        value={transferForm.currencyCode}
                        onChange={handleInputChange}
                      >
                        <MenuItem value="EUR">EUR</MenuItem>
                        <MenuItem value="USD">USD</MenuItem>
                        <MenuItem value="GBP">GBP</MenuItem>
                      </Select>
                    </FormControl>
                  ),
                }}
              />
            </Grid>

            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Purpose"
                name="purpose"
                value={transferForm.purpose}
                onChange={handleInputChange}
                error={!!formErrors.purpose}
                helperText={formErrors.purpose}
              />
            </Grid>

            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Valuation Timestamp"
                name="valuationTimestamp"
                type="datetime-local"
                value={transferForm.valuationTimestamp}
                onChange={handleInputChange}
                InputLabelProps={{
                  shrink: true,
                }}
              />
            </Grid>

            <Grid item xs={12}>
              <Button
                type="submit"
                variant="contained"
                startIcon={<SendIcon />}
                disabled={submitting}
                sx={{ mt: 2 }}
              >
                {submitting ? 'Processing...' : 'Send Transfer'}
              </Button>
            </Grid>
          </Grid>
        </Paper>
      )}

      {/* Snackbar for notifications */}
      <Snackbar open={openSnackbar} autoHideDuration={6000} onClose={handleCloseSnackbar}>
        <Alert onClose={handleCloseSnackbar} severity={snackbarSeverity} sx={{ width: '100%' }}>
          {snackbarMessage}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default TransfersPage;
