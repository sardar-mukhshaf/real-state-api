# API contract and examples

Default API: http://localhost:3000. Existing route names are retained. [openapi.json](openapi.json) is the machine-readable contract; import it into an OpenAPI-compatible viewer or client. It is a checked-in document, not a runtime Swagger endpoint.

## Common behavior

Send JSON with Content-Type: application/json and protected requests with Authorization: Bearer <access token>. Register/login/refresh require X-Device-ID: 8–128 characters matching [A-Za-z0-9._:-]+. Device IDs identify sessions, not cryptographic identity.

Success bodies contain title, status, type, success, message and data. List data remains an array. Property/transaction searches also include pagination (page, size, totalElements, totalPages) and X-Total-Count. Paging is zero based, defaults to size 100, and caps size at 100. Other list routes accept page/size with bounded arrays.

Errors use ProblemDetail with HTTP status, stable code, safe detail, instance, timestamp and traceId. Validation may add errors. Statuses: 400 invalid input, 401 missing/invalid authentication, 403 forbidden, 404 missing record, 409 conflicting state, 413 oversized request, 415 unsupported media, 429 rate limit, 503 unavailable protection/storage. A 429 supplies Retry-After in seconds.

Fields are predominantly snake_case. Retained exceptions include is_VAT, imageIds, propertyName, types, startDate/endDate and report response keys. UUID IDs are strings. Amounts accept numbers or decimal strings; new input cannot contain fractional pennies. Required monetary values are at least 1.00, matching legacy validation; generated fee/payment amounts may be smaller. PUT is partial: omitted fields are preserved; explicit null cannot clear required values.

## Route catalog

Admin = ADMIN bearer token. Owner = indicated role, using the verified token's user ID.

| Method | Path | Access and behavior |
|---|---|---|
| GET | / and /health | Public liveness |
| POST | /api/auth/register | Public; USER registration only |
| POST | /api/auth/login | Public; access and refresh tokens |
| POST | /api/auth/refresh | Valid refresh token and matching device |
| POST | /api/auth/logout | Authenticated; revoke current family |
| POST | /api/auth/logout-all | Authenticated; revoke all sessions |
| POST | /api/auth/change-password | Authenticated; current password, all sessions revoked |
| POST | /api/user/landlord/create, /api/user/tenant/create | Admin; account and subtype atomically |
| GET | /api/user/landlord/all, /api/user/tenant/all | Admin; paged role lists |
| GET | /api/user/single/{id} | Admin; safe user view |
| PUT | /api/user/landlord/update/{id}, /api/user/tenant/update/{id} | Admin; partial change, immutable role |
| DELETE | /api/user/landlord/delete/{id}, /api/user/tenant/delete/{id} | Admin; matching account |
| POST / GET | /api/address/create, /api/address/all | Admin |
| PUT / DELETE | /api/address/update/{id}, /api/address/delete/{id} | Admin |
| POST / GET | /api/company/create, /api/company/all | Admin |
| GET / PUT / DELETE | /api/company/single/{id}, /api/company/update/{id}, /api/company/delete/{id} | Admin |
| POST | /api/property/create | Admin; JSON or multipart fields + images |
| GET | /api/property/all, /api/property/single/{id} | Public, privacy-filtered |
| GET | /api/property/landlord, /api/property/tenant | Owner; own properties |
| PUT / DELETE | /api/property/update/{id}, /api/property/delete/{id} | Admin |
| POST | /api/property/images/{id}, /api/property/documents/{id} | Admin; multipart images/documents |
| POST / GET | /api/contract/create, /api/contract/all | Admin |
| GET / PUT / DELETE | /api/contract/single/{id}, /api/contract/update/{id}, /api/contract/delete/{id} | Admin; rent references constrain changes |
| POST | /api/{transaction,rent,expense,management-fee,landlord-payment}/create | Admin; optional Idempotency-Key |
| PUT | /api/{transaction,rent,expense,management-fee,landlord-payment}/update/{id} | Admin; base Transaction ID |
| GET | /api/transaction/all, /api/transaction/all/{propertyId} | Admin; paged search |
| GET / DELETE | /api/transaction/single/{id}, /api/transaction/delete/{id} | Admin |
| PUT | /api/landlord-payment/update/payment-status/{id} | Admin; base Transaction ID; PENDING → PAID |
| POST | /api/report/summary, /api/report/monthly | Admin; JSON reports |
| GET | /api/dashboard | Admin counts |
| POST | /api/upload and /api/upload/ | Admin; multipart images, 1–10 files |
| POST | /api/upload/get-images, /api/upload/delete-images | Admin; imageIds array |
| GET | /api/upload/property_images/{propertyId}, /api/upload/single/{id} | Admin |
| DELETE | /api/upload/delete/{id} | Admin; File ID |

Curly-brace feature alternatives expand into separate paths. Some subtype responses use a subtype ID: use transaction_id for transaction updates/payment status. Property attachment IDs differ from File IDs; use file_id with upload metadata/deletion endpoints.

## Example workflow

Replace illustrative IDs with IDs returned by earlier calls.

1. Create landlord and tenant through their /api/user/.../create paths:

~~~json
{"first_name":"Alex","last_name":"Owner","email":"owner@example.test","password":"UseYourOwnPassword123!","type":"LANDLORD"}
~~~

Use TENANT for a tenant. Passwords require at least eight characters, uppercase, lowercase and a digit, with at most 72 UTF-8 bytes for BCrypt. Choose longer generated passwords in practice.

2. Create a property:

~~~json
{"name":"Baker Street House","size":80,"price":"250000.00","type":"HOUSE","status":"RENT","landlord_id":"LANDLORD_ID","house_number":"42","building_name":"","street":"Baker Street","town":"Westminster","city":"London","postal_code":"NW1"}
~~~

Types: HOUSE, APARTMENT, COMMERCIAL. Statuses: RENT, SELL, RENTED, SOLD. Size is a positive whole number. Supply address_id or a complete inline address; address is optional if neither is supplied. Inline address changes create a new history link, so provide its required fields together. A price-only PUT can be {"price":"260000.00"}.

3. Create a contract:

~~~json
{"property_id":"PROPERTY_ID","tenant_id":"TENANT_ID","start_date":"2026-01-01","end_date":"2026-12-31","mng_fee_percentage":"10.0000","rent_per_month":"1000.00"}
~~~

Dates are ordered and fee is 0–100 percent. Rents must agree with the contract's property, tenant and dates.

4. Create rent through /api/rent/create with a unique Idempotency-Key:

~~~json
{"property_id":"PROPERTY_ID","tenant_id":"TENANT_ID","tenancy_contract_id":"CONTRACT_ID","type":"RENT","amount":"1000.00","description":"September rent","is_VAT":false,"transaction_date":"2026-09-01","transaction_month":9,"start_date":"2026-09-01","end_date":"2026-09-30"}
~~~

This creates rent 1000.00, fee 100.00 and pending payment 900.00 atomically. The same actor/feature/key and identical payload returns the original result; changing the payload gives 409. Keys allow 8–128 safe characters and currently have no automatic expiry. Separate keys can still create multiple rent entries for one month.

Expenses use common transaction fields with EXPENSE; management fees use MANAGEMENT. Landlord payments use PAYMENT and landlord_id. Generic /transaction/create retains base-ledger creation; use feature endpoints when subtype behavior is needed.

5. Set payment status using {"status":"PAID"}. PAID cannot reopen. Rent-generated children are maintained through their source rent, not individually.

6. Reports:

~~~json
{"property_id":"PROPERTY_ID","year":2026}
~~~

Send to /api/report/summary. Alternatively provide startDate and endDate together; endpoints are inclusive and the range is capped at ten years.

~~~json
{"property_id":"PROPERTY_ID","month":9,"year":2026}
~~~

Send to /api/report/monthly. Omitted year means current UTC year. Month selects accounting month; year selects transaction-date year. Fee metadata retains active-or-latest contract selection.

## Session calls

Login/register/refresh return data.user, data.token, data.refresh_token, data.expires_in and data.token_type. Refresh uses {"refresh_token":"..."} and the same X-Device-ID. Replace both tokens after rotation and discard the old refresh token. Serialize refresh requests across client tabs.

Logout/logout-all require an access token and no body. Password change uses current_password and new_password. Successful change invalidates the token used in that request; sign in again.

## Search, files and privacy

Property search supports location, page, size, sort and descending. Sorts: id, name, price, size, created_at, relevance. Example: /api/property/all?location=Baker%20London&size=20&sort=price.

Transaction search supports propertyName, repeated types, page, size, sort and descending. Sorts: id, amount, transaction_date, created_at. Example: /api/transaction/all?types=RENT&types=EXPENSE. Property-specific path search uses the property ID.

JSON bodies are capped at 1 MiB, including streamed bodies. Files are at most 5 MiB each and multipart requests at most 26 MiB. Property attachment requests accept 1–5 files and a property supports 100 total attachments. General uploads accept 1–10 files within the total request cap.

Images allow JPEG/PNG/WebP; documents allow PDF. Extension, declared MIME and magic bytes must agree. These checks do not replace malware scanning or full content parsing. File path responses contain signed download URLs valid for five minutes; refetch metadata for another URL. PDFs use attachment disposition.

Public property views omit landlord email, documents and private address notes. List/detail endpoints stay privacy-filtered even if an administrator supplies a token; owner and administrative write responses expose the appropriate richer view.

