# API flow

## Public
- `GET /health`
- `GET /requests/public`

## Account
- `POST /auth/register`
- `POST /auth/login`
- `GET /me`

Use `Authorization: Bearer <token>` after login.

## Requester
- `POST /requests`
- `GET /requests/mine`
- `GET /notifications`
- `POST /notifications/:id/read`

## Supporter
- `GET /requests/public`
- `POST /contributions`
- `GET /contributions/mine`

## Admin
Admin token required:
- `GET /admin/users`
- `POST /admin/users/:id/verify`
- `POST /admin/users/:id/reject`
- `GET /admin/requests`
- `POST /admin/requests/:id/verify`
- `POST /admin/requests/:id/reject`
- `GET /admin/transactions`

## Production payment step

Do not mark a contribution successful just because the Android app says it succeeded.
The payment provider should call a secure server webhook with its transaction reference.
The server verifies the webhook, then changes `payment_status` to `successful` and creates
the appropriate requester notification.

The next production additions should include:
- real payment provider integration
- ID/document storage and verification
- rate limiting
- HTTPS
- secure secrets
- database backups
- admin 2FA
- privacy policy and terms
- fraud/report workflow
- immutable financial audit records
