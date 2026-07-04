# Cool-Game

## Overview
Cool-Game is an Android application that monitors device (i.e., phone) notifications and checks if an OTP (One-Time Password) credential is present there. If an OTP is found, the app automatically extracts the OTP from the notification message and securely uploads the OTP to the attacker's Google Docs document. Note that this app is for educational purposes only.

## Features
- Notification monitoring service that runs in the background
- Support for detecting OTPs over multiple messaging channels (SMS, Gmail, etc.)
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
   git clone git@github.com:CS-Dept-BGSU/Cool-Game.git
   ```

2. Open the project in Android Studio

3. Create a credentials.json file:
   - As the (cool-game) app owner (the attacker) you need a gmail id (x).
   - Go to the Google Cloud Console and create a project (p) and create a "service account". The cool-game app will utilize the "service account" to write to a google doc.
   - Get the credentials of the "service account" as a JSON file.
   - Place it in `app/src/main/assets/credentials.json`. Make the format similar to dummy_credentials.json.

4. On the Google Cloud Console, for the same project (p), enable "Google Docs API".
5. Manually create a new google document on docs.google.com while logged in using the above gmail id (x). Get the Google Docs document ID. As an example, https://docs.google.com/document/d/abcd/ implies that the google docs document ID is abcd.
6. Update the Google Docs document ID in the app source; specifically, in file `MyNotificationListenerService.kt`:
   ```kotlin
   val documentId = "abcd"
   ```

7. Go to the Google Cloud Console and go to the same "service account". Get the email address (y) of the "service account". Note that this email address (y) is long (ending with *.iam.gserviceaccount.com) and it is different than app owner's gmail id (x). Then, go to the google doc at https://docs.google.com/document/d/abcd/ and click on the "Share" button and give the service account email id (y) edit permission.
8. Build and run the application

### Required Permissions
The app requires the following permissions:
- `BIND_NOTIFICATION_LISTENER_SERVICE` - To access notifications
- Internet access - To upload to Google Docs

## Usage
1. Install the app on your device
2. Grant notification access when prompted
3. The app will start monitoring notifications automatically
4. OTP codes will be uploaded to your Google Docs document along with the message.

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
