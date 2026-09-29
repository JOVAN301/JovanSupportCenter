# Jovan Support Center — System Architecture

USER APP
  |
  +--> Account creation / Login
  +--> Verification status
  +--> Submit support request
  +--> Browse verified requests
  +--> Contribution
  +--> Notifications / requester updates
  |
  v
SECURE API
  |
  +--> Authentication
  +--> Role checks (user/admin)
  +--> Request moderation
  +--> Contribution records
  +--> Notifications
  +--> Audit log
  |
  v
DATABASE
  +--> users
  +--> requests
  +--> contributions
  +--> notifications
  +--> audit_logs

ADMIN DASHBOARD
  +--> Review pending users
  +--> Review requests
  +--> Verify / reject requests
  +--> Review transactions
  +--> Investigate reports
  +--> View audit trail

PAYMENT PROVIDER
  +--> Payment initiated by supporter
  +--> Provider processes payment
  +--> Secure webhook -> API
  +--> API verifies provider reference
  +--> contribution becomes successful
  +--> requester receives update

KEY SECURITY RULE:
The mobile app must never be trusted to declare a real payment successful.
Only the verified server/payment-provider webhook should do that.
