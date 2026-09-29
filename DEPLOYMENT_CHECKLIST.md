# Deployment checklist

Before putting Jovan Support Center online:

[ ] Generate a strong JWT secret
[ ] Set production environment variables
[ ] Use HTTPS
[ ] Use a managed production database
[ ] Configure CORS for the actual app/admin domains only
[ ] Add rate limiting
[ ] Add request size limits
[ ] Add structured server logs
[ ] Add database backups
[ ] Protect uploaded identity documents
[ ] Configure admin 2FA
[ ] Configure payment provider webhooks
[ ] Test webhook replay/duplicate protection
[ ] Test refund handling
[ ] Publish privacy policy and terms
[ ] Establish a process for scam reports
[ ] Establish a process for removing invalid requests

## Android API connection (v1.1)
- The Android app now supports account registration and login.
- Auth tokens are persisted locally for the prototype session.
- Support requests are submitted to the backend and remain pending until admin verification.
- Verified public requests are loaded from the backend.
- The app includes a Server Settings screen so a test device can point to a local LAN server or a deployed HTTPS API.
- For production, replace the local token storage with a secure Android credential/token store and use HTTPS only.
