CREATE DATABASE `myclientdb` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;


CREATE TABLE `bill_main` (
  `idbill_main` int NOT NULL AUTO_INCREMENT,
  `service_rendered` varchar(45) DEFAULT NULL,
  `UnitDay` tinyint DEFAULT '0',
  `startTime` datetime DEFAULT NULL,
  `endTime` datetime DEFAULT NULL,
  `duration_in_minutes` double DEFAULT '0',
  `language` varchar(45) DEFAULT NULL,
  `date_worked` date DEFAULT NULL,
  `client_id` int DEFAULT NULL,
  `paid` tinyint DEFAULT NULL,
  `bill_no` decimal(13,0) DEFAULT NULL,
  `billed_date` date DEFAULT NULL,
  `CityServiced` varchar(45) DEFAULT NULL,
  `insert_date` datetime DEFAULT NULL,
  `updated_date` datetime DEFAULT NULL,
  `dayOfTheWeek` varchar(15) DEFAULT NULL,
  `total_amt` decimal(13,2) DEFAULT NULL,
  `paid_date` date DEFAULT NULL,
  PRIMARY KEY (`idbill_main`),
  UNIQUE KEY `idbill_main_UNIQUE` (`idbill_main`)
) ENGINE=InnoDB AUTO_INCREMENT=83 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `client_main` (
  `idclient_main` int NOT NULL AUTO_INCREMENT,
  `client_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `client_address` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `client_rate` int DEFAULT NULL,

  `phone_number` varchar(45) DEFAULT NULL,
  `client_rate_per_day` int DEFAULT NULL,
  `soft_delete` tinyint DEFAULT NULL,
  `language` varchar(45) NOT NULL,
    `insert_date` datetime DEFAULT NULL,
    `update_date` datetime DEFAULT NULL,
  PRIMARY KEY (`idclient_main`,`language`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `language_main` (
  `lang_id` int NOT NULL AUTO_INCREMENT,
  `lang` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`lang_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `phone_main` (
  `idphone_main` int NOT NULL AUTO_INCREMENT,
  `client_id` int DEFAULT NULL,
  `phone_number` varchar(45) DEFAULT NULL,
  `soft_delete` tinyint DEFAULT NULL,
    `insert_date` datetime DEFAULT NULL,
    `update_date` datetime DEFAULT NULL,
  PRIMARY KEY (`idphone_main`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `rate_main` (
  `client_id` int NOT NULL,
  `language` varchar(45) NOT NULL,
  `rate_per_hour` double DEFAULT NULL,
  `rate_per_day` double DEFAULT NULL,
  `offsetBy` int DEFAULT NULL,
  `weekend` varchar(45) DEFAULT NULL,
  `insert_date` datetime DEFAULT NULL,
  `update_date` datetime DEFAULT NULL,
  `offsetUnit` int DEFAULT NULL,
  `rate_apply_date_from` date NOT NULL DEFAULT '2000-01-01',
  PRIMARY KEY (`client_id`,`language`,`rate_apply_date_from`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



