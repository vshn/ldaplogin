package store.memory;

import entities.Group;
import entities.GroupsMetadata;
import entities.memory.MemoryGroup;
import store.GroupsStore;
import util.Config;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class MemoryGroupsStore implements GroupsStore {
    private final Map<String, MemoryGroup> groups = new ConcurrentHashMap<>();

    @Override
    public void ensure(Collection<String> paths) {
        paths.stream().filter(p -> p.startsWith(Config.Option.LDAP_GROUPS_SCOPE.get())).forEach(path -> {
            if (!groups.containsKey(path)) {
                groups.put(path, new MemoryGroup(path));
            }
        });
    }

    @Override
    public void updateMetadata(Collection<GroupsMetadata> metadata) {
        for (GroupsMetadata m : metadata) {
            if (Group.isInScope(m.getPath())) {
                MemoryGroup g = getByPath(m.getPath());
                if (g == null) {
                    // shouldn't happen because we've called ensure() before but who knows. Maybe inconsistent data coming from IDP.
                    continue;
                }
                if (!Objects.equals(g.getDescription(), m.getDescription()) || !Objects.equals(g.getEmail(), m.getEmail())) {
                    g.setDescription(g.getDescription());
                    g.setEmail(g.getEmail());
                }
            }
        }
    }

    @Override
    public Stream<MemoryGroup> getAll() {
        List<MemoryGroup> groupsList = new LinkedList<>(groups.values());
        Collections.sort(groupsList, Comparator.comparing(Group::getPath));
        return groupsList.stream();
    }

    @Override
    public MemoryGroup getByCn(String cn) {
        return getAll().filter(g -> g.getCn().equals(cn)).findFirst().orElse(null);
    }

    @Override
    public MemoryGroup getByPath(String path) {
        return groups.get(path);
    }
}
