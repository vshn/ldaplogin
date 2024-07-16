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

import javax.inject.Inject;
import java.util.List;

public class HomeController extends Controller {

    @Inject
    private UsersStore usersStore;

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

    public Result pwgen(Http.Request request) {
        User user = usersStore.getFromRequest(request, openId);
        if (user == null) {
            throw new NotAllowedException();
        }
        String password = usersStore.generatePassword(user);
        return ok(Json.newObject().put("pw", password));
    }
}
