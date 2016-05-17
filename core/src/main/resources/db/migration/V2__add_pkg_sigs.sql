ALTER TABLE Package
-- Cryptographic signature of the package
ADD signature text;

ALTER TABLE UpdateRequest
ADD signature VARCHAR(256) NOT NULL,
ADD description text,
ADD request_confirmation BOOLEAN NOT NULL;

ALTER TABLE InstallHistory
ADD update_request_id CHAR(36) NOT NULL,
ADD FOREIGN KEY fk_install_history_update_request_id (update_request_id) REFERENCES UpdateRequest(update_request_id);

CREATE TABLE OperationResult (
    id CHAR(36) NOT NULL,
    update_request_id CHAR(36) NOT NULL,
    result_code INT NOT NULL,
    result_text TEXT NOT NULL,

    PRIMARY KEY (id),
    FOREIGN KEY fk_operation_result_update_request_id (update_request_id) REFERENCES UpdateRequest(update_request_id)
);

