package com.example.LoanMatrix.repository;

import com.example.LoanMatrix.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    User findByEmail(String email);


    @EntityGraph(attributePaths = {"role"})
    User findWithRoleByEmail(String email);
}
