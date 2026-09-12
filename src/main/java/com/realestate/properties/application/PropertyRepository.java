package com.realestate.properties.application;

import com.realestate.properties.domain.Property;
import com.realestate.shared.application.*;

public interface PropertyRepository extends Store<Property> {
    PageResult<Property> search(
            String location,
            String landlordId,
            String tenantId,
            int page,
            int size,
            String sort,
            boolean descending);
}
