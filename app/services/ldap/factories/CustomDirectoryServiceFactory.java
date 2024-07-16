package services.ldap.factories;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.schema.LdapComparator;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.comparators.NormalizingComparator;
import org.apache.directory.api.ldap.model.schema.registries.ComparatorRegistry;
import org.apache.directory.api.ldap.model.schema.registries.SchemaLoader;
import org.apache.directory.api.ldap.schema.extractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.loader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.FileUtils;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.schema.SchemaPartition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class CustomDirectoryServiceFactory {
    private DirectoryService directoryService;

    private JdbmPartitionFactory partitionFactory;

    public CustomDirectoryServiceFactory() {
        try {
            directoryService = new DefaultDirectoryService();
            //directoryService.setShutdownHookEnabled(false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            partitionFactory = new JdbmPartitionFactory();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void init(String name) throws Exception {
        if ((directoryService != null) && directoryService.isStarted()) {
            return;
        }

        build(name);
    }

    private void buildInstanceDirectory(String name) throws IOException {
        String instanceDirectory = System.getProperty("workingDirectory");

        if (instanceDirectory == null) {
            instanceDirectory = System.getProperty("java.io.tmpdir") + "/server-work-" + name;
        }

        InstanceLayout instanceLayout = new InstanceLayout(instanceDirectory);

        if (instanceLayout.getInstanceDirectory().exists()) {
            try {
                FileUtils.deleteDirectory(instanceLayout.getInstanceDirectory());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        directoryService.setInstanceLayout(instanceLayout);
    }

    private void initSchema() throws Exception {
        File workingDirectory = directoryService.getInstanceLayout().getPartitionsDirectory();

        // Extract the schema on disk (a brand new one) and load the registries
        File schemaRepository = new File(workingDirectory, "schema");
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor(workingDirectory);

        try {
            extractor.extractOrCopy();
        } catch (IOException ioe) {
            // The schema has already been extracted, bypass
        }

        SchemaLoader loader = new LdifSchemaLoader(schemaRepository);
        SchemaManager schemaManager = new DefaultSchemaManager(loader);

        // We have to load the schema now, otherwise we won't be able
        // to initialize the Partitions, as we won't be able to parse
        // and normalize their suffix Dn
        schemaManager.loadAllEnabled();

        // Tell all the normalizer comparators that they should not normalize anything
        ComparatorRegistry comparatorRegistry = schemaManager.getComparatorRegistry();

        for (LdapComparator<?> comparator : comparatorRegistry) {
            if (comparator instanceof NormalizingComparator) {
                ((NormalizingComparator) comparator).setOnServer();
            }
        }

        directoryService.setSchemaManager(schemaManager);

        // Init the LdifPartition
        LdifPartition ldifPartition = new LdifPartition(schemaManager, directoryService.getDnFactory());
        ldifPartition.setPartitionPath(new File(workingDirectory, "schema").toURI());
        SchemaPartition schemaPartition = new SchemaPartition(schemaManager);
        schemaPartition.setWrappedPartition(ldifPartition);
        directoryService.setSchemaPartition(schemaPartition);

        List<Throwable> errors = schemaManager.getErrors();

        if (!errors.isEmpty()) {
            throw new Exception(Exceptions.printErrors(errors));
        }
    }

    private void initSystemPartition() throws Exception {
        // Inject the System Partition
        Partition systemPartition = partitionFactory.createPartition(directoryService.getSchemaManager(),
                directoryService.getDnFactory(),
                "system", ServerDNConstants.SYSTEM_DN, 500,
                new File(directoryService.getInstanceLayout().getPartitionsDirectory(), "system"));
        systemPartition.setSchemaManager(directoryService.getSchemaManager());

        partitionFactory.addIndex(systemPartition, SchemaConstants.OBJECT_CLASS_AT, 100);

        directoryService.setSystemPartition(systemPartition);
    }

    private void build(String name) throws Exception {
        directoryService.setInstanceId(name);
        buildInstanceDirectory(name);

        // Init the service now
        initSchema();
        initSystemPartition();

        directoryService.startup();
    }

    public DirectoryService getDirectoryService() throws Exception {
        return directoryService;
    }
}
