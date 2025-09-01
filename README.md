# SIKAD OTP Server

This is the OTP (One-Time Password) server for the SIKAD application. It handles SMS verification using Twilio's Verify API.

## Features
- SMS OTP verification
- Firebase authentication integration
- Secure environment variable configuration
- Express.js REST API

## Setup

1. Install dependencies:
```bash
npm install
```

2. Create a `.env` file with your Twilio credentials:
```
TWILIO_ACCOUNT_SID=your_account_sid
TWILIO_AUTH_TOKEN=your_auth_token
TWILIO_VERIFY_SID=your_verify_service_sid
```

3. Run the server:
```bash
node server.js
```

The server will start on port 8080 by default.

## API Endpoints

### Health Check
- `GET /`
- Response: "✅ SIKAD OTP Server is running!"

### Start Verification
- `POST /otp/start`
- Body: `{ "phone": "+1234567890" }`
- Sends OTP code via SMS

### Verify OTP
- `POST /otp/check`
- Headers: `Authorization: Bearer <firebase-id-token>`
- Body: `{ "phone": "+1234567890", "code": "123456" }`
- Verifies OTP code and updates Firebase user claims