package com.realestate.properties.application;

import com.realestate.addresses.application.AddressService;
import com.realestate.properties.domain.Property;
import com.realestate.shared.application.*;
import com.realestate.shared.domain.*;
import com.realestate.uploads.application.UploadService;
import com.realestate.uploads.domain.Upload;
import com.realestate.users.application.*;
import com.realestate.users.domain.User;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PropertyService {
    private static final Set<String> ADDRESS_FIELDS =
            Set.of(
                    "house_number",
                    "building_name",
                    "street",
                    "town",
                    "city",
                    "postal_code",
                    "description",
                    "notes");
    private final PropertyRepository properties;
    private final UserRepository users;
    private final LandlordRepository landlords;
    private final AddressService addresses;
    private final UploadService uploads;
    private final Clock clock;

    public PropertyService(
            PropertyRepository properties,
            UserRepository users,
            LandlordRepository landlords,
            AddressService addresses,
            UploadService uploads,
            Clock clock) {
        this.properties = properties;
        this.users = users;
        this.landlords = landlords;
        this.addresses = addresses;
        this.uploads = uploads;
        this.clock = clock;
    }

    public Property get(String id) {
        return properties.require(Rules.id(id));
    }

    @Transactional
    public Property create(Changes data, List<Upload> images) {
        return save(UUID.randomUUID().toString(), data, null, images);
    }

    @Transactional
    public Property update(String id, Changes data, List<Upload> images) {
        return save(id, data, properties.requireLocked(Rules.id(id)), images);
    }

    private Property save(String id, Changes data, Property old, List<Upload> images) {
        data.only(
                "name",
                "size",
                "price",
                "type",
                "status",
                "landlord_id",
                "address_id",
                "house_number",
                "building_name",
                "street",
                "town",
                "city",
                "postal_code",
                "description",
                "notes");
        String landlordId = data.string("landlord_id", old == null ? null : old.landlordId());
        var landlord = users.require(Rules.id(landlordId));
        if (!landlord.type().equals("LANDLORD") || landlords.find(landlordId).isEmpty())
            throw BusinessException.invalid("Property owner must be a landlord");
        if (old == null && !data.has("size")) throw BusinessException.invalid("Size is required");
        var property =
                properties.save(
                        new Property(
                                id,
                                data.string("name", old == null ? null : old.name()),
                                data.decimal(
                                                "size",
                                                old == null ? null : BigDecimal.valueOf(old.size()))
                                        .doubleValue(),
                                data.decimal("price", old == null ? null : old.price()),
                                data.string("type", old == null ? "HOUSE" : old.type()),
                                data.string("status", old == null ? "SELL" : old.status()),
                                landlordId,
                                old == null ? clock.instant() : old.createdAt(),
                                clock.instant()));
        if (data.has("address_id")) addresses.link(id, data.string("address_id", null));
        else if (ADDRESS_FIELDS.stream().anyMatch(data::has)) {
            var fields = new LinkedHashMap<String, Object>();
            data.values()
                    .forEach(
                            (k, v) -> {
                                if (ADDRESS_FIELDS.contains(k)) fields.put(k, v);
                            });
            addresses.link(id, addresses.create(new Changes(fields)).id());
        }
        if (!images.isEmpty()) uploads.attach(id, images, false);
        return property;
    }

    public PageResult<Property> search(
            String location,
            String landlordId,
            String tenantId,
            int page,
            int size,
            String sort,
            boolean descending) {
        return properties.search(location, landlordId, tenantId, page, size, sort, descending);
    }

    @Transactional
    public boolean delete(String id) {
        properties.requireLocked(Rules.id(id));
        properties.delete(id);
        return true;
    }

    @Transactional
    public Property attach(String id, List<Upload> files, boolean document) {
        var property = properties.requireLocked(Rules.id(id));
        uploads.attach(id, files, document);
        return property;
    }

    public List<Map<String, Object>> views(List<Property> values, boolean publicView) {
        var ids = values.stream().map(Property::id).toList();
        var addressMap = addresses.forProperties(ids);
        var imageMap = uploads.forProperties(ids, false);
        var documentMap =
                publicView
                        ? Map.<String, List<Map<String, Object>>>of()
                        : uploads.forProperties(ids, true);
        var owners = new HashMap<String, User>();
        if (!values.isEmpty())
            users.query(
                            Query.where(
                                            "id",
                                            values.stream()
                                                    .map(Property::landlordId)
                                                    .distinct()
                                                    .toList())
                                    .limit(10000))
                    .forEach(u -> owners.put(u.id(), u));
        return values.stream()
                .map(
                        p -> {
                            var view = summary(p);
                            var owner = owners.get(p.landlordId());
                            view.put(
                                    "landlord",
                                    publicView
                                            ? Data.map("id", p.landlordId())
                                            : UserService.contact(owner));
                            var addresses = addressMap.getOrDefault(p.id(), List.of());
                            view.put(
                                    "property_address",
                                    publicView
                                            ? addresses.stream()
                                                    .map(
                                                            a ->
                                                                    Data.map(
                                                                            "id",
                                                                            a.id(),
                                                                            "house_number",
                                                                            a.houseNumber(),
                                                                            "building_name",
                                                                            a.buildingName(),
                                                                            "street",
                                                                            a.street(),
                                                                            "town",
                                                                            a.town(),
                                                                            "city",
                                                                            a.city(),
                                                                            "postal_code",
                                                                            a.postalCode(),
                                                                            "created_at",
                                                                            a.createdAt(),
                                                                            "updated_at",
                                                                            a.updatedAt()))
                                                    .toList()
                                            : addresses);
                            var images = imageMap.getOrDefault(p.id(), List.of());
                            view.put("property_image", images);
                            if (!images.isEmpty()) view.put("primary_image", images.getFirst());
                            view.put(
                                    "property_document",
                                    documentMap.getOrDefault(p.id(), List.of()));
                            return view;
                        })
                .toList();
    }

    public Map<String, Object> view(String id, boolean publicView) {
        return views(List.of(get(id)), publicView).getFirst();
    }

    public static Map<String, Object> summary(Property p) {
        return Data.map(
                "id",
                p.id(),
                "name",
                p.name(),
                "size",
                p.size(),
                "price",
                p.price(),
                "type",
                p.type(),
                "status",
                p.status(),
                "created_at",
                p.createdAt(),
                "updated_at",
                p.updatedAt());
    }
}
