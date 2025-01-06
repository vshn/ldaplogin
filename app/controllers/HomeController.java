package controllers;

import entities.NotAllowedException;
import entities.Service;
import entities.User;
import org.apache.directory.api.ldap.model.constants.LdapSecurityConstants;
import org.apache.directory.api.ldap.model.password.PasswordUtil;
import play.libs.Json;
import play.mvc.*;
import services.OpenId;
import store.ServicesStore;
import store.UsersStore;
import util.CustomLogger;
import util.InputUtils;

import javax.inject.Inject;
import java.time.Duration;
import java.util.List;

public class HomeController extends Controller {

    @Inject
    private UsersStore usersStore;

    @Inject
    private ServicesStore servicesStore;

    @Inject
    private OpenId openId;

    private final CustomLogger logger = new CustomLogger(this.getClass());

    public Result index(Http.Request request) {
        User user = usersStore.getFromRequest(request, openId);
        if (user == null) {
            return LoginController.loginFirst(request);
        }

        List<Service> services = ServicesStore.getServicesByUser(user);
        return ok(views.html.index.render(services));
    }

    public Result dynamicPwGen(Http.Request request) {
        User user = usersStore.getFromRequest(request, openId);
        String serviceId = InputUtils.trimToNull(request.body().asFormUrlEncoded().get("serviceId"));
        Service service = ServicesStore.getServicesByUser(user).stream().filter(s -> s.getId().equals(serviceId)).findFirst().orElse(null);
        if (user == null || service == null) {
            throw new NotAllowedException();
        }

        String password = usersStore.generateDynamicPassword(user, service);
        return ok(Json.newObject().put("pw", password));
    }

    public Result staticPwGen(Http.Request request) {
        User user = usersStore.getFromRequest(request, openId);
        String serviceId = InputUtils.trimToNull(request.body().asFormUrlEncoded().get("serviceId"));
        Service service = ServicesStore.getServicesByUser(user).stream().filter(s -> s.getId().equals(serviceId)).findFirst().orElse(null);
        if (user == null || service == null || !service.hasStaticPasswords()) {
            throw new NotAllowedException();
        }

        String password = usersStore.generateStaticPassword(user, service);
        return ok(Json.newObject().put("pw", password));
    }
}
