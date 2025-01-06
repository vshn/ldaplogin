package store.memory;

import entities.Resource;
import store.ResourcesStore;

import java.util.ArrayList;
import java.util.stream.Stream;

public class MemoryResourcesStore implements ResourcesStore {
    @Override
    public Stream<Resource> getAll() {
        return new ArrayList<Resource>().stream();
    }

    @Override
    public Resource getByName(String cn) {
        return null;
    }
}
