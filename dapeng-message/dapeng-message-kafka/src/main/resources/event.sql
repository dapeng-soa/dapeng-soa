/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 50721
 Source Host           : localhost
 Source Database       : maple

 Target Server Type    : MySQL
 Target Server Version : 50721
 File Encoding         : utf-8

 Date: 02/24/2018 14:32:27 PM
*/

SET NAMES utf8;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `common_event`
-- ----------------------------
DROP TABLE IF EXISTS `common_event`;
CREATE TABLE `common_event` (
  `id` bigint(20) NOT NULL COMMENT '事件id',
  `event_type` varchar(255) DEFAULT NULL COMMENT '事件类型',
  `event_binary` blob COMMENT '事件内容',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;
