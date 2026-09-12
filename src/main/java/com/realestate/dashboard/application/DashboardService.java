package com.realestate.dashboard.application;

import com.realestate.properties.application.PropertyRepository;
import com.realestate.shared.application.*;
import com.realestate.transactions.application.RentDetailRepository;
import com.realestate.users.application.*;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService {
    private final UserRepository users;
    private final LandlordRepository landlords;
    private final TenantRepository tenants;
    private final PropertyRepository properties;
    private final RentDetailRepository rents;

    public DashboardService(
            UserRepository users,
            LandlordRepository landlords,
            TenantRepository tenants,
            PropertyRepository properties,
            RentDetailRepository rents) {
        this.users = users;
        this.landlords = landlords;
        this.tenants = tenants;
        this.properties = properties;
        this.rents = rents;
    }

    public Map<String, Object> data() {
        return Data.map(
                "total_users",
                users.count(Query.all()),
                "total_landlords",
                landlords.count(Query.all()),
                "total_tenants",
                tenants.count(Query.all()),
                "total_properties",
                properties.count(Query.all()),
                "total_rentals",
                rents.count(Query.all()));
    }
}
