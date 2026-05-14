-- ───────────────────────────────────────────────────────────────
-- V004 — 3-tier Architecture necessary migration.
-- ───────────────────────────────────────────────────────────────

-- 1. Extend the users role enum to include 'barangay'.
--    MariaDB requires re-declaring the full ENUM.
ALTER TABLE `users`
  MODIFY COLUMN `role` ENUM('admin','barangay','staff') NOT NULL DEFAULT 'barangay';

-- 2. Add structural status to evacuation_centers.
--    Separate from occupancy — this is about building integrity.
ALTER TABLE `evacuation_centers`
  ADD COLUMN `structural_status` ENUM('safe','damaged','unsafe')
      NOT NULL DEFAULT 'safe' AFTER `is_active`,
  ADD COLUMN `structural_notes` TEXT DEFAULT NULL AFTER `structural_status`,
  ADD COLUMN `structural_updated_at` TIMESTAMP NULL DEFAULT NULL
      AFTER `structural_notes`,
  ADD COLUMN `structural_updated_by` BIGINT(20) UNSIGNED DEFAULT NULL
      AFTER `structural_updated_at`,
  ADD CONSTRAINT `fk_ec_structural_updater`
      FOREIGN KEY (`structural_updated_by`) REFERENCES `users`(`id`) ON DELETE SET NULL;

-- 3. Create supply_requests (request header).
CREATE TABLE `supply_requests` (
  `id` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `requesting_barangay` VARCHAR(128) NOT NULL,
  `requesting_user_id` BIGINT(20) UNSIGNED NOT NULL,
  `evacuation_center_id` BIGINT(20) UNSIGNED DEFAULT NULL,
  `status` ENUM('pending','approved','partially_fulfilled','fulfilled','rejected')
      NOT NULL DEFAULT 'pending',
  `notes` TEXT DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT current_timestamp(),
  `reviewed_by` BIGINT(20) UNSIGNED DEFAULT NULL,
  `reviewed_at` TIMESTAMP NULL DEFAULT NULL,
  `admin_notes` TEXT DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_status_created` (`status`,`created_at`),
  KEY `idx_barangay` (`requesting_barangay`),
  CONSTRAINT `fk_sr_user` FOREIGN KEY (`requesting_user_id`)
      REFERENCES `users`(`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_sr_center` FOREIGN KEY (`evacuation_center_id`)
      REFERENCES `evacuation_centers`(`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_sr_reviewer` FOREIGN KEY (`reviewed_by`)
      REFERENCES `users`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Create supply_request_items (line items — what was asked, what was sent).
CREATE TABLE `supply_request_items` (
  `id` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `request_id` BIGINT(20) UNSIGNED NOT NULL,
  `item_id` BIGINT(20) UNSIGNED NOT NULL,
  `quantity_requested` INT(11) NOT NULL,
  `quantity_approved` INT(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_request` (`request_id`),
  CONSTRAINT `fk_sri_request` FOREIGN KEY (`request_id`)
      REFERENCES `supply_requests`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_sri_item` FOREIGN KEY (`item_id`)
      REFERENCES `inventory_items`(`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Seed barangay user accounts. Bcrypt hash below is for password "changeme123"
--    — replace via the admin UI after first login.
--    Each row's assigned_barangay must match a barangays.name value.
INSERT INTO `users`
    (`username`, `password_hash`, `email`, `display_name`, `role`, `assigned_barangay`)
VALUES
  ('brgy_lahug',     '$2a$10$REPLACE_WITH_REAL_BCRYPT_HASH', 'lahug@civicguard.test',     'Brgy. Lahug',     'barangay', 'Lahug'),
  ('brgy_mabolo',    '$2a$10$REPLACE_WITH_REAL_BCRYPT_HASH', 'mabolo@civicguard.test',    'Brgy. Mabolo',    'barangay', 'Mabolo'),
  ('brgy_guadalupe', '$2a$10$REPLACE_WITH_REAL_BCRYPT_HASH', 'guadalupe@civicguard.test', 'Brgy. Guadalupe', 'barangay', 'Guadalupe');

-- 6. Replace the admin placeholder with a real (temporary) bcrypt hash.
UPDATE `users` SET `password_hash` = '$2a$10$REPLACE_WITH_REAL_BCRYPT_HASH'
WHERE `id` = 1;

-- 7. Optional but recommended: enforce that a barangay user must have assigned_barangay.
ALTER TABLE `users`
  ADD CONSTRAINT `chk_barangay_role_has_assignment`
  CHECK (role != 'barangay' OR assigned_barangay IS NOT NULL);



-- 8. DID THIS WHILE EDITING CODEBASE.

        -- Update the admin account
        UPDATE users
        SET password_hash = '$2a$10$aD.dpzxcUPA9a8Yh9Hg4tej/RuyE9AVakVaGkvFlfjQwAfTwn0L2G'
        WHERE username = 'admin';

        -- Update the three seeded barangay accounts
        UPDATE users
        SET password_hash = '$2a$10$aD.dpzxcUPA9a8Yh9Hg4tej/RuyE9AVakVaGkvFlfjQwAfTwn0L2G'
        WHERE username IN ('brgy_lahug', 'brgy_mabolo', 'brgy_guadalupe');