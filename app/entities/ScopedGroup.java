package entities;

import java.util.Objects;

public class ScopedGroup {
    private final Group realGroup;

    private final String path;

    public static ScopedGroup scoped(Group group, String scope) {
        if (group == null || !group.getPath().startsWith(scope)) {
            return null;
        }
        return new ScopedGroup(group, scope);
    }

   private ScopedGroup(Group group, String scope) {
        this.realGroup = group;
        this.path = group.getPath().substring(scope.length());
    }

    public String getPath() {
        return path;
    }

    public String getCn() {
        return Group.cnFromPath(getPath());
    }

    public Group getRealGroup() {
        return realGroup;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScopedGroup group = (ScopedGroup) o;
        return Objects.equals(path, group.path);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(path);
    }
}
