package com.smartstock.identity.domain.repository;

import com.smartstock.identity.domain.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {

    @Query("SELECT t FROM PasswordResetToken t WHERE t.token = :token AND t.used = false")
    Optional<PasswordResetToken> findValidToken(@Param("token") String token);

    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.used = true WHERE t.userId = :userId")
    void invalidateAllForUser(@Param("userId") String userId);
}
