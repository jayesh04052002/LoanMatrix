package com.example.LoanMatrix.repository.authentication;

import com.example.LoanMatrix.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Role findByRoleName(String roleName);
}