-- Review legacy values before this migration. Failed conversions/constraints abort atomically.
ALTER TABLE "Property" ALTER COLUMN "price" TYPE NUMERIC(19,2) USING round("price"::numeric,2);
ALTER TABLE "Transaction" ALTER COLUMN "amount" TYPE NUMERIC(19,2) USING round("amount"::numeric,2);
ALTER TABLE "TenancyContract" ALTER COLUMN "rent_per_month" TYPE NUMERIC(19,2) USING round("rent_per_month"::numeric,2);
ALTER TABLE "TenancyContract" ALTER COLUMN "mng_fee_percentage" TYPE NUMERIC(7,4) USING round("mng_fee_percentage"::numeric,4);
ALTER TABLE "User" ADD COLUMN "security_version" BIGINT NOT NULL DEFAULT 0;
CREATE UNIQUE INDEX "User_email_normalized_key" ON "User" (lower(trim("email")));
UPDATE "User" SET "email" = lower(trim("email"));
ALTER TABLE "User" ADD CONSTRAINT user_type_valid CHECK ("type" IN ('USER','ADMIN','TENANT','LANDLORD'));
ALTER TABLE "Property" ADD CONSTRAINT property_type_valid CHECK ("type" IN ('HOUSE','APARTMENT','COMMERCIAL'));
ALTER TABLE "Property" ADD CONSTRAINT property_status_valid CHECK ("status" IN ('RENT','SELL','RENTED','SOLD'));
ALTER TABLE "Property" ADD CONSTRAINT property_values_valid CHECK ("price" >= 1 AND "size" >= 1 AND "size" < 'Infinity'::float8);
ALTER TABLE "Transaction" ADD CONSTRAINT transaction_values_valid CHECK ("amount" >= 0 AND "transaction_month" BETWEEN 1 AND 12 AND "type" IN ('RENT','EXPENSE','MANAGEMENT','PAYMENT'));
ALTER TABLE "TenancyContract" ADD CONSTRAINT contract_values_valid CHECK ("end_date" >= "start_date" AND "rent_per_month" >= 1 AND "mng_fee_percentage" BETWEEN 0 AND 100);
ALTER TABLE "RentTransaction" ADD CONSTRAINT rent_dates_valid CHECK ("end_date" >= "start_date");
ALTER TABLE "LandlordPaymentTransaction" ADD CONSTRAINT payment_status_valid CHECK ("status" IN ('PENDING','PAID'));
ALTER TABLE "Transaction" ADD COLUMN "source_rent_id" TEXT REFERENCES "Transaction"("id") ON DELETE CASCADE;
CREATE UNIQUE INDEX generated_entry_per_rent ON "Transaction" ("source_rent_id","type") WHERE "source_rent_id" IS NOT NULL;
CREATE INDEX property_landlord_idx ON "Property" ("landlord_id");
CREATE INDEX contract_property_dates_idx ON "TenancyContract" ("property_id","start_date","end_date");
CREATE INDEX contract_tenant_idx ON "TenancyContract" ("tenant_id");
CREATE INDEX transaction_property_month_idx ON "Transaction" ("property_id","transaction_month","transaction_date","type");
CREATE INDEX transaction_date_idx ON "Transaction" ("transaction_date");
CREATE INDEX rent_tenant_idx ON "RentTransaction" ("tenant_id");
CREATE INDEX rent_contract_idx ON "RentTransaction" ("tenancy_contract_id");
CREATE INDEX payment_landlord_idx ON "LandlordPaymentTransaction" ("landlord_id");
CREATE INDEX property_address_property_idx ON "PropertyAddress" ("property_id");
CREATE INDEX property_address_address_idx ON "PropertyAddress" ("address_id");
CREATE INDEX user_address_user_idx ON "UserAddress" ("user_id");
CREATE INDEX user_address_address_idx ON "UserAddress" ("address_id");
CREATE INDEX property_image_property_idx ON "PropertyImage" ("property_id");
CREATE INDEX property_image_file_idx ON "PropertyImage" ("file_id");
CREATE INDEX property_document_property_idx ON "PropertyDocument" ("property_id");
CREATE INDEX property_document_file_idx ON "PropertyDocument" ("file_id");
CREATE TABLE "RefreshSession" (
 "id" TEXT PRIMARY KEY, "user_id" TEXT NOT NULL REFERENCES "User"("id") ON DELETE CASCADE,
 "device_id" TEXT NOT NULL, "family_id" TEXT NOT NULL, "token_hash" TEXT NOT NULL UNIQUE,
 "expires_at" TIMESTAMP(3) NOT NULL, "revoked_at" TIMESTAMP(3), "replaced_by" TEXT,
 "created_at" TIMESTAMP(3) NOT NULL, "updated_at" TIMESTAMP(3) NOT NULL
);
CREATE INDEX refresh_user_idx ON "RefreshSession" ("user_id");
CREATE INDEX refresh_family_idx ON "RefreshSession" ("family_id");
CREATE INDEX refresh_expiry_idx ON "RefreshSession" ("expires_at");
CREATE TABLE "IdempotencyRecord" (
 "key" TEXT PRIMARY KEY, "fingerprint" TEXT NOT NULL, "result_id" TEXT NOT NULL,
 "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE "StorageDeletion" (
 "object_key" TEXT PRIMARY KEY, "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- File deletion is transactional; object deletion is retried independently after commit.
CREATE FUNCTION enqueue_storage_deletion() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 INSERT INTO "StorageDeletion" ("object_key") VALUES (OLD."path") ON CONFLICT DO NOTHING;
 RETURN OLD;
END;
$$;
CREATE TRIGGER file_storage_deletion AFTER DELETE ON "File" FOR EACH ROW EXECUTE FUNCTION enqueue_storage_deletion();

