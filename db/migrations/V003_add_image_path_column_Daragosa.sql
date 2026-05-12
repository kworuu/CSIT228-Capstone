-----------------------------------------------------------------------------------------
-- V003: Seed the initial photo_path for Lahug Elementary School. DARAGOSA
-- This migration populates the `photo_path` column (added in V002)
-- with the correct path for the sample image, allowing it to be displayed in the UI.
-----------------------------------------------------------------------------------------
UPDATE `evacuation_centers`
SET `photo_path` = '/images/lahug_elem.jpg'
WHERE `id` = 1 AND `name` = 'Lahug Elementary School';