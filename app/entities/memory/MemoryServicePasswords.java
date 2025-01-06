package entities.memory;

import entities.DynamicPassword;
import entities.ServicePasswords;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class MemoryServicePasswords implements ServicePasswords {
    private String staticPwHash;
    private List<MemoryDynamicPassword> dynamicPasswords = new ArrayList<>();

    public void addDynamicPassword(MemoryDynamicPassword dynamicPassword) {
        dynamicPasswords.add(dynamicPassword);
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
    public List<? extends DynamicPassword> getDynamicPasswords() {
        return dynamicPasswords;
    }

    public void setStaticPwHash(byte[] pwHash) {
        this.staticPwHash = Base64.getEncoder().encodeToString(pwHash);
    }
}
