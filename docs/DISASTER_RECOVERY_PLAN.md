# Disaster Recovery Plan - ICSM System

This document outlines the disaster recovery procedure for the ICSM (Integrated Claim Support Management) system to ensure compliance with Requirement REQ-80.

## 1. Objectives
*   **Recovery Point Objective (RPO):** Maximum 30 minutes (Data loss will not exceed 30 minutes).
*   **Recovery Time Objective (RTO):** Maximum 4 hours (System will be back online within 4 hours of disaster declaration).

## 2. Backup Strategy (Meeting RPO)
To meet the 30-minute RPO, the following backup schedule is implemented:

### 2.1 Database Backups
*   **Target:** Full database state (H2).
*   **Frequency:** Every 30 minutes.
*   **Implementation:** 
    *   Service: `DatabaseBackupService.java`
    *   Automation: Handled internally by the Spring Boot application using `@Scheduled`.
*   **Method:** The system uses the H2 native `BACKUP` command to create consistent snapshots even while the application is running. Files are stored in the `backups/` directory.
*   **Storage:** Backups must be replicated to an off-site location (e.g., AWS S3, Azure Blob Storage, or a secure remote NAS).



### 2.2 Application Artifacts
*   **Source Code:** Managed via Git and pushed to the remote repository (GitHub/GitLab) immediately after every commit.
*   **Builds:** The `target/*.jar` file should be stored in an artifact repository (e.g., Nexus or GitHub Packages).

## 3. Recovery Procedure (Meeting RTO)
In the event of a total system failure, follow these steps to restore service:

### Step 1: Environment Provisioning (Est: 60 mins)
1.  Deploy a new server instance (Linux/Windows) with Java 17+ installed.
2.  Clone the repository or download the latest stable `.jar` file.

### Step 2: Data Restoration (Est: 30 mins)
1.  Download the latest backup from the off-site storage.
2.  Stop the application if it is running.
3.  Place the backup files into the `./data/` directory.
4.  Ensure file permissions allow the application to read/write to the directory.

### Step 3: Application Deployment (Est: 30 mins)
1.  Run the application using:
    ```bash
    mvn spring-boot:run
    ```
    or
    ```bash
    java -jar target/icsm-0.0.1-SNAPSHOT.jar
    ```
2.  Verify the `application.properties` points to the correct database path.

### Step 4: Verification (Est: 30 mins)
1.  Verify the H2 console is accessible (if enabled).
2.  Perform a "Smoke Test":
    *   Login to the system.
    *   Check if recent claims (within the last hour before failure) are present.
    *   Test one core functionality (e.g., viewing a profile).

## 4. Responsibility Matrix
| Task | Responsible |
| :--- | :--- |
| Backup Monitoring | DevOps / System Admin |
| Disaster Declaration | Project Manager |
| Recovery Execution | Engineering Team |

## 5. Revision History
| Date | Version | Description |
| :--- | :--- | :--- |
| 2026-05-13 | 1.0 | Initial DR Plan for REQ-80 |
