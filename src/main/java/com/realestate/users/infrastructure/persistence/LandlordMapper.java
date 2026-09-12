package com.realestate.users.infrastructure.persistence;

import com.realestate.users.domain.Landlord;

public final class LandlordMapper {
    private LandlordMapper() {}

    public static Landlord toDomain(LandlordEntity entity) {
        return new Landlord(entity.id);
    }

    public static LandlordEntity toEntity(Landlord value) {
        var entity = new LandlordEntity();
        entity.id = value.id();
        return entity;
    }
}
