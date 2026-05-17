-- 1. Seed Major Barangays with accurate geographic spread
UPDATE users SET latitude = 10.3280, longitude = 123.8980, zoom = 14 WHERE display_name = 'Brgy. Lahug';
UPDATE users SET latitude = 10.3175, longitude = 123.9160, zoom = 14 WHERE display_name = 'Brgy. Mabolo';
UPDATE users SET latitude = 10.3015, longitude = 123.8750, zoom = 14 WHERE display_name = 'Brgy. Tisa';
UPDATE users SET latitude = 10.3660, longitude = 123.9150, zoom = 14 WHERE display_name = 'Brgy. Talamban';
UPDATE users SET latitude = 10.3225, longitude = 123.8800, zoom = 14 WHERE display_name = 'Brgy. Guadalupe';
UPDATE users SET latitude = 10.3160, longitude = 123.8900, zoom = 14 WHERE display_name = 'Brgy. Capitol Site';
UPDATE users SET latitude = 10.2940, longitude = 123.9015, zoom = 14 WHERE display_name = 'Brgy. Sto. Nino';
UPDATE users SET latitude = 10.4080, longitude = 123.8730, zoom = 14 WHERE display_name = 'Brgy. Sirao';
UPDATE users SET latitude = 10.3340, longitude = 123.9050, zoom = 14 WHERE display_name = 'Brgy. Apas';
UPDATE users SET latitude = 10.3080, longitude = 123.8920, zoom = 14 WHERE display_name = 'Brgy. Sambag I';
UPDATE users SET latitude = 10.3050, longitude = 123.8950, zoom = 14 WHERE display_name = 'Brgy. Sambag II';
UPDATE users SET latitude = 10.3800, longitude = 123.9200, zoom = 14 WHERE display_name = 'Brgy. Pit-os';
UPDATE users SET latitude = 10.2800, longitude = 123.8500, zoom = 14 WHERE display_name = 'Brgy. Poblacion Pardo';
UPDATE users SET latitude = 10.3130, longitude = 123.9030, zoom = 14 WHERE display_name = 'Brgy. Tejero';
UPDATE users SET latitude = 10.3100, longitude = 123.9080, zoom = 14 WHERE display_name = 'Brgy. Tinago';
UPDATE users SET latitude = 10.3420, longitude = 123.8780, zoom = 14 WHERE display_name = 'Brgy. Busay';

-- 2. The Catch-All Safety Net
-- This guarantees that ANY remaining barangay in your SQL file that wasn't specifically
-- named above gets a default coordinate (Cebu City Center) so the prefetcher never crashes on a NULL.
UPDATE users
SET latitude = 10.3157,
    longitude = 123.8854,
    zoom = 14
WHERE latitude IS NULL AND role = 'barangay';