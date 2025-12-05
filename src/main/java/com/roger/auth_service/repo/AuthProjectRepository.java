package com.roger.auth_service.repo;

import com.roger.auth_service.entity.AuthProject;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthProjectRepository extends JpaRepository<AuthProject, String> {
}
