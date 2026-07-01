package com.smartstock.identity.domain.repository;

import com.smartstock.identity.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    @Query("SELECT u FROM User u WHERE u.username = :username AND u.deletedAt IS NULL")
    Optional<User> findByUsernameAndNotDeleted(@Param("username") String username);

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<User> findByEmailAndNotDeleted(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.id = :id AND u.deletedAt IS NULL")
    Optional<User> findByIdAndNotDeleted(@Param("id") String id);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u " +
           "WHERE u.email = :email AND u.deletedAt IS NULL")
    boolean existsByEmailAndNotDeleted(@Param("email") String email);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u " +
           "WHERE u.username = :username AND u.deletedAt IS NULL")
    boolean existsByUsernameAndNotDeleted(@Param("username") String username);

    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL " +
           "AND (:role IS NULL OR EXISTS (SELECT r FROM u.roles r WHERE r.name = :role)) " +
           "AND (:active IS NULL OR u.active = :active) " +
           "AND (:search IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
           "     OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<User> findAllWithFilters(
            @Param("role")   String role,
            @Param("active") Boolean active,
            @Param("search") String search,
            Pageable pageable);
}
