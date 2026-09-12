package com.realestate.uploads.application;

import com.realestate.shared.application.*;
import com.realestate.shared.domain.*;
import com.realestate.uploads.domain.*;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UploadService {
    private final StoredFileRepository files;
    private final PropertyImageRepository images;
    private final PropertyDocumentRepository documents;
    private final FileStorage storage;
    private final TransactionCallbacks callbacks;
    private final Clock clock;

    public UploadService(
            StoredFileRepository files,
            PropertyImageRepository images,
            PropertyDocumentRepository documents,
            FileStorage storage,
            TransactionCallbacks callbacks,
            Clock clock) {
        this.files = files;
        this.images = images;
        this.documents = documents;
        this.storage = storage;
        this.callbacks = callbacks;
        this.clock = clock;
    }

    @Transactional
    public List<StoredFile> upload(List<Upload> uploads) {
        if (uploads.isEmpty() || uploads.size() > 10)
            throw BusinessException.invalid("Upload 1 to 10 files");
        var result = new ArrayList<StoredFile>();
        for (var upload : uploads) {
            String id = UUID.randomUUID().toString(), key = "uploads/" + id;
            callbacks.onRollback(() -> storage.delete(key));
            storage.put(key, upload.bytes(), upload.contentType());
            result.add(
                    files.save(
                            new StoredFile(
                                    id,
                                    upload.name(),
                                    key,
                                    upload.contentType(),
                                    upload.bytes().length,
                                    clock.instant(),
                                    clock.instant())));
        }
        return result;
    }

    @Transactional
    public void attach(String propertyId, List<Upload> uploads, boolean document) {
        if (uploads.isEmpty() || uploads.size() > 5)
            throw BusinessException.invalid("Upload 1 to 5 files per request");
        if (uploads.stream().anyMatch(u -> document ? u.image() : !u.image()))
            throw BusinessException.invalid(
                    document ? "Documents must be PDFs" : "Images must be JPEG, PNG or WebP");
        long existing =
                images.count(Query.where("propertyId", propertyId))
                        + documents.count(Query.where("propertyId", propertyId));
        if (existing + uploads.size() > 100)
            throw BusinessException.invalid("A property supports at most 100 attached files");
        for (var file : upload(uploads)) {
            if (document)
                documents.save(
                        new PropertyDocument(
                                UUID.randomUUID().toString(),
                                propertyId,
                                file.id(),
                                "property",
                                "Property document",
                                "",
                                clock.instant(),
                                clock.instant()));
            else
                images.save(
                        new PropertyImage(
                                UUID.randomUUID().toString(),
                                propertyId,
                                file.id(),
                                "property",
                                "Property image",
                                "",
                                clock.instant(),
                                clock.instant()));
        }
    }

    public StoredFile get(String id) {
        return files.require(Rules.id(id));
    }

    public List<StoredFile> getMany(List<String> ids) {
        if (ids == null || ids.isEmpty() || ids.size() > 100)
            throw BusinessException.invalid("Provide 1 to 100 file IDs");
        ids.forEach(Rules::id);
        return files.query(Query.where("id", ids).limit(100));
    }

    @Transactional
    public boolean delete(String id) {
        files.requireLocked(Rules.id(id));
        files.delete(id);
        return true;
    }

    @Transactional
    public boolean deleteMany(List<String> ids) {
        var found = getMany(ids);
        found.forEach(f -> files.delete(f.id()));
        return !found.isEmpty();
    }

    public Map<String, Object> view(StoredFile file) {
        return Data.map(
                "id",
                file.id(),
                "name",
                file.name(),
                "path",
                storage.downloadUrl(file.path(), !file.type().startsWith("image/")),
                "type",
                file.type(),
                "size",
                file.size(),
                "created_at",
                file.createdAt(),
                "updated_at",
                file.updatedAt());
    }

    public Map<String, List<Map<String, Object>>> forProperties(
            Collection<String> propertyIds, boolean document) {
        if (propertyIds.isEmpty()) return Map.of();
        record Link(
                String id, String propertyId, String fileId, String description, String notes) {}
        List<Link> links =
                document
                        ? documents
                                .query(
                                        Query.where("propertyId", propertyIds)
                                                .sorted("createdAt", false)
                                                .limit(10000))
                                .stream()
                                .map(
                                        d ->
                                                new Link(
                                                        d.id(),
                                                        d.propertyId(),
                                                        d.fileId(),
                                                        d.description(),
                                                        d.notes()))
                                .toList()
                        : images
                                .query(
                                        Query.where("propertyId", propertyIds)
                                                .sorted("createdAt", false)
                                                .limit(10000))
                                .stream()
                                .map(
                                        i ->
                                                new Link(
                                                        i.id(),
                                                        i.propertyId(),
                                                        i.fileId(),
                                                        i.description(),
                                                        i.notes()))
                                .toList();
        if (links.isEmpty()) return Map.of();
        var byId = new HashMap<String, StoredFile>();
        files.query(
                        Query.where("id", links.stream().map(Link::fileId).distinct().toList())
                                .limit(10000))
                .forEach(f -> byId.put(f.id(), f));
        var result = new HashMap<String, List<Map<String, Object>>>();
        for (var link : links) {
            var file = byId.get(link.fileId());
            if (file == null) continue;
            var value = view(file);
            value.put("id", link.id());
            value.put("file_id", file.id());
            value.put("description", link.description());
            value.put("notes", link.notes());
            result.computeIfAbsent(link.propertyId(), k -> new ArrayList<>()).add(value);
        }
        return result;
    }

    public List<Map<String, Object>> propertyImages(String propertyId) {
        return forProperties(List.of(Rules.id(propertyId)), false)
                .getOrDefault(propertyId, List.of())
                .stream()
                .map(
                        v -> {
                            var copy = new LinkedHashMap<>(v);
                            copy.put("id", copy.get("file_id"));
                            return (Map<String, Object>) copy;
                        })
                .toList();
    }
}
