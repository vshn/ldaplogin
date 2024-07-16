package services.ldap;

import com.github.benmanes.caffeine.cache.Cache;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.server.core.api.interceptor.context.ModDnAva;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.xdbm.*;

import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

public class OpenIdStore implements Store {
    @Override
    public void setPartitionPath(URI uri) {

    }

    @Override
    public URI getPartitionPath() {
        return null;
    }

    @Override
    public void setSyncOnWrite(boolean b) {

    }

    @Override
    public boolean isSyncOnWrite() {
        return false;
    }

    @Override
    public void setCacheSize(int i) {

    }

    @Override
    public int getCacheSize() {
        return 0;
    }

    @Override
    public void addIndex(Index<?, String> index) throws Exception {

    }

    @Override
    public Index<String, String> getPresenceIndex() {
        return null;
    }

    @Override
    public Index<Dn, String> getAliasIndex() {
        return null;
    }

    @Override
    public Index<String, String> getOneAliasIndex() {
        return null;
    }

    @Override
    public Index<String, String> getSubAliasIndex() {
        return null;
    }

    @Override
    public String getSuffixId(PartitionTxn partitionTxn) throws LdapException {
        return "";
    }

    @Override
    public Index<ParentIdAndRdn, String> getRdnIndex() {
        return null;
    }

    @Override
    public Index<String, String> getObjectClassIndex() {
        return null;
    }

    @Override
    public Index<String, String> getEntryCsnIndex() {
        return null;
    }

    @Override
    public Iterator<String> getUserIndices() {
        return null;
    }

    @Override
    public Iterator<String> getSystemIndices() {
        return null;
    }

    @Override
    public boolean hasIndexOn(AttributeType attributeType) throws LdapException {
        return false;
    }

    @Override
    public boolean hasUserIndexOn(AttributeType attributeType) throws LdapException {
        return false;
    }

    @Override
    public boolean hasSystemIndexOn(AttributeType attributeType) throws LdapException {
        return false;
    }

    @Override
    public Index<?, String> getIndex(AttributeType attributeType) throws IndexNotFoundException {
        return null;
    }

    @Override
    public Index<?, String> getUserIndex(AttributeType attributeType) throws IndexNotFoundException {
        return null;
    }

    @Override
    public Index<?, String> getSystemIndex(AttributeType attributeType) throws IndexNotFoundException {
        return null;
    }

    @Override
    public String getEntryId(PartitionTxn partitionTxn, Dn dn) throws LdapException {
        return "";
    }

    @Override
    public Dn getEntryDn(PartitionTxn partitionTxn, String s) throws LdapException {
        return null;
    }

    @Override
    public String getParentId(PartitionTxn partitionTxn, String s) throws LdapException {
        return "";
    }

    @Override
    public long count(PartitionTxn partitionTxn) throws LdapException {
        return 0;
    }

    @Override
    public Entry delete(PartitionTxn partitionTxn, String s) throws LdapException {
        return null;
    }

    @Override
    public Entry fetch(PartitionTxn partitionTxn, String s) throws LdapException {
        return null;
    }

    @Override
    public Entry fetch(PartitionTxn partitionTxn, String s, Dn dn) throws LdapException {
        return null;
    }

    @Override
    public long getChildCount(PartitionTxn partitionTxn, String s) throws LdapException {
        return 0;
    }

    @Override
    public Entry modify(PartitionTxn partitionTxn, Dn dn, Modification... modifications) throws LdapException {
        return null;
    }

    @Override
    public void rename(PartitionTxn partitionTxn, Dn dn, Rdn rdn, boolean b, Entry entry) throws LdapException {

    }

    @Override
    public void moveAndRename(PartitionTxn partitionTxn, Dn dn, Dn dn1, Rdn rdn, Map<String, List<ModDnAva>> map, Entry entry) throws LdapException {

    }

    @Override
    public void move(PartitionTxn partitionTxn, Dn dn, Dn dn1, Dn dn2, Entry entry) throws LdapException {

    }

    @Override
    public MasterTable getMasterTable() {
        return null;
    }

    @Override
    public ReadWriteLock getReadWriteLock() {
        return null;
    }

    @Override
    public Cache<String, Dn> getAliasCache() {
        return null;
    }
}
