CREATE DATABASE `myclientdb` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;


CREATE TABLE `bill_main` (
  `idbill_main` int NOT NULL AUTO_INCREMENT,
  `service_rendered` varchar(45) DEFAULT NULL,
  `UnitDay` tinyint DEFAULT '0',
  `workedDayOrHours` int DEFAULT '0',
  `CityServiced` varchar(45) DEFAULT NULL,
  `insert_date` datetime DEFAULT NULL,
  `updated_date` datetime DEFAULT NULL,
  `client_id` int DEFAULT NULL,
  `date_worked` datetime DEFAULT NULL,
  `paid` tinyint DEFAULT NULL,
  `language` varchar(45) DEFAULT NULL,
  `bill_no` decimal(10,0) DEFAULT NULL,
  PRIMARY KEY (`idbill_main`),
  UNIQUE KEY `idbill_main_UNIQUE` (`idbill_main`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `client_main` (
  `idclient_main` int NOT NULL AUTO_INCREMENT,
  `client_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `client_address` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `client_rate` int DEFAULT NULL,
  `insert_date` datetime DEFAULT NULL,
  `update_date` datetime DEFAULT NULL,
  `phone_number` varchar(45) DEFAULT NULL,
  `client_rate_per_day` int DEFAULT NULL,
  `soft_delete` tinyint DEFAULT NULL,
  PRIMARY KEY (`idclient_main`),
  UNIQUE KEY `idclient_main_UNIQUE` (`idclient_main`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `phone_main` (
  `idphone_main` int NOT NULL AUTO_INCREMENT,
  `client_id` int DEFAULT NULL,
  `phone_number` varchar(45) DEFAULT NULL,
  `soft_delete` tinyint DEFAULT NULL,
  PRIMARY KEY (`idphone_main`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
