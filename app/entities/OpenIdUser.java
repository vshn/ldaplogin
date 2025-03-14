package entities;

import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.auth.openidconnect.IdToken;
import com.google.api.client.json.gson.GsonFactory;
import util.InputUtils;

import java.util.*;

public class OpenIdUser {
    private String uid;
    private String firstName;
    private String lastName;
    private String email;
    private Integer emailQuota;
    private boolean emailVerified;
    private List<String> alias;
    private List<String> groupPaths;
    private List<GroupsMetadata> groupsMetadata = new ArrayList<>();

    public static OpenIdUser fromTokenResponse(TokenResponse tokenResponse) {
        IdToken.Payload idTokenPayload;
        try {
            String idToken = (String)tokenResponse.get("id_token");
            idTokenPayload = IdToken.parse(new GsonFactory(), idToken).getPayload();
        } catch (Exception e) {
            return null;
        }
        return new OpenIdUser(idTokenPayload);
    }

    @SuppressWarnings("unchecked")
    private OpenIdUser(IdToken.Payload idTokenPayload) {
        try { uid = InputUtils.trimToNull((String) idTokenPayload.get("preferred_username")); } catch (Exception e) {}
        try { firstName = InputUtils.trimToNull((String) idTokenPayload.get("given_name")); } catch (Exception e) {}
        try { lastName = InputUtils.trimToNull((String) idTokenPayload.get("family_name")); } catch (Exception e) {}
        try { email = InputUtils.trimToNull((String) idTokenPayload.get("email")); } catch (Exception e) {}
        try { emailQuota = InputUtils.toInteger("" + idTokenPayload.get("emailQuota")); } catch (Exception e) {}
        try { emailVerified = Boolean.TRUE.equals(idTokenPayload.get("email_verified")); } catch (Exception e) {}
        try { alias = (List<String>)idTokenPayload.get("alias"); } catch (Exception e) {}
        if (alias == null) {
            alias = new ArrayList<>();
        }
        Collections.sort(alias);
        // The groups only exist if you have a groups mapper configured.
        try { groupPaths = (List<String>) idTokenPayload.get("groups"); } catch (Exception e) {}
        if (groupPaths == null) {
            groupPaths = new ArrayList<>();
        }
        try {
            // The groups metadata only exists if your groups have an attribute "group_metadata" containing a json string (see GroupsMetadata class)
            // and there is a user attribute mapper which turns these group_metadata into an aggregated multi-valued JSON 'groups_metadata' attribute
            List<Map<String, String>> groupsMetadataList = (List<Map<String, String>>)idTokenPayload.get("groups_metadata");
            for (Map<String, String> gm : groupsMetadataList) {
                groupsMetadata.add(new GroupsMetadata(gm));
            }
            groupsMetadata = Collections.unmodifiableList(groupsMetadata);
        } catch (Exception e) {
            // too bad
        }
    }

    public String getUid() {
        return uid;
    }

    public String getEmail() {
        return email;
    }

    public Integer getEmailQuota() {
        return emailQuota;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public List<String> getAlias() {
        return alias;
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

    public List<GroupsMetadata> getGroupsMetadata() {
        return groupsMetadata;
    }
}
