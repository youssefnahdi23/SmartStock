package com.smartstock.identity.domain.repository;

import com.smartstock.identity.domain.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.token = :token AND rt.revoked = false")
    Optional<RefreshToken> findByTokenAndNotRevoked(@Param("token") String token);

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.userId = :userId AND rt.revoked = false")
    Optional<RefreshToken> findActiveTokenByUserId(@Param("userId") String userId);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.userId = :userId")
    void revokeAllByUserId(@Param("userId") String userId);
}
