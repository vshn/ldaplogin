package services;

import com.google.inject.Inject;
import com.google.inject.Injector;
import play.inject.ApplicationLifecycle;
import services.ldap.EmbeddedLdapServer;

import java.util.concurrent.CompletableFuture;

public class Ldap {
    private static EmbeddedLdapServer s;

    @Inject
    public Ldap(Injector injector, ApplicationLifecycle appLifecycle) {
        s = new EmbeddedLdapServer();
        injector.injectMembers(s);

        appLifecycle.addStopHook(()  -> {
            s.destroy();
            return CompletableFuture.completedFuture(null);
        });

        try {
            s.init();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
