-- Final Prisma schema, expressed directly. Do not replay destructive historical enum migrations.
CREATE TABLE "User" (
  "id" TEXT NOT NULL PRIMARY KEY,
  "first_name" TEXT NOT NULL,
  "last_name" TEXT NOT NULL,
  "email" TEXT NOT NULL,
  "password" TEXT NOT NULL,
  "email_verified" BOOLEAN DEFAULT false NOT NULL,
  "remember_me" BOOLEAN DEFAULT false NOT NULL,
  "is_active" BOOLEAN DEFAULT true NOT NULL,
  "type" TEXT DEFAULT 'USER' NOT NULL,
  "created_at" TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "updated_at" TIMESTAMP(3) NOT NULL
);

CREATE TABLE "Tenant" (
  "id" TEXT NOT NULL PRIMARY KEY
);

CREATE TABLE "Landlord" (
  "id" TEXT NOT NULL PRIMARY KEY
);

CREATE TABLE "Property" (
  "id" TEXT NOT NULL PRIMARY KEY,
  "name" TEXT NOT NULL,
  "size" DOUBLE PRECISION NOT NULL,
  "price" DOUBLE PRECISION NOT NULL,
  "type" TEXT DEFAULT 'HOUSE' NOT NULL,
  "status" TEXT DEFAULT 'SELL' NOT NULL,
  "created_at" TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "updated_at" TIMESTAMP(3) NOT NULL,
  "landlord_id" TEXT NOT NULL
);

CREATE TABLE "Transaction" (
  "id" TEXT NOT NULL PRIMARY KEY,
  "amount" DOUBLE PRECISION NOT NULL,
  "is_VAT" BOOLEAN DEFAULT false NOT NULL,
  "type" TEXT DEFAULT 'RENT' NOT NULL,
  "description" TEXT NOT NULL,
  "transaction_date" TIMESTAMP(3) NOT NULL,
  "transaction_month" INTEGER NOT NULL,
  "created_at" TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "updated_at" TIMESTAMP(3) NOT NULL,
  "property_id" TEXT NOT NULL
);

CREATE TABLE "LandlordPaymentTransaction" (
  "id" TEXT NOT NULL PRIMARY KEY,
  "status" TEXT DEFAULT 'PENDING' NOT NULL,
  "landlord_id" TEXT NOT NULL,
  "transaction_id" TEXT NOT NULL,
  "created_at" TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "updated_at" TIMESTAMP(3) NOT NULL
);

CREATE TABLE "ExpenseTransaction" (
  "id" TEXT NOT NULL PRIMARY KEY,
  "transaction_id" TEXT NOT NULL,
  "created_at" TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "updated_at" TIMESTAMP(3) NOT NULL
);

CREATE TABLE "ManagementFeeTransaction" (
  "id" TEXT NOT NULL PRIMARY KEY,
  "transaction_id" TEXT NOT NULL,
  "created_at" TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "updated_at" TIMESTAMP(3) NOT NULL
);

CREATE TABLE "TenancyContract" (
  "id" TEXT NOT NULL PRIMARY KEY,
  "tenant_id" TEXT NOT NULL,
  "property_id" TEXT NOT NULL,
  "start_date" TIMESTAMP(3) NOT NULL,
  "end_date" TIMESTAMP(3) NOT NULL,
  "mng_fee_percentage" DOUBLE PRECISION NOT NULL,
  "rent_per_month" DOUBLE PRECISION NOT NULL,
  "created_at" TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "updated_at" TIMESTAMP(3) NOT NULL
);

CREATE TABLE "RentTransaction" (
  "id" TEXT NOT NULL PRIMARY KEY,
  "tenant_id" TEXT NOT NULL,
  "transaction_id" TEXT NOT NULL,
  "tenancy_contract_id" TEXT NOT NULL,
  "start_date" TIMESTAMP(3) NOT NULL,
  "end_date" TIMESTAMP(3) NOT NULL,
  "created_at" TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "updated_at" TIMESTAMP(3) NOT NULL
);

CREATE TABLE "Address" (
  "id" TEXT NOT NULL PRIMARY KEY,
  "house_number" TEXT NOT NULL,
  "building_name" TEXT NOT NULL,
  "street" TEXT NOT NULL,
  "town" TEXT NOT NULL,
  "city" TEXT NOT NULL,
  "postal_code" TEXT NOT NULL,
  "description" TEXT NOT NULL,
  "notes" TEXT NOT NULL,
  "created_at" TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "updated_at" TIMESTAMP(3) NOT NULL
);

CREATE TABLE "PropertyAddress" (
  "id" TEXT NOT NULL PRIMARY KEY,
  "current" BOOLEAN DEFAULT true NOT NULL,
  "property_id" TEXT NOT NULL,
  "address_id" TEXT NOT NULL,
  "created_at" TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "updated_at" TIMESTAMP(3) NOT NULL
);

CREATE TABLE "UserAddress" (
  "id" TEXT NOT NULL PRIMARY KEY,
  "current" BOOLEAN DEFAULT true NOT NULL,
  "user_id" TEXT NOT NULL,
  "address_id" TEXT NOT NULL,
  "created_at" TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "updated_at" TIMESTAMP(3) NOT NULL
);

CREATE TABLE "File" (
  "id" TEXT NOT NULL PRIMARY KEY,
  "name" TEXT NOT NULL,
  "path" TEXT NOT NULL,
  "type" TEXT NOT NULL,
  "size" INTEGER NOT NULL,
  "created_at" TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "updated_at" TIMESTAMP(3) NOT NULL
);

CREATE TABLE "PropertyImage" (
  "id" TEXT NOT NULL PRIMARY KEY,
  "property_id" TEXT NOT NULL,
  "file_id" TEXT NOT NULL,
  "image_type" TEXT NOT NULL,
  "description" TEXT NOT NULL,
  "notes" TEXT NOT NULL,
  "created_at" TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "updated_at" TIMESTAMP(3) NOT NULL
);

CREATE TABLE "PropertyDocument" (
  "id" TEXT NOT NULL PRIMARY KEY,
  "property_id" TEXT NOT NULL,
  "file_id" TEXT NOT NULL,
  "document_type" TEXT NOT NULL,
  "description" TEXT NOT NULL,
  "notes" TEXT NOT NULL,
  "created_at" TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "updated_at" TIMESTAMP(3) NOT NULL
);

CREATE TABLE "CompanyDetails" (
  "id" TEXT NOT NULL PRIMARY KEY,
  "name" TEXT NOT NULL,
  "vat_reg_no" TEXT NOT NULL,
  "created_at" TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "updated_at" TIMESTAMP(3) NOT NULL
);

CREATE UNIQUE INDEX "User_email_key" ON "User" ("email");
ALTER TABLE "Tenant" ADD CONSTRAINT "Tenant_id_fkey" FOREIGN KEY ("id") REFERENCES "User" ("id") ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE "Landlord" ADD CONSTRAINT "Landlord_id_fkey" FOREIGN KEY ("id") REFERENCES "User" ("id") ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE "Property" ADD CONSTRAINT "Property_landlord_id_fkey" FOREIGN KEY ("landlord_id") REFERENCES "Landlord" ("id") ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE "Transaction" ADD CONSTRAINT "Transaction_property_id_fkey" FOREIGN KEY ("property_id") REFERENCES "Property" ("id") ON DELETE CASCADE ON UPDATE CASCADE;
CREATE UNIQUE INDEX "LandlordPaymentTransaction_transaction_id_key" ON "LandlordPaymentTransaction" ("transaction_id");
ALTER TABLE "LandlordPaymentTransaction" ADD CONSTRAINT "LandlordPaymentTransaction_landlord_id_fkey" FOREIGN KEY ("landlord_id") REFERENCES "Landlord" ("id") ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE "LandlordPaymentTransaction" ADD CONSTRAINT "LandlordPaymentTransaction_transaction_id_fkey" FOREIGN KEY ("transaction_id") REFERENCES "Transaction" ("id") ON DELETE CASCADE ON UPDATE CASCADE;
CREATE UNIQUE INDEX "ExpenseTransaction_transaction_id_key" ON "ExpenseTransaction" ("transaction_id");
ALTER TABLE "ExpenseTransaction" ADD CONSTRAINT "ExpenseTransaction_transaction_id_fkey" FOREIGN KEY ("transaction_id") REFERENCES "Transaction" ("id") ON DELETE CASCADE ON UPDATE CASCADE;
CREATE UNIQUE INDEX "ManagementFeeTransaction_transaction_id_key" ON "ManagementFeeTransaction" ("transaction_id");
ALTER TABLE "ManagementFeeTransaction" ADD CONSTRAINT "ManagementFeeTransaction_transaction_id_fkey" FOREIGN KEY ("transaction_id") REFERENCES "Transaction" ("id") ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE "TenancyContract" ADD CONSTRAINT "TenancyContract_tenant_id_fkey" FOREIGN KEY ("tenant_id") REFERENCES "Tenant" ("id") ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE "TenancyContract" ADD CONSTRAINT "TenancyContract_property_id_fkey" FOREIGN KEY ("property_id") REFERENCES "Property" ("id") ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE "RentTransaction" ADD CONSTRAINT "RentTransaction_tenant_id_fkey" FOREIGN KEY ("tenant_id") REFERENCES "Tenant" ("id") ON DELETE CASCADE ON UPDATE CASCADE;
CREATE UNIQUE INDEX "RentTransaction_transaction_id_key" ON "RentTransaction" ("transaction_id");
ALTER TABLE "RentTransaction" ADD CONSTRAINT "RentTransaction_transaction_id_fkey" FOREIGN KEY ("transaction_id") REFERENCES "Transaction" ("id") ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE "RentTransaction" ADD CONSTRAINT "RentTransaction_tenancy_contract_id_fkey" FOREIGN KEY ("tenancy_contract_id") REFERENCES "TenancyContract" ("id") ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE "PropertyAddress" ADD CONSTRAINT "PropertyAddress_property_id_fkey" FOREIGN KEY ("property_id") REFERENCES "Property" ("id") ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE "PropertyAddress" ADD CONSTRAINT "PropertyAddress_address_id_fkey" FOREIGN KEY ("address_id") REFERENCES "Address" ("id") ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE "UserAddress" ADD CONSTRAINT "UserAddress_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "User" ("id") ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE "UserAddress" ADD CONSTRAINT "UserAddress_address_id_fkey" FOREIGN KEY ("address_id") REFERENCES "Address" ("id") ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE "PropertyImage" ADD CONSTRAINT "PropertyImage_property_id_fkey" FOREIGN KEY ("property_id") REFERENCES "Property" ("id") ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE "PropertyImage" ADD CONSTRAINT "PropertyImage_file_id_fkey" FOREIGN KEY ("file_id") REFERENCES "File" ("id") ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE "PropertyDocument" ADD CONSTRAINT "PropertyDocument_property_id_fkey" FOREIGN KEY ("property_id") REFERENCES "Property" ("id") ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE "PropertyDocument" ADD CONSTRAINT "PropertyDocument_file_id_fkey" FOREIGN KEY ("file_id") REFERENCES "File" ("id") ON DELETE CASCADE ON UPDATE CASCADE;
