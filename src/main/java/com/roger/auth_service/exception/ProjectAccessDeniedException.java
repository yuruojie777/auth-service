package com.roger.auth_service.exception;

public class ProjectAccessDeniedException extends RuntimeException {
    public ProjectAccessDeniedException(String projectId) {
        super("User has no access to project: " + projectId);
    }
}
