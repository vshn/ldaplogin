package store.mongodb;

import dev.morphia.query.filters.Filters;
import entities.mongodb.MongoDbResource;
import store.ResourcesStore;

import java.util.stream.Stream;

public class MongoDbResourcesStore extends MongoDbStore<MongoDbResource> implements ResourcesStore {
    @Override
    public Stream<MongoDbResource> getAll() {
        return super.getAll();
    }

    @Override
    public MongoDbResource getByName(String name) {
        return query().filter(Filters.eq("name", name)).stream(CASE_INSENSITIVE_FIND_OPTIONS).findFirst().orElse(null);
    }
}
