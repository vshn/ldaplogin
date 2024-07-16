package services;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import entities.mongodb.MongoDbUser;
import org.bouncycastle.util.encoders.Hex;
import play.inject.ApplicationLifecycle;
import util.Config;
import util.CustomLogger;
import util.InputUtils;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;

public class MongoDb {
    private static final int TIMEOUT_CONNECT = 15 * 1000;

    private final MongoClient mongoClient;
    private final MongoDatabase db;
    private final Datastore ds;

    private final CustomLogger logger = new CustomLogger(this.getClass());

    private final byte[] encryptionKey;

    @Inject
    public MongoDb(ApplicationLifecycle appLifecycle) {
        String username = Config.get(Config.Option.MONGODB_USERNAME);
        String password = Config.get(Config.Option.MONGODB_PASSWORD);
        String hostname = Config.get(Config.Option.MONGODB_HOSTNAME);
        String database = Config.get(Config.Option.MONGODB_DATABASE);
        // Don't use TLS by default for local development environments and for MongoDBs in OpenShift containers
        Boolean tls = !(Config.getBoolean(Config.Option.MONGODB_DISABLE_TLS) || "localhost".equals(hostname));
        String mongoUrl;
        if (username != null && password != null) {
            mongoUrl = "mongodb://" + username + ":" + password + "@" + hostname + ":27017/" + database + "?tls=" + tls.toString().toLowerCase() + "&connecttimeoutms=" + TIMEOUT_CONNECT;
            logger.info(null, "Connecting to " + mongoUrl.replace(password, "***"));
        } else {
            mongoUrl = "mongodb://" + hostname + ":27017/?tls=" + tls.toString().toLowerCase() + "&connecttimeoutms=" + TIMEOUT_CONNECT;
            logger.info(null, "Connecting to " + mongoUrl);
        }

        mongoClient = MongoClients.create(mongoUrl);
        db = this.mongoClient.getDatabase(database);
        MapperOptions mapperOptions = MapperOptions.builder().storeEmpties(false).storeNulls(false).build();
        ds = Morphia.createDatastore(mongoClient, database, mapperOptions);
        ds.getMapper().map(MongoDbUser.class);
        ds.ensureIndexes();
        ds.ensureCaps();

        appLifecycle.addStopHook(() -> {
            logger.info(null, "Shutting down MongoDB connection");
            mongoClient.close();
            logger.info(null, "MongoDB connection shutdown complete");
            return CompletableFuture.completedFuture(null);
        });

        byte[] key = Hex.decode("0000000000000000000000000000000000000000000000000000000000000000");
        try {
            if (InputUtils.trimToNull(Config.Option.MONGODB_ENCRYPTION_KEY.get()) == null) {
                throw new IllegalArgumentException("Variable is empty");
            }
            key = Hex.decode(Config.Option.MONGODB_ENCRYPTION_KEY.get());
            if (key.length != 32) {
                throw new IllegalArgumentException("Length is not 64 HEX characters (256 bit)");
            }
        } catch (Exception e) {
            logger.warn(null, "Could not decode environment variable MONGODB_ENCRYPTION_KEY: " + e.getMessage());
            logger.warn(null, "!!! Database encryption is using an insecure default key. Please set up the MONGODB_ENCRYPTION_KEY environment variable (64 character HEX string) !!!");
        }
        encryptionKey = key;

        logger.info(null, "MongoDB connection startup complete");
    }

    public MongoDatabase get() {
        return db;
    }

    public Datastore getDS() {
        return ds;
    }

    public byte[] getEncryptionKey() {
        return encryptionKey;
    }
}
