# How to use Dovecot with ldaplogin

This document contains some examples of how you can configure Dovecot to work with ldaplogin.

## Global configuration

This needs to go to the global Dovecot configuration.

```
passdb {
  driver = ldap
  args = /etc/dovecot/dovecot-ldap.conf
}
userdb {
  driver = ldap
  args = /etc/dovecot/dovecot-ldap.conf
}
```

## LDAP configuration

This needs to go to the config file referenced by the global configuration, e.g. `/etc/dovecot/dovecot-ldap.conf`.

Note that this example assumes that you use Dovecot with SOGo, hence it uses the SOGo credentials to access LDAP. This is also important to ensure that Dovecot sees the same data as SOGo sees.

```
uris = ldaps://ldap.ldaplogin.example.com:10636
dn = uid=sogo,ou=Services,dc=example,dc=com
dnpass = _$PASSWORD}
ldap_version = 3
base = dc=example,dc=com
deref = never
scope = subtree
user_filter = (&(mail=%u)(objectclass=inetorgperson))
user_attrs = mailQuota=quota_rule=*:storage=%$
auth_bind = yes
pass_filter = (&(mail=%u)(objectclass=inetorgperson))
pass_attrs =
```
