package com.realestate.auth.infrastructure.persistence;

import com.realestate.auth.application.RefreshSessionRepository;
import com.realestate.auth.domain.RefreshSession;
import com.realestate.shared.infrastructure.persistence.JpaStore;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.springframework.stereotype.Repository;

@Repository
public class RefreshSessionAdapter extends JpaStore<RefreshSession, RefreshSessionEntity>
        implements RefreshSessionRepository {
    public RefreshSessionAdapter(RefreshSessionJpaRepository repository, EntityManager em) {
        super(
                repository,
                em,
                RefreshSessionEntity.class,
                RefreshSessionMapper::toDomain,
                RefreshSessionMapper::toEntity);
    }

    public void revokeFamily(String family, Instant now) {
        em.createQuery(
                        "update RefreshSessionEntity s set s.revokedAt=:now, s.updatedAt=:now where s.familyId=:family and s.revokedAt is null")
                .setParameter("now", now)
                .setParameter("family", family)
                .executeUpdate();
        em.clear();
    }

    public void revokeUser(String userId, Instant now) {
        em.createQuery(
                        "update RefreshSessionEntity s set s.revokedAt=:now, s.updatedAt=:now where s.userId=:user and s.revokedAt is null")
                .setParameter("now", now)
                .setParameter("user", userId)
                .executeUpdate();
        em.clear();
    }

    public boolean familyActive(String family, String userId, Instant now) {
        return em.createQuery(
                                "select count(s) from RefreshSessionEntity s where s.familyId=:family and s.userId=:user and s.revokedAt is null and s.expiresAt>:now",
                                Long.class)
                        .setParameter("family", family)
                        .setParameter("user", userId)
                        .setParameter("now", now)
                        .getSingleResult()
                > 0;
    }
}
