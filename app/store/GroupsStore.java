package store;

import entities.Group;
import entities.GroupsMetadata;

import java.util.*;
import java.util.stream.Stream;

public interface GroupsStore {
    void ensure(Collection<String> paths);

    void updateMetadata(Collection<GroupsMetadata> metadata);

    Stream<? extends Group> getAll();

    Group getByCn(String cn);

    Group getByPath(String path);
}
