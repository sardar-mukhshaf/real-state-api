package com.realestate.uploads.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "\"PropertyDocument\"")
public class PropertyDocumentEntity {
    protected PropertyDocumentEntity() {}

    @Id
    @Column(name = "\"id\"", nullable = false, columnDefinition = "text")
    String id;

    @Column(name = "\"property_id\"", nullable = false, columnDefinition = "text")
    String propertyId;

    @Column(name = "\"file_id\"", nullable = false, columnDefinition = "text")
    String fileId;

    @Column(name = "\"document_type\"", nullable = false, columnDefinition = "text")
    String documentType;

    @Column(name = "\"description\"", nullable = false, columnDefinition = "text")
    String description;

    @Column(name = "\"notes\"", nullable = false, columnDefinition = "text")
    String notes;

    @Column(name = "\"created_at\"", nullable = false, columnDefinition = "timestamp(3)")
    Instant createdAt;

    @Column(name = "\"updated_at\"", nullable = false, columnDefinition = "timestamp(3)")
    Instant updatedAt;
}
