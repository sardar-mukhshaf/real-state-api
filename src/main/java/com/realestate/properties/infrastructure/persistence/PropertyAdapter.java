package com.realestate.properties.infrastructure.persistence;

import com.realestate.properties.application.PropertyRepository;
import com.realestate.properties.domain.Property;
import com.realestate.shared.application.PageResult;
import com.realestate.shared.domain.BusinessException;
import com.realestate.shared.infrastructure.persistence.JpaStore;
import jakarta.persistence.EntityManager;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class PropertyAdapter extends JpaStore<Property, PropertyEntity>
        implements PropertyRepository {
    public PropertyAdapter(PropertyJpaRepository repository, EntityManager em) {
        super(
                repository,
                em,
                PropertyEntity.class,
                PropertyMapper::toDomain,
                PropertyMapper::toEntity);
    }

    public PageResult<Property> search(
            String location,
            String landlordId,
            String tenantId,
            int page,
            int size,
            String sort,
            boolean descending) {
        com.realestate.shared.application.Query.all().page(page, size);
        var sorts =
                Map.of(
                        "id",
                        "p.id",
                        "name",
                        "p.name",
                        "price",
                        "p.price",
                        "size",
                        "p.size",
                        "created_at",
                        "p.created_at");
        if (!sorts.containsKey(sort) && !sort.equals("relevance"))
            throw BusinessException.invalid("Unsupported property sort");
        var params = new LinkedHashMap<String, Object>();
        String where = " WHERE true", score = "p.id";
        if (landlordId != null) {
            where += " AND p.landlord_id=:landlord";
            params.put("landlord", landlordId);
        }
        if (tenantId != null) {
            where +=
                    " AND EXISTS (SELECT 1 FROM \"TenancyContract\" c WHERE c.property_id=p.id AND c.tenant_id=:tenant)";
            params.put("tenant", tenantId);
        }
        if (location != null && !location.isBlank()) {
            if (location.length() > 255) throw BusinessException.invalid("Location is too long");
            String text = location.trim().toLowerCase(Locale.ROOT);
            var words =
                    Arrays.stream(text.split("[\\s,.-]+"))
                            .filter(w -> w.length() > 1)
                            .distinct()
                            .limit(12)
                            .toList();
            if (words.size() <= 1) words = List.of(text);
            String join =
                    " FROM \"PropertyAddress\" pa JOIN \"Address\" a ON a.id=pa.address_id WHERE pa.property_id=p.id";
            String[] fields = {
                "house_number", "building_name", "street", "town", "city", "postal_code"
            };
            var matches = new ArrayList<String>();
            var scoring = new ArrayList<String>();
            String address =
                    "lower(concat_ws(' ',a.house_number,a.building_name,a.street,a.town,a.city,a.postal_code))";
            params.put("phrase", text);
            params.put("phraseLike", "%" + escape(text) + "%");
            scoring.add(
                    "CASE WHEN " + address + " LIKE :phraseLike ESCAPE '!' THEN 100 ELSE 0 END");
            int n = 0;
            for (String word : words) {
                String name = "word" + n++;
                params.put(name, "%" + escape(word) + "%");
                for (String field : fields)
                    matches.add("lower(a." + field + ") LIKE :" + name + " ESCAPE '!'");
                scoring.add(
                        "CASE WHEN "
                                + address
                                + " LIKE :"
                                + name
                                + " ESCAPE '!' THEN 10 ELSE 0 END");
            }
            for (String field : fields)
                matches.add("lower(a." + field + ") LIKE :phraseLike ESCAPE '!'");
            scoring.add(
                    "CASE WHEN a.postal_code<>'' AND position(lower(a.postal_code) in :phrase)>0 THEN 30 ELSE 0 END");
            scoring.add(
                    "CASE WHEN a.house_number<>'' AND position(lower(a.house_number) in :phrase)>0 THEN 20 ELSE 0 END");
            where +=
                    " AND EXISTS (SELECT 1" + join + " AND (" + String.join(" OR ", matches) + "))";
            score = "(SELECT coalesce(max(" + String.join("+", scoring) + "),0)" + join + ")";
        }
        String order =
                sort.equals("relevance")
                        ? score + " DESC"
                        : sorts.get(sort) + (descending ? " DESC" : " ASC");
        var query =
                em.createNativeQuery(
                        "SELECT p.* FROM \"Property\" p" + where + " ORDER BY " + order + ",p.id",
                        PropertyEntity.class);
        var count = em.createNativeQuery("SELECT count(*) FROM \"Property\" p" + where);
        for (var p : params.entrySet()) {
            if (!p.getKey().equals("phrase") || sort.equals("relevance"))
                query.setParameter(p.getKey(), p.getValue());
            if (!p.getKey().equals("phrase")) count.setParameter(p.getKey(), p.getValue());
        }
        @SuppressWarnings("unchecked")
        List<PropertyEntity> entities =
                query.setFirstResult(page * size).setMaxResults(size).getResultList();
        return new PageResult<>(
                entities.stream().map(PropertyMapper::toDomain).toList(),
                page,
                size,
                ((Number) count.getSingleResult()).longValue());
    }

    private String escape(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
