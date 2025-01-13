package entities.memory;

import entities.Group;

public class MemoryGroup implements Group {
    private String path;

    private String description;

    private String email;

    public MemoryGroup() {
        // dummy constructor for Morphia
    }

    public MemoryGroup(String path) {
        this.path = path;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String getPath() {
        return path;
    }
}
