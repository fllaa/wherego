# BLOCKED — S3 Firebase gates

S3 ships with `AuthRepository` / `CloudDataSource` **interfaces** and a **local-file fake**. No Firebase SDK. Capture still works offline as a guest.

Do these before real Google Sign-In, Firestore, or Crashlytics.

## H1 — Firebase Android app

1. Create a Firebase project.
2. Add Android app with applicationId `app.wherego`.
3. Download `google-services.json` into `app/`.

## H2 — Debug SHA + Google Sign-In

1. Add this machine’s debug keystore SHA-1 and SHA-256 to the Firebase Android app.
2. Enable Google Sign-In in Authentication.

```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android
```

## H3 — Firestore rules

Paste into Firestore rules:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{db}/documents {
    match /users/{uid}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == uid;
    }
  }
}
```

## After H1–H3

A later S3 pass can replace `FakeCloudDataSource` / `FakeAuthRepository` with Credential Manager + Firestore + Crashlytics. Do **not** hardcode Firebase in `feature/*`.

Collections (when live):

- `users/{uid}/transactions/{id}`
- `users/{uid}/categories/{id}`
- `users/{uid}/profile`

Crashlytics is also gated on H1.
