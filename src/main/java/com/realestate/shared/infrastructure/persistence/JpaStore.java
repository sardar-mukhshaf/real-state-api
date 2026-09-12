package com.realestate.shared.infrastructure.persistence;

import com.realestate.shared.application.*;
import com.realestate.shared.application.Query;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import java.util.*;
import java.util.function.Function;
import org.springframework.data.jpa.repository.JpaRepository;

public abstract class JpaStore<D, E> implements Store<D> {
    private final JpaRepository<E, String> repository;
    protected final EntityManager em;
    private final Class<E> entityType;
    private final Function<E, D> toDomain;
    private final Function<D, E> toEntity;

    protected JpaStore(
            JpaRepository<E, String> repository,
            EntityManager em,
            Class<E> type,
            Function<E, D> toDomain,
            Function<D, E> toEntity) {
        this.repository = repository;
        this.em = em;
        entityType = type;
        this.toDomain = toDomain;
        this.toEntity = toEntity;
    }

    public Optional<D> find(String id) {
        return repository.findById(id).map(toDomain);
    }

    public Optional<D> lock(String id) {
        E entity = em.find(entityType, id, LockModeType.PESSIMISTIC_WRITE);
        if (entity != null) em.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
        return Optional.ofNullable(entity).map(toDomain);
    }

    public D save(D value) {
        return toDomain.apply(repository.saveAndFlush(toEntity.apply(value)));
    }

    public void delete(String id) {
        repository.deleteById(id);
        repository.flush();
    }

    private Predicate[] predicates(Query query, CriteriaBuilder cb, Root<E> root) {
        var list = new ArrayList<Predicate>();
        query.equal()
                .forEach(
                        (key, value) ->
                                list.add(
                                        value instanceof Collection<?> values
                                                ? root.get(key).in(values)
                                                : cb.equal(root.get(key), value)));
        query.contains()
                .forEach(
                        (key, value) ->
                                list.add(
                                        cb.like(
                                                cb.lower(root.get(key)),
                                                "%"
                                                        + value.toLowerCase(Locale.ROOT)
                                                                .replace("!", "!!")
                                                                .replace("%", "!%")
                                                                .replace("_", "!_")
                                                        + "%",
                                                '!')));
        return list.toArray(Predicate[]::new);
    }

    public List<D> query(Query query) {
        var cb = em.getCriteriaBuilder();
        var cq = cb.createQuery(entityType);
        var root = cq.from(entityType);
        cq.where(predicates(query, cb, root));
        cq.orderBy(
                query.descending()
                        ? cb.desc(root.get(query.order()))
                        : cb.asc(root.get(query.order())),
                cb.asc(root.get("id")));
        return em
                .createQuery(cq)
                .setFirstResult(query.offset())
                .setMaxResults(query.size())
                .getResultList()
                .stream()
                .map(toDomain)
                .toList();
    }

    public long count(Query query) {
        var cb = em.getCriteriaBuilder();
        var cq = cb.createQuery(Long.class);
        var root = cq.from(entityType);
        cq.select(cb.count(root)).where(predicates(query, cb, root));
        return em.createQuery(cq).getSingleResult();
    }
}
