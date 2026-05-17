ALTER TABLE supply_requests
ADD COLUMN target_center_id BIGINT UNSIGNED NULL AFTER item_id;

ALTER TABLE supply_requests
ADD CONSTRAINT fk_sr_center
FOREIGN KEY (target_center_id) REFERENCES evacuation_centers(id)
ON DELETE SET NULL;