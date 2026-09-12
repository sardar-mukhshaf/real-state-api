-- Read-only checks to run on a restored copy BEFORE baselining an existing Prisma database.
-- Do not run the historical destructive enum migration to prepare a database.
SELECT lower(trim(email)) AS normalized_email,count(*) FROM "User" GROUP BY 1 HAVING count(*)>1;
SELECT id,type FROM "User" WHERE type NOT IN ('USER','ADMIN','TENANT','LANDLORD');
SELECT u.id,u.type FROM "User" u
LEFT JOIN "Tenant" t ON t.id=u.id LEFT JOIN "Landlord" l ON l.id=u.id
WHERE (u.type='TENANT' AND t.id IS NULL) OR (u.type='LANDLORD' AND l.id IS NULL);
SELECT id,price,size FROM "Property" WHERE price<1 OR size<1 OR size<>trunc(size) OR price='Infinity'::float8 OR size='Infinity'::float8;
SELECT id,amount,transaction_month,type FROM "Transaction" WHERE amount<0 OR amount='Infinity'::float8 OR transaction_month NOT BETWEEN 1 AND 12 OR type NOT IN ('RENT','EXPENSE','PAYMENT','MANAGEMENT');
SELECT id,mng_fee_percentage,rent_per_month FROM "TenancyContract" WHERE end_date<start_date OR mng_fee_percentage NOT BETWEEN 0 AND 100 OR rent_per_month<1;
SELECT r.id FROM "RentTransaction" r JOIN "TenancyContract" c ON c.id=r.tenancy_contract_id JOIN "Transaction" t ON t.id=r.transaction_id
WHERE r.tenant_id<>c.tenant_id OR t.property_id<>c.property_id OR r.start_date<c.start_date OR r.end_date>c.end_date OR r.end_date<r.start_date;
-- Review rounding deltas; NUMERIC conversion is intentional and irreversible without a backup.
SELECT id,amount,round(amount::numeric,2) AS migrated_amount FROM "Transaction" WHERE amount::numeric<>round(amount::numeric,2);
-- Existing local objects must be copied into S3 and their File.path changed to object keys during cutover.
SELECT id,path,type FROM "File" ORDER BY id;

