-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: May 15, 2026 at 04:58 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `civicguard`
--

-- --------------------------------------------------------

--
-- Table structure for table `activity_log`
--

CREATE TABLE `activity_log` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `user_id` bigint(20) UNSIGNED NOT NULL,
  `action` varchar(64) NOT NULL,
  `target_type` varchar(64) DEFAULT NULL,
  `target_id` bigint(20) UNSIGNED DEFAULT NULL,
  `metadata` longtext DEFAULT NULL CHECK (json_valid(`metadata`)),
  `timestamp` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `barangays`
--

CREATE TABLE `barangays` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `name` varchar(128) NOT NULL,
  `center_lat` decimal(10,6) NOT NULL,
  `center_lng` decimal(10,6) NOT NULL,
  `default_zoom` int(11) NOT NULL DEFAULT 15
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `barangays`
--

INSERT INTO `barangays` (`id`, `name`, `center_lat`, `center_lng`, `default_zoom`) VALUES
(1, 'Lahug', 10.334000, 123.895000, 15),
(2, 'Mabolo', 10.320000, 123.915000, 15),
(3, 'Guadalupe', 10.315000, 123.882000, 15);

-- --------------------------------------------------------

--
-- Table structure for table `center_status_updates`
--

CREATE TABLE `center_status_updates` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `center_id` bigint(20) UNSIGNED NOT NULL,
  `event_label` varchar(255) NOT NULL DEFAULT 'No active event',
  `available_item_ids` longtext DEFAULT NULL CHECK (json_valid(`available_item_ids`)),
  `notes` text DEFAULT NULL,
  `updated_by` bigint(20) UNSIGNED DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `center_status_updates`
--

INSERT INTO `center_status_updates` (`id`, `center_id`, `event_label`, `available_item_ids`, `notes`, `updated_by`, `updated_at`) VALUES
(1, 1, 'No active event', '[]', NULL, NULL, '2026-05-10 16:05:05'),
(2, 2, 'No active event', '[]', NULL, NULL, '2026-05-10 16:05:05'),
(3, 3, 'No active event', '[]', NULL, NULL, '2026-05-10 16:05:05'),
(4, 1, '\"Relief Good Operations\"', '[5, 4, 2]', NULL, NULL, '2026-05-11 04:40:34'),
(5, 1, 'No active event', '[6]', NULL, NULL, '2026-05-11 04:43:47'),
(6, 1, '\"gwapo andre\"', '[4, 1]', NULL, NULL, '2026-05-11 04:43:56'),
(7, 1, 'No active event', '[5, 4, 6, 2, 1, 3]', NULL, NULL, '2026-05-12 17:03:46'),
(8, 1, 'No active event', '[5]', NULL, NULL, '2026-05-13 02:14:04'),
(9, 1, 'No active event', '[]', NULL, NULL, '2026-05-14 16:28:58'),
(10, 1, 'No active event', '[]', NULL, NULL, '2026-05-14 16:29:22');

-- --------------------------------------------------------

--
-- Table structure for table `evacuation_centers`
--

CREATE TABLE `evacuation_centers` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `name` varchar(255) NOT NULL,
  `address` varchar(500) NOT NULL,
  `barangay` varchar(128) NOT NULL,
  `photo_path` varchar(500) DEFAULT NULL,
  `latitude` decimal(10,6) DEFAULT NULL,
  `longitude` decimal(10,6) DEFAULT NULL,
  `managed_by` bigint(20) UNSIGNED DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `structural_status` enum('safe','damaged','unsafe') NOT NULL DEFAULT 'safe',
  `structural_notes` text DEFAULT NULL,
  `structural_updated_at` timestamp NULL DEFAULT NULL,
  `structural_updated_by` bigint(20) UNSIGNED DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `evacuation_centers`
--

INSERT INTO `evacuation_centers` (`id`, `name`, `address`, `barangay`, `photo_path`, `latitude`, `longitude`, `managed_by`, `is_active`, `structural_status`, `structural_notes`, `structural_updated_at`, `structural_updated_by`, `created_at`) VALUES
(1, 'Lahug Elementary School', 'Salinas Drive, Lahug, Cebu City', 'Lahug', '/images/lahug_elem.jpg', 10.343900, 123.900000, NULL, 1, 'unsafe', NULL, '2026-05-14 16:29:22', 2, '2026-05-07 13:04:41'),
(2, 'Mabolo National High School', 'A. Soriano Avenue, Mabolo, Cebu City', 'Mabolo', NULL, 10.323200, 123.912300, NULL, 1, 'safe', NULL, NULL, NULL, '2026-05-07 13:04:41'),
(3, 'Guadalupe Elementary School', 'V. Rama Avenue, Guadalupe, Cebu City', 'Guadalupe', NULL, 10.312000, 123.884300, NULL, 1, 'safe', NULL, NULL, NULL, '2026-05-07 13:04:41');

-- --------------------------------------------------------

--
-- Table structure for table `evacuees`
--

CREATE TABLE `evacuees` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `full_name_enc` varchar(500) NOT NULL,
  `contact_enc` varchar(500) DEFAULT NULL,
  `barangay` varchar(128) DEFAULT NULL,
  `photo_path` varchar(500) DEFAULT NULL,
  `evacuation_center_id` bigint(20) UNSIGNED NOT NULL,
  `notes` text DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `evacuees`
--

INSERT INTO `evacuees` (`id`, `full_name_enc`, `contact_enc`, `barangay`, `photo_path`, `evacuation_center_id`, `notes`, `created_at`) VALUES
(1, 'Charles Daragosa', '0922', 'Lahug', NULL, 1, NULL, '2026-05-15 14:37:19'),
(2, 'Mark Villagonzales', '0922', 'Lahug', NULL, 1, NULL, '2026-05-15 14:54:49');

-- --------------------------------------------------------

--
-- Table structure for table `inventory_items`
--

CREATE TABLE `inventory_items` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `name` varchar(255) NOT NULL,
  `category` varchar(64) NOT NULL,
  `unit` varchar(32) NOT NULL,
  `critical_threshold` int(11) NOT NULL,
  `low_threshold` int(11) NOT NULL,
  `stock_quantity` int(11) NOT NULL DEFAULT 0,
  `created_by_user_id` bigint(20) UNSIGNED DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `inventory_items`
--

INSERT INTO `inventory_items` (`id`, `name`, `category`, `unit`, `critical_threshold`, `low_threshold`, `stock_quantity`, `created_by_user_id`, `created_at`) VALUES
(1, 'Rice (5kg sack)', 'food', 'sack', 100, 300, 0, NULL, '2026-05-07 13:04:41'),
(2, 'Pancit canton (pack)', 'food', 'pack', 500, 1200, 0, NULL, '2026-05-07 13:04:41'),
(3, 'Sardines (can)', 'food', 'can', 600, 1500, 0, NULL, '2026-05-07 13:04:41'),
(4, 'Drinking water (1L bottle)', 'water', 'bottle', 1000, 2500, 0, NULL, '2026-05-07 13:04:41'),
(5, 'Blanket', 'non-food', 'piece', 120, 300, 0, NULL, '2026-05-07 13:04:41'),
(6, 'Hygiene kit', 'non-food', 'kit', 200, 500, 0, NULL, '2026-05-07 13:04:41');

-- --------------------------------------------------------

--
-- Table structure for table `lgu_warehouses`
--

CREATE TABLE `lgu_warehouses` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `name` varchar(255) NOT NULL,
  `address` varchar(500) NOT NULL,
  `lgu_code` varchar(32) NOT NULL,
  `latitude` decimal(10,6) DEFAULT NULL,
  `longitude` decimal(10,6) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `lgu_warehouses`
--

INSERT INTO `lgu_warehouses` (`id`, `name`, `address`, `lgu_code`, `latitude`, `longitude`, `created_at`) VALUES
(1, 'Cebu City DRRMO Central Warehouse', 'N. Bacalso Avenue, Cebu City', 'CEB-CTY', 10.300000, 123.893000, '2026-05-07 13:04:41');

-- --------------------------------------------------------

--
-- Table structure for table `supply_requests`
--

CREATE TABLE `supply_requests` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `requesting_barangay` varchar(128) NOT NULL,
  `requesting_user_id` bigint(20) UNSIGNED NOT NULL,
  `evacuation_center_id` bigint(20) UNSIGNED DEFAULT NULL,
  `status` enum('pending','approved','partially_fulfilled','fulfilled','rejected') NOT NULL DEFAULT 'pending',
  `notes` text DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `reviewed_by` bigint(20) UNSIGNED DEFAULT NULL,
  `reviewed_at` timestamp NULL DEFAULT NULL,
  `admin_notes` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `supply_requests`
--

INSERT INTO `supply_requests` (`id`, `requesting_barangay`, `requesting_user_id`, `evacuation_center_id`, `status`, `notes`, `created_at`, `reviewed_by`, `reviewed_at`, `admin_notes`) VALUES
(1, 'Mabolo', 3, NULL, 'pending', '', '2026-05-15 00:35:52', NULL, NULL, NULL),
(2, 'Lahug', 2, NULL, 'pending', '', '2026-05-15 01:55:22', NULL, NULL, NULL),
(3, 'Lahug', 2, NULL, 'pending', 'No more blankets left', '2026-05-15 02:01:56', NULL, NULL, NULL),
(4, 'Lahug', 2, NULL, 'pending', 'Need water, no supply almost.', '2026-05-15 02:30:58', NULL, NULL, NULL);

-- --------------------------------------------------------

--
-- Table structure for table `supply_request_items`
--

CREATE TABLE `supply_request_items` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `request_id` bigint(20) UNSIGNED NOT NULL,
  `item_id` bigint(20) UNSIGNED NOT NULL,
  `quantity_requested` int(11) NOT NULL,
  `quantity_approved` int(11) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `supply_request_items`
--

INSERT INTO `supply_request_items` (`id`, `request_id`, `item_id`, `quantity_requested`, `quantity_approved`) VALUES
(1, 1, 4, 100, 0),
(2, 2, 5, 123, 0),
(3, 3, 5, 12, 0),
(4, 4, 4, 200, 0);

-- --------------------------------------------------------

--
-- Table structure for table `transactions`
--

CREATE TABLE `transactions` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `direction` enum('inflow','outflow') NOT NULL,
  `item_id` bigint(20) UNSIGNED NOT NULL,
  `quantity` int(11) NOT NULL,
  `warehouse_id` bigint(20) UNSIGNED NOT NULL,
  `destination_center_id` bigint(20) UNSIGNED DEFAULT NULL,
  `source_label` varchar(255) DEFAULT NULL,
  `created_by` bigint(20) UNSIGNED NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `notes` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `username` varchar(64) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `display_name` varchar(128) NOT NULL,
  `role` enum('admin','barangay','staff') NOT NULL DEFAULT 'barangay',
  `assigned_center_id` bigint(20) UNSIGNED DEFAULT NULL,
  `assigned_barangay` varchar(128) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `last_login_at` timestamp NULL DEFAULT NULL
) ;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id`, `username`, `password_hash`, `email`, `display_name`, `role`, `assigned_center_id`, `assigned_barangay`, `created_at`, `last_login_at`) VALUES
(1, 'admin', '$2a$10$aD.dpzxcUPA9a8Yh9Hg4tej/RuyE9AVakVaGkvFlfjQwAfTwn0L2G', 'admin@civicguard.test', 'Cebu City Admin', 'admin', NULL, 'Lahug', '2026-05-07 13:04:41', '2026-05-14 15:25:26'),
(2, 'brgy_lahug', '$2a$10$aD.dpzxcUPA9a8Yh9Hg4tej/RuyE9AVakVaGkvFlfjQwAfTwn0L2G', 'lahug@civicguard.test', 'Brgy. Lahug', 'barangay', NULL, 'Lahug', '2026-05-14 10:31:37', '2026-05-15 14:54:31'),
(3, 'brgy_mabolo', '$2a$10$aD.dpzxcUPA9a8Yh9Hg4tej/RuyE9AVakVaGkvFlfjQwAfTwn0L2G', 'mabolo@civicguard.test', 'Brgy. Mabolo', 'barangay', NULL, 'Mabolo', '2026-05-14 10:31:37', '2026-05-15 01:07:29'),
(4, 'brgy_guadalupe', '$2a$10$aD.dpzxcUPA9a8Yh9Hg4tej/RuyE9AVakVaGkvFlfjQwAfTwn0L2G', 'guadalupe@civicguard.test', 'Brgy. Guadalupe', 'barangay', NULL, 'Guadalupe', '2026-05-14 10:31:37', '2026-05-15 01:29:55');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `activity_log`
--
ALTER TABLE `activity_log`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_user_time` (`user_id`,`timestamp`),
  ADD KEY `idx_target` (`target_type`,`target_id`);

--
-- Indexes for table `barangays`
--
ALTER TABLE `barangays`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_name` (`name`);

--
-- Indexes for table `center_status_updates`
--
ALTER TABLE `center_status_updates`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_center_time` (`center_id`,`updated_at`),
  ADD KEY `csu_user_fk` (`updated_by`);

--
-- Indexes for table `evacuation_centers`
--
ALTER TABLE `evacuation_centers`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_barangay` (`barangay`),
  ADD KEY `idx_active` (`is_active`),
  ADD KEY `managed_by` (`managed_by`),
  ADD KEY `fk_ec_structural_updater` (`structural_updated_by`);

--
-- Indexes for table `evacuees`
--
ALTER TABLE `evacuees`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_status_center` (`evacuation_center_id`),
  ADD KEY `idx_created` (`created_at`),
  ADD KEY `evacuation_center_id` (`evacuation_center_id`);

--
-- Indexes for table `inventory_items`
--
ALTER TABLE `inventory_items`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_category` (`category`),
  ADD KEY `fk_inventory_items_user` (`created_by_user_id`);

--
-- Indexes for table `lgu_warehouses`
--
ALTER TABLE `lgu_warehouses`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `supply_requests`
--
ALTER TABLE `supply_requests`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_status_created` (`status`,`created_at`),
  ADD KEY `idx_barangay` (`requesting_barangay`),
  ADD KEY `fk_sr_user` (`requesting_user_id`),
  ADD KEY `fk_sr_center` (`evacuation_center_id`),
  ADD KEY `fk_sr_reviewer` (`reviewed_by`);

--
-- Indexes for table `supply_request_items`
--
ALTER TABLE `supply_request_items`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_request` (`request_id`),
  ADD KEY `fk_sri_item` (`item_id`);

--
-- Indexes for table `transactions`
--
ALTER TABLE `transactions`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_warehouse_date` (`warehouse_id`,`created_at`),
  ADD KEY `idx_item_date` (`item_id`,`created_at`),
  ADD KEY `destination_center_id` (`destination_center_id`),
  ADD KEY `created_by` (`created_by`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `username` (`username`),
  ADD KEY `fk_users_assigned_center` (`assigned_center_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `activity_log`
--
ALTER TABLE `activity_log`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `barangays`
--
ALTER TABLE `barangays`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `center_status_updates`
--
ALTER TABLE `center_status_updates`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT for table `evacuation_centers`
--
ALTER TABLE `evacuation_centers`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `evacuees`
--
ALTER TABLE `evacuees`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `inventory_items`
--
ALTER TABLE `inventory_items`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT for table `lgu_warehouses`
--
ALTER TABLE `lgu_warehouses`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `supply_requests`
--
ALTER TABLE `supply_requests`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `supply_request_items`
--
ALTER TABLE `supply_request_items`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `transactions`
--
ALTER TABLE `transactions`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `activity_log`
--
ALTER TABLE `activity_log`
  ADD CONSTRAINT `activity_log_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `center_status_updates`
--
ALTER TABLE `center_status_updates`
  ADD CONSTRAINT `csu_center_fk` FOREIGN KEY (`center_id`) REFERENCES `evacuation_centers` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `csu_user_fk` FOREIGN KEY (`updated_by`) REFERENCES `users` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `evacuation_centers`
--
ALTER TABLE `evacuation_centers`
  ADD CONSTRAINT `evacuation_centers_ibfk_1` FOREIGN KEY (`managed_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_ec_structural_updater` FOREIGN KEY (`structural_updated_by`) REFERENCES `users` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `evacuees`
--
ALTER TABLE `evacuees`
  ADD CONSTRAINT `evacuees_ibfk_1` FOREIGN KEY (`evacuation_center_id`) REFERENCES `evacuation_centers` (`id`);

--
-- Constraints for table `inventory_items`
--
ALTER TABLE `inventory_items`
  ADD CONSTRAINT `fk_inventory_items_user` FOREIGN KEY (`created_by_user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Constraints for table `supply_requests`
--
ALTER TABLE `supply_requests`
  ADD CONSTRAINT `fk_sr_center` FOREIGN KEY (`evacuation_center_id`) REFERENCES `evacuation_centers` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_sr_reviewer` FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_sr_user` FOREIGN KEY (`requesting_user_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `supply_request_items`
--
ALTER TABLE `supply_request_items`
  ADD CONSTRAINT `fk_sri_item` FOREIGN KEY (`item_id`) REFERENCES `inventory_items` (`id`),
  ADD CONSTRAINT `fk_sri_request` FOREIGN KEY (`request_id`) REFERENCES `supply_requests` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `transactions`
--
ALTER TABLE `transactions`
  ADD CONSTRAINT `transactions_ibfk_1` FOREIGN KEY (`item_id`) REFERENCES `inventory_items` (`id`),
  ADD CONSTRAINT `transactions_ibfk_2` FOREIGN KEY (`warehouse_id`) REFERENCES `lgu_warehouses` (`id`),
  ADD CONSTRAINT `transactions_ibfk_3` FOREIGN KEY (`destination_center_id`) REFERENCES `evacuation_centers` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `transactions_ibfk_4` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`);

--
-- Constraints for table `users`
--
ALTER TABLE `users`
  ADD CONSTRAINT `fk_users_assigned_center` FOREIGN KEY (`assigned_center_id`) REFERENCES `evacuation_centers` (`id`) ON DELETE SET NULL;
COMMIT;

-- NOT NULL CREATED BY USERS IN INVENTORY
UPDATE inventory_items SET created_by_user_id = 1 WHERE created_by_user_id IS NULL;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
