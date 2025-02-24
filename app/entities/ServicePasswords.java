package entities;

import java.util.List;

public interface ServicePasswords {
    String getStaticPwHashBase64();
    byte[] getStaticPwHash();
    List<? extends DynamicPassword> getDynamicPasswords();
}
