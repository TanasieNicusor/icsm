package com.example.icsm.service;

import org.springframework.stereotype.Service;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BackupService {

    private final String backupDirPath = "backups";

    public List<BackupInfo> getAllBackups() {
        File backupDir = new File(backupDirPath);
        if (!backupDir.exists() || !backupDir.isDirectory()) {
            return new ArrayList<>();
        }

        File[] files = backupDir.listFiles((dir, name) -> name.endsWith(".zip"));
        if (files == null) {
            return new ArrayList<>();
        }

        return Arrays.stream(files)
                .map(file -> new BackupInfo(
                        file.getName(),
                        file.length(),
                        file.lastModified()
                ))
                .sorted(Comparator.comparing(BackupInfo::lastModified).reversed())
                .collect(Collectors.toList());
    }

    public record BackupInfo(String filename, long size, long lastModified) {
        public String getFormattedSize() {
            if (size < 1024) return size + " B";
            int z = (63 - Long.numberOfLeadingZeros(size)) / 10;
            return String.format("%.1f %sB", (double)size / (1L << (z * 10)), " KMGTPE".charAt(z));
        }

        public String getFormattedDate() {
            return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(lastModified));
        }
    }
}
