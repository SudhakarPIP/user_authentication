-- Fix Flyway Checksum Mismatch
-- Run this SQL script to repair Flyway checksums

-- Option 1: Repair checksum for version 2
-- First, get the correct checksum by running: mvn flyway:repair
-- Then update with the correct checksum value

-- Check current checksums
SELECT version, description, checksum, installed_on 
FROM flyway_schema_history 
ORDER BY installed_rank;

-- Option 2: Reset Flyway (DEVELOPMENT ONLY - Will lose migration history)
-- Uncomment below if you want to start fresh (only in development!)

-- DROP TABLE IF EXISTS flyway_schema_history;
-- DROP TABLE IF EXISTS verification_tokens;
-- DROP TABLE IF EXISTS users;

-- Option 3: Repair checksum manually
-- After running mvn flyway:repair, update with the new checksum
-- UPDATE flyway_schema_history 
-- SET checksum = <new_checksum_from_repair> 
-- WHERE version = '2';

