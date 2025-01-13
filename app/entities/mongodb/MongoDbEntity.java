package entities.mongodb;

import org.bson.types.ObjectId;

public interface MongoDbEntity {
    ObjectId getObjectId();
}
