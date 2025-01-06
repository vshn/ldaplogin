package store;

import entities.Resource;

import java.util.stream.Stream;

public interface ResourcesStore {
    Stream<? extends Resource> getAll();

    Resource getByName(String cn);
}
