import axios from 'axios';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { 
  Notification, 
  NotificationResponse, 
  NotificationSubscription, 
  NotificationSubscriptionRequest, 
  NotificationSubscriptionResponse, 
  NotificationEventType, 
  PageResponse, 
  UnreadNotificationCount 
} from '../types';

const NOTIFICATIONS_API_URL = '/api/notifications';
const SUBSCRIPTIONS_API_URL = '/api/subscriptions';

// Notification functions
export const getAllNotifications = async (
  page: number = 0, 
  size: number = 10
): Promise<PageResponse<NotificationResponse>> => {
  const response = await axios.get<PageResponse<NotificationResponse>>(
    NOTIFICATIONS_API_URL,
    { params: { page, size } }
  );
  return response.data;
};

export const getAccountNotifications = async (
  accountId: string,
  page: number = 0, 
  size: number = 10
): Promise<PageResponse<NotificationResponse>> => {
  const response = await axios.get<PageResponse<NotificationResponse>>(
    `${NOTIFICATIONS_API_URL}/account/${accountId}`,
    { params: { page, size } }
  );
  return response.data;
};

export const getUnreadNotifications = async (
  page: number = 0, 
  size: number = 10
): Promise<PageResponse<NotificationResponse>> => {
  const response = await axios.get<PageResponse<NotificationResponse>>(
    `${NOTIFICATIONS_API_URL}/unread`,
    { params: { page, size } }
  );
  return response.data;
};

export const getUnreadNotificationsForAccount = async (
  accountId: string,
  page: number = 0, 
  size: number = 10
): Promise<PageResponse<NotificationResponse>> => {
  const response = await axios.get<PageResponse<NotificationResponse>>(
    `${NOTIFICATIONS_API_URL}/account/${accountId}/unread`,
    { params: { page, size } }
  );
  return response.data;
};

export const markNotificationAsRead = async (notificationId: string): Promise<void> => {
  await axios.post(`${NOTIFICATIONS_API_URL}/${notificationId}/read`);
};

export const countUnreadNotifications = async (): Promise<UnreadNotificationCount> => {
  const response = await axios.get<UnreadNotificationCount>(`${NOTIFICATIONS_API_URL}/count`);
  return response.data;
};

// Subscription functions
export const createSubscription = async (
  subscriptionRequest: NotificationSubscriptionRequest
): Promise<NotificationSubscriptionResponse> => {
  const response = await axios.post<NotificationSubscriptionResponse>(
    SUBSCRIPTIONS_API_URL, 
    subscriptionRequest
  );
  return response.data;
};

export const getAllSubscriptions = async (): Promise<NotificationSubscriptionResponse[]> => {
  const response = await axios.get<NotificationSubscriptionResponse[]>(SUBSCRIPTIONS_API_URL);
  return response.data;
};

export const getAccountSubscriptions = async (
  accountId: string
): Promise<NotificationSubscriptionResponse[]> => {
  const response = await axios.get<NotificationSubscriptionResponse[]>(
    `${SUBSCRIPTIONS_API_URL}/account/${accountId}`
  );
  return response.data;
};

export const deactivateSubscription = async (subscriptionId: string): Promise<void> => {
  await axios.delete(`${SUBSCRIPTIONS_API_URL}/${subscriptionId}`);
};

// WebSocket connection for real-time notifications using STOMP directly to RabbitMQ
export const connectToNotifications = (
  onNotification: (notification: Notification) => void
): () => void => {
  // Create a STOMP client
  const client = new Client({
    // Connect to RabbitMQ's STOMP WebSocket endpoint through the nginx proxy
    brokerURL: `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/ws`,
    // Credentials for RabbitMQ (using default guest/guest)
    connectHeaders: {
      login: 'guest',
      passcode: 'guest',
    },
    // Disable debug logging
    debug: (str) => {
      // Uncomment for debugging
      // console.log(str);
    },
    // Reconnect automatically if the connection is lost
    reconnectDelay: 5000,
    // Heartbeat settings
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000
  });

  // Handle connection errors
  client.onStompError = (frame) => {
    console.error('STOMP error', frame);
  };

  // Subscribe to the notifications topic when connected
  client.onConnect = (frame) => {
    console.log('Connected to RabbitMQ STOMP WebSocket');

    // Subscribe to the notifications exchange
    // The destination format for RabbitMQ STOMP is /exchange/{exchangeName}/{routingKey}
    const subscription = client.subscribe('/exchange/piggybank.notifications/notification.created', (message) => {
      try {
        // Parse the notification from the message body
        const notification = JSON.parse(message.body) as Notification;
        // Call the callback function with the notification
        onNotification(notification);
      } catch (error) {
        console.error('Failed to parse notification', error);
      }
    });
  };

  // Start the connection
  client.activate();

  // Return a function to close the connection
  return () => {
    if (client.connected) {
      client.deactivate();
    }
  };
};
