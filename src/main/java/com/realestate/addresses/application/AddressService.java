package com.realestate.addresses.application;

import com.realestate.addresses.domain.*;
import com.realestate.shared.application.*;
import com.realestate.shared.domain.Rules;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AddressService {
    private final AddressRepository addresses;
    private final PropertyAddressRepository links;
    private final Clock clock;

    public AddressService(
            AddressRepository addresses, PropertyAddressRepository links, Clock clock) {
        this.addresses = addresses;
        this.links = links;
        this.clock = clock;
    }

    @Transactional
    public Address create(Changes data) {
        return save(UUID.randomUUID().toString(), data, null);
    }

    @Transactional
    public Address update(String id, Changes data) {
        return save(id, data, addresses.requireLocked(Rules.id(id)));
    }

    private Address save(String id, Changes data, Address old) {
        data.only(
                "house_number",
                "building_name",
                "street",
                "town",
                "city",
                "postal_code",
                "description",
                "notes");
        return addresses.save(
                new Address(
                        id,
                        data.string("house_number", old == null ? null : old.houseNumber()),
                        data.string("building_name", old == null ? "" : old.buildingName()),
                        data.string("street", old == null ? null : old.street()),
                        data.string("town", old == null ? null : old.town()),
                        data.string("city", old == null ? null : old.city()),
                        data.string("postal_code", old == null ? null : old.postalCode()),
                        data.string("description", old == null ? "" : old.description()),
                        data.string("notes", old == null ? "" : old.notes()),
                        old == null ? clock.instant() : old.createdAt(),
                        clock.instant()));
    }

    public Address get(String id) {
        return addresses.require(Rules.id(id));
    }

    public List<Address> list(int page, int size) {
        return addresses.query(Query.all().page(page, size));
    }

    @Transactional
    public boolean delete(String id) {
        addresses.requireLocked(Rules.id(id));
        addresses.delete(id);
        return true;
    }

    @Transactional
    public void link(String propertyId, String addressId) {
        get(addressId);
        for (var old : links.query(Query.where("propertyId", propertyId).limit(10000))) {
            if (old.current())
                links.save(
                        new PropertyAddress(
                                old.id(),
                                false,
                                old.propertyId(),
                                old.addressId(),
                                old.createdAt(),
                                clock.instant()));
        }
        links.save(
                new PropertyAddress(
                        UUID.randomUUID().toString(),
                        true,
                        propertyId,
                        addressId,
                        clock.instant(),
                        clock.instant()));
    }

    public Map<String, List<Address>> forProperties(Collection<String> propertyIds) {
        if (propertyIds.isEmpty()) return Map.of();
        var associations = links.query(Query.where("propertyId", propertyIds).limit(10000));
        var ids = associations.stream().map(PropertyAddress::addressId).distinct().toList();
        if (ids.isEmpty()) return Map.of();
        var byId = new HashMap<String, Address>();
        addresses.query(Query.where("id", ids).limit(10000)).forEach(a -> byId.put(a.id(), a));
        var result = new HashMap<String, List<Address>>();
        associations.stream()
                .sorted(
                        Comparator.comparing(PropertyAddress::current)
                                .reversed()
                                .thenComparing(PropertyAddress::createdAt))
                .forEach(
                        l -> {
                            if (byId.containsKey(l.addressId()))
                                result.computeIfAbsent(l.propertyId(), k -> new ArrayList<>())
                                        .add(byId.get(l.addressId()));
                        });
        return result;
    }
}
