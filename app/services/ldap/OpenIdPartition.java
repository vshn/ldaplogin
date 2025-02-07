package services.ldap;

import entities.*;
import org.apache.directory.api.ldap.model.cursor.ListCursor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.*;
import org.apache.directory.server.core.api.DnFactory;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursorImpl;
import org.apache.directory.server.core.api.interceptor.context.*;
import org.apache.directory.server.core.api.partition.*;
import org.apache.directory.server.xdbm.search.Evaluator;
import services.OpenId;
import store.GroupsStore;
import store.ResourcesStore;
import store.UsersStore;
import store.ServicesStore;
import util.CustomLogger;

import javax.inject.Inject;
import javax.naming.InvalidNameException;
import java.io.File;
import java.util.*;

public class OpenIdPartition extends AbstractPartition {

    private final CustomLogger logger = new CustomLogger(this.getClass());

    private DnFactory dnFactory;

    private final Dn peopleDn;

    private final Dn groupsDn;

    private final Dn resourcesDn;

    private final CustomEvaluatorBuilder evaluatorBuilder;

    private final Map<Dn, Service> serviceAccounts;

    @Inject
    private UsersStore usersStore;

    @Inject
    private GroupsStore groupsStore;

    @Inject
    private ResourcesStore resourcesStore;

    @Inject
    private OpenId openId;

    public static OpenIdPartition createPartition(SchemaManager schemaManager, DnFactory dnFactory, String id, String suffix, int cacheSize, File workingDirectory) throws Exception {
        OpenIdPartition partition = new OpenIdPartition(schemaManager, dnFactory, dnFactory.create(suffix));
        partition.setId(id);
        return partition;
    }

    public OpenIdPartition(SchemaManager schemaManager, DnFactory dnFactory, Dn suffixDn) {
        try {
            this.dnFactory = dnFactory;
            this.suffixDn = suffixDn;
            this.peopleDn = suffixDn.add("ou=People");
            this.groupsDn = suffixDn.add("ou=Groups");
            this.resourcesDn = suffixDn.add("ou=Resources");
            Map<Dn, Service> serviceAccounts = new HashMap<>();
            for (Service service : ServicesStore.getAll()) {
                Entry serviceAccount = entryFromService(service);
                serviceAccounts.put(serviceAccount.getDn(), service);
            }
            this.serviceAccounts = Collections.unmodifiableMap(serviceAccounts);
            setSchemaManager(schemaManager);
            evaluatorBuilder = new CustomEvaluatorBuilder(new OpenIdStore(), getSchemaManager());
        } catch (LdapInvalidDnException e) {
            throw new RuntimeException();
        }
    }

    @Override
    protected void doDestroy(PartitionTxn partitionTxn) throws LdapException {

    }

    @Override
    protected void doInit() throws InvalidNameException, LdapException {

    }

    @Override
    protected void doRepair() throws LdapException {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public PartitionReadTxn beginReadTransaction() {
        return null;
    }

    @Override
    public PartitionWriteTxn beginWriteTransaction() {
        return null;
    }

    @Override
    public Entry delete(DeleteOperationContext deleteContext) throws LdapException {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public void add(AddOperationContext addContext) throws LdapException {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public void modify(ModifyOperationContext modifyContext) throws LdapException {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public EntryFilteringCursor search(SearchOperationContext searchContext) throws LdapException {
        logger.info(null, "search '" + searchContext.getDn().toString() + "' with filter '" + searchContext.getFilter() + "'");
        final Service service = getServiceFromPrincipal(searchContext.getEffectivePrincipal());
        Rdn serviceRdn = new Rdn(schemaManager, "ou", service.getId());

        try {
            Evaluator evaluator = evaluatorBuilder.build(null, searchContext.getFilter());

            // If the search directly matches an entry then return that
            Entry lookupEntry = lookupInternal(service, new LookupOperationContext(searchContext.getSession(), searchContext.getDn(), searchContext.getReturningAttributesString()));
            if (lookupEntry != null) {
                List<Entry> entries = evaluate(evaluator, lookupEntry) ? List.of(lookupEntry) : Collections.emptyList();
                return new EntryFilteringCursorImpl(new ListCursor<>(entries), searchContext, schemaManager);
            }

            List<Entry> entries = new ArrayList<>();

            Dn servicePeopleDn = peopleDn.add(serviceRdn);
            if (searchContext.getDn().equals(servicePeopleDn)
                    || (searchContext.getScope() == SearchScope.ONELEVEL && searchContext.getDn().equals(peopleDn))
                    || (searchContext.getScope() == SearchScope.SUBTREE && searchContext.getDn().isAncestorOf(servicePeopleDn))) {
                usersStore.getByGroupPath(service.getGroup())
                    .map(u -> entryFromUser(u, service, false))
                    .filter(e -> evaluate(evaluator, e))
                    .forEach(entries::add);
            }

            Dn serviceGroupsDn = groupsDn.add(serviceRdn);
            if (searchContext.getDn().equals(serviceGroupsDn)
                    || (searchContext.getScope() == SearchScope.ONELEVEL && searchContext.getDn().equals(groupsDn))
                    || (searchContext.getScope() == SearchScope.SUBTREE && searchContext.getDn().isAncestorOf(serviceGroupsDn))) {
                groupsStore.getAll()
                    .map(g -> entryFromGroup(g, service))
                    .filter(e -> evaluate(evaluator, e))
                    .forEach(entries::add);
            }

            if (searchContext.getDn().equals(resourcesDn)
                    || (searchContext.getScope() == SearchScope.ONELEVEL && searchContext.getDn().equals(resourcesDn.getParent()))
                    || (searchContext.getScope() == SearchScope.SUBTREE && searchContext.getDn().isAncestorOf(resourcesDn))) {
                resourcesStore.getAll()
                        .map(r -> entryFromResource(r))
                        .filter(e -> evaluate(evaluator, e))
                        .forEach(entries::add);
            }

            return new EntryFilteringCursorImpl(new ListCursor<>(entries), searchContext, schemaManager);
        } catch (Exception e) {
            logger.error(null, e.getMessage(), e);
            throw e;
        }
    }

    private Service getServiceFromPrincipal(LdapPrincipal principal) {
        Service service = null;
        if (principal != null && principal.getDn() != null) {
            // Assume the service itself is logged in
            Dn loggedIn = principal.getDn();
            service = serviceAccounts.get(loggedIn);
            if (service == null) {
                service = getServiceFromUserDn(loggedIn);
            }
        }
        return service;
    }

    private Entry lookupInternal(Service service, LookupOperationContext lookupContext) {
        // Note that this method is also called on authentication. In this case "service" will be null because nobody's logged in yet.
        if (serviceAccounts.containsKey(lookupContext.getDn())) {
            return entryFromService(serviceAccounts.get(lookupContext.getDn()));
        }

        // Either search for a user in the given service context or extract the service context from the Dn
        Service userService = service == null ? getServiceFromUserDn(lookupContext.getDn()) : service;
        User user = getUserByDn(lookupContext.getDn(), userService);
        if (user != null) {
            return entryFromUser(user, userService, true);
        }


        // Either search for a group in the given service context or extract the service context from the Dn
        Service groupService = service == null ? getServiceFromGroupsDn(lookupContext.getDn()) : service;
        // while the groups are the same for all services, we still can only return groups if a service is given
        Group group = groupService == null ? null : getGroupByDn(lookupContext.getDn());
        if (group != null) {
            return entryFromGroup(group, groupService);
        }

        return null;
    }

    @Override
    public Entry lookup(LookupOperationContext lookupContext) throws LdapException {
        logger.info(null, "lookup '" + lookupContext.getDn().toString() + "'");
        final Service service = getServiceFromPrincipal(lookupContext.getEffectivePrincipal());

        try {
            return lookupInternal(service, lookupContext);
        } catch (Exception e) {
            logger.error(null, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public boolean hasEntry(HasEntryOperationContext hasEntryContext) throws LdapException {
        logger.info(null, "hasEntry '" + hasEntryContext.getDn().toString() + "'");
        return false;
    }

    @Override
    public void rename(RenameOperationContext renameContext) throws LdapException {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public void move(MoveOperationContext moveContext) throws LdapException {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public void moveAndRename(MoveAndRenameOperationContext moveAndRenameContext) throws LdapException {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public void unbind(UnbindOperationContext unbindContext) throws LdapException {
        // do nothing
    }

    @Override
    public void saveContextCsn(PartitionTxn partitionTxn) throws LdapException {
        // do nothing
    }

    @Override
    public Subordinates getSubordinates(PartitionTxn partitionTxn, Entry entry) throws LdapException {
        throw new IllegalStateException("not implemented");
    }

    private Entry entryFromUser(User user, Service service, boolean includePwHashes) {
        Entry entry = new DefaultEntry(schemaManager, userDn(user, service));
        entry.put("uid", user.getUid());
        entry.put("mail", user.getEmail());
        entry.put("givenName", user.getFirstName());
        entry.put("sn", user.getLastName());
        entry.put("displayName", user.getFirstName() + " " + user.getLastName());
        entry.put("cn", user.getFirstName() + " " + user.getLastName());
        if (user.getEmailQuota() != null) {
            entry.put("mailQuota", "" + user.getEmailQuota());
        }
        if (includePwHashes) {
            byte[][] activePasswords = user.getActivePasswords(service.getId(), openId);
            if (activePasswords.length > 0) {
                entry.put("userPassword", activePasswords);
            }
        }
        entry.put("objectClass", "inetOrgPerson", "inetUser", "mailRecipient", "organizationalPerson", "person", "top", "groupMember");
        String[] groups = user.getGroupPaths().stream()
                .map(groupsStore::getByPath)
                .filter(Objects::nonNull)
                .map(g -> groupDn(g, service))
                .map(Dn::toString)
                .toArray(String[]::new);
        entry.put("memberOf", groups);
        return entry;
    }

    private Dn userDn(User user, Service service) {
        try {
            return dnFactory.create("uid=" + user.getUid(), "ou=" + service.getId(), "ou=People", getSuffixDn().toString());
        } catch (LdapInvalidDnException e) {
            throw new RuntimeException(e);
        }
    }

    private Dn groupDn(Group group, Service service) {
        try {
            return dnFactory.create("cn=" + group.getCn(), "ou=" + service.getId(), "ou=Groups", getSuffixDn().toString());
        } catch (LdapInvalidDnException e) {
            throw new RuntimeException(e);
        }
    }

    private Entry entryFromGroup(Group group, Service service) {
        Entry entry = new DefaultEntry(schemaManager, groupDn(group, service));
        entry.put("cn", group.getCn());
        entry.put("objectClass", "groupofuniquenames", "top");
        entry.put("uniqueMember", usersStore.getByGroupPath(group.getPath()).map(u -> userDn(u, service)).map(Dn::toString).toArray(String[]::new));
        if (group.getDescription() != null) {
            entry.put("description", group.getDescription());
        }
        if (group.getEmail() != null) {
            entry.put("mail", group.getEmail());
        }
        return entry;
    }

    private Entry entryFromService(Service service) {
        try {
            Dn dn = dnFactory.create("uid=" + service.getId(), "ou=Services", getSuffixDn().toString());
            Entry entry = new DefaultEntry(schemaManager, dn);
            entry.put("uid", service.getId());
            entry.put("userPassword", service.getPasswordHash());
            entry.put("objectClass", "top");
            return entry;
        } catch (LdapInvalidDnException e) {
            throw new RuntimeException(e);
        }
    }

    private Entry entryFromResource(Resource resource) {
        try {
            Dn dn = dnFactory.create("cn=" + resource.getName(), "ou=Resources", getSuffixDn().toString());
            Entry entry = new DefaultEntry(schemaManager, dn);
            entry.put("cn", resource.getName());
            entry.put("displayName", resource.getName());
            entry.put("sn", resource.getName());
            entry.put("givenName", resource.getName());
            if (resource.getEmail() != null) {
                entry.put("mail", resource.getEmail());
            }
            if (resource.getMultipleBookings() != null) {
                entry.put("Multiplebookings", "" + resource.getMultipleBookings());
            }
            if (resource.getKind() != null) {
                entry.put("Kind", resource.getKind());
            }
            entry.put("objectClass", "top", "person", "inetOrgPerson", "organizationalPerson", "calEntry", "CalendarResource");
            return entry;
        } catch (LdapInvalidDnException e) {
            throw new RuntimeException(e);
        }
    }

    private User getUserByDn(Dn dn, Service service) {
        if (service == null) {
            return null;
        }
        if (dn.getParent().getParent().equals(peopleDn) && "uid".equals(dn.getRdn().getType())) {
            User user = usersStore.getByUid(dn.getRdn().getValue());
            return user.getGroupPaths().contains(service.getGroup()) ? user : null;
        }
        return null;
    }

    public Service getServiceFromUserDn(Dn dn) {
        if (dn.getParent().getParent().equals(peopleDn) && "ou".equals(dn.getRdn(1).getType())) {
            // we have something like uid=john.doe,ou=SERVICE,ou=People,dc=example,dc=com
            String serviceId = dn.getRdn(1).getValue();
            try {
                Dn serviceDn = dnFactory.create("uid=" + serviceId, "ou=Services", getSuffixDn().toString());
                return serviceAccounts.get(serviceDn);
            } catch (LdapInvalidDnException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    public Service getServiceFromGroupsDn(Dn dn) {
        if (dn.getParent().getParent().equals(groupsDn) && "ou".equals(dn.getRdn(1).getType())) {
            // we have something like uid=john.doe,ou=SERVICE,ou=People,dc=example,dc=com
            String serviceId = dn.getRdn(1).getValue();
            try {
                Dn serviceDn = dnFactory.create("uid=" + serviceId, "ou=Services", getSuffixDn().toString());
                return serviceAccounts.get(serviceDn);
            } catch (LdapInvalidDnException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    private Group getGroupByDn(Dn dn) {
        if (dn.getParent().getParent().equals(groupsDn) && "cn".equals(dn.getRdn().getType())) {
            return groupsStore.getByCn(dn.getRdn().getValue());
        }
        return null;
    }

    private boolean evaluate(Evaluator evaluator, Entry entry) {
        try {
            return evaluator.evaluate(entry);
        } catch (LdapException e) {
            return false;
        }
    }
}
