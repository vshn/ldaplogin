package util;

import java.util.*;
import java.util.stream.Collectors;

public class Config {
    public enum Option {
        LDAP_BASE_DOMAIN, //
        LDAP_BASE_PARTITION_NAME, //
        LDAP_TLS_PRIVATE_KEY, //
        LDAP_TLS_CERTIFICATE, //
        LDAP_PLAIN_PORT, //
        LDAP_TLS_PORT, //
        LDAP_GROUPS_SCOPE, //
        MONGODB_ENABLE, //
        MONGODB_DATABASE, //
        MONGODB_HOSTNAME, //
        MONGODB_USERNAME, //
        MONGODB_PASSWORD, //
        MONGODB_DISABLE_TLS, //
        MONGODB_ENCRYPTION_KEY, //
        OPENID_CLIENT_ID, //
        OPENID_SECRET, //
        OPENID_URL_REALM, //
        OPENID_URL_TOKEN, //
        OPENID_URL_AUTH, //
        SERVICES, //
        USER_DYNAMIC_PASSWORD_EXPIRES, //
        USER_EXPIRES, //
        USER_NEVER_EXPIRES_GROUPS, //
        USER_SESSION_EXPIRES; //

        public String get() {
            return Config.get(this);
        }

        public Integer getInteger() {
            return Config.getInteger(this);
        }

        public Long getLong() {
            return Config.getLong(this);
        }


        public List<String> getStringList() {
            return Config.getStringList(this);
        }

        public Boolean getBoolean() {
            return Config.getBoolean(this);
        }
    }

    private static final Map<Config.Option, String> config;

    static {
        Map<String, String> env = System.getenv();

        Map<Config.Option, String> cfg = new HashMap<>();

        // Set some defaults which mostly help with running tests
        cfg.put(Option.LDAP_BASE_DOMAIN, "com");
        cfg.put(Option.LDAP_BASE_PARTITION_NAME, "example");
        cfg.put(Option.LDAP_PLAIN_PORT, "10389");
        cfg.put(Option.LDAP_TLS_PORT, "10636");
        cfg.put(Option.LDAP_GROUPS_SCOPE, "");
        cfg.put(Option.MONGODB_ENABLE, "false");
        cfg.put(Option.MONGODB_DATABASE, "ldaplogin");
        cfg.put(Option.MONGODB_HOSTNAME, "localhost");
        cfg.put(Option.MONGODB_USERNAME, "ldaplogin");
        cfg.put(Option.USER_DYNAMIC_PASSWORD_EXPIRES, "" + 60L * 60L); // 1 hour by default
        cfg.put(Option.USER_EXPIRES, "" + 60L * 60L * 24L * 365L); // 1 year by default
        cfg.put(Option.USER_SESSION_EXPIRES, "" + 60L * 60L * 24L * 30L); // 30 days by default


        for (Option o : Option.values()) {
            if (env.containsKey(o.name())) {
                cfg.put(o, env.get(o.name()));
            }
        }

        config = Collections.unmodifiableMap(cfg);
    }

    public static final String SCOPE;
    static {
        // We need to normalize the configuration value for reliable results
        String scope = Option.LDAP_GROUPS_SCOPE.get();
        if (scope == null || scope.isBlank()) {
            scope = "/"; // We don't accept groups that don't start with a '/'
        } else {
            if (!scope.startsWith("/")) {
                scope = "/" + scope;
            }
            if (!scope.endsWith("/")) {
                scope = scope + "/";
            }
        }
        SCOPE = scope;
    }

    public static String get(Option o) {
        return config.get(o);
    }

    public static Integer getInteger(Option o) {
        if (!config.containsKey(o)) {
            return null;
        }
        try {
            return Integer.parseInt(config.get(o));
        } catch (Exception e) {
            throw new RuntimeException("Can't parse option " + o + ": " + e.getMessage());
        }
    }

    public static Long getLong(Option o) {
        if (!config.containsKey(o)) {
            return null;
        }
        try {
            return Long.parseLong(config.get(o));
        } catch (Exception e) {
            throw new RuntimeException("Can't parse option " + o + ": " + e.getMessage());
        }
    }

    public static List<String> getStringList(Option o) {
        if (!config.containsKey(o)) {
            return Collections.emptyList();
        }
        return Arrays.asList(config.get(o).split(",")).stream().map(String::trim).collect(Collectors.toList());
    }

    public static boolean getBoolean(Option o) {
        return Boolean.parseBoolean(get(o));
    }
}
