import com.google.inject.AbstractModule;
import services.Ldap;
import services.MongoDb;
import services.OpenId;
import store.GroupsStore;
import store.UsersStore;
import store.memory.MemoryGroupsStore;
import store.memory.MemoryUsersStore;
import store.mongodb.MongoDbGroupsStore;
import store.mongodb.MongoDbUsersStore;
import util.Config;

public class Module extends AbstractModule {
        @Override
        protected void configure() {
            bind(Ldap.class).asEagerSingleton();
            bind(OpenId.class).asEagerSingleton();
            if (Config.Option.MONGODB_ENABLE.getBoolean()) {
                bind(MongoDb.class).asEagerSingleton(); // We only want one for proper connection pooling etc.
                bind(UsersStore.class).to(MongoDbUsersStore.class);
                bind(GroupsStore.class).to(MongoDbGroupsStore.class);
            } else {
                bind(UsersStore.class).to(MemoryUsersStore.class).asEagerSingleton();
                bind(GroupsStore.class).to(MemoryGroupsStore.class).asEagerSingleton();
            }
        }
}
