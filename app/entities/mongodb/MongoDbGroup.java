package entities.mongodb;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexed;
import entities.Group;
import org.bson.types.ObjectId;

@Entity(value = "groups", useDiscriminator = false)
public class MongoDbGroup implements MongoDbEntity, Group, Comparable<MongoDbGroup> {
    @Id
    private ObjectId _id;

    @Indexed(options = @IndexOptions(unique = true))
    private String path;

    @Indexed
    private String cn;

    private String description;

    private String email;

    public MongoDbGroup() {
        // dummy constructor for Morphia
    }

    public MongoDbGroup(String path) {
        this.path = path;
        this.cn = getCn();
    }

    public ObjectId getObjectId() {
        return _id;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public int compareTo(MongoDbGroup mongoDbGroup) {
        return getPath().compareTo(mongoDbGroup.getPath());
    }
}
