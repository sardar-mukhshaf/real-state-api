package com.realestate.users.application;

import com.realestate.contracts.application.*;
import com.realestate.contracts.domain.TenancyContract;
import com.realestate.properties.application.*;
import com.realestate.properties.domain.Property;
import com.realestate.shared.application.*;
import com.realestate.users.domain.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserViews {
    private final UserService users;
    private final PropertyRepository properties;
    private final TenancyContractRepository contracts;
    private final ContractService contractService;

    public UserViews(
            UserService users,
            PropertyRepository properties,
            TenancyContractRepository contracts,
            ContractService contractService) {
        this.users = users;
        this.properties = properties;
        this.contracts = contracts;
        this.contractService = contractService;
    }

    public Map<String, Object> one(String id) {
        return views(List.of(users.get(id))).getFirst();
    }

    public List<Map<String, Object>> views(List<User> values) {
        if (values.isEmpty()) return List.of();
        var owners =
                values.stream().filter(u -> u.type().equals("LANDLORD")).map(User::id).toList();
        var tenants = values.stream().filter(u -> u.type().equals("TENANT")).map(User::id).toList();
        var ownerProperties =
                owners.isEmpty()
                        ? List.<Property>of()
                        : properties.query(Query.where("landlordId", owners).limit(10000));
        var tenantContracts =
                tenants.isEmpty()
                        ? List.<TenancyContract>of()
                        : contracts.query(Query.where("tenantId", tenants).limit(10000));
        var propertyContracts =
                ownerProperties.isEmpty()
                        ? List.<TenancyContract>of()
                        : contracts.query(
                                Query.where(
                                                "propertyId",
                                                ownerProperties.stream().map(Property::id).toList())
                                        .limit(10000));
        var allContracts = new ArrayList<>(tenantContracts);
        allContracts.addAll(propertyContracts);
        var contractViews = new HashMap<String, Map<String, Object>>();
        contractService
                .views(allContracts)
                .forEach(c -> contractViews.put((String) c.get("id"), c));
        return values.stream()
                .map(
                        u -> {
                            var view = UserService.view(u);
                            if (u.type().equals("LANDLORD"))
                                view.put(
                                        "property",
                                        ownerProperties.stream()
                                                .filter(p -> p.landlordId().equals(u.id()))
                                                .map(
                                                        p -> {
                                                            var item = PropertyService.summary(p);
                                                            item.put(
                                                                    "tenancy_contract",
                                                                    propertyContracts.stream()
                                                                            .filter(
                                                                                    c ->
                                                                                            c.propertyId()
                                                                                                    .equals(
                                                                                                            p
                                                                                                                    .id()))
                                                                            .map(
                                                                                    c ->
                                                                                            contractViews
                                                                                                    .get(
                                                                                                            c
                                                                                                                    .id()))
                                                                            .toList());
                                                            return item;
                                                        })
                                                .toList());
                            if (u.type().equals("TENANT"))
                                view.put(
                                        "tenancy_contract",
                                        tenantContracts.stream()
                                                .filter(c -> c.tenantId().equals(u.id()))
                                                .map(c -> contractViews.get(c.id()))
                                                .toList());
                            return view;
                        })
                .toList();
    }
}
