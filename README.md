# LDAPLogin

LDAPLogin provides OpenID login for legacy LDAP-only applications. It does this by providing a simple read-only LDAP server which contains all user data learned from ID tokens.

LDAPLogin operates as follows:

* User logs in to LDAPLogin via OpenID. LDAPLogin creates an internal "shadow user" with the information it received via the ID token
* User clicks on a service link. This creates a temporary password which gets copied to the user's clipboard. The user is then forwarded to the service.
* User logs in to the service using the regular user name and the temporary password
* Service connects to LDAPLogin's LDAP port in order to validate the temporary password and get all required user information

## Known issues

* Performance isn't optimized at all, especially handling of groups. You'll probably start noticing issues if you get into the 1000s of users.
* Some classes from the ApacheDS project had to be copied and modified because they don't provide the required flexibility/extendability. They can be found in the `services.ldap` package.
* Some useless warnings have been hidden via the logging configuration because there is no good way to fix them, see [logback.xml](conf/logback.xml).

## Persistence

You can choose whether your user and group information is persisted or not.

By default it is not persisted, which means all the users are lost when restarting the service. This is fine for many services because they only fetch user data from LDAP once, however it will fail for services which try to fetch user data from LDAP again and again.

If you want to persist your user and group information then you need to enable MongoDB support and configure a connection using the MONGODB_\* environment variables. See [Configuration](#configuration) for more information.

## Groups support

Your IDP should be set up to provide a "groups" field in the ID token with a list of the user's groups. If applicable the groups should be in full path form (e.g. "/myorg/admins").

Groups are used to restrict which users can log in to which services. Each service is configured with a full group path. The service view only shows the users which are members of the service's group.

Group information is provided via LDAP in the `ou=Groups,ou=myorg,ou=com` subtree. Groups have `uniqueMember` attributes listing DNs of all member users and users have "memberOf" attributes listing DNs of all groups the user is a member of. Because the hierarchical group structure provided e.g. by Keycloak is not suitable for LDAP the setting `LDAP_GROUPS_SCOPE` can be used to restrict which subpath is shown in LDAP. E.g. with `LDAP_GROUPS_SCOPE=/LDAP` the group "/LDAP/admins" would show up in the LDAP tree as "admin" but the group "/myorg/admins" would not show up. Group names are modified if necessary to fit the LDAP format (e.g. "/foo/bar" gets translated to "foo::bar" because '/' in group names is problematic in LDAP).

Note that the `LDAP_GROUPS_SCOPE` not only selects _which_ groups are visible via the LDAP protocol, but it also _removes the scope prefix_ in order to make the group structure look flat in LDAP (despite being backed by a hiearchical structure in your IDP).

## LDAP structure example

An LDAP structure provided by LDAPLogin could look like this:

* `ou=People,ou=myorg,ou=com`: Contains all users known to LDAPLogin
* `uid=john.doe,ou=People,ou=myorg,ou=com`: Typical user DN (e.g. used for authentication when validating the user's password)
* `uid=icinga,ou=Services,ou=myorg,ou=com`: Typical service DN (e.g. used for autentication when checking the view to figure out if the user is allowed to log in)
* `ou=icinga,ou=Service Access,ou=Views,ou=myorg,ou=com`: View containing all users that can log in to the service "icinga"
* `cn=admins,ou=Groups,ou=myorg,ou=com`: Group containing all admin users

For now the `ou=People`, `ou=Services`, `ou=Service Access,ou=Views` and `ou=Groups` are fixed and can't be changed on the LDAPLogin side. However these are configurable in virtually all LDAP clients, therefore this shouldn't be a problem.

## Configuration

LDAPLogin can only be configured via environment variables.

* `LDAP_BASE_DOMAIN`: The top level OU of the LDAP tree. Usually something like "com", "ch", "net".
* `LDAP_BASE_PARTITION_NAME`: The second level OU of the LDAP tree. Usually something like "myorg", possibly equal to your internet domain.
* `LDAP_TLS_PRIVATE_KEY`: Private key in RSA/PKCS1/PKCS8 format used for the LDAPS port.
* `LDAP_TLS_CERTIFICATE`: Certificate chain in X.509 format used for the LDAPS port.
* `LDAP_PLAIN_PORT`: As the name says, defaults to 10389.
* `LDAP_TLS_PORT`: As the name says, defaults to 10636.
* `LDAP_GROUPS_SCOPE`: See [Groups support](#groups-support). Defaults to "".
* `MONGODB_ENABLE`: Set to "true" to enable a MongoDB connection. Defaults to "false".
* `MONGODB_DATABASE`: Configure the MongoDB database name. Defaults to "ldaplogin".
* `MONGODB_HOSTNAME`: Configure the MongoDB server name. Defaults to "localhost".
* `MONGODB_USERNAME`: Configure the MongoDB user name. Defaults to "ldaplogin".
* `MONGODB_PASSWORD`: Configure the MongoDB password.
* `MONGODB_DISABLE_TLS`: Disable TLS support. Defaults to "false" (TLS enabled) except for "localhost".
* `MONGODB_ENCRYPTION_KEY`: Credentials are encrypted using this key. An insecure default key is provided.
* `OPENID_CLIENT_ID`: Client ID used to connect to our OpenID provider.
* `OPENID_SECRET`: Secret to authenticate LDAPLogin against your OpenID provider.
* `OPENID_URL_REALM`: Full OpenID provider URL including realm. E.g. 'https://keycloak.example.com/auth/realms/MYREALM/'
* `OPENID_URL_TOKEN`: Full OpenID provider URL for the token endpoint. E.g. 'https://keycloak.example.com/auth/realms/MYREALM/protocol/openid-connect/token'
* `OPENID_URL_AUTH`: Full OpenID provider URL for the auth endpoint. E.g. 'https://keycloak.example.com/auth/realms/MYREALM/protocol/openid-connect/auth'
* `OPENID_URL_LOGOUT`: Full OpenID provider URL for the logout endpoint. E.g. 'https://keycloak.example.com/auth/realms/MYREALM/protocol/openid-connect/logout'
* `SERVICES`: Comma-separated list of services, e.g. "icinga,grafana". Don't use special characters as you will have to define environment variables containing these service IDs in upper case.
* `SERVICE_$SERVICEUPPER_PASSWORD`: Plaintext password used by the service to authenticate itself.
* `SERVICE_$SERVICEUPPER_NAME`: User-friendly service name.
* `SERVICE_$SERVICEUPPER_URL`: URL of the service's login screen.
* `SERVICE_$SERVICEUPPER_GROUP`: IDP group required for the user to use this service.
* `USER_EXPIRES`: Number of seconds until a user expires and gets deleted automatically. Default '7776000' (90 days).
* `USER_SESSION_EXPIRES`: Number of seconds until a user's session expires and gets deleted automatically. Default '86400' (1 day).

## License

This project includes code from the [ApacheDS project](https://directory.apache.org/) (Thank you!). See [APACHE-2.0.txt](APACHE-2.0.txt).

This project itself is licensed under BSD 3-Clause, see [LICENSE](LICENSE).
