-- --------------------------------------------------------
-- V002 MIGRATION SCRIPT - DARAGOSA
-- --------------------------------------------------------

-- 1. Create the new `barangays` table
CREATE TABLE `barangays` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL,
  `center_lat` decimal(10,6) NOT NULL,
  `center_lng` decimal(10,6) NOT NULL,
  `default_zoom` int(11) NOT NULL DEFAULT 15,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Insert initial data into `barangays`
INSERT INTO `barangays` (`id`, `name`, `center_lat`, `center_lng`, `default_zoom`) VALUES
(1, 'Lahug', 10.334000, 123.895000, 15),
(2, 'Mabolo', 10.320000, 123.915000, 15),
(3, 'Guadalupe', 10.315000, 123.882000, 15);

-- 3. Create the new `center_status_updates` table
CREATE TABLE `center_status_updates` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `center_id` bigint(20) UNSIGNED NOT NULL,
  `event_label` varchar(255) NOT NULL DEFAULT 'No active event',
  `available_item_ids` longtext DEFAULT NULL CHECK (json_valid(`available_item_ids`)),
  `notes` text DEFAULT NULL,
  `updated_by` bigint(20) UNSIGNED DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_center_time` (`center_id`,`updated_at`),
  KEY `csu_user_fk` (`updated_by`),
  CONSTRAINT `csu_center_fk` FOREIGN KEY (`center_id`) REFERENCES `evacuation_centers` (`id`) ON DELETE CASCADE,
  CONSTRAINT `csu_user_fk` FOREIGN KEY (`updated_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Insert initial data into `center_status_updates`
INSERT INTO `center_status_updates` (`id`, `center_id`, `event_label`, `available_item_ids`, `notes`, `updated_by`, `updated_at`) VALUES
(1, 1, 'No active event', '[]', NULL, NULL, '2026-05-10 16:05:05'),
(2, 2, 'No active event', '[]', NULL, NULL, '2026-05-10 16:05:05'),
(3, 3, 'No active event', '[]', NULL, NULL, '2026-05-10 16:05:05');

-- 5. Add new `photo_path` column to `evacuation_centers` table
ALTER TABLE `evacuation_centers`
ADD COLUMN `photo_path` varchar(500) DEFAULT NULL AFTER `barangay`;

-- 6. Add new `assigned_barangay` column to `users` table
ALTER TABLE `users`
ADD COLUMN `assigned_barangay` varchar(128) DEFAULT NULL AFTER `assigned_center_id`;

-- 7. Update the existing Admin user with the new assigned_barangay data from V002
UPDATE `users` SET `assigned_barangay` = 'Lahug' WHERE `id` = 1;