import { Outlet } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { 
  AppBar, 
  Box, 
  Container, 
  Toolbar, 
  Typography, 
  Button, 
  Badge, 
  IconButton 
} from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import NotificationsIcon from '@mui/icons-material/Notifications';
import * as notificationService from '../services/notificationService';

const Layout = () => {
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    // Fetch unread notification count when component mounts
    fetchUnreadCount();

    // Set up WebSocket connection for real-time notifications
    const closeConnection = notificationService.connectToNotifications((notification) => {
      // When a new notification is received, only increment the unread count if it's not read
      if (!notification.read) {
        setUnreadCount(prevCount => prevCount + 1);
      }
    });

    // Clean up WebSocket connection when component unmounts
    return () => {
      closeConnection();
    };
  }, []);

  const fetchUnreadCount = async () => {
    try {
      const data = await notificationService.countUnreadNotifications();
      setUnreadCount(data.count);
    } catch (err) {
      console.error('Failed to fetch unread notification count:', err);
      // Don't show an error message to the user, just assume there are no unread notifications
      setUnreadCount(0);
    }
  };

  return (
    <>
      <AppBar position="static">
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            <RouterLink to="/" style={{ color: 'white', textDecoration: 'none' }}>
              PiggyBank
            </RouterLink>
          </Typography>
          <Button color="inherit" component={RouterLink} to="/accounts">
            Accounts
          </Button>
          <Button color="inherit" component={RouterLink} to="/transfers">
            Transfers
          </Button>
          <IconButton 
            color="inherit" 
            component={RouterLink} 
            to="/notifications"
            aria-label="notifications"
            sx={{ ml: 1 }}
          >
            <Badge badgeContent={unreadCount} color="error">
              <NotificationsIcon />
            </Badge>
          </IconButton>
        </Toolbar>
      </AppBar>
      <Container component="main" sx={{ mt: 4, mb: 4 }}>
        <Outlet />
      </Container>
      <Box component="footer" sx={{ py: 3, px: 2, mt: 'auto', backgroundColor: (theme) => theme.palette.grey[200] }}>
        <Container maxWidth="sm">
          <Typography variant="body2" color="text.secondary" align="center">
            PiggyBank UI - {new Date().getFullYear()}
          </Typography>
        </Container>
      </Box>
    </>
  );
};

export default Layout;
