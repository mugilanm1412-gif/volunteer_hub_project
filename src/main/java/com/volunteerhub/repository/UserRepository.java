package com.volunteerhub.repository;

import com.volunteerhub.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for User entity operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE " +
           "(:query IS NULL OR :query = '' OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:status IS NULL OR u.status = :status)")
    org.springframework.data.domain.Page<User> searchUsers(
           @org.springframework.data.repository.query.Param("query") String query,
           @org.springframework.data.repository.query.Param("role") com.volunteerhub.entity.Role role,
           @org.springframework.data.repository.query.Param("status") com.volunteerhub.entity.UserStatus status,
           org.springframework.data.domain.Pageable pageable);

    long countByRole(com.volunteerhub.entity.Role role);

    long countByStatus(com.volunteerhub.entity.UserStatus status);
}
