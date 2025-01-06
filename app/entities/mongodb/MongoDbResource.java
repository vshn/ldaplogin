package entities.mongodb;

import com.mongodb.client.model.CollationStrength;
import dev.morphia.annotations.*;
import entities.Resource;
import org.bson.types.ObjectId;


@Entity(value = "resources", useDiscriminator = false)
public class MongoDbResource implements MongoDbEntity, Resource {
    @Id
    private ObjectId _id;

    private Integer multipleBookings;

    @Indexed(options = @IndexOptions(unique = true, collation = @Collation(locale = "en", strength = CollationStrength.PRIMARY)))
    private String name;

    private String kind;

    private String email;

    @Override
    public Integer getMultipleBookings() {
        return multipleBookings;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getKind() {
        return kind;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public ObjectId getObjectId() {
        return _id;
    }
}
