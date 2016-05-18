ALTER TABLE UpdateSpec
DROP FOREIGN KEY fk_update_specs_vehicle;

ALTER TABLE InstallHistory
DROP FOREIGN KEY install_history_vin_fk;

ALTER TABLE OperationResult
DROP FOREIGN KEY fk_operation_result_vin;

DROP TABLE Vehicle;
