package entities;

import util.Config;
import java.util.List;

public interface ServicePasswords {
    String getStaticPwHashBase64();
    byte[] getStaticPwHash();
    List<? extends DynamicPassword> getDynamicPasswords();

    long DYNAMIC_PASSWORD_EXPIRES = 1000L * Config.Option.USER_DYNAMIC_PASSWORD_EXPIRES.getLong();
}
