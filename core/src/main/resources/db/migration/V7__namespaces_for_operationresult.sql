ALTER TABLE OperationResult
ADD vin varchar(64) NOT NULL,
ADD namespace CHAR(200) NOT NULL,
ADD FOREIGN KEY fk_operation_result_vin (namespace, vin) REFERENCES Vehicle(namespace, vin);
