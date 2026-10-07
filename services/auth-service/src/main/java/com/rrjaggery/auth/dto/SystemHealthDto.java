package com.rrjaggery.auth.dto;

import java.util.ArrayList;
import java.util.List;

public class SystemHealthDto {

    private String overallStatus;
    private String checkedAt;
    private HealthSummary summary;
    private List<ServiceHealthItem> services = new ArrayList<>();
    private DatabaseHealthItem database;
    private List<SchemaHealthItem> schemas = new ArrayList<>();
    private List<ContainerHealthItem> containers = new ArrayList<>();
    private RedisHealthItem redis;
    private GatewayHealthItem gateway;
    private FrontendHealthItem frontend;
    private RuntimeHealthItem runtime;
    private MigrationHealthItem structureAndMigration;
    private BackupHealthItem backup;

    public SystemHealthDto() {}

    public String getOverallStatus() { return overallStatus; }
    public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }

    public String getCheckedAt() { return checkedAt; }
    public void setCheckedAt(String checkedAt) { this.checkedAt = checkedAt; }

    public HealthSummary getSummary() { return summary; }
    public void setSummary(HealthSummary summary) { this.summary = summary; }

    public List<ServiceHealthItem> getServices() { return services; }
    public void setServices(List<ServiceHealthItem> services) { this.services = services; }

    public DatabaseHealthItem getDatabase() { return database; }
    public void setDatabase(DatabaseHealthItem database) { this.database = database; }

    public List<SchemaHealthItem> getSchemas() { return schemas; }
    public void setSchemas(List<SchemaHealthItem> schemas) { this.schemas = schemas; }

    public List<ContainerHealthItem> getContainers() { return containers; }
    public void setContainers(List<ContainerHealthItem> containers) { this.containers = containers; }

    public RedisHealthItem getRedis() { return redis; }
    public void setRedis(RedisHealthItem redis) { this.redis = redis; }

    public GatewayHealthItem getGateway() { return gateway; }
    public void setGateway(GatewayHealthItem gateway) { this.gateway = gateway; }

    public FrontendHealthItem getFrontend() { return frontend; }
    public void setFrontend(FrontendHealthItem frontend) { this.frontend = frontend; }

    public RuntimeHealthItem getRuntime() { return runtime; }
    public void setRuntime(RuntimeHealthItem runtime) { this.runtime = runtime; }

    public MigrationHealthItem getStructureAndMigration() { return structureAndMigration; }
    public void setStructureAndMigration(MigrationHealthItem structureAndMigration) { this.structureAndMigration = structureAndMigration; }

    public BackupHealthItem getBackup() { return backup; }
    public void setBackup(BackupHealthItem backup) { this.backup = backup; }

    // Nested classes
    public static class HealthSummary {
        private int totalServices;
        private int healthyServices;
        private boolean databaseHealthy;
        private int totalSchemas;
        private int healthySchemas;
        private boolean redisHealthy;
        private boolean gatewayHealthy;
        private int containersTotal;
        private int containersHealthy;

        public HealthSummary() {}

        public int getTotalServices() { return totalServices; }
        public void setTotalServices(int totalServices) { this.totalServices = totalServices; }

        public int getHealthyServices() { return healthyServices; }
        public void setHealthyServices(int healthyServices) { this.healthyServices = healthyServices; }

        public boolean isDatabaseHealthy() { return databaseHealthy; }
        public void setDatabaseHealthy(boolean databaseHealthy) { this.databaseHealthy = databaseHealthy; }

        public int getTotalSchemas() { return totalSchemas; }
        public void setTotalSchemas(int totalSchemas) { this.totalSchemas = totalSchemas; }

        public int getHealthySchemas() { return healthySchemas; }
        public void setHealthySchemas(int healthySchemas) { this.healthySchemas = healthySchemas; }

        public boolean isRedisHealthy() { return redisHealthy; }
        public void setRedisHealthy(boolean redisHealthy) { this.redisHealthy = redisHealthy; }

        public boolean isGatewayHealthy() { return gatewayHealthy; }
        public void setGatewayHealthy(boolean gatewayHealthy) { this.gatewayHealthy = gatewayHealthy; }

        public int getContainersTotal() { return containersTotal; }
        public void setContainersTotal(int containersTotal) { this.containersTotal = containersTotal; }

        public int getContainersHealthy() { return containersHealthy; }
        public void setContainersHealthy(int containersHealthy) { this.containersHealthy = containersHealthy; }
    }

    public static class ServiceHealthItem {
        private String serviceKey;
        private String displayName;
        private String status; // UP, DOWN, DEGRADED, NOT CONFIGURED
        private int port;
        private String schema;
        private String healthEndpoint;
        private long responseTimeMs;
        private String version;
        private String springBootVersion;
        private String javaVersion;
        private String databaseConnectivity;
        private String error;

        public ServiceHealthItem() {}

        public String getServiceKey() { return serviceKey; }
        public void setServiceKey(String serviceKey) { this.serviceKey = serviceKey; }

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }

        public String getSchema() { return schema; }
        public void setSchema(String schema) { this.schema = schema; }

        public String getHealthEndpoint() { return healthEndpoint; }
        public void setHealthEndpoint(String healthEndpoint) { this.healthEndpoint = healthEndpoint; }

        public long getResponseTimeMs() { return responseTimeMs; }
        public void setResponseTimeMs(long responseTimeMs) { this.responseTimeMs = responseTimeMs; }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }

        public String getSpringBootVersion() { return springBootVersion; }
        public void setSpringBootVersion(String springBootVersion) { this.springBootVersion = springBootVersion; }

        public String getJavaVersion() { return javaVersion; }
        public void setJavaVersion(String javaVersion) { this.javaVersion = javaVersion; }

        public String getDatabaseConnectivity() { return databaseConnectivity; }
        public void setDatabaseConnectivity(String databaseConnectivity) { this.databaseConnectivity = databaseConnectivity; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }

    public static class DatabaseHealthItem {
        private String engine;
        private String databaseName;
        private String status;
        private String connection;
        private String version;
        private long responseTimeMs;
        private int schemaCount;
        private int totalTables;

        public DatabaseHealthItem() {}

        public String getEngine() { return engine; }
        public void setEngine(String engine) { this.engine = engine; }

        public String getDatabaseName() { return databaseName; }
        public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getConnection() { return connection; }
        public void setConnection(String connection) { this.connection = connection; }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }

        public long getResponseTimeMs() { return responseTimeMs; }
        public void setResponseTimeMs(long responseTimeMs) { this.responseTimeMs = responseTimeMs; }

        public int getSchemaCount() { return schemaCount; }
        public void setSchemaCount(int schemaCount) { this.schemaCount = schemaCount; }

        public int getTotalTables() { return totalTables; }
        public void setTotalTables(int totalTables) { this.totalTables = totalTables; }
    }

    public static class SchemaHealthItem {
        private String schemaName;
        private String ownerService;
        private String status;
        private int tableCount;
        private long totalEstimatedRecords;
        private List<TableHealthItem> tables = new ArrayList<>();

        public SchemaHealthItem() {}

        public String getSchemaName() { return schemaName; }
        public void setSchemaName(String schemaName) { this.schemaName = schemaName; }

        public String getOwnerService() { return ownerService; }
        public void setOwnerService(String ownerService) { this.ownerService = ownerService; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public int getTableCount() { return tableCount; }
        public void setTableCount(int tableCount) { this.tableCount = tableCount; }

        public long getTotalEstimatedRecords() { return totalEstimatedRecords; }
        public void setTotalEstimatedRecords(long totalEstimatedRecords) { this.totalEstimatedRecords = totalEstimatedRecords; }

        public List<TableHealthItem> getTables() { return tables; }
        public void setTables(List<TableHealthItem> tables) { this.tables = tables; }
    }

    public static class TableHealthItem {
        private String tableName;
        private long recordCount;
        private boolean isEstimate;
        private String status;

        public TableHealthItem() {}

        public TableHealthItem(String tableName, long recordCount, boolean isEstimate, String status) {
            this.tableName = tableName;
            this.recordCount = recordCount;
            this.isEstimate = isEstimate;
            this.status = status;
        }

        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }

        public long getRecordCount() { return recordCount; }
        public void setRecordCount(long recordCount) { this.recordCount = recordCount; }

        public boolean isEstimate() { return isEstimate; }
        public void setEstimate(boolean estimate) { isEstimate = estimate; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class ContainerHealthItem {
        private String containerName;
        private String service;
        private String status;
        private String health;
        private int port;
        private String uptime;
        private String note;

        public ContainerHealthItem() {}

        public ContainerHealthItem(String containerName, String service, String status, String health, int port, String uptime, String note) {
            this.containerName = containerName;
            this.service = service;
            this.status = status;
            this.health = health;
            this.port = port;
            this.uptime = uptime;
            this.note = note;
        }

        public String getContainerName() { return containerName; }
        public void setContainerName(String containerName) { this.containerName = containerName; }

        public String getService() { return service; }
        public void setService(String service) { this.service = service; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getHealth() { return health; }
        public void setHealth(String health) { this.health = health; }

        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }

        public String getUptime() { return uptime; }
        public void setUptime(String uptime) { this.uptime = uptime; }

        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }

    public static class RedisHealthItem {
        private String status;
        private String host;
        private int port;
        private long responseTimeMs;
        private String version;
        private String usedMemory;
        private boolean connected;

        public RedisHealthItem() {}

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }

        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }

        public long getResponseTimeMs() { return responseTimeMs; }
        public void setResponseTimeMs(long responseTimeMs) { this.responseTimeMs = responseTimeMs; }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }

        public String getUsedMemory() { return usedMemory; }
        public void setUsedMemory(String usedMemory) { this.usedMemory = usedMemory; }

        public boolean isConnected() { return connected; }
        public void setConnected(boolean connected) { this.connected = connected; }
    }

    public static class GatewayHealthItem {
        private String name;
        private String status;
        private int port;
        private long responseTimeMs;
        private String details;

        public GatewayHealthItem() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }

        public long getResponseTimeMs() { return responseTimeMs; }
        public void setResponseTimeMs(long responseTimeMs) { this.responseTimeMs = responseTimeMs; }

        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }
    }

    public static class FrontendHealthItem {
        private String status;
        private String gateway;
        private String buildMode;

        public FrontendHealthItem() {}

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getGateway() { return gateway; }
        public void setGateway(String gateway) { this.gateway = gateway; }

        public String getBuildMode() { return buildMode; }
        public void setBuildMode(String buildMode) { this.buildMode = buildMode; }
    }

    public static class RuntimeHealthItem {
        private String hostArchitecture;
        private String osName;
        private String osVersion;
        private String javaVersion;
        private String javaVendor;
        private int availableProcessors;
        private long freeMemoryMb;
        private long totalMemoryMb;

        public RuntimeHealthItem() {}

        public String getHostArchitecture() { return hostArchitecture; }
        public void setHostArchitecture(String hostArchitecture) { this.hostArchitecture = hostArchitecture; }

        public String getOsName() { return osName; }
        public void setOsName(String osName) { this.osName = osName; }

        public String getOsVersion() { return osVersion; }
        public void setOsVersion(String osVersion) { this.osVersion = osVersion; }

        public String getJavaVersion() { return javaVersion; }
        public void setJavaVersion(String javaVersion) { this.javaVersion = javaVersion; }

        public String getJavaVendor() { return javaVendor; }
        public void setJavaVendor(String javaVendor) { this.javaVendor = javaVendor; }

        public int getAvailableProcessors() { return availableProcessors; }
        public void setAvailableProcessors(int availableProcessors) { this.availableProcessors = availableProcessors; }

        public long getFreeMemoryMb() { return freeMemoryMb; }
        public void setFreeMemoryMb(long freeMemoryMb) { this.freeMemoryMb = freeMemoryMb; }

        public long getTotalMemoryMb() { return totalMemoryMb; }
        public void setTotalMemoryMb(long totalMemoryMb) { this.totalMemoryMb = totalMemoryMb; }
    }

    public static class MigrationHealthItem {
        private String migrationStatus;
        private int expectedSchemas;
        private int detectedSchemas;
        private int missingSchemas;
        private int structuralTablesDetected;

        public MigrationHealthItem() {}

        public String getMigrationStatus() { return migrationStatus; }
        public void setMigrationStatus(String migrationStatus) { this.migrationStatus = migrationStatus; }

        public int getExpectedSchemas() { return expectedSchemas; }
        public void setExpectedSchemas(int expectedSchemas) { this.expectedSchemas = expectedSchemas; }

        public int getDetectedSchemas() { return detectedSchemas; }
        public void setDetectedSchemas(int detectedSchemas) { this.detectedSchemas = detectedSchemas; }

        public int getMissingSchemas() { return missingSchemas; }
        public void setMissingSchemas(int missingSchemas) { this.missingSchemas = missingSchemas; }

        public int getStructuralTablesDetected() { return structuralTablesDetected; }
        public void setStructuralTablesDetected(int structuralTablesDetected) { this.structuralTablesDetected = structuralTablesDetected; }
    }

    public static class BackupHealthItem {
        private String status;
        private String lastBackupTime;
        private String details;

        public BackupHealthItem() {}

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getLastBackupTime() { return lastBackupTime; }
        public void setLastBackupTime(String lastBackupTime) { this.lastBackupTime = lastBackupTime; }

        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }
    }
}
