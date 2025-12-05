package com.roger.auth_service.exception;

public class ProjectNotFoundException extends RuntimeException {
    public ProjectNotFoundException(String projectId) {
        super("Project not found: " + projectId);
    }
}
