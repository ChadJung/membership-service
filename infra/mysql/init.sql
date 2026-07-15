-- Database-per-service: each microservice owns its schema exclusively.
CREATE DATABASE IF NOT EXISTS member_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS payment_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS benefit_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
