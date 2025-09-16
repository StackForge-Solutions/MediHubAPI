package com.MediHubAPI.repository;

import com.MediHubAPI.model.ERole;
import com.MediHubAPI.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

    public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    @Query("SELECT u FROM User u JOIN FETCH u.roles WHERE u.username = :value OR u.email = :value")
    Optional<User> findByUsernameOrEmailWithRoles(@Param("value") String value);
    boolean existsByRoles_Name(ERole roleName);

    // For hospitalId uniqueness during generation
    boolean existsByHospitalId(String hospitalId);
}
