# How to set up Keycloak for ldaplogin

In order to use ldaplogin you need to connect it to your IDP. This page explains how to do this with Keycloak.

## Basics

ldaplogin is a mostly normal Keycloak client, but for all the features to work you should add some additional data to the ID tokens.

* A `groups` field: List of strings containing full group paths. This is pretty much required for basic ldaplogin functionality.
* A `emailQuota` field: For some use cases you may need a `mailQuota` field in LDAP. ldaplogin can do this if it gets a `emailQuota` field with the ID token. Note that we use `emailQuota` on the Keycloak side of things while calling it `mailQuota` in LDAP. This is in line with the fact that Keycloak calls it `email` while LDAP calls it `mail`. The field contains an integer, preferably the quota in KiB.
* A `groups_metadata` field: List of JSON objects containing keys `path`, `description` and `email`. This is needed if you want to have the `description` and `mail` fields of Groups available in LDAP

## `groups` field

You should probably configure a Keycloak-wide Client scope called `groups` which adds this field.

* Log in to your Keycloak instance as admin and go to your realm
* Go to 'Client scopes'
* Click 'Create client scope' and use name "groups"
* Disable 'Display on consent screen'
* Choose Type 'Default'
* Disable 'Display on consent screen'
* Save
* Go to 'Mappers'
* Click 'Configure a new mapper'
* Click 'Group Membership'
* Choose Name "groups" and Token Claim Name "groups" (the latter is important for ldaplogin to work properly!)
* Make sure the following are on: 'Full group path', 'Add to ID token', 'Add to access token', 'Add to userinfo'
* Save

Now that you have the global Client scope configured, you can edit your client.

* Go to 'Clients'
* Find your ldaplogin client
* Go to tab 'Client scopes'
* Click 'Add client scope'
* Find the "groups" scope and add it

Now test the result.

* Below 'Client scopes' there is a second level of tabs. Click 'Evaluate'.
* Select a user in the 'Users' input field. The user must be member of at least one group.
* Click 'Generated user info'
* The resulting JSON needs to have a field `groups` with a list of full group paths. 

## `emailQuota` field

Depending on your use cases it may make sense to configure a Keycloak-wide Client scope called `emailQuota` which adds this field.

* Log in to your Keycloak instance as admin and go to your realm
* Go to 'Client scopes'
* Click 'Create client scope' and use name "emailQuota"
* Choose Type 'Default'
* Disable 'Display on consent screen'
* Save
* Go to 'Mappers'
* Click 'Configure a new mapper'
* Click 'User Attribute'
* Choose Name "emailQuota", User Attribute "emailQuota" and Token Claim Name "emailQuota"
* Choose Claim JSON Type "String"
* Make sure the following are on: 'Full group path', 'Add to ID token', 'Add to access token', 'Add to userinfo'
* Save

Now that you have the global Client scope configured, you can edit your client.

* Go to 'Clients'
* Find your ldaplogin client
* Go to tab 'Client scopes'
* Click 'Add client scope'
* Find the "emailQuota" scope and add it

Now test the result.

* Below 'Client scopes' there is a second level of tabs. Click 'Evaluate'.
* Select a user in the 'Users' input field. The user must have an "emailQuota" attribute.
* Click 'Generated user info'
* The resulting JSON needs to have a field `emailQuota`.

## `groups_metadata` field

### Prerequisites

Keycloak groups can have attributes like `email` or `description`. However by default ldaplogin does not have access to these attributes, because they don't show up in the ID token. This section explains a configuration to make these group attributes available to ldaplogin.

In order to make this work, every group needs to have a single `group\_metadata` attribute (note the singular!) containing a string of JSON with the following format:
```
{"path":"_$PATH_","description":"_$DESCRIPTION_","email":"_$EMAIL_"}
```

You need to manually maintain this information somehow! There are no mechanisms in Keycloak to do this for you. Also note that even if your group already has an `email` or `description` attribute, you will have to maintain the `group\_metadata` attribute as well, even though it seems redundant.

With this 'group\_metadata' JSON available Keycloak can compile a 'groups\_metadata' (note the plural!) field in the ID token which contains detail about all of the user's groups. This section explains how to set this up.

### Setup

You should probably configure a Keycloak-wide Client scope called 'groups\_metadata' which adds this field.

* Log in to your Keycloak instance as admin and go to your realm
* Go to 'Client scopes'
* Click 'Create client scope' and use name "groups\_metadata"
* Choose Type 'Default'
* Disable 'Display on consent screen'
* Save
* Go to 'Mappers'
* Click 'Configure a new mapper'
* Click 'User Attribute'
* Choose Name "groups\_metadata"
* Choose User Attribute "group\_metadata" (note the singular!)
* Choose Token Claim Name "groups\_metadata" (note the plural!)
* Choose Claim JSON Type "JSON"
* Make sure the following are on: "Full group path", "Add to ID token", "Add to access token", "Add to userinfo"
* Enable "Multivalued" and "Aggregate attribute values"
* Save

Now that you have the global Client scope configured, you can edit your client.

* Go to 'Clients'
* Find your ldaplogin client
* Go to tab 'Client scopes'
* Click 'Add client scope'
* Find the 'groups\_metadata' scope and add it

Now test the result.

* Below 'Client scopes' there is a second level of tabs. Click 'Evaluate'.
* Select a user in the 'Users' input field. The user must be member of at least one group.
* Click 'Generated user info'
* The resulting JSON needs to have a field `groups_metadata` with a list of JSON objects describing the user's groups.

If it doesn't work check the Mapper configuration of your client scope and verify that the user is member of a group, and the group has a `group_metadata` attribute with valid JSON in it.
