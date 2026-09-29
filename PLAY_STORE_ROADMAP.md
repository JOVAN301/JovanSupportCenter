# Jovan Support Center — Roadmap to Google Play

## Stage A — Working system foundation
- Android app source
- Node/Express API
- SQLite development database
- User accounts
- User verification
- Support requests
- Admin verification
- Public verified requests
- Contribution records
- Notifications
- Audit logs
- Basic admin dashboard

## Stage B — Production backend
1. Deploy the API on a secure HTTPS server.
2. Move from SQLite to PostgreSQL or another managed production database.
3. Store identity documents in private object storage.
4. Add email/phone verification.
5. Add rate limiting and abuse protection.
6. Add admin 2-factor authentication.
7. Add backups and monitoring.
8. Add privacy policy, terms and data-retention rules.

## Stage C — Real payments
- Choose a legitimate payment provider available in Uganda.
- Create server-side payment requests.
- Receive provider webhooks.
- Verify the provider's transaction reference and signature.
- Only then mark a contribution as successful.
- Reconcile transactions and keep an audit trail.

## Stage D — Android release
- Connect the Android app to the production API.
- Replace prototype screens with polished screens.
- Add loading/error/empty states.
- Add secure session handling.
- Test on multiple Android phones.
- Generate a signed Android App Bundle (.aab).
- Create Play Store icon, screenshots, description and privacy-policy URL.
- Use Play Console testing before production release.

## Important
The current project is source code/prototype infrastructure. It is not yet a live public fundraising service and should not be advertised as one until the production security, payment, privacy and verification work is completed.
