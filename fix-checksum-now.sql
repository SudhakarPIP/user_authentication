-- Immediate Fix: Update Flyway Checksum
-- Run this SQL to fix the checksum mismatch

USE pip;

-- Update checksum for version 2 to match current file
UPDATE flyway_schema_history 
SET checksum = -919341583 
WHERE version = '2';

-- Verify the update
SELECT version, description, checksum, installed_on 
FROM flyway_schema_history 
WHERE version = '2';

-- Expected result: checksum should be -919341583

