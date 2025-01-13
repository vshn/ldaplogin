package entities;

import java.util.Objects;

public class Service implements Comparable<Service> {
    private final String id;
    private final byte[] passwordHash;
    private final String name;
    private final String url;
    private final String group;
    private final boolean hasStaticPasswords;
    private final long dynamicPasswordExpires;

    public Service(String id, byte[] passwordHash, String name, String url, String group, boolean hasStaticPasswords, long dynamicPasswordExpires) {
        this.id = id;
        this.passwordHash = passwordHash;
        this.name = name;
        this.url = url;
        this.group = group;
        this.hasStaticPasswords = hasStaticPasswords;
        this.dynamicPasswordExpires = dynamicPasswordExpires;
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

    public boolean hasStaticPasswords() {
        return hasStaticPasswords;
    }

    public long getDynamicPasswordExpires() {
        return dynamicPasswordExpires;
    }

    @Override
    public int compareTo(Service service) {
        return getName().compareTo(service.getName());
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Service service)) return false;
        return Objects.equals(id, service.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
