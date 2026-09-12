package com.realestate.users.infrastructure.persistence;

import com.realestate.shared.infrastructure.persistence.JpaStore;
import com.realestate.users.application.UserRepository;
import com.realestate.users.domain.User;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
public class UserAdapter extends JpaStore<User, UserEntity> implements UserRepository {
    public UserAdapter(UserJpaRepository repository, EntityManager em) {
        super(repository, em, UserEntity.class, UserMapper::toDomain, UserMapper::toEntity);
    }
}
