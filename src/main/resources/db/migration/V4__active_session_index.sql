CREATE INDEX refresh_active_family_idx ON "RefreshSession" (family_id,user_id,expires_at) WHERE revoked_at IS NULL;

