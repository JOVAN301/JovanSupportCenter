# Jovan Support Center — Backend System v1

This is a starter backend for:
1. Account creation and login
2. User verification
3. Support request submission
4. Admin review/verification
5. Public verified requests
6. Supporter contribution records
7. Transaction recording
8. Requester updates/notifications

## Stack
- Node.js + Express
- SQLite for the starter database
- JWT authentication
- bcrypt password hashing

## Important production note
This starter records contribution/payment intent. It does NOT claim to move real money.
Before public fundraising, connect a verified payment provider and add webhook verification,
fraud controls, audit logs, KYC/identity checks, privacy policy, terms, and secure deployment.

## Run locally
```bash
npm install
npm start
```
Server defaults to `http://localhost:3000`.

Create the first admin:
```bash
ADMIN_EMAIL=your-email@example.com ADMIN_PASSWORD='change-this' npm run seed-admin
```

Then use the API endpoints in `API.md`.

## Admin dashboard
Open `../admin-dashboard/index.html` after the API is running. For a deployed system, host the dashboard over HTTPS and set its API URL to the production API.

## Production deployment
A Dockerfile and Render Blueprint are included in the parent project's `render.yaml` and this backend's `Dockerfile`. The v5 deployment guide is in `docs/DEPLOY_V5.md`.
