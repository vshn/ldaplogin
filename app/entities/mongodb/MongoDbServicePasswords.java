package entities.mongodb;

import dev.morphia.annotations.Entity;
import entities.ServicePasswords;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;


@Entity(useDiscriminator = false)
public class MongoDbServicePasswords implements ServicePasswords {
    private String staticPwHash;
    private List<MongoDbDynamicPassword> dynamicPasswords = new ArrayList<>();

    public void addDynamicPassword(MongoDbDynamicPassword dynamicPassword) {
        dynamicPasswords.add((MongoDbDynamicPassword)dynamicPassword);
    }

    @Override
    public String getStaticPwHashBase64() {
        return staticPwHash;
    }

    @Override
    public byte[] getStaticPwHash() {
        try {
            return Base64.getDecoder().decode(staticPwHash);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<MongoDbDynamicPassword> getDynamicPasswords() {
        return dynamicPasswords;
    }

    public void setStaticPwHash(byte[] pwHash) {
        this.staticPwHash = Base64.getEncoder().encodeToString(pwHash);
    }
}
