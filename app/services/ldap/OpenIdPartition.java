package services.ldap;

import entities.Group;
import entities.ScopedGroup;
import entities.Service;
import entities.User;
import org.apache.directory.api.ldap.model.cursor.ListCursor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.*;
import org.apache.directory.server.core.api.DnFactory;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursorImpl;
import org.apache.directory.server.core.api.interceptor.context.*;
import org.apache.directory.server.core.api.partition.*;
import org.apache.directory.server.xdbm.search.Evaluator;
import store.GroupsStore;
import store.UsersStore;
import store.ServicesStore;
import util.Config;
import util.CustomLogger;

import javax.inject.Inject;
import javax.naming.InvalidNameException;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class OpenIdPartition extends AbstractPartition {

    private final CustomLogger logger = new CustomLogger(this.getClass());

    private final String groupsScope;

    private DnFactory dnFactory;

    private final Dn peopleDn;

    private final Dn groupsDn;

    private final CustomEvaluatorBuilder evaluatorBuilder;

    private final Map<Dn, Service> views;
    private final Map<Dn, Service> serviceAccounts;

    @Inject
    private UsersStore usersStore;

    @Inject
    private GroupsStore groupsStore;

    public static OpenIdPartition createPartition(SchemaManager schemaManager, DnFactory dnFactory, String id, String suffix, int cacheSize, File workingDirectory) throws Exception {
        OpenIdPartition partition = new OpenIdPartition(schemaManager, dnFactory, dnFactory.create(suffix));
        partition.setId(id);
        return partition;
    }

    public OpenIdPartition(SchemaManager schemaManager, DnFactory dnFactory, Dn suffixDn) {
        try {
            this.groupsScope = normalizeScope(Config.Option.LDAP_GROUPS_SCOPE.get());
            this.dnFactory = dnFactory;
            this.suffixDn = suffixDn;
            this.peopleDn = suffixDn.add("ou=People");
            this.groupsDn = suffixDn.add("ou=Groups");
            Map<Dn, Service> views = new HashMap<>();
            Map<Dn, Service> serviceAccounts = new HashMap<>();
            for (Service service : ServicesStore.getAll()) {
                Dn dn = dnFactory.create("ou=" + service.getId(), "ou=Service Access", "ou=Views", suffixDn.toString());
                views.put(dn, service);
                Entry serviceAccount = serviceAccountEntryFromService(service);
                serviceAccounts.put(serviceAccount.getDn(), service);
            }
            this.views = Collections.unmodifiableMap(views);
            this.serviceAccounts = Collections.unmodifiableMap(serviceAccounts);
            setSchemaManager(schemaManager);
            evaluatorBuilder = new CustomEvaluatorBuilder(new OpenIdStore(), getSchemaManager());
        } catch (LdapInvalidDnException e) {
            throw new RuntimeException();
        }
    }

    private static String normalizeScope(String scope) {
        scope = scope.trim();
        if (!scope.startsWith("/")) {
            scope = "/" + scope;
        }
        if (scope.endsWith("/")) {
            scope = scope.substring(0, scope.length() - 1);
        }
        return scope;
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

        try {
            // If the search directly matches an entry then return that
            Entry lookupEntry = lookupInternal(new LookupOperationContext(searchContext.getSession(), searchContext.getDn(), searchContext.getReturningAttributesString()));
            if (lookupEntry != null) {
                Evaluator evaluator = evaluatorBuilder.build(null, searchContext.getFilter());
                List<Entry> entries = evaluate(evaluator, lookupEntry) ? List.of(lookupEntry) : Collections.emptyList();
                return new EntryFilteringCursorImpl(new ListCursor<>(entries), searchContext, schemaManager);
            }

            if (views.containsKey(searchContext.getDn())) {
                Evaluator evaluator = evaluatorBuilder.build(null, searchContext.getFilter());
                Service service = views.get(searchContext.getDn());
                List<Entry> users = usersStore.getByGroupPath(service.getGroup())
                        .map(u -> peopleEntryFromUser(u))
                        .filter(e -> evaluate(evaluator, e))
                        .collect(Collectors.toList());
                return new EntryFilteringCursorImpl(new ListCursor<>(users), searchContext, schemaManager);
            }

            if (peopleDn.equals(searchContext.getDn())) {
                Evaluator evaluator = evaluatorBuilder.build(null, searchContext.getFilter());
                List<Entry> entries = usersStore.getAll().stream()
                        .map(this::peopleEntryFromUser)
                        .filter(e -> evaluate(evaluator, e))
                        .collect(Collectors.toList());
                return new EntryFilteringCursorImpl(new ListCursor<>(entries), searchContext, schemaManager);
            }

            if (groupsDn.equals(searchContext.getDn())) {
                Evaluator evaluator = evaluatorBuilder.build(null, searchContext.getFilter());
                List<Entry> entries = groupsStore.getAll().stream()
                        .map(g -> ScopedGroup.scoped(g, groupsScope))
                        .filter(Objects::nonNull)
                        .map(this::groupsEntryFromGroup)
                        .filter(e -> evaluate(evaluator, e))
                        .collect(Collectors.toList());
                return new EntryFilteringCursorImpl(new ListCursor<>(entries), searchContext, schemaManager);
            }

            return new EntryFilteringCursorImpl(new ListCursor<>(Collections.emptyList()), searchContext, schemaManager);
        } catch (Exception e) {
            logger.error(null, e.getMessage(), e);
            throw e;
        }
    }

    private Entry lookupInternal(LookupOperationContext lookupContext) {
        if (serviceAccounts.containsKey(lookupContext.getDn())) {
            return serviceAccountEntryFromService(serviceAccounts.get(lookupContext.getDn()));
        }

        User user = getUserByDn(lookupContext.getDn());
        if (user != null) {
            return peopleEntryFromUser(user);
        }

        ScopedGroup group = ScopedGroup.scoped(getGroupByDn(lookupContext.getDn()), groupsScope);
        if (group != null) {
            return groupsEntryFromGroup(group);
        }

        return null;
    }

    @Override
    public Entry lookup(LookupOperationContext lookupContext) throws LdapException {
        logger.info(null, "lookup " + lookupContext.getDn().toString());
        try {
            return lookupInternal(lookupContext);
        } catch (Exception e) {
            logger.error(null, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public boolean hasEntry(HasEntryOperationContext hasEntryContext) throws LdapException {
        logger.info(null, "hasEntry " + hasEntryContext.getDn().toString());
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

    private Entry peopleEntryFromUser(User user) {
        Entry entry = new DefaultEntry(schemaManager, userDn(user));
        entry.put("uid", user.getUid());
        entry.put("mail", user.getEmail());
        entry.put("givenName", user.getFirstName());
        entry.put("sn", user.getLastName());
        entry.put("displayName", user.getFirstName() + " " + user.getLastName());
        entry.put("cn", user.getFirstName() + " " + user.getLastName());
        entry.put("userPassword", user.getPasswordHash());
        entry.put("objectClass", "inetOrgPerson", "inetUser", "mailRecipient", "organizationalPerson", "person", "top", "groupMember");
        String[] groups = user.getGroupPaths().stream()
                .map(groupsStore::getByPath)
                .map(g -> ScopedGroup.scoped(g, groupsScope))
                .filter(Objects::nonNull)
                .map(this::groupDn)
                .map(Dn::toString)
                .toArray(String[]::new);
        entry.put("memberOf", groups);
        return entry;
    }

    private Dn userDn(User user) {
        try {
            return dnFactory.create("uid=" + user.getUid(), "ou=People", getSuffixDn().toString());
        } catch (LdapInvalidDnException e) {
            throw new RuntimeException(e);
        }
    }

    private Dn groupDn(ScopedGroup group) {
        try {
            return dnFactory.create("cn=" + group.getCn(), "ou=Groups", getSuffixDn().toString());
        } catch (LdapInvalidDnException e) {
            throw new RuntimeException(e);
        }
    }

    private Entry groupsEntryFromGroup(ScopedGroup group) {
        Entry entry = new DefaultEntry(schemaManager, groupDn(group));
        entry.put("cn", group.getCn());
        entry.put("objectClass", "groupofuniquenames", "top");
        entry.put("uniqueMember", usersStore.getByGroupPath(group.getRealGroup().getPath()).map(this::userDn).map(Dn::toString).toArray(String[]::new));
        return entry;
    }

    private Entry serviceAccountEntryFromService(Service service) {
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

    private User getUserByDn(Dn dn) {
        if (dn.getParent().equals(peopleDn) && "uid".equals(dn.getRdn().getType())) {
            return usersStore.getByUid(dn.getRdn().getValue());
        }
        return null;
    }

    private Group getGroupByDn(Dn dn) {
        if (dn.getParent().equals(groupsDn) && "cn".equals(dn.getRdn().getType())) {
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
