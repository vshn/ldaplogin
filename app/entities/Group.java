package entities;

import java.util.Objects;

public class Group {
    private final String path;

    public Group(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public String getCn() {
        return cnFromPath(getPath());
    }

    public static String cnFromPath(String path) {
        // NOTE: This may produce group collisions. We don't care for now.
        String cn = path;
        if (cn.startsWith("/")) {
            cn = cn.substring(1);
        }
        cn = cn.replace(",", "\\,");
        cn = cn.replace("+", "\\+");
        cn = cn.replace("<", "\\<");
        cn = cn.replace(">", "\\>");
        cn = cn.replace(";", "\\;");

        cn = cn.replace("/", "::");

        cn = cn.replace("\"", " ");
        cn = cn.replace("\r", " ");
        cn = cn.replace("\n", " ");
        cn = cn.replace("=", " ");

        cn = cn.replaceAll("\\s+", " ");
        cn = cn.trim();
        return cn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Group group = (Group) o;
        return Objects.equals(path, group.path);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(path);
    }
}
