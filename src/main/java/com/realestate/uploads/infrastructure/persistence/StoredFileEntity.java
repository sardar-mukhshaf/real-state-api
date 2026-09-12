package com.realestate.uploads.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "\"File\"")
public class StoredFileEntity {
    protected StoredFileEntity() {}

    @Id
    @Column(name = "\"id\"", nullable = false, columnDefinition = "text")
    String id;

    @Column(name = "\"name\"", nullable = false, columnDefinition = "text")
    String name;

    @Column(name = "\"path\"", nullable = false, columnDefinition = "text")
    String path;

    @Column(name = "\"type\"", nullable = false, columnDefinition = "text")
    String type;

    @Column(name = "\"size\"", nullable = false)
    int size;

    @Column(name = "\"created_at\"", nullable = false, columnDefinition = "timestamp(3)")
    Instant createdAt;

    @Column(name = "\"updated_at\"", nullable = false, columnDefinition = "timestamp(3)")
    Instant updatedAt;
}
