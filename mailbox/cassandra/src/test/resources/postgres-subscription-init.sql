CREATE TABLE IF NOT EXISTS subscription
(
    username varchar(255) not null,
    mailbox  varchar(500) not null,
    domain  varchar(255) not null,
    constraint usenrame_mailbox_pk unique (username, mailbox, domain)
);

-- Enable RLS on subscription table
ALTER TABLE subscription ENABLE ROW LEVEL SECURITY;

-- Create RLS policy on subscription table, USING domain column
CREATE POLICY domain_scoped_policy
    ON subscription
    AS PERMISSIVE
    FOR ALL
    TO public
    USING (subscription.domain=current_user);

-- Create Role by domains (could be created programmatically upon domain creation)
CREATE ROLE "a.com";
CREATE ROLE "b.com";

-- Grant privilege for roles to access the subscription table (could be created programmatically upon domain creation)
GRANT ALL
    ON subscription
    TO "a.com";
GRANT ALL
    ON subscription
    TO "b.com";