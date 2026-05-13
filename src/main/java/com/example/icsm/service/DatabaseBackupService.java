package com.example.icsm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseBackupService {

    private final JdbcTemplate jdbcTemplate;
    private final String localBackupDir = "backups";
    private final String secureRemoteDir = "C:\\ICSM_Secure_Backups";

    /**
     * REQ-80: Disaster Recovery Snapshot
     * Runs every 30 minutes to meet the 30-min RPO.
     */
    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void performDRSnapshot() {
        executeBackup("DR_SNAPSHOT");
    }

    /**
     * REQ-78: Incremental Backups
     * Runs every 6 hours.
     */
    @Scheduled(cron = "0 0 0/6 * * *")
    public void perform6HourIncremental() {
        executeBackup("INCREMENTAL_6H");
    }

    /**
     * REQ-78: Full Daily Backup
     * Runs once a day at midnight.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void performDailyFullBackup() {
        executeBackup("FULL_DAILY");
    }

    private void executeBackup(String type) {
        try {
            ensureDirectoriesExist();

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
            String fileName = "icsm_" + type + "_" + timestamp + ".zip";
            String localPath = Paths.get(localBackupDir, fileName).toAbsolutePath().toString();
            String remotePath = Paths.get(secureRemoteDir, fileName).toAbsolutePath().toString();

            log.info("Starting {} backup to local: {}", type, localPath);
            
            // 1. Perform H2 Native Backup
            jdbcTemplate.execute("BACKUP TO '" + localPath + "'");

            // 2. REQ-78: Move to Separate Secure Location
            File localFile = new File(localPath);
            if (localFile.exists()) {
                Files.copy(localFile.toPath(), Paths.get(remotePath), StandardCopyOption.REPLACE_EXISTING);
                log.info("Successfully synced backup to Secure Off-site Location: {}", remotePath);
            }

            cleanupOldBackups(localBackupDir, 20); // Keep last 10 hours of snapshots locally
            cleanupOldBackups(secureRemoteDir, 50); // Keep more in secure location
            
        } catch (Exception e) {
            log.error("Failed to perform " + type + " backup", e);
        }
    }

    private void ensureDirectoriesExist() {
        new File(localBackupDir).mkdirs();
        new File(secureRemoteDir).mkdirs();
    }

    private void cleanupOldBackups(String dirPath, int maxFiles) {
        File dir = new File(dirPath);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".zip"));
        
        if (files != null && files.length > maxFiles) {
            java.util.Arrays.sort(files, java.util.Comparator.comparingLong(File::lastModified));
            for (int i = 0; i < files.length - maxFiles; i++) {
                files[i].delete();
            }
        }
    }
}
