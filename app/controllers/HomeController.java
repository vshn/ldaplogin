package controllers;

import entities.NotAllowedException;
import entities.Service;
import entities.User;
import org.apache.directory.api.ldap.model.password.PasswordUtil;
import org.apache.directory.api.util.Strings;
import play.libs.Json;
import play.mvc.*;
import services.OpenId;
import store.ServicesStore;
import store.UsersStore;
import util.CustomLogger;
import util.InputUtils;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

public class HomeController extends Controller {

    private final CustomLogger logger = new CustomLogger(this.getClass());

    @Inject
    private UsersStore usersStore;

    @Inject
    private ServicesStore servicesStore;

    @Inject
    private OpenId openId;

    public Result index(Http.Request request) {
        User user = usersStore.getFromRequest(request, openId);
        if (user == null) {
            return LoginController.loginFirst(request);
        }

        List<Service> services = ServicesStore.getServicesByUser(user);
        return ok(views.html.index.render(services, user));
    }

    public Result dynamicPwGen(Http.Request request) {
        User user = usersStore.getFromRequest(request, openId);
        String serviceId = InputUtils.trimToNull(request.body().asFormUrlEncoded().get("serviceId"));
        Service service = ServicesStore.getServicesByUser(user).stream().filter(s -> s.getId().equals(serviceId)).findFirst().orElse(null);
        if (user == null || service == null) {
            throw new NotAllowedException();
        }

        String password = usersStore.generateDynamicPassword(user, service);
        logger.info(request, user + " generated new dynamic password for service " + service);
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
        logger.info(request, user + " generated new static password for service " + service);
        return ok(Json.newObject().put("pw", password));
    }

    public Result staticPwCheck(Http.Request request) {
        User user = usersStore.getFromRequest(request, openId);
        Map<String, String[]> data = request.body().asFormUrlEncoded();
        String serviceId = InputUtils.trimToNull(data.get("serviceId"));
        String pw = InputUtils.trimToNull(data.get("pw"));
        if (user == null || serviceId == null || pw == null || user.getServicePasswords(serviceId) == null) {
            return ok(Json.newObject().put("check", false));
        }
        return ok(Json.newObject().put("check", PasswordUtil.compareCredentials(Strings.getBytesUtf8(pw), user.getServicePasswords(serviceId).getStaticPwHash())));
    }
}
