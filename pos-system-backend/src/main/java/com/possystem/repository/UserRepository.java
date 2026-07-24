package com.possystem.repository;

import com.possystem.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = "role")
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsernameAndIdNot(String username, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);

    @EntityGraph(attributePaths = "role")
    List<User> findByRole_Name(String roleName);

    @EntityGraph(attributePaths = "role")
    Optional<User> findByIdAndRole_Name(Long id, String roleName);

}
