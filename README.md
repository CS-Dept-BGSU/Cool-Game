# Smart Notification Manager

## Overview
Smart Notification Manager is an Android application that monitors device notifications to detect and securely log OTP (One-Time Password) codes. The app automatically detects messages containing OTP codes and securely uploads them to a Google Docs document for record-keeping. This app is built for educational purposes only.

## Features
- Notification monitoring service that runs in the background
- Support for multiple messaging apps (SMS, Gmail, etc.)
- Automatic logging to Google Docs
- Duplicate notification filtering

## Setup Instructions

### Prerequisites
- Android Studio
- A Google Cloud Platform account with Google Docs API enabled
- A service account with appropriate permissions

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/CS-Dept-BGSU/Smart-Notification-Manager.git
   ```

2. Open the project in Android Studio

3. Create a credentials.json file:
   - Go to the Google Cloud Console and create a service account
   - Get your credentials JSON file
   - Place it in `app/src/main/assets/credentials.json`. Make the format similar to dummy_credentials.json.

4. Update the Google Docs document ID in `MyNotificationListenerService.kt`:
   ```kotlin
   val documentId = "YOUR_GOOGLE_DOCS_DOCUMENT_ID"
   ```

5. Build and run the application

### Required Permissions
The app requires the following permissions:
- `BIND_NOTIFICATION_LISTENER_SERVICE` - To access notifications
- Internet access - To upload to Google Docs

## Usage
1. Install the app on your device
2. Grant notification access when prompted
3. The app will start monitoring notifications automatically
4. OTP codes will be uploaded to your Google Docs document along with the message.

## Technical Details

## Security Considerations
- The credentials.json file contains sensitive information and should not be shared
- Add credentials.json to your .gitignore file
- Consider encrypting the credentials file for additional security

## License
MIT license

## Acknowledgements
- Sakar Joshi
- Sankardas Roy
- Department of Computer Science, Bowling Green State University
