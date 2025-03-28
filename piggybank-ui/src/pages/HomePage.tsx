import { Box, Typography, Button, Grid, Paper } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import AccountBalanceIcon from '@mui/icons-material/AccountBalance';
import SendIcon from '@mui/icons-material/Send';

const HomePage = () => {
  return (
    <Box sx={{ textAlign: 'center', py: 4 }}>
      <Typography variant="h3" component="h1" gutterBottom>
        Welcome to PiggyBank
      </Typography>
      <Typography variant="h5" component="h2" color="text.secondary" paragraph>
        Monitor your accounts and manage transfers in one place
      </Typography>
      
      <Grid container spacing={4} sx={{ mt: 4 }}>
        <Grid item xs={12} md={6}>
          <Paper 
            elevation={3} 
            sx={{ 
              p: 4, 
              height: '100%', 
              display: 'flex', 
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center'
            }}
          >
            <AccountBalanceIcon sx={{ fontSize: 60, color: 'primary.main', mb: 2 }} />
            <Typography variant="h5" component="h3" gutterBottom>
              Account Management
            </Typography>
            <Typography paragraph>
              Add accounts to monitor, view account details, and track transaction history.
            </Typography>
            <Button 
              variant="contained" 
              component={RouterLink} 
              to="/accounts"
              sx={{ mt: 2 }}
            >
              Manage Accounts
            </Button>
          </Paper>
        </Grid>
        
        <Grid item xs={12} md={6}>
          <Paper 
            elevation={3} 
            sx={{ 
              p: 4, 
              height: '100%', 
              display: 'flex', 
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center'
            }}
          >
            <SendIcon sx={{ fontSize: 60, color: 'primary.main', mb: 2 }} />
            <Typography variant="h5" component="h3" gutterBottom>
              Transfer Money
            </Typography>
            <Typography paragraph>
              Send money between accounts quickly and securely.
            </Typography>
            <Button 
              variant="contained" 
              component={RouterLink} 
              to="/transfers"
              sx={{ mt: 2 }}
            >
              Make Transfers
            </Button>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
};

export default HomePage;