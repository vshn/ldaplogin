import com.google.inject.AbstractModule;
import services.Ldap;
import services.MongoDb;
import services.OpenId;
import store.GroupsStore;
import store.ResourcesStore;
import store.UsersStore;
import store.memory.MemoryGroupsStore;
import store.memory.MemoryResourcesStore;
import store.memory.MemoryUsersStore;
import store.mongodb.MongoDbGroupsStore;
import store.mongodb.MongoDbResourcesStore;
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
                bind(ResourcesStore.class).to(MongoDbResourcesStore.class);
            } else {
                bind(UsersStore.class).to(MemoryUsersStore.class).asEagerSingleton();
                bind(GroupsStore.class).to(MemoryGroupsStore.class).asEagerSingleton();
                bind(ResourcesStore.class).to(MemoryResourcesStore.class);
            }
        }
}
