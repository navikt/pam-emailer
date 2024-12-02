CREATE TABLE outbox_email
(
    id         UUID PRIMARY KEY,
    email_id   TEXT                     NOT NULL,
    status     TEXT                     NOT NULL,
    priority   INTEGER                  NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    retries    INTEGER                  NOT NULL,
    payload    TEXT                     NOT NULL
);
