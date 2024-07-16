package store;

import entities.Group;

import java.util.*;

public interface GroupsStore {
    void ensure(Collection<String> paths);

    List<Group> getAll();

    Group getByCn(String cn);

    Group getByPath(String path);
}
