-- -----------------------------------------------------------
-- Always insert a known “approved employee” user on startup
-- -----------------------------------------------------------

INSERT INTO app_user (
  email,
  name,
  password,
  registration_status,
  role
) VALUES (
  'employee@bank.test',
  'Auto Seed Employee',
  '$2a$10$L03L.dfOsk4s7iRxWc8.wOop9OPrj7FdUqoxc8hZIQgA37/pSTtp.', -- Secret123
  'APPROVED',
  'EMPLOYEE'
);