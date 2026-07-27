# Demonstrate Kerberos GSSAPI with Thunderbird

This demonstration starts the embedded Apache Kerby KDC and the James memory server used by `SaslGssapiIntegrationTest`. Thunderbird obtains IMAP and SMTP service tickets from the KDC and authenticates to James through GSSAPI without receiving the James account password.

The setup is intended for a local demonstration on macOS. The KDC, James server, principals, and credentials are temporary and stop working when the test process exits.

## Prerequisites

Install a current Thunderbird release from the Thunderbird website or with Homebrew:

```bash
brew install --cask thunderbird
```

macOS already provides the required Heimdal Kerberos commands:

```bash
command -v kinit klist kdestroy
kinit --version
```

The commands should resolve under `/usr/bin`. The optional `kvno` command is not needed.

Build the GSSAPI changes before running the demonstration:

```bash
mvn -pl server/apps/memory-app -am -DskipTests install
```

## Start the KDC and James

From the James project root, run only the manual demonstration method:

```bash
mvn -pl server/apps/memory-app \
  -Dtest='SaslGssapiIntegrationTest#thunderbirdDemo' \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Djames.test.kerberos.demo=true \
  test
```

The process prints generated Kerberos configurations, Alice's credentials, and the random IMAP and SMTP ports. It remains available for 15 minutes by default. A different duration can be requested with, for example:

```bash
-Djames.test.kerberos.demo.durationMinutes=30
```

Keep this Maven process running throughout the demonstration.

## Obtain Alice's Kerberos ticket-granting ticket

Open another terminal and copy the generated **macOS** Kerberos configuration path from the test output:

```bash
export KRB5_CONFIG=/printed/path/krb5-macos.conf
export KRB5CCNAME=FILE:/tmp/james-thunderbird.ccache
```

Remove any previous demonstration ticket, then authenticate Alice to the embedded KDC:

```bash
kdestroy 2>/dev/null || true
kinit alice@JAMES.TEST
```

Enter the Kerberos password printed by the test:

```text
alice-kerberos-password
```

This is Alice's temporary KDC password. It is distinct from the deliberately unused James account password.

Use `krb5-macos.conf`, not the JDK configuration used internally by the automated tests. The macOS configuration declares the explicit TCP endpoint required by Apple's Heimdal client for the TCP-only embedded KDC.

Confirm that the credential cache initially contains Alice's ticket-granting ticket:

```bash
klist
```

The output should contain:

```text
krbtgt/JAMES.TEST@JAMES.TEST
```

## Start Thunderbird with the Kerberos environment

Quit any existing Thunderbird process, then launch a separate English-language profile through macOS LaunchServices. The `user.js` preference applies only to this temporary profile. Pass `KRB5_CONFIG` and `KRB5CCNAME` explicitly so that the new Thunderbird process receives them:

```bash
mkdir -p /tmp/thunderbird-james-gssapi
printf '%s\n' 'user_pref("intl.locale.requested", "en-US");' \
  > /tmp/thunderbird-james-gssapi/user.js

open -n -a Thunderbird \
  --env "KRB5_CONFIG=$KRB5_CONFIG" \
  --env "KRB5CCNAME=$KRB5CCNAME" \
  --args -no-remote -profile /tmp/thunderbird-james-gssapi
```

Using `open` lets macOS perform its normal Gatekeeper handling for the signed application. Invoking `/Applications/Thunderbird.app/Contents/MacOS/thunderbird` directly can instead trigger extended-attribute permission errors when the application is still quarantined.

Thunderbird receives the Kerberos configuration and credential-cache variables from `open`. It can therefore use Alice's ticket without asking for her James password.

## Configure the account

Configure the account manually with the following values. Use the random ports printed by the demonstration test.

| Setting | IMAP | SMTP |
|---|---|---|
| Hostname | `localhost` | `localhost` |
| Port | Printed IMAP port | Printed SMTP port |
| Connection security | STARTTLS | STARTTLS |
| Authentication method | Kerberos / GSSAPI | Kerberos / GSSAPI |
| Username | `alice@JAMES.TEST` | `alice@JAMES.TEST` |

Leave the account password empty and do not save a James password. Thunderbird's GSSAPI implementation uses the operating-system credential cache instead.

The hostname must be `localhost`, not `127.0.0.1`. Thunderbird derives the Kerberos targets from that hostname, and the test provisions exactly these principals:

```text
imap/localhost@JAMES.TEST
smtp/localhost@JAMES.TEST
```

The test TLS keystore contains an old self-signed certificate. Accept Thunderbird's temporary certificate exception for both local endpoints. This certificate warning is independent of Kerberos authentication.

## Demonstrate IMAP and SMTP authentication

1. Open Alice's inbox. Thunderbird should authenticate to IMAP without requesting the James account password.
2. Send a message from `alice@JAMES.TEST` to `alice@JAMES.TEST`. Thunderbird should authenticate to SMTP through GSSAPI.
3. Receive the message in Alice's inbox.
4. Return to the terminal and inspect the credential cache:

```bash
klist
```

After both connections, the cache should contain service tickets similar to:

```text
krbtgt/JAMES.TEST@JAMES.TEST
imap/localhost@JAMES.TEST
smtp/localhost@JAMES.TEST
```

These tickets demonstrate that Thunderbird used the KDC-issued credentials for both protocols without receiving Alice's James password.

## Inspect the James logs

The integration-test logging configuration writes the complete James server log to `server/apps/memory-app/target/test-run.log`. From the James project root, follow it while using Thunderbird:

```bash
tail -F server/apps/memory-app/target/test-run.log
```

To display only the authentication and delivery evidence:

```bash
tail -F server/apps/memory-app/target/test-run.log \
  | rg --line-buffered 'GSSAPI authentication succeeded|AUTH method GSSAPI succeeded|SMTP Authentication succeeded|Successfully spooled|Local delivered'
```

A successful demonstration produces entries similar to:

```text
GSSAPI authentication succeeded.
AUTH method GSSAPI succeeded
SMTP Authentication succeeded.
Successfully spooled mail ... from alice@james.test ... for [alice@james.test]
Local delivered mail ... successfully from alice@james.test to <alice@james.test>
```

The console appender intentionally displays only errors during tests, while the file appender records James logs at debug level. The log file is replaced when a new test run starts.

## Stop and clean up

Stop the Maven process or wait for the configured demonstration duration to expire. Then destroy the temporary credential cache:

```bash
kdestroy
```

Remove Thunderbird temp profile directory:
```bash
rm -rf /tmp/thunderbird-james-gssapi
```
