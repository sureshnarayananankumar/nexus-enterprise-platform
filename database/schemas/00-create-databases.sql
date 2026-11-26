-- Runs once when the local Postgres volume is first created (docker-entrypoint-initdb.d).
-- One logical database per service (ADR-0002). Each service owns its schema via Flyway.
CREATE DATABASE identity_db;
CREATE DATABASE user_db;
CREATE DATABASE organization_db;
CREATE DATABASE employee_db;
CREATE DATABASE customer_db;
CREATE DATABASE project_db;
CREATE DATABASE task_db;
CREATE DATABASE workflow_db;
CREATE DATABASE finance_db;
CREATE DATABASE inventory_db;
CREATE DATABASE order_db;
CREATE DATABASE vendor_db;
CREATE DATABASE document_db;
CREATE DATABASE reporting_db;
CREATE DATABASE notification_db;
CREATE DATABASE config_db;
