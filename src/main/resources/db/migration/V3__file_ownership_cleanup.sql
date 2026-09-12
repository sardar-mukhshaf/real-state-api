-- Delete a formerly attached file only after its last property reference disappears.
CREATE FUNCTION cleanup_detached_property_file() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 DELETE FROM "File" f WHERE f.id=OLD.file_id
   AND NOT EXISTS (SELECT 1 FROM "PropertyImage" i WHERE i.file_id=f.id)
   AND NOT EXISTS (SELECT 1 FROM "PropertyDocument" d WHERE d.file_id=f.id);
 RETURN OLD;
END;
$$;
CREATE TRIGGER image_file_cleanup AFTER DELETE ON "PropertyImage" FOR EACH ROW EXECUTE FUNCTION cleanup_detached_property_file();
CREATE TRIGGER document_file_cleanup AFTER DELETE ON "PropertyDocument" FOR EACH ROW EXECUTE FUNCTION cleanup_detached_property_file();

