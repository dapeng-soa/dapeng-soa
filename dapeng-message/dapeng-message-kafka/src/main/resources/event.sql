
SET NAMES utf8;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `common_event`
-- ----------------------------

DROP TABLE IF EXISTS `common_event`;
CREATE TABLE `common_event` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '事件id',
  `event_type` varchar(255) DEFAULT NULL COMMENT '事件类型',
  `event_binary` blob DEFAULT NULL COMMENT '事件内容',
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;
