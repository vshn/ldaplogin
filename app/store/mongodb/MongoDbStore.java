package store.mongodb;

import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CollationStrength;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.filters.Filters;
import entities.mongodb.MongoDbEntity;
import services.MongoDb;

import javax.inject.Inject;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.stream.Stream;

public class MongoDbStore<T extends MongoDbEntity> {
    public static final FindOptions CASE_INSENSITIVE_FIND_OPTIONS = new FindOptions().collation(Collation.builder().locale("en").collationStrength(CollationStrength.PRIMARY).build());

    @Inject
    protected MongoDb mongoDb;

    private final Class<T> clazz;

    public MongoDbStore() {
        // Some shenanigans required to get a class reference
        Type superclass = getClass().getGenericSuperclass();
        ParameterizedType parameterized = (ParameterizedType) superclass;
        clazz = (Class<T>) parameterized.getActualTypeArguments()[0];
    }

    protected Query<T> query() {
        return mongoDb.getDS().find(clazz);
    }

    protected Query<T> query(Object entity) {
        return query().filter(Filters.eq("_id", ((MongoDbEntity)entity).getObjectId()));
    }

    public Stream<T> getAll() {
        return query().stream();
    }

    public void delete(Object entity) {
        query(entity).delete();
    }
}
