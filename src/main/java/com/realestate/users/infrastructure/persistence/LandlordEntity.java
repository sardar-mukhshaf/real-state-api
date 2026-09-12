package com.realestate.users.infrastructure.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "\"Landlord\"")
public class LandlordEntity {
    protected LandlordEntity() {}

    @Id
    @Column(name = "\"id\"", nullable = false, columnDefinition = "text")
    String id;
}
