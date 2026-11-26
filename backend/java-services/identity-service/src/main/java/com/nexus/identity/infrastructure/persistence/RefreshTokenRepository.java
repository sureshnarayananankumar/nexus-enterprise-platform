package com.nexus.identity.infrastructure.persistence;

import com.nexus.identity.domain.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /** Row lock prevents two concurrent refreshes of the same token both succeeding. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RefreshToken t join fetch t.user where t.tokenHash = :hash")
    Optional<RefreshToken> findByHashForUpdate(@Param("hash") String hash);

    @Modifying
    @Query("""
           update RefreshToken t set t.revokedAt = :now, t.revokedReason = :reason
           where t.familyId = :familyId and t.revokedAt is null
           """)
    int revokeFamily(@Param("familyId") UUID familyId, @Param("reason") RefreshToken.RevokeReason reason,
                     @Param("now") Instant now);

    @Modifying
    @Query("""
           update RefreshToken t set t.revokedAt = :now, t.revokedReason = :reason
           where t.user.id = :userId and t.revokedAt is null
           """)
    int revokeAllForUser(@Param("userId") UUID userId, @Param("reason") RefreshToken.RevokeReason reason,
                         @Param("now") Instant now);

    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
