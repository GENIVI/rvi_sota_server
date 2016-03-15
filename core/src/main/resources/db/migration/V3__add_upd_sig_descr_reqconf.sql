ALTER TABLE UpdateRequest
ADD signature VARCHAR(256) NOT NULL,
ADD description text,
ADD request_confirmation BOOLEAN NOT NULL
