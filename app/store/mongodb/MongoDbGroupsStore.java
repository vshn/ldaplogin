package store.mongodb;

import entities.Group;
import store.GroupsStore;

import javax.inject.Inject;
import java.util.*;

public class MongoDbGroupsStore implements GroupsStore {

    @Inject
    private MongoDbUsersStore usersStore;

    @Override
    public void ensure(Collection<String> paths) {
        // Nothing - we fetch groups directly from users
    }

    @Override
    public List<Group> getAll() {
        // This isn't efficient at all but it'll do for now
        Set<Group> groupSet = new HashSet<>();
        usersStore.getAll().forEach(user -> {
            user.getGroupPaths().forEach(path -> groupSet.add(new Group(path)));
        });
        List<Group> groups = new ArrayList<>(groupSet);
        Collections.sort(groups, Comparator.comparing(Group::getPath));
        return groups;
    }

    @Override
    public Group getByCn(String cn) {
        // This isn't efficient at all but it'll do for now
        return getAll().stream().filter(g -> g.getCn().equalsIgnoreCase(cn)).findFirst().orElse(null);
    }

    @Override
    public Group getByPath(String path) {
        // This isn't efficient at all but it'll do for now
        return getAll().stream().filter(g -> g.getPath().equals(path)).findFirst().orElse(null);
    }
}
