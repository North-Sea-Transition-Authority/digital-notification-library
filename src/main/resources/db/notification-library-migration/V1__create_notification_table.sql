CREATE TABLE notification_library_notifications (
  id UUID PRIMARY KEY,
  type TEXT NOT NULL,
  status TEXT NOT NULL,
  notify_template_id TEXT NOT NULL,
  notify_notification_id TEXT,
  notify_status TEXT,
  recipient TEXT NOT NULL,
  mail_merge_fields JSONB,
  domain_reference_id TEXT NOT NULL,
  domain_reference_type TEXT NOT NULL,
  log_correlation_id TEXT,
  requested_on TIMESTAMPTZ NOT NULL,
  failure_reason TEXT
);

CREATE TABLE notification_library_notifications_aud (
  rev SERIAL,
  revtype NUMERIC,
  id UUID,
  type TEXT,
  status TEXT,
  notify_template_id TEXT,
  notify_notification_id TEXT,
  notify_status TEXT,
  recipient TEXT,
  mail_merge_fields JSONB,
  domain_reference_id TEXT,
  domain_reference_type TEXT,
  log_correlation_id TEXT,
  requested_on TIMESTAMPTZ,
  failure_reason TEXT
);