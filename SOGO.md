# How to use SOGo with ldaplogin

This document contains some examples of how you can configure SOGo to work with ldaplogin.

## ldaplogin service configuration

This ldaplogin configuration is needed to provide LDAP for SOGo.

```
SERVICES=sogo

SERVICE_SOGO_PASSWORD=_$PASSWORD_
SERVICE_SOGO_NAME=SOGo
SERVICE_SOGO_URL=https://sogo.example.com
SERVICE_SOGO_GROUP=/mygroups/staff
SERVICE_SOGO_STATIC_PASSWORDS=true
SERVICE_SOGO_DYNAMIC_PASSWORDS_EXPIRE=604800 # 7 days, needs to be slightly longer than the maximum SOGo web session lifetime
```

## SOGoUserSources

This configuration adds:
* A user directory for authentication and as an address book
* A groups directory as an address book
* A resources directory for booking resources

```
  /* LDAP authentication */
  SOGoUserSources = (
    {
      type = ldap;
      CNFieldName = cn;
      UIDFieldName = uid;
      IDFieldName = uid;
      MailFieldNames = (mail, alias);
      IMAPLoginFieldName = mail;
      bindFields = (uid, mail);
      baseDN = "ou=sogo,ou=People,dc=example,dc=com";
      bindDN = "uid=sogo,ou=Services,dc=example,dc=com";
      bindPassword = "_$PASSWORD_";
      canAuthenticate = YES;
      displayName = "Users";
      hostname = "ldaps://ldap.ldaplogin.example.com:10636";
      id = public;
      isAddressBook = YES;
      listRequiresDot = NO;
    },
    {
      type = ldap;
      CNFieldName = cn;
      UIDFieldName = cn;
      IDFieldName = cn;
      baseDN = "ou=sogo,ou=Groups,dc=example,dc=com";
      bindDN = "uid=sogo,ou=Services,dc=example,dc=com";
      bindPassword = "_$PASSWORD_";
      displayName = "Groups";
      hostname = "ldaps://ldap.ldaplogin.example.com:10636";
      id = example_com_groups;
      isAddressBook = YES;
      listRequiresDot = NO;
    },
    {
      type = ldap;
      CNFieldName = cn;
      UIDFieldName = cn;
      IDFieldName = cn;
      MultipleBookingsFieldName = Multiplebookings;
      KindFieldName = Kind;
      baseDN = "ou=Resources,dc=example,dc=com";
      bindDN = "uid=sogo,ou=Services,dc=example,dc=com";
      bindPassword = "_$PASSWORD_";
      /* sogo needs this to track permissions to the resource users */
      canAuthenticate = YES;
      displayName = "Resources";
      hostname = "ldaps://ldap.ldaplogin.example.com:10636";
      id = example_com_resources;
      isAddressBook = YES;
      listRequiresDot = NO;
    }
  );
```
