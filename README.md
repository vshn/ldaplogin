# LDAPLogin

LDAPLogin provides OpenID login for legacy LDAP-only applications. It does this by providing a simple read-only LDAP server which contains all user data learned from ID tokens.

LDAPLogin operates as follows:

* User logs in to LDAPLogin via OpenID. LDAPLogin creates an internal "shadow user" with the information it received via the ID token
* User clicks on a service link. This creates a temporary password which gets copied to the user's clipboard. The user is then forwarded to the service.
* User logs in to the service using the regular user name and the temporary password
* Service connects to LDAPLogin's LDAP port in order to validate the temporary password and get all required user information

This document describes the configuration of ldaplogin itself. Other relevant documentation:

* [How to set up Keycloak for ldaplogin](KEYCLOAK.md)
* [How to use SOGo with ldaplogin](SOGO.md)
* [How to use Dovecot with ldaplogin](DOVECOT.md)

## Known issues

* Performance isn't optimized at all, especially handling of groups. You'll probably start noticing issues if you get into the 1000s of users.
* Some classes from the ApacheDS project had to be copied and modified because they don't provide the required flexibility/extendability. They can be found in the `services.ldap` package.
* Some useless warnings have been hidden via the logging configuration because there is no good way to fix them, see [logback.xml](conf/logback.xml).

## Groups support

Your IDP should be set up to provide a "groups" field in the ID token with a list of the user's groups. If applicable the groups should be in full path form (e.g. "/myorg/admins").

Groups are used to restrict which users can log in to which services. Each service is configured with a full group path. The service view only shows the users which are members of the service's group.

Group information is provided via LDAP in the `ou=Groups,ou=myorg,ou=com` subtree. Groups have `uniqueMember` attributes listing DNs of all member users and users have "memberOf" attributes listing DNs of all groups the user is a member of. Because the hierarchical group structure provided e.g. by Keycloak is not suitable for LDAP the setting `LDAP_GROUPS_SCOPE` can be used to restrict which subpath is shown in LDAP. E.g. with `LDAP_GROUPS_SCOPE=/LDAP` the group "/LDAP/admins" would show up in the LDAP tree as "admin" but the group "/myorg/admins" would not show up. Group names are modified if necessary to fit the LDAP format (e.g. "/foo/bar" gets translated to "foo::bar" because '/' in group names is problematic in LDAP).

Note that the `LDAP_GROUPS_SCOPE` not only selects _which_ groups are visible via the LDAP protocol, but it also _removes the scope prefix_ in order to make the group structure look flat in LDAP (despite being backed by a hiearchical structure in your IDP).

## Persistence

You can choose whether your user and group information is persisted or not.

By default it is not persisted, which means all the users are lost when restarting the service. This is fine for many services because they only fetch user data from LDAP once, however it will fail for services which try to fetch user data from LDAP again and again.

If you want to persist your user and group information then you need to enable MongoDB support and configure a connection using the MONGODB_\* environment variables. See [Configuration](#configuration) for more information.

### User expiration

If you use persistence then users can expire. The following rules apply:

* If a session hasn't been used for `USER_SESSION_EXPIRES` seconds then the session gets removed from the database
* If a user hasn't logged in for `USER_EXPIRES` seconds then it gets removed from the database

### Password expiration

Dynamic passwords (generated whenever the user wants to log in to a service) have a limited lifetime.

Static passwords (manually generated) have an indefinite lifetime in principle, however they will become inactive after a while if the user hasn't logged in via IDP. This is to force the user to update his/her information via IDP login (there is no other mechanism to update the user's information).

* If a user hasn't logged in for `USER_STATIC_PASSWORD_EXPIRES` seconds then all static passwords become inactive. They will reactivate once the user logs in via IDP.
* If a dynamic password is older than `USER_DYNAMIC_PASSWORD_EXPIRES` seconds then the dynamic password becomes inactive. It will never reactivate.

### Groups expiration

Groups currently do not expire. This will need to be fixed at some point. 

## Resources support

There is limited support for Resources (e.g. meeting rooms). If you have MongoDB persistence enabled, ldaplogin will serve all resources found in the 'resources' collection. There is no UI to edit the resources (yet), hence you will have to manage them manually on the MongoDB CLI. Resources in MongoDB have the following attributes:

* `name`: String, everything that can be used as an LDAP cn will work.
* `kind`: 'GROUP' or 'LOCATION'
* `multipleBookings`: Integer
* `email`: String

In contrast to users and groups, resource DNs do not contain the service id. All services will see the exact same resource DNs.

## LDAP structure

The LDAP structure provided by ldaplogin is built as follows. Note that every service sees different data (the service name is part of the DN). The reason for this is because each IDP user has a different password per service, and in order to know which password to validate we need to have the service name in the user DN.

* `ou=_$SERVICE_,ou=People,dc=_$MYORG_,dc=_$MYDOMAIN_`: Contains all users known to LDAPLogin
* `uid=_$USERID_,ou=_$SERVICE_,ou=People,dc=_$MYORG_,dc=_$MYDOMAIN_`: Typical user DN (e.g. used for authentication when validating the user's password)
* `uid=_$SERVICE_,ou=Services,dc=_$MYORG_,dc=_$MYDOMAIN_`: Typical service DN (e.g. used for autentication when checking the view to figure out if the user is allowed to log in)
* `cn=admins,ou=_$SERVICE_,ou=Groups,dc=_$MYORG_,dc=_$MYDOMAIN_`: Group containing all admin users
* `cn=_$RESOURCENAME_,ou=Resources,dc=_$MYORG_,dc=_$MYDOMAIN_`: A resource

For now the `ou=People`, `ou=Services` and `ou=Groups` are fixed and can't be changed on the LDAPLogin side. However these are configurable in virtually all LDAP clients, therefore this shouldn't be a problem.

## LDAP structure example

* `ou=icinga,ou=People,dc=example,dc=com`: Per service user directory
* `uid=john.doe,ou=icinga,ou=People,dc=example,dc=com`: User DN (only correct for service icinga)
* `uid=icinga,ou=Services,dc=example,dc=com`: Service DN
* `cn=admins,ou=icinga,ou=Groups,dc=example,dc=com`: Group DN
* `cn=Meetingroom 1st floor,ou=Resources,dc=example,dc=com`: Resource DN

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
* `SERVICE_$SERVICEUPPER_STATIC_PASSWORDS`: Set to "true" if the service should offer the use of static (non-expiring, non-changing) passwords. Defaults to "false". 
* `USER_EXPIRES`: Number of seconds until a user expires and gets deleted automatically. Defaults to "31536000" (365 days).
* `USER_SESSION_EXPIRES`: Number of seconds until a user's session expires and gets deleted automatically. Defaults to "86400" (1 day).
* `USER_DYNAMIC_PASSWORD_EXPIRES`: Number of seconds since password generation until a user's dynamic password for a service expires. Defaults to "3600" (1 hour).
* `USER_STATIC_PASSWORD_EXPIRES`: Number of seconds since last IDP login until a user's static passwords (all of them) expire (can be reactivated by logging in via IDP). Defaults to "2592000" (30 days).
* `USER_NEVER_EXPIRES_GROUPS`: Comma-separated list of full group paths. If the user is member of one of those groups, it will never expire. Intended for non-human accounts. Defaults to empty list.

## License

This project includes code from the [ApacheDS project](https://directory.apache.org/) (Thank you!). See [APACHE-2.0.txt](APACHE-2.0.txt).

This project itself is licensed under BSD 3-Clause, see [LICENSE](LICENSE).
