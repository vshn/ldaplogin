package entities;

import util.Config;

public interface Group {
    String getDescription();
    String getEmail();
    String getPath();

    default String getCn() {
        return Group.cnFromPath(getPath());
    }

    static boolean isInScope(Group group) {
        return group == null ? null : isInScope(group.getPath());
    }

    static boolean isInScope(String path) {
        if (path == null) {
            return false;
        }
        return path.startsWith(Config.SCOPE) && path.length() > Config.SCOPE.length();
    }

    static String cnFromPath(String path) {
        // NOTE: This may produce group collisions. We don't care for now.
        if (!isInScope(path)) {
            return null;
        }
        String cn = path.substring(Config.SCOPE.length());

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
}
