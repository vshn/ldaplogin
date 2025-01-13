package store.mongodb;

import dev.morphia.UpdateOptions;
import dev.morphia.query.filters.Filters;
import dev.morphia.query.updates.UpdateOperators;
import entities.Group;
import entities.GroupsMetadata;
import entities.mongodb.MongoDbGroup;
import store.GroupsStore;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MongoDbGroupsStore extends MongoDbStore<MongoDbGroup> implements GroupsStore {

    @Inject
    private MongoDbUsersStore usersStore;

    @Override
    public void ensure(Collection<String> pathsCollection) {
        Set<String> paths = pathsCollection.stream().filter(Group::isInScope).collect(Collectors.toSet());
        query().filter(Filters.in("path", pathsCollection)).stream().forEach(g -> paths.remove(g.getPath()));
        for (String path : paths) {
            try {
                MongoDbGroup group = new MongoDbGroup(path);
                mongoDb.getDS().save(group);
            } catch (Exception e) {
                // there may be concurrency errors due to duplicate keys, ignore
            }
        }
    }

    @Override
    public void updateMetadata(Collection<GroupsMetadata> metadata) {
        for (GroupsMetadata m : metadata) {
            if (Group.isInScope(m.getPath())) {
                MongoDbGroup g = getByPath(m.getPath());
                if (g == null) {
                    // shouldn't happen because we've called ensure() before but who knows. Maybe inconsistent data coming from IDP.
                    continue;
                }
                if (!Objects.equals(g.getDescription(), m.getDescription()) || !Objects.equals(g.getEmail(), m.getEmail())) {
                    g.setDescription(m.getDescription());
                    g.setEmail(m.getEmail());
                    query(g).update(new UpdateOptions(), UpdateOperators.set("description", g.getDescription()), UpdateOperators.set("email", g.getEmail()));
                }
            }
        }
    }

    @Override
    public Stream<MongoDbGroup> getAll() {
        // we need to filter the groups because there may be old out-of-scope groups in the database
        return super.getAll().filter(Group::isInScope).sorted();
    }

    @Override
    public MongoDbGroup getByCn(String cn) {
        // we need to filter the results because there may be old out-of-scope groups in the database
        // scope filtering could happen on the database, but then we would need to construct a regex safely... not worth it.
        return query().filter(Filters.eq("cn", cn)).stream().filter(Group::isInScope).findFirst().orElse(null);
    }

    @Override
    public MongoDbGroup getByPath(String path) {
        // we need to filter the results because there may be old out-of-scope groups in the database
        // scope filtering could happen on the database, but then we would need to construct a regex safely... not worth it.
        return query().filter(Filters.eq("path", path)).stream().filter(Group::isInScope).findFirst().orElse(null);
    }
}
