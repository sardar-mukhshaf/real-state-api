# Operations and migration runbook

## Configuration

Application-owned settings bind to validated AppProperties records. Spring datasource, Redis, HTTP and Actuator settings use Boot's typed configuration. Startup rejects weak/missing JWT configuration, invalid lifetimes, missing required properties and invalid application limits. Secrets come from environment/secret management; never put production credentials in application.yml.

| Environment setting | Default / meaning |
|---|---|
| DATABASE_URL | jdbc:postgresql://localhost:5432/real_estate |
| DATABASE_USER | real_estate |
| DATABASE_PASSWORD | Required; Compose additionally requires nonempty value |
| DB_POOL_SIZE | 10 per replica; budget total connections across replicas/workers |
| REDIS_HOST / REDIS_PORT | localhost / 6379 |
| REDIS_PASSWORD | Optional on host; required by supplied Compose stack |
| APP_ENV | local; namespaces Redis keys, set independently per environment |
| JWT_SECRET | Required; at least 32 bytes, generated independently of other secrets |
| JWT_ISSUER / JWT_AUDIENCE | real-estate-api / real-estate-clients |
| ACCESS_TOKEN_SECONDS | 900; allowed 60–3600 |
| REFRESH_TOKEN_SECONDS | 2592000; greater than access TTL, at most 7776000 |
| CORS_ORIGINS | http://localhost:5173; comma-separated explicit origins |
| PORT / MANAGEMENT_PORT | 3000 / 3001; use separate ports |
| S3_BUCKET / AWS_REGION | real-estate / us-east-1 |
| S3_ENDPOINT | Empty for AWS; custom HTTP(S) endpoint for local S3 |
| S3_PUBLIC_ENDPOINT | Browser-reachable presigning endpoint for local S3 |
| AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY | Local development only; prefer workload identity on AWS |
| BOOTSTRAP_ADMIN_EMAIL / BOOTSTRAP_ADMIN_PASSWORD | Both blank by default; both required for first-admin provisioning |

Application rate-limit overrides use Spring properties, e.g. app.rate-limit.login.limit=10 and app.rate-limit.login.window-seconds=60. Keys include real-estate:<APP_ENV>:rate:<policy>:<SHA-256 identifier>. Set environment via APP_ENV and keep shared Redis credentials/private transport within your environment.

| Policy | Requests / 60 seconds | Use |
|---|---:|---|
| GLOBAL | 300 | API source-address budget |
| LOGIN | 10 | Login source/device and normalized account protection |
| REGISTER | 5 | Registration |
| REFRESH | 30 | Refresh |
| PASSWORD_RESET | 5 | Reserved preset; no email reset flow is implemented |
| API | 120 | Other API requests |
| UPLOAD | 10 | Upload paths |
| STRICT | 10 | Password/session-sensitive actions |

Readiness includes PostgreSQL and Redis; liveness does not fail simply because infrastructure is down. Rate limiting fails closed with 503 when Redis is unavailable. Redis holds expiring protection state, not business data.

## Fresh database

Flyway automatically applies V1–V4 at startup. V1 represents the final 17-table Prisma schema, without replaying destructive historical migrations. V2 converts money to NUMERIC, validates values and adds indexes/session/accounting/outbox state. V3 cleans file metadata when its final property reference is removed. V4 indexes active refresh families.

There are 20 application tables after migration: 17 legacy tables plus RefreshSession, IdempotencyRecord and StorageDeletion. Flyway also maintains its schema-history table. Hibernate uses ddl-auto=validate; baseline-on-migrate is false and Flyway clean is disabled.

## Existing Prisma database: explicit cutover

Do not point the Java application at an unaudited existing database. This runbook is an operator procedure; no production database was changed during development.

1. Stop writes to the TypeScript application and take a PostgreSQL backup plus a copy/inventory of local uploads. Test restore into a separate environment.
2. Compare the deployed schema against V1, including quoted names, nullability, subtype foreign keys and TIMESTAMP(3). A database at an earlier Prisma migration must be reconciled first. Do not blindly replay the final migration that drops/recreates role/type/status columns.
3. Run [preflight.sql](preflight.sql) against the restored copy. Resolve duplicate normalized emails, lost/invalid roles, missing tenant/landlord subtype rows, inconsistent contracts/rents and invalid money/date/status values.
4. Review monetary rounding differences before approving NUMERIC conversion. V2 uses PostgreSQL round to two monetary/four percentage decimal places. Record any approved reconciliation separately.
5. Existing generated rent fees/payments do not have a reliable source link in the legacy schema. Reconcile them from business evidence; populate source_rent_id only when certain. The application does not guess links based solely on amounts/dates. Existing orphaned or miscalculated legacy data cannot be automatically repaired safely.
6. On the reviewed restored database, baseline Flyway explicitly at version 1, then start Java to run subsequent migrations and schema validation.
7. Migrate local file objects as described below and reconcile object inventory against File rows.
8. Compare user/property/contract counts, statement totals, representative responses and login behavior. Old bcrypt hashes remain verifiable; users must sign in again because old JWTs lack the new issuer/type/session requirements.
9. Repeat the approved procedure in a maintenance window, deploy one bootstrap instance if required, then enable traffic/replicas after probes and smoke checks succeed.

Use environment variables understood by Flyway's Maven plugin for the **reviewed copy**, keeping credentials out of command history:

~~~powershell
$env:FLYWAY_URL='jdbc:postgresql://HOST:5432/REVIEWED_COPY'
$env:FLYWAY_USER='MIGRATION_USER'
# Supply FLYWAY_PASSWORD through your secret-management process.
.\mvnw.cmd flyway:baseline '-Dflyway.baselineVersion=1'
~~~

Run from the repository root. Never enable automatic baselining to bypass a schema mismatch. Flyway migration permissions may be separated from runtime permissions by applying migrations as a deployment job; the normal runtime then needs only validation and business/table access.

Rollback after schema/data changes means restoring the tested backup and matching object snapshot during downtime. There is no destructive automatic down migration. Do not run old Prisma tooling against a Flyway-owned database.

## Existing local files

The legacy File.path may be an absolute/local path, while Java interprets it as an S3 key. A database baseline alone does not copy bytes.

For each recorded legacy file, verify that the resolved path belongs to the approved legacy upload directory, verify its content/size/type, and copy the bytes to uploads/<File.id> in the private target bucket. Keep File.id and property links stable. Record checksum verification, then update that File.path to the new key in the reviewed database. Missing or invalid files require an explicit reconciliation decision. Retain the original snapshot until cutover has been accepted.

This task supplies the new storage implementation; it did not copy private customer files to an external account. Avoid sending uploads to production or editing legacy paths without an audited manifest and verified destination.

## Production deployment

Build the root Dockerfile with Docker; its final image contains the application/JRE, not Maven caches or source. The process runs as UID 10001, supports a read-only root filesystem with writable /tmp, uses container-aware memory settings and shuts down gracefully on SIGTERM.

The supplied [Kubernetes manifest](../deploy/kubernetes.yml) is a deployment template. Supply the image registry/tag, real-estate-config ConfigMap and real-estate-secrets Secret. Set APP_ENV, database/Redis endpoints and CORS origins; omit S3 endpoint overrides for AWS. Ensure the service account has S3 GetObject/PutObject/DeleteObject permissions scoped to the target bucket/prefix, with private bucket access and encryption policies managed by your cloud configuration.

Expose only business port 3000 via ingress. Management port 3001 permits health/info/prometheus on its separate listener and relies on network isolation; it is not bearer protected. The application listener denies management routes. Keep management traffic private through network policy/firewall rules. Do not configure both listeners on the same port.

Terminate HTTPS at a configured ingress. The application ignores forwarded headers by default to avoid spoofable source-IP rate-limit bypass. Behind a proxy this groups requests by the proxy address; deliberately configure a trusted forwarding chain or enforce source-aware limits at ingress. Enable secure-request awareness only when the proxy strips/replaces untrusted forwarding headers; HSTS depends on that HTTPS awareness.

Provision the first administrator with one instance using both bootstrap settings, verify the account, then remove those settings before enabling replicas. An existing non-admin email is never promoted by bootstrap. Rotating the single HS256 signing secret invalidates all existing tokens; plan coordinated rollout/re-login. A future key-ring design would be needed for overlapping signing-key rotation.

PostgreSQL must provide backups/restore testing and connection capacity. Redis needs private authenticated connectivity; configure TLS through Spring Redis SSL settings for remote deployments. The S3 SDK uses 3-second connect, 10-second socket, 12-second attempt and 30-second total API timeouts with bounded standard retry. No generic retry wraps financial transactions.

## Observability and background work

Logs are structured Logback JSON with request IDs propagated through MDC. Request IDs are validated/generated; request bodies, passwords, authorization headers and raw tokens are not logged. Unexpected client errors contain safe messages; logs identify exception classes without spilling SQL/PII.

Actuator exposes health, info and prometheus only. Micrometer provides HTTP latency, JVM and Hikari metrics, plus security rejection/failure and storage deletion counters. Monitor error/latency changes, pool saturation, readiness and deletion queue depth.

StorageDeletion is a durable database queue. File deletion enqueues its object key in the same transaction; workers claim up to 20 rows with FOR UPDATE SKIP LOCKED and retry failures. A failed multi-file upload rolls back metadata and attempts object cleanup. A process crash after S3 upload and before SQL commit can leave an unreferenced object: reconcile S3 inventory against File metadata using an age grace period. This is the unavoidable cross-system gap without a staging/finalization protocol.

Expired refresh rows are cleaned in bounded batches after a seven-day grace period. Idempotency records remain durable without automatic cleanup. Define retention and capacity policies before very high-volume use.

## Boundaries to understand before production use

* Legacy hard-delete cascades remain. Transaction APIs protect paid payments, but this is not an immutable statutory ledger: administrative deletion of a parent account/property may cascade through history. If your product requires retention, remove those delete permissions and introduce an explicit archival/reversal workflow before use.
* Upload signature checks reject obvious MIME/extension mismatches; they are not malware scanning. Add a quarantine/scanner workflow if the deployment's document threat model requires one.
* There is no payment gateway, email verification delivery, forgotten-password email flow, PDF generation, full audit trail or multi-currency ledger; none existed in the supplied application.
* Bounded lists and batched relationships cap memory use. Large legacy collections beyond the documented bounds require explicit pagination/read-model work; load-test your own dataset before sizing replicas.
* The implemented VAT calculation is legacy behavior, not jurisdiction-specific tax advice.
* Docker/Kubernetes/cloud assets still need execution in an environment with Docker and your deployment credentials; see the actual verification record.

## Troubleshooting

| Symptom | Likely cause / response |
|---|---|
| Enforcer rejects Java | JAVA_HOME points to another version; select JDK 26. |
| Nonempty schema has no Flyway history | Complete the reviewed baseline procedure; do not turn on automatic baseline. |
| Startup configuration validation fails | Inspect required settings/TTL bounds without printing secrets. |
| API returns 503 protection unavailable | Redis is unavailable or credentials/TLS settings are wrong. |
| Readiness 503, liveness 200 | Infrastructure dependency is unavailable; process is alive but should receive no traffic. |
| Refresh reuse returns 401 | Old token was rotated/revoked; log in again and serialize refresh calls. |
| S3 links use an unreachable host | Set browser-reachable S3_PUBLIC_ENDPOINT for the local emulator. |
| Upload gives 400/413 | Check field names, file count, byte signature, per-file and total request limits. |
| Testcontainers cannot start PostgreSQL | Start Docker or use a disposable external PostgreSQL test URL. |
| Maven prints an Unsafe warning | Maven's own Guice runtime on Java 26 may emit it; it is not application password/token logging. |

