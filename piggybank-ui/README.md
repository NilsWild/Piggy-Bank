# PiggyBank UI

A modern web interface for the PiggyBank application, allowing users to monitor accounts and manage transfers.

## Features

- **Account Management**: Add, view, and delete accounts
- **Transaction History**: View transaction history for each account
- **Transfer Money**: Send money between accounts

## Technology Stack

- **React 18**: Latest version of React for building the user interface
- **React Router 6**: For client-side routing
- **Material-UI**: Component library for consistent styling
- **TypeScript**: For type safety
- **Vite**: Modern build tool for faster development
- **Axios**: For API communication

## Project Structure

```
piggybank-ui/
├── public/
│   └── piggy-bank.svg       # Application icon
├── src/
│   ├── components/          # Reusable UI components
│   │   └── Layout.tsx       # Common layout with navigation
│   ├── pages/               # Page components
│   │   ├── HomePage.tsx     # Landing page
│   │   ├── AccountsPage.tsx # Account management
│   │   ├── AccountDetailPage.tsx # Account details and transactions
│   │   ├── TransfersPage.tsx # Transfer money between accounts
│   │   ├── NotificationsPage.tsx # Notification management
│   │   └── NotFoundPage.tsx # 404 page
│   ├── services/            # API services
│   │   ├── accountService.ts # Account-related API calls
│   │   ├── transactionService.ts # Transaction-related API calls
│   │   ├── transferService.ts # Transfer-related API calls
│   │   └── notificationService.ts # Notification-related API calls
│   ├── utils/               # Utility functions
│   ├── types.ts             # TypeScript type definitions
│   ├── App.tsx              # Main application component with routing
│   ├── main.tsx             # Application entry point
│   ├── index.css            # Global styles
│   └── vite-env.d.ts        # Vite type declarations
├── index.html               # HTML template
├── package.json             # Dependencies and scripts
├── tsconfig.json            # TypeScript configuration
├── tsconfig.node.json       # TypeScript configuration for Node.js
└── vite.config.ts           # Vite configuration
```

## Getting Started

### Prerequisites

- Node.js 16 or higher
- npm or yarn

### Installation

1. Clone the repository
2. Navigate to the UI directory:
   ```
   cd piggybank-ui
   ```
3. Install dependencies:
   ```
   npm install
   ```
   or
   ```
   yarn
   ```

### Development

Start the development server:

```
npm run dev
```

or

```
yarn dev
```

This will start the development server at http://localhost:3000.

### Building for Production

Build the application for production:

```
npm run build
```

or

```
yarn build
```

### Preview Production Build

Preview the production build:

```
npm run preview
```

or

```
yarn preview
```

## API Integration

The UI communicates with three backend services:

1. **Account Twin Service** (port 8081):
   - Account management
   - Transaction history

2. **Transfer Gateway** (port 8080):
   - Transfer processing
   - Monitored accounts management

3. **Notification Service** (port 8082):
   - Notification subscriptions
   - Notification delivery and management

The Vite development server is configured to proxy API requests to these services.
