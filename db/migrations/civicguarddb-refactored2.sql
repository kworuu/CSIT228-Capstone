-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: May 16, 2026 at 02:40 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.0.30

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `civicguarddb-refactored`
--

-- --------------------------------------------------------

--
-- Table structure for table `activity_log`
--

CREATE TABLE `activity_log` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `user_id` bigint(20) UNSIGNED NOT NULL,
  `action` varchar(64) NOT NULL,
  `metadata` longtext DEFAULT NULL CHECK (json_valid(`metadata`)),
  `timestamp` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
  `user_id` bigint(20) UNSIGNED NOT NULL,
  `photo_path` varchar(500) DEFAULT NULL,
  `latitude` decimal(10,6) DEFAULT NULL,
  `longitude` decimal(10,6) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `evacuation_centers`
--

INSERT INTO `evacuation_centers` (`id`, `name`, `address`, `user_id`, `photo_path`, `latitude`, `longitude`, `created_at`) VALUES
(1, 'Lahug Elementary School', 'Salinas Drive, Lahug, Cebu City', 1, '/images/lahug_elem.jpg', 10.343900, 123.900000, '2026-05-07 13:04:41'),
(2, 'Mabolo National High School', 'A. Soriano Avenue, Mabolo, Cebu City', 2, NULL, 10.323200, 123.912300, '2026-05-07 13:04:41'),
(3, 'Guadalupe Elementary School', 'V. Rama Avenue, Guadalupe, Cebu City', 3, NULL, 10.312000, 123.884300, '2026-05-07 13:04:41');

-- --------------------------------------------------------

--
-- Table structure for table `evacuees`
--

CREATE TABLE `evacuees` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `full_name_enc` varchar(500) NOT NULL,
  `contact_enc` varchar(500) DEFAULT NULL,
  `user_id` bigint(20) UNSIGNED DEFAULT NULL,
  `photo_path` varchar(500) DEFAULT NULL,
  `evacuation_center_id` bigint(20) UNSIGNED NOT NULL,
  `notes` text DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `evacuees`
--

INSERT INTO `evacuees` (`id`, `full_name_enc`, `contact_enc`, `user_id`, `photo_path`, `evacuation_center_id`, `notes`, `created_at`) VALUES
(1, 'Charles Daragosa', '0922', 1, NULL, 1, NULL, '2026-05-15 14:37:19'),
(2, 'Mark Villagonzales', '0922', 1, NULL, 1, NULL, '2026-05-15 14:54:49');

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
-- Table structure for table `supply_requests`
--

CREATE TABLE `supply_requests` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `requesting_user_id` bigint(20) UNSIGNED NOT NULL,
  `item_id` bigint(20) UNSIGNED DEFAULT NULL,
  `quantity` int(11) NOT NULL DEFAULT 0,
  `status` enum('pending','approved','partially_fulfilled','fulfilled','rejected') NOT NULL DEFAULT 'pending',
  `notes` text DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `supply_requests`
--

INSERT INTO `supply_requests` (`id`, `requesting_user_id`, `item_id`, `quantity`, `status`, `notes`, `created_at`) VALUES
(1, 3, NULL, 0, 'pending', '', '2026-05-15 00:35:52'),
(2, 2, NULL, 0, 'pending', '', '2026-05-15 01:55:22'),
(3, 2, NULL, 0, 'pending', 'No more blankets left', '2026-05-15 02:01:56'),
(4, 2, NULL, 0, 'pending', 'Need water, no supply almost.', '2026-05-15 02:30:58'),
(5, 2, NULL, 0, 'pending', 'please', '2026-05-15 15:02:12');

-- --------------------------------------------------------

--
-- Table structure for table `transactions`
--

CREATE TABLE `transactions` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `direction` enum('outflow') NOT NULL,
  `item_id` bigint(20) UNSIGNED NOT NULL,
  `quantity` int(11) NOT NULL,
  `destination_id` bigint(20) UNSIGNED DEFAULT NULL,
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
  `display_name` varchar(128) NOT NULL,
  `role` enum('admin','barangay','staff') NOT NULL DEFAULT 'barangay',
  `latitude` decimal(10,6) DEFAULT NULL,
  `longitude` decimal(10,6) DEFAULT NULL,
  `zoom` int(11) DEFAULT NULL,
  `last_login_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id`, `username`, `password_hash`, `display_name`, `role`, `latitude`, `longitude`, `zoom`, `last_login_at`) VALUES
(1, 'admin', '$2a$10$aD.dpzxcUPA9a8Yh9Hg4tej/RuyE9AVakVaGkvFlfjQwAfTwn0L2G', 'Cebu City Admin', 'admin', NULL, NULL, NULL, '2026-05-16 03:21:52'),
(2, 'brgy_lahug', '$2a$10$aD.dpzxcUPA9a8Yh9Hg4tej/RuyE9AVakVaGkvFlfjQwAfTwn0L2G', 'Brgy. Lahug', 'barangay', NULL, NULL, NULL, '2026-05-16 04:12:02'),
(3, 'brgy_mabolo', '$2a$10$aD.dpzxcUPA9a8Yh9Hg4tej/RuyE9AVakVaGkvFlfjQwAfTwn0L2G', 'Brgy. Mabolo', 'barangay', NULL, NULL, NULL, '2026-05-16 03:07:37'),
(4, 'brgy_guadalupe', '$2a$10$aD.dpzxcUPA9a8Yh9Hg4tej/RuyE9AVakVaGkvFlfjQwAfTwn0L2G', 'Brgy. Guadalupe', 'barangay', NULL, NULL, NULL, '2026-05-15 01:29:55');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `activity_log`
--
ALTER TABLE `activity_log`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_user_time` (`user_id`,`timestamp`);

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
  ADD KEY `idx_barangay_id` (`user_id`);

--
-- Indexes for table `evacuees`
--
ALTER TABLE `evacuees`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_status_center` (`evacuation_center_id`),
  ADD KEY `idx_created` (`created_at`),
  ADD KEY `idx_barangay_id` (`user_id`);

--
-- Indexes for table `inventory_items`
--
ALTER TABLE `inventory_items`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_category` (`category`),
  ADD KEY `fk_inventory_items_user` (`created_by_user_id`);

--
-- Indexes for table `supply_requests`
--
ALTER TABLE `supply_requests`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_status_created` (`status`,`created_at`),
  ADD KEY `fk_sr_user` (`requesting_user_id`),
  ADD KEY `fk_sr_item` (`item_id`);

--
-- Indexes for table `transactions`
--
ALTER TABLE `transactions`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_warehouse_date` (`created_at`),
  ADD KEY `idx_item_date` (`item_id`,`created_at`),
  ADD KEY `destination_center_id` (`destination_id`),
  ADD KEY `created_by` (`created_by`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `username` (`username`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `activity_log`
--
ALTER TABLE `activity_log`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT;

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
-- AUTO_INCREMENT for table `supply_requests`
--
ALTER TABLE `supply_requests`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT for table `transactions`
--
ALTER TABLE `transactions`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

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
  ADD CONSTRAINT `fk_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `evacuees`
--
ALTER TABLE `evacuees`
  ADD CONSTRAINT `evacuees_ibfk_1` FOREIGN KEY (`evacuation_center_id`) REFERENCES `evacuation_centers` (`id`),
  ADD CONSTRAINT `fk_userID` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `inventory_items`
--
ALTER TABLE `inventory_items`
  ADD CONSTRAINT `fk_inventory_items_user` FOREIGN KEY (`created_by_user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Constraints for table `supply_requests`
--
ALTER TABLE `supply_requests`
  ADD CONSTRAINT `fk_sr_item` FOREIGN KEY (`item_id`) REFERENCES `inventory_items` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_sr_user` FOREIGN KEY (`requesting_user_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `transactions`
--
ALTER TABLE `transactions`
  ADD CONSTRAINT `transactions_ibfk_1` FOREIGN KEY (`item_id`) REFERENCES `inventory_items` (`id`),
  ADD CONSTRAINT `transactions_ibfk_3` FOREIGN KEY (`destination_id`) REFERENCES `users` (`id`) ON DELETE SET NULL;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
