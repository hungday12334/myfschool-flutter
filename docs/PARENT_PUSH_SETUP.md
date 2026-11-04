# Parent push notification setup

## Current status

The repository has no Firebase dependencies, Firebase app configuration, FCM token source, APNs key, Web VAPID configuration, or backend delivery implementation. `PushNotificationService` in Spring is an interface only. Actual remote delivery is therefore **PARTIAL** and cannot be exercised until a real provider is configured.

`ParentNotificationService` provides the session lifecycle and API integration. Its default `UnavailableParentPushTokenSource` is explicitly unsupported and performs no registration or delivery. It can be replaced with an adapter implementing `ParentPushTokenSource`; it does not require replacing the API client or login flow.

The implemented lifecycle registers only after a Parent session exists, deduplicates successful registration, registers a refreshed token and removes the previous one, and cancels listeners before logout. Logout attempts backend removal before JWT deletion and also revokes the provider token even when the removal API fails. No raw push token is logged or returned by the UI. Tokens remain only in memory in this service.

## Verified backend contract

- `POST /api/parent/devices`: authenticated JSON `{ "token": "<provider token>", "platform": "ANDROID|IOS|WEB", "deviceName": "optional name" }`; token is nonblank and at most 512 characters, device name at most 150.
- `DELETE /api/parent/devices`: authenticated JSON `{ "token": "<provider token>" }`.
- Both return the existing API wrapper with `data: null` on success.
- The backend rejects a token already owned by a different user with 409, including an inactive token. The provider adapter must revoke the token on logout and retrieve a fresh one for the next account; it must not retry transferring another account's token.

## Remaining setup

1. Register the real Android, iOS and optional Web apps in the project's Firebase account. The current Android application ID and iOS bundle ID are `com.example.myschoolse1947`; use the final production identifiers if these will change.
2. Add `firebase_core` and `firebase_messaging`, run the FlutterFire configuration workflow for the intended environments, and initialize Firebase before constructing the provider adapter. Keep service-account/private credentials on the server. See the [official Flutter Firebase setup](https://firebase.google.com/docs/flutter/setup).
3. Configure iOS Push Notifications and background remote notification capabilities, and upload the APNs authentication key in Firebase. Configure Web VAPID and the Firebase messaging service worker for Web delivery. Complete platform-specific requirements from the [FCM Flutter setup guide](https://firebase.google.com/docs/cloud-messaging/flutter/get-started).
4. Implement `ParentPushTokenSource` with the configured SDK. `supported` must be false on unsupported platforms. Return exactly `ANDROID`, `IOS`, or `WEB` from `platform`. `requestPermissionAndToken()` requests notification permission and returns null when denied or unavailable. Map token refresh to `tokenRefresh`, foreground SDK messages to `foregroundMessages`, opened messages to `openedMessages`, terminated-app launch data to `initialMessage()`, and SDK token deletion to `deleteToken()`. Map each message into `{notification: {title, body}, data: {...}}`. Never log tokens.
5. Inject the adapter into the Parent home integration. Keep all Firebase SDK imports inside that adapter, use the unavailable source on unsupported platforms, and subscribe to the existing service events for navigation/foreground UI.
6. Implement a backend provider for `PushNotificationService`, configure its credentials through the server environment, resolve eligible parents through existing announcement visibility rules, and discard invalid/unregistered provider tokens. The current `sendToUser(userId, title, body)` boundary has no structured data parameter; extend the server adapter contract to include the agreed payload before enabling tap navigation. Sending and authorization are server responsibilities.
7. Verify on real Android/iOS devices and an HTTPS Web deployment: permission denial, foreground/background/terminated receipt, valid and invalid taps, token refresh, logout while offline, and switching Parent accounts. A successful token registration alone does not prove remote delivery.

## Explicit navigation payload contract

The Flutter service accepts only positive resource IDs in these data payloads:

```json
{ "type": "ANNOUNCEMENT", "announcementId": "123" }
```

```json
{ "type": "LEAVE_REQUEST", "requestId": "456" }
```

These are proposed app-side integration contracts for the future sender, not fields currently emitted by the backend. Unknown types, missing IDs and malformed IDs are ignored. The destination always fetches details through its authenticated REST API, so payload IDs do not establish authorization. Foreground events may present the notification text and provide navigation only when the target is valid.

## Automated verification

`flutter test test/parent_notification_test.dart` covers the unavailable provider, duplicate registration, token rotation, logout cleanup, registration failure/retry, cleanup failure with provider revocation, late permission completion after logout, and validated target parsing. No production credentials or actual push delivery are used by these tests.
