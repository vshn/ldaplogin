package store.memory;

import entities.Group;
import store.GroupsStore;

import java.util.*;

public class MemoryGroupsStore implements GroupsStore {
    private final Map<String, Group> groups = new HashMap<>();

    @Override
    public void ensure(Collection<String> paths) {
        paths.forEach(p -> {
            if (!groups.containsKey(Group.cnFromPath(p))) {
                groups.put(Group.cnFromPath(p), new Group(p));
            }
        });
    }

    @Override
    public List<Group> getAll() {
        List<Group> groupsList = new LinkedList<>(groups.values());
        Collections.sort(groupsList, Comparator.comparing(Group::getPath));
        return groupsList;
    }

    @Override
    public Group getByCn(String cn) {
        return groups.get(cn);
    }

    @Override
    public Group getByPath(String path) {
        return groups.get(Group.cnFromPath(path));
    }
}
