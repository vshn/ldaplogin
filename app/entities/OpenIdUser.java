package entities;

import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.auth.openidconnect.IdToken;
import com.google.api.client.json.gson.GsonFactory;
import util.InputUtils;

import java.util.ArrayList;
import java.util.List;

public class OpenIdUser {
    private String uid;
    private String firstName;
    private String lastName;
    private String email;
    private boolean emailVerified;
    private List<String> groupPaths = new ArrayList<>();
    private String idToken;

    public static OpenIdUser fromTokenResponse(TokenResponse tokenResponse) {
        String idToken;
        IdToken.Payload idTokenPayload;
        try {
            idToken = (String)tokenResponse.get("id_token");
            idTokenPayload = IdToken.parse(new GsonFactory(), idToken).getPayload();
        } catch (Exception e) {
            return null;
        }
        return new OpenIdUser(idToken, idTokenPayload);
    }

    private OpenIdUser(String idToken, IdToken.Payload idTokenPayload) {
        this.idToken = idToken;
        try { uid = InputUtils.trimToNull((String) idTokenPayload.get("preferred_username")); } catch (Exception e) {}
        try { firstName = InputUtils.trimToNull((String) idTokenPayload.get("given_name")); } catch (Exception e) {}
        try { lastName = InputUtils.trimToNull((String) idTokenPayload.get("family_name")); } catch (Exception e) {}
        try { email = InputUtils.trimToNull((String) idTokenPayload.get("email")); } catch (Exception e) {}
        try { emailVerified = Boolean.TRUE.equals(idTokenPayload.get("email_verified")); } catch (Exception e) {}
        try { groupPaths = (List<String>) idTokenPayload.get("groups"); } catch (Exception e) {}
    }

    public String getUid() {
        return uid;
    }

    public String getEmail() {
        return email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }


    public List<String> getGroupPaths() {
        return groupPaths;
    }

    public String getIdToken() {
        return idToken;
    }
}
