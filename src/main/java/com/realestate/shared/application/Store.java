package com.realestate.shared.application;

import com.realestate.shared.domain.BusinessException;
import java.util.*;

public interface Store<T> {
    Optional<T> find(String id);

    Optional<T> lock(String id);

    T save(T value);

    void delete(String id);

    List<T> query(Query query);

    long count(Query query);

    default T require(String id) {
        return find(id).orElseThrow(() -> BusinessException.missing("Resource"));
    }

    default T requireLocked(String id) {
        return lock(id).orElseThrow(() -> BusinessException.missing("Resource"));
    }

    default Optional<T> first(String field, Object value) {
        return query(Query.where(field, value).limit(1)).stream().findFirst();
    }
}
