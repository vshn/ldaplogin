package entities;

public class Service implements Comparable<Service> {
    private final String id;
    private final byte[] passwordHash;
    private final String name;
    private final String url;
    private final String group;

    public Service(String id, byte[] passwordHash, String name, String url, String group) {
        this.id = id;
        this.passwordHash = passwordHash;
        this.name = name;
        this.url = url;
        this.group = group;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getGroup() {
        return group;
    }

    public byte[] getPasswordHash() {
        return passwordHash;
    }

    @Override
    public int compareTo(Service service) {
        return getName().compareTo(service.getName());
    }

    @Override
    public String toString() {
        return getName();
    }
}
