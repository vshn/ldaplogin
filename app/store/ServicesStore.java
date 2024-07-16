package store;

import entities.Service;
import entities.User;
import org.apache.directory.api.ldap.model.constants.LdapSecurityConstants;
import org.apache.directory.api.ldap.model.password.PasswordUtil;
import util.Config;
import util.CustomLogger;
import util.InputUtils;

import java.util.*;
import java.util.stream.Collectors;

public class ServicesStore {
    private static final CustomLogger logger = new CustomLogger(new ServicesStore().getClass());

    private static final List<Service> services;

    static {
        List<Service> srvs = new ArrayList<>();

        Map<String, String> env = System.getenv();
        List<String> serviceIds = Config.Option.SERVICES.getStringList();

        for (String serviceId : serviceIds) {
            Service service = fromEnv(env, serviceId);
            if (service == null) {
                logger.warn(null, "Could Service '" + serviceId + "' configuration is incomplete. Check env variables " + "SERVICE_" + serviceId + "_[PASSWORD,NAME,URL,GROUP]");
                continue;
            }
            srvs.add(service);
        }

        if (srvs.isEmpty()) {
            logger.warn(null, "No services configured. Env variable '" + Config.Option.SERVICES.name() + "' must contain a comma-separated list of service IDs.");
            logger.warn(null, "For every service ID in '" + Config.Option.SERVICES.name() + "' the following env variables must be present and set ($SERVICEID is upper case):");
            logger.warn(null, "  * SERICE_$SERVICEID_PASSWORD  (plain-text LDAP login password)");
            logger.warn(null, "  * SERICE_$SERVICEID_NAME      (user-friendly name)");
            logger.warn(null, "  * SERICE_$SERVICEID_URL       (link to login form)");
            logger.warn(null, "  * SERICE_$SERVICEID_GROUP     (OpenID group required for users to log in to this service, optional)");
        }

        logger.info(null, "Configured " + srvs.size() + " services");

        Collections.sort(srvs);
        services = Collections.unmodifiableList(srvs);
    }

    private static Service fromEnv(Map<String, String> env, String serviceId) {
        serviceId = serviceId.toUpperCase(Locale.ENGLISH);
        String password = InputUtils.trimToNull(env.get("SERVICE_" + serviceId + "_PASSWORD"));
        String name = InputUtils.trimToNull(env.get("SERVICE_" + serviceId + "_NAME"));
        String url = InputUtils.trimToNull(env.get("SERVICE_" + serviceId + "_URL"));
        String group = InputUtils.trimToNull(env.get("SERVICE_" + serviceId + "_GROUP"));
        if (password == null || name == null || url == null) {
            return null;
        }
        return new Service(serviceId.toLowerCase(Locale.ENGLISH), PasswordUtil.createStoragePassword(password, LdapSecurityConstants.HASH_METHOD_SSHA512), name, url, group);
    }

    public static List<Service> getServicesByUser(User user) {
        return services.stream().filter(service -> user.getGroupPaths().contains(service.getGroup())).collect(Collectors.toList());
    }

    public static List<Service> getAll() {
        return services;
    }
}
