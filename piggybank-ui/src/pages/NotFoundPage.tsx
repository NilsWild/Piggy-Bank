import { Box, Typography, Button } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline';

const NotFoundPage = () => {
  return (
    <Box sx={{ textAlign: 'center', py: 8 }}>
      <ErrorOutlineIcon sx={{ fontSize: 100, color: 'error.main', mb: 4 }} />
      <Typography variant="h3" component="h1" gutterBottom>
        404 - Page Not Found
      </Typography>
      <Typography variant="h6" color="text.secondary" paragraph>
        The page you are looking for doesn't exist or has been moved.
      </Typography>
      <Button 
        variant="contained" 
        component={RouterLink} 
        to="/"
        sx={{ mt: 4 }}
      >
        Go to Home Page
      </Button>
    </Box>
  );
};

export default NotFoundPage;