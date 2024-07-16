package services.ldap;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaErrorHandler;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.registries.SchemaLoader;
import org.apache.directory.api.ldap.schema.loader.SingleLdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.DnFactory;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.slf4j.Logger;
import services.ldap.factories.CustomDirectoryServiceFactory;
import util.Config;
import util.CustomLogger;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class EmbeddedLdapServer {
    @Inject
    private Injector injector;

    private final CustomLogger logger = new CustomLogger(this.getClass());

    private static String BASE_PARTITION_NAME = Config.Option.LDAP_BASE_PARTITION_NAME.get();
    private static String BASE_DOMAIN = Config.Option.LDAP_BASE_DOMAIN.get();
    private static String BASE_STRUCTURE = "dc=" + BASE_PARTITION_NAME + ",dc=" + BASE_DOMAIN;

    private static int PLAIN_PORT = Config.Option.LDAP_PLAIN_PORT.getInteger();
    private static int TLS_PORT = Config.Option.LDAP_TLS_PORT.getInteger();
    private static int BASE_CACHE_SIZE = 1000;

    private DirectoryService _directoryService;
    private LdapServer _ldapServer;
    private OpenIdPartition _basePartition;
    private boolean _deleteInstanceDirectoryOnStartup = true;
    private boolean _deleteInstanceDirectoryOnShutdown = true;

    public String getBasePartitionName() {
        return BASE_PARTITION_NAME;
    }

    public String getBaseStructure() {
        return BASE_STRUCTURE;
    }

    public int getBaseCacheSize() {
        return BASE_CACHE_SIZE;
    }

    public void init() throws Exception {
        if (getDirectoryService() == null) {
            if (getDeleteInstanceDirectoryOnStartup()) {
                deleteDirectory(getGuessedInstanceDirectory());
            }

            CustomDirectoryServiceFactory serviceFactory = new CustomDirectoryServiceFactory();
            serviceFactory.init(getDirectoryServiceName());
            setDirectoryService(serviceFactory.getDirectoryService());

            SchemaManager schemaManager = serviceFactory.getDirectoryService().getSchemaManager();

            // for some reason the default error handler doesn't actually print errors, replace it...
            ((DefaultSchemaManager)schemaManager).setErrorHandler(new SchemaErrorHandler() {
                @Override
                public void handle(Logger logger, String s, Throwable throwable) {
                    logger.warn(s, throwable);
                }

                @Override
                public boolean wasError() {
                    return false;
                }

                @Override
                public List<Throwable> getErrors() {
                    return List.of();
                }

                @Override
                public void reset() {
                }
            });

            // In order to be able to set "memberOf" attributes we need to load a schema extension.
            // This isn't actually the way this should be done, the "memberOf" attribute should be virtual only, but
            // good luck finding out how to implement that.
            SchemaLoader l = new SingleLdifSchemaLoader("conf/ldaplogin.ldif");
            schemaManager.load(l.getSchema("ldaplogin"));

            getDirectoryService().getChangeLog().setEnabled(false);
            getDirectoryService().setDenormalizeOpAttrsEnabled(true);

            // Delete admin user to make login with default credentials impossible.
            // The DefaultDirectoryService still warns during startup about the default password being in use (because
            // it creates the user and then immediately warns about its presence), thus we just hide the warning via the
            // logging configuration.
            // Overall the approach we chose here should be fine though because the admin user gets deleted before the
            // LDAP service is started.
            CoreSession adminSession = getDirectoryService().getAdminSession();
            DnFactory dnFactory = getDirectoryService().getDnFactory();
            Dn adminDn = dnFactory.create(ServerDNConstants.ADMIN_SYSTEM_DN);
            LookupOperationContext lookupEntryContext = new LookupOperationContext(adminSession, adminDn);
            Entry entry = getDirectoryService().getPartitionNexus().lookup(lookupEntryContext);
            DeleteOperationContext deleteContext = new DeleteOperationContext(adminSession, adminDn);
            deleteContext.setEntry(entry); // it's a bit stupid that we need to do a lookup first before we can delete, probably doing something wrong here
            getDirectoryService().getPartitionNexus().delete(deleteContext);
            
            createBasePartition();
            getDirectoryService().startup();
        }

        if (getLdapServer() == null) {
            setLdapServer(new TlsLdapServer());
            getLdapServer().setDirectoryService(getDirectoryService());
            TcpTransport plainTcpTransport = new TcpTransport(PLAIN_PORT);
            TcpTransport tlsTcpTransport = new TcpTransport(TLS_PORT);
            tlsTcpTransport.enableSSL(true);
            getLdapServer().setTransports(plainTcpTransport, tlsTcpTransport);
            getLdapServer().start();
        }
    }

    public void destroy() throws Exception {
        File instanceDirectory = getDirectoryService().getInstanceLayout().getInstanceDirectory();
        getLdapServer().stop();
        getDirectoryService().shutdown();
        setLdapServer(null);
        setDirectoryService(null);
        if (getDeleteInstanceDirectoryOnShutdown()) {
            deleteDirectory(instanceDirectory);
        }
    }

    public String getDirectoryServiceName() {
        return getBasePartitionName() + "DirectoryService";
    }

    private static void deleteDirectory(File path) throws IOException {
        //FileUtils.deleteDirectory(path);
    }

    protected void createBasePartition() throws Exception {
        if ("dc=example,dc=com".equals(BASE_STRUCTURE)) {
            logger.warn(null, "LDAP tree root is set to " + BASE_STRUCTURE + ", you should configure this via environment variables " + Config.Option.LDAP_BASE_PARTITION_NAME.name() + " and " + Config.Option.LDAP_BASE_DOMAIN.name());
        }
        OpenIdPartition partition = OpenIdPartition.createPartition(getDirectoryService().getSchemaManager(), getDirectoryService().getDnFactory(), getBasePartitionName(), getBaseStructure(), getBaseCacheSize(), getBasePartitionPath());
        injector.injectMembers(partition);
        setBasePartition(partition);
        getDirectoryService().addPartition(getBasePartition());
    }

    public File getPartitionsDirectory() {
        return getDirectoryService().getInstanceLayout().getPartitionsDirectory();
    }

    public File getBasePartitionPath() {
        return new File(getPartitionsDirectory(), getBasePartitionName());
    }

    public File getGuessedInstanceDirectory() {
        final String property = System.getProperty("workingDirectory");
        return new File(property != null ? property : System.getProperty("java.io.tmpdir") + File.separator + "server-work-" + getDirectoryServiceName());
    }

    public DirectoryService getDirectoryService() {
        return _directoryService;
    }

    public void setDirectoryService(DirectoryService directoryService) {
        this._directoryService = directoryService;
    }

    public LdapServer getLdapServer() {
        return _ldapServer;
    }

    public void setLdapServer(LdapServer ldapServer) {
        this._ldapServer = ldapServer;
    }

    public OpenIdPartition getBasePartition() {
        return _basePartition;
    }

    public void setBasePartition(OpenIdPartition basePartition) {
        this._basePartition = basePartition;
    }

    public boolean getDeleteInstanceDirectoryOnStartup() {
        return _deleteInstanceDirectoryOnStartup;
    }

    public boolean getDeleteInstanceDirectoryOnShutdown() {
        return _deleteInstanceDirectoryOnShutdown;
    }
}
