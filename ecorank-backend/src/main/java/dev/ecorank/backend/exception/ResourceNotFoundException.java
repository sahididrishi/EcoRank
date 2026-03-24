package dev.ecorank.backend.exception;

public class ResourceNotFoundException extends RuntimeException {

    private final String entityName;
    private final Object identifier;

    public ResourceNotFoundException(String entityName, Object identifier) {
        super(String.format("%s not found with identifier: %s", entityName, identifier));
        this.entityName = entityName;
        this.identifier = identifier;
    }

    public String getEntityName() {
        return entityName;
    }

    public Object getIdentifier() {
        return identifier;
    }
}
