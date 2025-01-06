package entities;

import java.util.Map;

public class GroupsMetadata {
    private final String path;
    private final String description;
    private final String email;

    public GroupsMetadata(Map<String, String> metadata) {
        path = metadata.get("path");
        description = metadata.get("description");
        email = metadata.get("email");
    }

    public String getPath() {
        return path;
    }

    public String getDescription() {
        return description;
    }

    public String getEmail() {
        return email;
    }
}
