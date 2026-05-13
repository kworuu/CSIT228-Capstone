-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: May 07, 2026 at 04:49 PM
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
  `metadata` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`metadata`)),
  `timestamp` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `evacuation_centers`
--

CREATE TABLE `evacuation_centers` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `name` varchar(255) NOT NULL,
  `address` varchar(500) NOT NULL,
  `barangay` varchar(128) NOT NULL,
  `capacity` int(11) NOT NULL,
  `current_occupancy` int(11) NOT NULL DEFAULT 0,
  `latitude` decimal(10,6) DEFAULT NULL,
  `longitude` decimal(10,6) DEFAULT NULL,
  `managed_by` bigint(20) UNSIGNED DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `evacuation_centers`
--

INSERT INTO `evacuation_centers` (`id`, `name`, `address`, `barangay`, `capacity`, `current_occupancy`, `latitude`, `longitude`, `managed_by`, `is_active`, `created_at`) VALUES
(1, 'Lahug Elementary School', 'Salinas Drive, Lahug, Cebu City', 'Lahug', 600, 0, 10.343900, 123.900000, NULL, 1, '2026-05-07 13:04:41'),
(2, 'Mabolo National High School', 'A. Soriano Avenue, Mabolo, Cebu City', 'Mabolo', 800, 0, 10.323200, 123.912300, NULL, 1, '2026-05-07 13:04:41'),
(3, 'Guadalupe Elementary School', 'V. Rama Avenue, Guadalupe, Cebu City', 'Guadalupe', 500, 0, 10.312000, 123.884300, NULL, 1, '2026-05-07 13:04:41');

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
  `family_group_id` bigint(20) UNSIGNED DEFAULT NULL,
  `verification_status` enum('pending','verified','rejected') NOT NULL DEFAULT 'pending',
  `verified_by` bigint(20) UNSIGNED DEFAULT NULL,
  `verified_at` timestamp NULL DEFAULT NULL,
  `notes` text DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `family_groups`
--

CREATE TABLE `family_groups` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `head_of_family` varchar(255) NOT NULL,
  `member_count` int(11) NOT NULL DEFAULT 1,
  `evacuation_center_id` bigint(20) UNSIGNED NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `inventory_items`
--

INSERT INTO `inventory_items` (`id`, `name`, `category`, `unit`, `critical_threshold`, `low_threshold`, `created_at`) VALUES
(1, 'Rice (5kg sack)', 'food', 'sack', 100, 300, '2026-05-07 13:04:41'),
(2, 'Pancit canton (pack)', 'food', 'pack', 500, 1200, '2026-05-07 13:04:41'),
(3, 'Sardines (can)', 'food', 'can', 600, 1500, '2026-05-07 13:04:41'),
(4, 'Drinking water (1L bottle)', 'water', 'bottle', 1000, 2500, '2026-05-07 13:04:41'),
(5, 'Blanket', 'non-food', 'piece', 120, 300, '2026-05-07 13:04:41'),
(6, 'Hygiene kit', 'non-food', 'kit', 200, 500, '2026-05-07 13:04:41');

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `lgu_warehouses`
--

INSERT INTO `lgu_warehouses` (`id`, `name`, `address`, `lgu_code`, `latitude`, `longitude`, `created_at`) VALUES
(1, 'Cebu City DRRMO Central Warehouse', 'N. Bacalso Avenue, Cebu City', 'CEB-CTY', 10.300000, 123.893000, '2026-05-07 13:04:41');

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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
  `role` enum('admin','staff') NOT NULL DEFAULT 'admin',
  `assigned_center_id` bigint(20) UNSIGNED DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `last_login_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id`, `username`, `password_hash`, `email`, `display_name`, `role`, `assigned_center_id`, `created_at`, `last_login_at`) VALUES
(1, 'admin', 'PLACEHOLDER_REPLACE_ON_FIRST_RUN', 'admin@civicguard.test', 'Cebu City Admin', 'admin', NULL, '2026-05-07 13:04:41', NULL);

-- --------------------------------------------------------

--
-- Table structure for table `warehouse_stock`
--

CREATE TABLE `warehouse_stock` (
  `warehouse_id` bigint(20) UNSIGNED NOT NULL,
  `item_id` bigint(20) UNSIGNED NOT NULL,
  `quantity` int(11) NOT NULL DEFAULT 0,
  `last_updated` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `warehouse_stock`
--

INSERT INTO `warehouse_stock` (`warehouse_id`, `item_id`, `quantity`, `last_updated`) VALUES
(1, 1, 450, '2026-05-07 13:04:41'),
(1, 2, 2000, '2026-05-07 13:04:41'),
(1, 3, 2400, '2026-05-07 13:04:41'),
(1, 4, 3500, '2026-05-07 13:04:41'),
(1, 5, 450, '2026-05-07 13:04:41'),
(1, 6, 650, '2026-05-07 13:04:41');

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
-- Indexes for table `evacuation_centers`
--
ALTER TABLE `evacuation_centers`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_barangay` (`barangay`),
  ADD KEY `idx_active` (`is_active`),
  ADD KEY `managed_by` (`managed_by`);

--
-- Indexes for table `evacuees`
--
ALTER TABLE `evacuees`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_status_center` (`verification_status`,`evacuation_center_id`),
  ADD KEY `idx_created` (`created_at`),
  ADD KEY `evacuation_center_id` (`evacuation_center_id`),
  ADD KEY `family_group_id` (`family_group_id`),
  ADD KEY `verified_by` (`verified_by`);

--
-- Indexes for table `family_groups`
--
ALTER TABLE `family_groups`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_center` (`evacuation_center_id`);

--
-- Indexes for table `inventory_items`
--
ALTER TABLE `inventory_items`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_category` (`category`);

--
-- Indexes for table `lgu_warehouses`
--
ALTER TABLE `lgu_warehouses`
  ADD PRIMARY KEY (`id`);

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
-- Indexes for table `warehouse_stock`
--
ALTER TABLE `warehouse_stock`
  ADD PRIMARY KEY (`warehouse_id`,`item_id`),
  ADD KEY `item_id` (`item_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `activity_log`
--
ALTER TABLE `activity_log`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `evacuation_centers`
--
ALTER TABLE `evacuation_centers`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `evacuees`
--
ALTER TABLE `evacuees`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `family_groups`
--
ALTER TABLE `family_groups`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT;

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
-- AUTO_INCREMENT for table `transactions`
--
ALTER TABLE `transactions`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `activity_log`
--
ALTER TABLE `activity_log`
  ADD CONSTRAINT `activity_log_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `evacuation_centers`
--
ALTER TABLE `evacuation_centers`
  ADD CONSTRAINT `evacuation_centers_ibfk_1` FOREIGN KEY (`managed_by`) REFERENCES `users` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `evacuees`
--
ALTER TABLE `evacuees`
  ADD CONSTRAINT `evacuees_ibfk_1` FOREIGN KEY (`evacuation_center_id`) REFERENCES `evacuation_centers` (`id`),
  ADD CONSTRAINT `evacuees_ibfk_2` FOREIGN KEY (`family_group_id`) REFERENCES `family_groups` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `evacuees_ibfk_3` FOREIGN KEY (`verified_by`) REFERENCES `users` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `family_groups`
--
ALTER TABLE `family_groups`
  ADD CONSTRAINT `family_groups_ibfk_1` FOREIGN KEY (`evacuation_center_id`) REFERENCES `evacuation_centers` (`id`);

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

--
-- Constraints for table `warehouse_stock`
--
ALTER TABLE `warehouse_stock`
  ADD CONSTRAINT `warehouse_stock_ibfk_1` FOREIGN KEY (`warehouse_id`) REFERENCES `lgu_warehouses` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `warehouse_stock_ibfk_2` FOREIGN KEY (`item_id`) REFERENCES `inventory_items` (`id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
