/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 50735 (5.7.35)
 Source Host           : localhost:3306
 Source Schema         : ingot_nacos_config

 Target Server Type    : MySQL
 Target Server Version : 50735 (5.7.35)
 File Encoding         : 65001

 Date: 31/10/2023 11:48:02
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for config_info
-- ----------------------------
DROP TABLE IF EXISTS `config_info`;
CREATE TABLE `config_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `data_id` varchar(255) COLLATE utf8_bin NOT NULL COMMENT 'data_id',
  `group_id` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `content` longtext COLLATE utf8_bin NOT NULL COMMENT 'content',
  `md5` varchar(32) COLLATE utf8_bin DEFAULT NULL COMMENT 'md5',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `src_user` text COLLATE utf8_bin COMMENT 'source user',
  `src_ip` varchar(50) COLLATE utf8_bin DEFAULT NULL COMMENT 'source ip',
  `app_name` varchar(128) COLLATE utf8_bin DEFAULT NULL,
  `tenant_id` varchar(128) COLLATE utf8_bin DEFAULT '' COMMENT '租户字段',
  `c_desc` varchar(256) COLLATE utf8_bin DEFAULT NULL,
  `c_use` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `effect` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `type` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `c_schema` text COLLATE utf8_bin,
  `encrypted_data_key` text COLLATE utf8_bin NOT NULL COMMENT '秘钥',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_configinfo_datagrouptenant` (`data_id`,`group_id`,`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=114 DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='config_info';

-- ----------------------------
-- Records of config_info
-- ----------------------------
BEGIN;
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`, `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`, `encrypted_data_key`) VALUES (1, 'application-common.yml', 'DEFAULT_GROUP', 'jasypt:\n  encryptor:\n    password: ingot\n\nserver:\n  tomcat:\n    uri-encoding: UTF-8\n    remote-ip-header: x-forwarded-for\n  use-forward-headers: true\n\nmanagement:\n  endpoints:\n    web:\n      exposure:\n        include: \'*\'\n\nspring:\n  servlet:\n    multipart:\n      max-file-size: 100MB\n      max-request-size: 100MB\n  jackson:\n    date-format: yyyy-MM-dd HH:mm:ss\n    time-zone: GMT+8\n    default-property-inclusion: non_null\n  data:\n    redis:\n      database: 0\n      host: ${REDIS_HOST:ingot-db-redis}\n      port: ${REDIS_PORT:6379}\n      jedis:\n        pool:\n          max-active: 20\n  cloud:\n    sentinel:\n      eager: true\n      log:\n        dir: /ingot-data/sentinel/\n        switch-pid: true\n    bus:\n      id: ${spring.application.name}:${spring.cloud.client.ip-address}:${server.port}\n\nfeign:\n  sentinel:\n    enabled: true\n  okhttp:\n    enabled: true\n  httpclient:\n    enabled: false\n  client:\n    config:\n      default:\n        connectTimeout: 10000\n        readTimeout: 10000\n  compression:\n    request:\n      enabled: true\n    response:\n      enabled: true\n\nswagger:\n  title: Ingot Cloud Swagger API\n  license: Powered By Ingot\n  licenseUrl: https://secingot.com/\n  terms-of-service-url: https://secingot.com/\n  contact:\n    email: magician.of.technique@aliyun.com\n    url: https://wangchao.im\n  authorization:\n    name: Ingot OAuth\n    auth-regex: ^.*$\n    authorization-scope-list:\n      - scope: server\n        description: server all\n    token-url-list:\n      - http://${GATEWAY-HOST:ingot-gateway}:${GATEWAY-PORT:7980}/ac/oauth/token\n', '2a7668e656cf3bae09d2b2635d0e7154', '2022-12-30 10:23:41', '2023-08-22 15:33:29', 'nacos', '172.88.0.1', '', 'ingot', '', '', '', 'yaml', '', '');
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`, `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`, `encrypted_data_key`) VALUES (2, 'application-mysql.yml', 'DEFAULT_GROUP', 'spring:\n  datasource:\n    driver-class-name: com.mysql.cj.jdbc.Driver\n    filters: stat,wall,log4j\n    type: com.alibaba.druid.pool.DruidDataSource\n    druid:\n      initial-size: 10\n      min-idle: 10\n      max-active: 30\n      test-on-borrow: true\n      time-between-eviction-runs-millis: 60000\n      min-evictable-idle-time-millis: 300000\n      web-stat-filter:\n        exclusions: \'*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*\'\n      stat-view-servlet:\n        allow:\n        login-username: admin\n        login-password: admin\n      connection-init-sqls:\n      - SET NAMES utf8mb4\n\nmybatis:\n  configuration:\n    map-underscore-to-camel-case: true\n\nmybatis-plus:\n  mapper-locations: classpath:/mapper/*Mapper.xml,classpath*:sdk/mapper/*.xml\n  configuration:\n    default-enum-type-handler: com.baomidou.mybatisplus.core.handlers.CompositeEnumTypeHandler\n  global-config:\n    banner: false\n    db-config:\n      #logic-delete-field: delete_at  # 全局逻辑删除的实体字段名(since 3.3.0,配置后可以忽略不配置步骤2)\n      logic-delete-value: now() # 逻辑已删除值(默认为 1)\n      logic-not-delete-value: \"null\" # 逻辑未删除值(默认为 0)\n\n', 'c9c386bf0d6133aedef947a167a40ce2', '2022-12-30 10:23:41', '2023-02-06 08:40:35', 'nacos', '172.88.0.1', '', 'ingot', '', '', '', 'yaml', '', '');
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`, `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`, `encrypted_data_key`) VALUES (3, 'ingot-service-pms.yml', 'DEFAULT_GROUP', 'spring:\n  datasource:\n    url: jdbc:mysql://${MYSQL_HOST:ingot-db-mysql}:${MYSQL_PORT:3306}/ingot_core?useUnicode=true&characterEncoding=UTF8&useSSL=false&serverTimezone=GMT%2B8\n  security:\n    oauth2:\n      resourceserver:\n        jwt:\n          issuer-uri: http://ingot-auth-server:5100\n\ningot:\n  security:\n    ignoreTenantValidateRoleCodeList:\n      - role_admin\n  tenant:\n    tables:\n      - sys_authority\n      - sys_dept\n      - sys_menu\n      - sys_role\n      - sys_role_user\n      - sys_role_group\n      - sys_user_dept\n  crypto:\n    secretKeys:\n      aes: ingotingotingot1\n  mybatis:\n    showSqlLog: true\n  minio:\n    url: http://ingot-cloud:5000\n    publicUrl: http://ingot-cloud:5000\n    accessKey: wnkZeaug4yV6ztKJ\n    secretKey: 6H3vEHUtxMfoQ8c6K6qmEKlDZpJ1BOCh\n  swagger:\n    title: ${spring.application.name}\n    description: 权限管理系统\n  job:\n    xxl:\n      admin:\n        addresses: http://${INGOT_TMC_HOST:ingot-task-manager-center}:8060\n      executor:\n        appname: ${spring.application.name}\n        ip:\n        port: 9800\n        logpath: /data/ingot/logs/${spring.application.name}/jobhandler\n        logretentiondays: -1\n      accessToken: IngotTmcAccessToken\n', '419a1097d6d41c0ff4aef72a59a6da03', '2022-12-30 10:23:41', '2023-09-25 15:58:36', 'nacos', '172.88.0.1', '', 'ingot', '', '', '', 'yaml', '', '');
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`, `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`, `encrypted_data_key`) VALUES (4, 'ingot-service-gateway.yml', 'DEFAULT_GROUP', 'spring:\n  cloud:\n    gateway:\n      locator:\n        enabled: true\n      routes:\n      - id: ingot-service-auth\n        uri: lb://ingot-service-auth\n        predicates:\n        - Path=/auth/**\n        filters:\n        - StripPrefix=1\n        - TokenPasswordDecoderFilter\n      - id: ingot-service-pms\n        uri: lb://ingot-service-pms\n        predicates:\n        - Path=/pms/**\n        filters:\n        - StripPrefix=1\n        #限流过滤器\n        - name: RequestRateLimiter\n          args:\n            key-resolver: \'#{@remoteAddrKeyResolver}\'\n            # 每秒最大访问次数（放令牌桶的速率）\n            redis-rate-limiter.replenishRate: 50\n            # 令牌桶最大容量（令牌桶的大小）\n            redis-rate-limiter.burstCapacity: 100\n \ningot:\n  crypto:\n    secretKeys:\n      aes: ingotingotingot1\n  vc:\n    verifyUrls:\n      - image,/auth/oauth2/token,POST\n      - image,/auth/oauth2/pre_authorize,POST', '37b828f3234434f04b254ff69969aff1', '2022-12-30 10:23:41', '2023-09-25 15:58:17', 'nacos', '172.88.0.1', '', 'ingot', '', '', '', 'yaml', '', '');
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`, `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`, `encrypted_data_key`) VALUES (5, 'ingot-service-auth.yml', 'DEFAULT_GROUP', 'spring:\n  mvc:\n    static-path-pattern: /static/**\n  thymeleaf:\n    cache: false\n    prefix: classpath:/templates/\n    suffix: .html\n    encoding: UTF-8\n    servlet:\n      content-type: text/html\n  datasource:\n    url: jdbc:mysql://${MYSQL_HOST:ingot-db-mysql}:${MYSQL_PORT:3306}/ingot_core?useUnicode=true&characterEncoding=UTF8&useSSL=false&serverTimezone=GMT%2B8\n\ningot:\n  security:\n    oauth2:\n      auth:\n        loginFormUrl: https://login.ingotcloud.top/oauth2/challenge\n        issuer: http://ingot-auth-server:5100\n      resource:\n        publicUrls:\n          - /favicon.ico,GET\n          - /static/**,GET\n          - /logout,*\n  swagger:\n    title: ${spring.application.name}\n    description: 鉴权中心\n  job:\n    xxl:\n      admin:\n        addresses: http://${INGOT_TMC_HOST:ingot-task-manager-center}:8060\n      executor:\n        appname: ${spring.application.name}\n        ip:\n        port: 9900\n        logpath: /data/ingot/logs/${spring.application.name}/jobhandler\n        logretentiondays: -1\n      accessToken: IngotTmcAccessToken', '8d740a128891043a9d475c433989ef25', '2022-12-30 10:23:41', '2023-09-11 15:11:15', 'nacos', '172.88.0.1', '', 'ingot', '', '', '', 'yaml', '', '');
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`, `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`, `encrypted_data_key`) VALUES (71, 'application-common.yml', 'DEFAULT_GROUP', 'jasypt:\n  encryptor:\n    password: ingot\n\nserver:\n  tomcat:\n    uri-encoding: UTF-8\n    remote-ip-header: x-forwarded-for\n  use-forward-headers: true\n\nmanagement:\n  endpoints:\n    web:\n      exposure:\n        include: \'*\'\n\nspring:\n  servlet:\n    multipart:\n      max-file-size: 100MB\n      max-request-size: 100MB\n  jackson:\n    date-format: yyyy-MM-dd HH:mm:ss\n    time-zone: GMT+8\n    default-property-inclusion: non_null\n  redis:\n    database: 4\n    host: ${REDIS_HOST:hongya-db-redis}\n    port: ${REDIS_PORT:6379}\n    jedis:\n      pool:\n        max-active: 20\n  cloud:\n    sentinel:\n      eager: true\n      log:\n        dir: /ingot-data/sentinel/\n        switch-pid: true\n    bus:\n      id: ${spring.application.name}:${spring.cloud.client.ip-address}:${server.port}\n\nfeign:\n  sentinel:\n    enabled: true\n  okhttp:\n    enabled: true\n  httpclient:\n    enabled: false\n  client:\n    config:\n      default:\n        connectTimeout: 10000\n        readTimeout: 10000\n  compression:\n    request:\n      enabled: true\n    response:\n      enabled: true\n', '55093912c854b2192af81ece67aa666f', '2023-03-17 09:52:01', '2023-03-17 09:52:01', NULL, '172.88.0.1', '', 'hongya', NULL, NULL, NULL, 'yaml', NULL, '');
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`, `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`, `encrypted_data_key`) VALUES (72, 'application-mysql.yml', 'DEFAULT_GROUP', 'spring:\n  datasource:\n    driver-class-name: com.mysql.cj.jdbc.Driver\n    filters: stat,wall,log4j\n    type: com.alibaba.druid.pool.DruidDataSource\n    druid:\n      initial-size: 10\n      min-idle: 10\n      max-active: 30\n      test-on-borrow: true\n      time-between-eviction-runs-millis: 60000\n      min-evictable-idle-time-millis: 300000\n      web-stat-filter:\n        exclusions: \'*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*\'\n      stat-view-servlet:\n        allow:\n        login-username: admin\n        login-password: admin\n      connection-init-sqls:\n      - SET NAMES utf8mb4\n\nmybatis:\n  configuration:\n    map-underscore-to-camel-case: true\n\nmybatis-plus:\n  mapper-locations: classpath:/mapper/*Mapper.xml,classpath*:sdk/mapper/*.xml\n  configuration:\n    default-enum-type-handler: com.baomidou.mybatisplus.core.handlers.CompositeEnumTypeHandler\n  global-config:\n    banner: false\n    db-config:\n      #logic-delete-field: delete_at  # 全局逻辑删除的实体字段名(since 3.3.0,配置后可以忽略不配置步骤2)\n      logic-delete-value: now() # 逻辑已删除值(默认为 1)\n      logic-not-delete-value: \"null\" # 逻辑未删除值(默认为 0)\n\n', 'c9c386bf0d6133aedef947a167a40ce2', '2023-03-17 09:52:01', '2023-03-17 09:52:01', NULL, '172.88.0.1', '', 'hongya', NULL, NULL, NULL, 'yaml', NULL, '');
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`, `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`, `encrypted_data_key`) VALUES (73, 'hongya-service-pms.yml', 'DEFAULT_GROUP', 'spring:\n  datasource:\n    url: jdbc:mysql://${MYSQL_HOST:hongya-db-mysql}:${MYSQL_PORT:3306}/hongya_core?useUnicode=true&characterEncoding=UTF8&useSSL=false&serverTimezone=GMT%2B8\n  security:\n    oauth2:\n      resourceserver:\n        jwt:\n          issuer-uri: http://hongya-auth-server:5100\n\ningot:\n  security:\n    ignoreTenantValidateRoleCodeList:\n      - role_admin\n      - role_manager\n  tenant:\n    tables:\n      - sys_dept\n      - sys_user\n  minio:\n    url: https://oss-api.cdhyrz.com\n    accessKey: 1TBpVdvN0Ktwq5BQ\n    secretKey: WhIKl8BINhZkie0cXX5QA8SIoVhIPRoY\n  crypto:\n    aesKey: ingotingotingot1\n  mybatis:\n    showSqlLog: true\n', '0f60245ceada3e0b9d1f09c3c68d342a', '2023-03-17 09:52:01', '2023-05-31 17:18:12', 'nacos', '172.88.0.1', '', 'hongya', '', '', '', 'yaml', '', '');
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`, `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`, `encrypted_data_key`) VALUES (74, 'hongya-service-gateway.yml', 'DEFAULT_GROUP', 'spring:\n  cloud:\n    gateway:\n      locator:\n        enabled: true\n      routes:\n      - id: hongya-service-auth\n        uri: lb://hongya-service-auth\n        predicates:\n        - Path=/auth/**\n        filters:\n        - StripPrefix=1\n        - TokenPasswordDecoderFilter\n      - id: hongya-service-pms\n        uri: lb://hongya-service-pms\n        predicates:\n        - Path=/pms/**\n        filters:\n        - StripPrefix=1\n        #限流过滤器\n        - name: RequestRateLimiter\n          args:\n            key-resolver: \'#{@remoteAddrKeyResolver}\'\n            # 每秒最大访问次数（放令牌桶的速率）\n            redis-rate-limiter.replenishRate: 50\n            # 令牌桶最大容量（令牌桶的大小）\n            redis-rate-limiter.burstCapacity: 100\n \ningot:\n  crypto:\n    aesKey: ingotingotingot1', '6a12f0c32e2020db7398772dee490325', '2023-03-17 09:52:01', '2023-03-17 09:52:01', NULL, '172.88.0.1', '', 'hongya', NULL, NULL, NULL, 'yaml', NULL, '');
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`, `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`, `encrypted_data_key`) VALUES (75, 'hongya-service-auth.yml', 'DEFAULT_GROUP', 'spring:\n  mvc:\n    static-path-pattern: /static/**\n  thymeleaf:\n    cache: false\n    prefix: classpath:/templates/\n    suffix: .html\n    encoding: UTF-8\n    servlet:\n      content-type: text/html\n  datasource:\n    url: jdbc:mysql://${MYSQL_HOST:hongya-db-mysql}:${MYSQL_PORT:3306}/hongya_core?useUnicode=true&characterEncoding=UTF8&useSSL=false&serverTimezone=GMT%2B8\n\ningot:\n  security:\n    oauth2:\n      auth:\n        issuer: http://hongya-auth-server:5100\n      resource:\n        publicUrls:\n          - /favicon.ico,GET\n          - /static/**,GET', '94dc5a09387d4709c997f02c48db3d38', '2023-03-17 09:52:01', '2023-03-17 09:52:01', NULL, '172.88.0.1', '', 'hongya', NULL, NULL, NULL, 'yaml', NULL, '');
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`, `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`, `encrypted_data_key`) VALUES (76, 'application-common.yml', 'DEFAULT_GROUP', 'jasypt:\n  encryptor:\n    password: ingot\n\nserver:\n  tomcat:\n    uri-encoding: UTF-8\n    remote-ip-header: x-forwarded-for\n  use-forward-headers: true\n\nmanagement:\n  endpoints:\n    web:\n      exposure:\n        include: \'*\'\n\nspring:\n  servlet:\n    multipart:\n      max-file-size: 100MB\n      max-request-size: 100MB\n  jackson:\n    date-format: yyyy-MM-dd HH:mm:ss\n    time-zone: GMT+8\n    default-property-inclusion: non_null\n  redis:\n    database: 10\n    host: ${REDIS_HOST:tonykancai-db-redis}\n    port: ${REDIS_PORT:6379}\n    jedis:\n      pool:\n        max-active: 20\n  cloud:\n    sentinel:\n      eager: true\n      log:\n        dir: /ingot-data/sentinel/\n        switch-pid: true\n    bus:\n      id: ${spring.application.name}:${spring.cloud.client.ip-address}:${server.port}\n\nfeign:\n  sentinel:\n    enabled: true\n  okhttp:\n    enabled: true\n  httpclient:\n    enabled: false\n  client:\n    config:\n      default:\n        connectTimeout: 10000\n        readTimeout: 10000\n  compression:\n    request:\n      enabled: true\n    response:\n      enabled: true\n\nswagger:\n  title: Ingot Cloud Swagger API\n  license: Powered By Ingot\n  licenseUrl: https://secingot.com/\n  terms-of-service-url: https://secingot.com/\n  contact:\n    email: magician.of.technique@aliyun.com\n    url: https://wangchao.im\n  authorization:\n    name: Ingot OAuth\n    auth-regex: ^.*$\n    authorization-scope-list:\n      - scope: server\n        description: server all\n    token-url-list:\n      - http://${GATEWAY-HOST:tonykancai-gateway}:${GATEWAY-PORT:7980}/ac/oauth/token\n', '2f75ba23b21f1be14cab4522eafbb6f8', '2023-03-17 09:52:13', '2023-03-17 09:52:13', NULL, '172.88.0.1', '', 'tonykancai', NULL, NULL, NULL, 'yaml', NULL, '');
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`, `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`, `encrypted_data_key`) VALUES (77, 'application-mysql.yml', 'DEFAULT_GROUP', 'spring:\n  datasource:\n    driver-class-name: com.mysql.cj.jdbc.Driver\n    filters: stat,wall,log4j\n    type: com.alibaba.druid.pool.DruidDataSource\n    druid:\n      initial-size: 10\n      min-idle: 10\n      max-active: 30\n      test-on-borrow: true\n      time-between-eviction-runs-millis: 60000\n      min-evictable-idle-time-millis: 300000\n      web-stat-filter:\n        exclusions: \'*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*\'\n      stat-view-servlet:\n        allow:\n        login-username: admin\n        login-password: admin\n      connection-init-sqls:\n      - SET NAMES utf8mb4\n\nmybatis:\n  configuration:\n    map-underscore-to-camel-case: true\n\nmybatis-plus:\n  mapper-locations: classpath:/mapper/*Mapper.xml,classpath*:sdk/mapper/*.xml\n  configuration:\n    default-enum-type-handler: com.baomidou.mybatisplus.core.handlers.CompositeEnumTypeHandler\n  global-config:\n    banner: false\n    db-config:\n      #logic-delete-field: delete_at  # 全局逻辑删除的实体字段名(since 3.3.0,配置后可以忽略不配置步骤2)\n      logic-delete-value: now() # 逻辑已删除值(默认为 1)\n      logic-not-delete-value: \"null\" # 逻辑未删除值(默认为 0)\n\n', 'c9c386bf0d6133aedef947a167a40ce2', '2023-03-17 09:52:13', '2023-03-17 09:52:13', NULL, '172.88.0.1', '', 'tonykancai', NULL, NULL, NULL, 'yaml', NULL, '');
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`, `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`, `encrypted_data_key`) VALUES (78, 'tonykancai-service-pms.yml', 'DEFAULT_GROUP', 'spring:\n  datasource:\n    url: jdbc:mysql://${MYSQL_HOST:ingot-db-mysql}:${MYSQL_PORT:3306}/tonykancai_core?useUnicode=true&characterEncoding=UTF8&useSSL=false&serverTimezone=GMT%2B8\n  security:\n    oauth2:\n      resourceserver:\n        jwt:\n          issuer-uri: http://ingot-auth-server:5100\n\ningot:\n  security:\n    ignoreTenantValidateRoleCodeList:\n      - role_admin\n      - role_manager\n  tenant:\n    tables:\n      - sys_dept\n      - sys_user\n  crypto:\n    aesKey: ingotingotingot1\n  mybatis:\n    showSqlLog: true\n  minio:\n    url: http://ingot-minio:5000\n    accessKey: yFyruNdKtxW7m9CO\n    secretKey: uXOt4PqVMKg7TF2LNCSMg6Mfz1RdxBKm\n', 'e1eb304fca5da317e7744f137bcfe447', '2023-03-17 09:52:13', '2023-03-17 09:56:00', 'nacos', '172.88.0.1', '', 'tonykancai', '', '', '', 'yaml', '', '');
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`, `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`, `encrypted_data_key`) VALUES (79, 'tonykancai-service-gateway.yml', 'DEFAULT_GROUP', 'spring:\n  cloud:\n    gateway:\n      locator:\n        enabled: true\n      routes:\n      - id: tonykancai-service-auth\n        uri: lb://tonykancai-service-auth\n        predicates:\n        - Path=/auth/**\n        filters:\n        - StripPrefix=1\n        - TokenPasswordDecoderFilter\n      - id: tonykancai-service-pms\n        uri: lb://tonykancai-service-pms\n        predicates:\n        - Path=/pms/**\n        filters:\n        - StripPrefix=1\n        #限流过滤器\n        - name: RequestRateLimiter\n          args:\n            key-resolver: \'#{@remoteAddrKeyResolver}\'\n            # 每秒最大访问次数（放令牌桶的速率）\n            redis-rate-limiter.replenishRate: 100\n            # 令牌桶最大容量（令牌桶的大小）\n            redis-rate-limiter.burstCapacity: 200\n \ningot:\n  crypto:\n    aesKey: ingotingotingot1', '1d560761223dd604136646e0adf4bbf9', '2023-03-17 09:52:13', '2023-03-17 09:52:13', NULL, '172.88.0.1', '', 'tonykancai', NULL, NULL, NULL, 'yaml', NULL, '');
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`, `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`, `encrypted_data_key`) VALUES (80, 'tonykancai-service-auth.yml', 'DEFAULT_GROUP', 'spring:\n  mvc:\n    static-path-pattern: /static/**\n  thymeleaf:\n    cache: false\n    prefix: classpath:/templates/\n    suffix: .html\n    encoding: UTF-8\n    servlet:\n      content-type: text/html\n  datasource:\n    url: jdbc:mysql://${MYSQL_HOST:ingot-db-mysql}:${MYSQL_PORT:3306}/tonykancai_core?useUnicode=true&characterEncoding=UTF8&useSSL=false&serverTimezone=GMT%2B8\n\ningot:\n  security:\n    oauth2:\n      auth:\n        issuer: http://ingot-auth-server:5100\n      resource:\n        publicUrls:\n          - /favicon.ico,GET\n          - /static/**,GET', '31adda1609255503f2c835ad660a0f1f', '2023-03-17 09:52:13', '2023-03-17 09:52:13', NULL, '172.88.0.1', '', 'tonykancai', NULL, NULL, NULL, 'yaml', NULL, '');
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`, `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`, `encrypted_data_key`) VALUES (81, 'application-common.yml', 'DEFAULT_GROUP', 'jasypt:\n  encryptor:\n    password: ingot\n\nserver:\n  tomcat:\n    uri-encoding: UTF-8\n    remote-ip-header: x-forwarded-for\n  use-forward-headers: true\n\nmanagement:\n  endpoints:\n    web:\n      exposure:\n        include: \'*\'\n\nspring:\n  servlet:\n    multipart:\n      max-file-size: 100MB\n      max-request-size: 100MB\n  jackson:\n    date-format: yyyy-MM-dd HH:mm:ss\n    time-zone: GMT+8\n    default-property-inclusion: non_null\n  redis:\n    database: 0\n    host: ${REDIS_HOST:ingot-db-redis}\n    port: ${REDIS_PORT:6379}\n    jedis:\n      pool:\n        max-active: 20\n  cloud:\n    sentinel:\n      eager: true\n      log:\n        dir: /ingot-data/sentinel/\n        switch-pid: true\n    bus:\n      id: ${spring.application.name}:${spring.cloud.client.ip-address}:${server.port}\n\nfeign:\n  sentinel:\n    enabled: true\n  okhttp:\n    enabled: true\n  httpclient:\n    enabled: false\n  client:\n    config:\n      default:\n        connectTimeout: 10000\n        readTimeout: 10000\n  compression:\n    request:\n      enabled: true\n    response:\n      enabled: true\n\nswagger:\n  title: Ingot Cloud Swagger API\n  license: Powered By Ingot\n  licenseUrl: https://secingot.com/\n  terms-of-service-url: https://secingot.com/\n  contact:\n    email: magician.of.technique@aliyun.com\n    url: https://wangchao.im\n  authorization:\n    name: Ingot OAuth\n    auth-regex: ^.*$\n    authorization-scope-list:\n      - scope: server\n        description: server all\n    token-url-list:\n      - http://${GATEWAY-HOST:ingot-gateway}:${GATEWAY-PORT:7980}/ac/oauth/token\n', '9c930824c1e608500d6a234a903ac590', '2023-03-17 09:52:46', '2023-03-17 09:52:46', NULL, '172.88.0.1', '', 'mstx', NULL, NULL, NULL, 'yaml', NULL, '');
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`, `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`, `encrypted_data_key`) VALUES (82, 'application-mysql.yml', 'DEFAULT_GROUP', 'spring:\n  datasource:\n    driver-class-name: com.mysql.cj.jdbc.Driver\n    filters: stat,wall,log4j\n    type: com.alibaba.druid.pool.DruidDataSource\n    druid:\n      initial-size: 10\n      min-idle: 10\n      max-active: 30\n      test-on-borrow: true\n      time-between-eviction-runs-millis: 60000\n      min-evictable-idle-time-millis: 300000\n      web-stat-filter:\n        exclusions: \'*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*\'\n      stat-view-servlet:\n        allow:\n        login-username: admin\n        login-password: admin\n      connection-init-sqls:\n      - SET NAMES utf8mb4\n\nmybatis:\n  configuration:\n    map-underscore-to-camel-case: true\n\nmybatis-plus:\n  mapper-locations: classpath:/mapper/*Mapper.xml,classpath*:sdk/mapper/*.xml\n  configuration:\n    default-enum-type-handler: com.baomidou.mybatisplus.core.handlers.CompositeEnumTypeHandler\n  global-config:\n    banner: false\n    db-config:\n      #logic-delete-field: delete_at  # 全局逻辑删除的实体字段名(since 3.3.0,配置后可以忽略不配置步骤2)\n      logic-delete-value: now() # 逻辑已删除值(默认为 1)\n      logic-not-delete-value: \"null\" # 逻辑未删除值(默认为 0)\n\n', 'c9c386bf0d6133aedef947a167a40ce2', '2023-03-17 09:52:46', '2023-03-17 09:52:46', NULL, '172.88.0.1', '', 'mstx', NULL, NULL, NULL, 'yaml', NULL, '');
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`, `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`, `encrypted_data_key`) VALUES (83, 'ingot-service-pms.yml', 'DEFAULT_GROUP', 'spring:\n  datasource:\n    url: jdbc:mysql://${MYSQL_HOST:ingot-db-mysql}:${MYSQL_PORT:3306}/ingot_core?useUnicode=true&characterEncoding=UTF8&useSSL=false&serverTimezone=GMT%2B8\n  security:\n    oauth2:\n      resourceserver:\n        jwt:\n          issuer-uri: http://ingot-auth-server:5100\n\ningot:\n  security:\n    ignoreTenantValidateRoleCodeList:\n      - role_admin\n  tenant:\n    tables:\n      - sys_dept\n      - sys_user\n  crypto:\n    aesKey: ingotingotingot1\n  mybatis:\n    showSqlLog: true\n  minio:\n    url: http://182.92.124.166:5000\n    accessKey: tsLoUVylEDSzvW3F\n    secretKey: 9Y7lmYDnXRx9tTqQ0ztbbUgHVjpIzfFa\n  swagger:\n    title: ${spring.application.name}\n    description: 权限管理系统\n  job:\n    xxl:\n      admin:\n        addresses: http://${INGOT_TMC_HOST:ingot-task-manager-center}:8060\n      executor:\n        appname: ${spring.application.name}\n        ip:\n        port: 9800\n        logpath: /data/ingot/logs/${spring.application.name}/jobhandler\n        logretentiondays: -1\n      accessToken: IngotTmcAccessToken\n', 'a25bb9c43e0888fa9e870ca523b27a7c', '2023-03-17 09:52:46', '2023-03-28 09:15:57', 'nacos', '172.88.0.1', '', 'mstx', '', '', '', 'yaml', '', '');
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`, `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`, `encrypted_data_key`) VALUES (84, 'ingot-service-gateway.yml', 'DEFAULT_GROUP', 'spring:\n  cloud:\n    gateway:\n      locator:\n        enabled: true\n      routes:\n      - id: ingot-service-auth\n        uri: lb://ingot-service-auth\n        predicates:\n        - Path=/auth/**\n        filters:\n        - StripPrefix=1\n        - TokenPasswordDecoderFilter\n      - id: ingot-service-pms\n        uri: lb://ingot-service-pms\n        predicates:\n        - Path=/pms/**\n        filters:\n        - StripPrefix=1\n        #限流过滤器\n        - name: RequestRateLimiter\n          args:\n            key-resolver: \'#{@remoteAddrKeyResolver}\'\n            # 每秒最大访问次数（放令牌桶的速率）\n            redis-rate-limiter.replenishRate: 50\n            # 令牌桶最大容量（令牌桶的大小）\n            redis-rate-limiter.burstCapacity: 100\n \ningot:\n  crypto:\n    aesKey: ingotingotingot1', '1937495d53a9faab9c3ae72c22b3d7eb', '2023-03-17 09:52:46', '2023-03-17 09:52:46', NULL, '172.88.0.1', '', 'mstx', NULL, NULL, NULL, 'yaml', NULL, '');
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`, `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`, `encrypted_data_key`) VALUES (85, 'ingot-service-auth.yml', 'DEFAULT_GROUP', 'spring:\n  mvc:\n    static-path-pattern: /static/**\n  thymeleaf:\n    cache: false\n    prefix: classpath:/templates/\n    suffix: .html\n    encoding: UTF-8\n    servlet:\n      content-type: text/html\n  datasource:\n    url: jdbc:mysql://${MYSQL_HOST:ingot-db-mysql}:${MYSQL_PORT:3306}/ingot_core?useUnicode=true&characterEncoding=UTF8&useSSL=false&serverTimezone=GMT%2B8\n\ningot:\n  security:\n    oauth2:\n      auth:\n        issuer: http://ingot-auth-server:5100\n      resource:\n        publicUrls:\n          - /favicon.ico,GET\n          - /static/**,GET\n  swagger:\n    title: ${spring.application.name}\n    description: 鉴权中心\n  job:\n    xxl:\n      admin:\n        addresses: http://${INGOT_TMC_HOST:ingot-task-manager-center}:8060\n      executor:\n        appname: ${spring.application.name}\n        ip:\n        port: 9900\n        logpath: /data/ingot/logs/${spring.application.name}/jobhandler\n        logretentiondays: -1\n      accessToken: IngotTmcAccessToken', '15b347fb27f6844665cbbfd599ffdb2f', '2023-03-17 09:52:46', '2023-03-17 09:52:46', NULL, '172.88.0.1', '', 'mstx', NULL, NULL, NULL, 'yaml', NULL, '');
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`, `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`, `encrypted_data_key`) VALUES (94, 'application-common.yml', 'DEFAULT_GROUP', 'jasypt:\n  encryptor:\n    password: ingox\n\nserver:\n  tomcat:\n    uri-encoding: UTF-8\n    remote-ip-header: x-forwarded-for\n  use-forward-headers: true\n\nmanagement:\n  endpoints:\n    web:\n      exposure:\n        include: \'*\'\n\nspring:\n  servlet:\n    multipart:\n      max-file-size: 100MB\n      max-request-size: 100MB\n  jackson:\n    date-format: yyyy-MM-dd HH:mm:ss\n    time-zone: GMT+8\n    default-property-inclusion: non_null\n  rabbitmq:\n    host: ${RABBIT_MQ_HOST:ingot-mq-rabbit}\n    port: ${RABBIT_MQ_PORT:5672}\n  redis:\n    database: 1\n    host: ${REDIS_HOST:ingot-db-redis}\n    port: ${REDIS_PORT:6379}\n    jedis:\n      pool:\n        max-active: 20\n  cloud:\n    sentinel:\n      eager: true\n      log:\n        dir: /data/ingot/sentinel/\n        switch-pid: true\n    bus:\n      id: ${spring.application.name}:${spring.cloud.client.ip-address}:${server.port}\n\nfeign:\n  sentinel:\n    enabled: true\n  okhttp:\n    enabled: true\n  httpclient:\n    enabled: false\n  client:\n    config:\n      default:\n        connectTimeout: 10000\n        readTimeout: 10000\n  compression:\n    request:\n      enabled: true\n    response:\n      enabled: true\n\nlogging:\n  level:\n    org.springframework.security: DEBUG\n    # nacos naming log 等级设置为 warning\n    com.alibaba.nacos.client.naming: WARN\n\nswagger:\n  title: Ingot Cloud Swagger API\n  license: Powered By Ingot\n  licenseUrl: https://secingot.com/\n  terms-of-service-url: https://secingot.com/\n  contact:\n    email: magician.of.technique@aliyun.com\n    url: https://wangchao.im\n  authorization:\n    name: Ingot OAuth\n    auth-regex: ^.*$\n    authorization-scope-list:\n      - scope: server\n        description: server all\n    token-url-list:\n      - http://${GATEWAY-HOST:ingot-gateway}:${GATEWAY-PORT:8020}/ac/oauth/token\n\nsecurity:\n  oauth2:\n    client:\n      grant-type: client_credentials\n      authentication-scheme: header\n      access-token-uri: http://ingot-auth-center:8050/oauth/token\n', 'b3f28f18528a294b9f98db6def510e56', '2023-04-07 11:30:49', '2023-04-07 11:30:49', NULL, '172.88.0.1', '', 'shareducks', NULL, NULL, NULL, 'yaml', NULL, '');
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`, `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`, `encrypted_data_key`) VALUES (95, 'application-mysql.yml', 'DEFAULT_GROUP', 'spring:\n  datasource:\n    driver-class-name: com.mysql.cj.jdbc.Driver\n    filters: stat,wall,log4j\n    type: com.alibaba.druid.pool.DruidDataSource\n    druid:\n      initial-size: 10\n      min-idle: 10\n      max-active: 30\n      test-on-borrow: true\n      time-between-eviction-runs-millis: 60000\n      min-evictable-idle-time-millis: 300000\n      web-stat-filter:\n        exclusions: \'*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*\'\n      stat-view-servlet:\n        allow:\n        login-username: admin\n        login-password: admin\n      connection-init-sqls:\n      - SET NAMES utf8mb4\n\nmybatis:\n  configuration:\n    map-underscore-to-camel-case: true\n\nmybatis-plus:\n  mapper-locations: classpath:/mapper/*Mapper.xml,classpath*:sdk/mapper/*.xml\n\n', 'f353c64c5dd0e37f4692ca41713634e2', '2023-04-07 11:30:49', '2023-04-07 11:30:49', NULL, '172.88.0.1', '', 'shareducks', NULL, NULL, NULL, 'yaml', NULL, '');
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`, `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`, `encrypted_data_key`) VALUES (96, 'ingot-client-ac.yml', 'DEFAULT_GROUP', 'spring:\n  jackson:\n    date-format: yyyy-MM-dd HH:mm:ss\n    time-zone: GMT+8\n    default-property-inclusion: non_null\n  resources:\n    static-locations: classpath:/static/\n  datasource:\n    url: jdbc:mysql://${MYSQL_HOST:ingot-db-mysql}:${MYSQL_PORT:3306}/rebate_ingot_pms_db?useUnicode=true&characterEncoding=UTF8&useSSL=false&serverTimezone=GMT%2B8\n\nsecurity:\n  oauth2:\n    client:\n      client-id: ${spring.application.name}\n      client-secret: GwCwru42UrxQTUFLWOAsJqa3/+WSTlRMwiQDBq8KOgI=\n    resource:\n      id: ingot-resource-ac\n\ningot:\n  swagger:\n    title: ${spring.application.name}\n    description: 鉴权中心\n  mq:\n    producerExchange: ingot.ac\n    listenerQueues: ingot.queue.uc\n  job:\n    xxl:\n      admin:\n        addresses: http://${INGOT_TMC_HOST:ingot-task-manager-center}:8060\n      executor:\n        appname: ${spring.application.name}\n        ip:\n        port: 9900\n        logpath: /data/ingot/logs/${spring.application.name}/jobhandler\n        logretentiondays: -1\n      accessToken: IngotTmcAccessToken\n  oauth2:\n    tokenStore: jwt\n    rsaSecret: IngotRsaSecret\n    resource:\n      ignoreUrls:\n      - /auth/login\n      - /auth/user/refreshToken\n      - /auth/modifyPasswordByMobile\n      - /token/**\n      ignoreUserUrls:\n      - /oauth/**\n      - /css/**', 'c367dd314d677293905d0fddeb5fd745', '2023-04-07 11:30:49', '2023-04-07 11:30:49', NULL, '172.88.0.1', '', 'shareducks', NULL, NULL, NULL, 'yaml', NULL, '');
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`, `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`, `encrypted_data_key`) VALUES (97, 'ingot-client-gateway.yml', 'DEFAULT_GROUP', 'spring:\n  cloud:\n    gateway:\n      locator:\n        enabled: true\n      routes:\n      # =====================================\n      - id: ingot-client-ac\n        uri: lb://ingot-client-ac\n        predicates:\n        - Path=/ac/**\n        filters:\n        - StripPrefix=1\n        - PasswordDecoderFilter\n      - id: ingot-client-pms\n        uri: lb://ingot-client-pms\n        predicates:\n        - Path=/pms/**\n        filters:\n        - StripPrefix=1\n        - PasswordDecoderFilter\n        #限流过滤器\n        - name: RequestRateLimiter\n          args:\n            key-resolver: \'#{@remoteAddrKeyResolver}\'\n            # 每秒最大访问次数（放令牌桶的速率）\n            redis-rate-limiter.replenishRate: 10\n            # 令牌桶最大容量（令牌桶的大小）\n            redis-rate-limiter.burstCapacity: 20\n      - id: ingot-client-tmc\n        uri: lb://ingot-client-tmc\n        predicates:\n        - Path=/tmc/**\n        filters:\n        - StripPrefix=1\n      - id: merchant-service\n        uri: lb://merchant-service\n        predicates:\n        - Path=/merchant/**\n        filters:\n        - StripPrefix=1\n        - PasswordDecoderFilter\n      - id: ingot-monitor\n        uri: lb://ingot-monitor\n        predicates:\n        - Path=/monitor/**\n        filters:\n        - StripPrefix=1\ningot:\n  security:\n    encryption:\n      secretKey: ingotingotingot1\n      params:\n      - password\n      - pass\n      - oldPass\n      - newPass\n      - pwd\n      - payPass\n      urls:\n      - /auth/login\n  validateCode:\n    sms:\n      length: 6\n      expireIn: 300\n      mobileMaxSendCount: 10\n      ipMaxSendCount: 10\n      totalMaxSendCount: 50\n      url: /ac/auth/mobile,/ac/auth/modifyPasswordByMobile\n    image:\n      model: ip\n      urlModel:\n        auth-admin-login: ip\n      ipModelDurationTime: 3600\n      ipModelIntervalTime: 5\n      ipModelThreshold: 3\n      length: 4\n      expireIn: 60\n      url: /ac/auth/login\n    email:\n      expireIn: 86400  \n  job:\n    xxl:\n      admin:\n        addresses: http://${INGOT_TMC_HOST:ingot-task-manager-center}:8060\n      executor:\n        appname: ${spring.application.name}\n        ip:\n        port: 9700\n        logpath: /data/ingot/logs/${spring.application.name}/jobhandler\n        logretentiondays: -1\n      accessToken: IngotTmcAccessToken\n \n', '204ad8b79b8e980a90c0285e2965992e', '2023-04-07 11:30:49', '2023-04-07 11:30:49', NULL, '172.88.0.1', '', 'shareducks', NULL, NULL, NULL, 'yaml', NULL, '');
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`, `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`, `encrypted_data_key`) VALUES (98, 'ingot-client-pms.yml', 'DEFAULT_GROUP', 'spring:\n  mvc:\n    date-format: yyyy-MM-dd HH:mm:ss\n  jackson:\n    joda-date-time-format: yyyy-MM-dd HH:mm:ss\n    date-format: yyyy-MM-dd HH:mm:ss\n    time-zone: GMT+8\n    default-property-inclusion: non_null\n  datasource:\n    url: jdbc:mysql://${MYSQL_HOST:ingot-db-mysql}:${MYSQL_PORT:3306}/rebate_ingot_pms_db?useUnicode=true&characterEncoding=UTF8&useSSL=false&serverTimezone=GMT%2B8\n\nsecurity:\n  oauth2:\n    client:\n      client-id: ${spring.application.name}\n      client-secret: V3T3545EGUDjNFbmON6P+Jd4PxH/MwyCt62NZj8Yx/s=\n    resource:\n      id: ingot-resource-pms\n      loadBalanced: true\n      prefer-token-info: false\n      jwt:\n        key-uri: http://ingot-client-ac/oauth/token_key\n\ningot:\n  id:\n    redis:\n      enable: true\n  minio:\n    url: http://ingot-cloud:7900\n    accessKey: ingotingot\n    secretKey: ingotingot\n  swagger:\n    title: ${spring.application.name}\n    description: 权限管理系统\n  mq:\n    producerExchange: ingot.uc\n  job:\n    xxl:\n      admin:\n        addresses: http://${INGOT_TMC_HOST:ingot-task-manager-center}:8060\n      executor:\n        appname: ${spring.application.name}\n        ip:\n        port: 9800\n        logpath: /data/ingot/logs/${spring.application.name}/jobhandler\n        logretentiondays: -1\n      accessToken: IngotTmcAccessToken\n', '30bb4d8e6af7eeaa632f5efdc4611e1b', '2023-04-07 11:30:49', '2023-04-07 11:30:49', NULL, '172.88.0.1', '', 'shareducks', NULL, NULL, NULL, 'yaml', NULL, '');
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`, `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`, `encrypted_data_key`) VALUES (99, 'ingot-client-tmc.yml', 'DEFAULT_GROUP', 'spring:\n  jackson:\n    joda-date-time-format: yyyy-MM-dd HH:mm:ss\n    date-format: yyyy-MM-dd HH:mm:ss\n    time-zone: GMT+8\n    default-property-inclusion: non_null\n  mvc:\n    date-format: yyyy-MM-dd HH:mm:ss\n    static-path-pattern: /static/**\n  resources:\n    static-locations: classpath:/static/\n  freemarker:\n    template-loader-path: classpath:/templates/\n    suffix: .ftl\n    charset: UTF-8\n    request-context-attribute: request\n    settings:\n      number_format: 0.##########\n  datasource:\n    url: jdbc:mysql://${MYSQL_HOST:ingot-db-mysql}:${MYSQL_PORT:3306}/rebate_ingot_tmc_db?useUnicode=true&characterEncoding=UTF8&useSSL=false&serverTimezone=GMT%2B8\n\nmybatis:\n  mapper-locations: classpath*:mapper/*.xml,classpath*:sdk/mapper/*.xml,classpath:/mybatis-mapper/*Mapper.xml\n\nsecurity:\n  oauth2:\n    client:\n      client-id: ${spring.application.name}\n      client-secret: yPTYMVkD1a+zvuusi1rV82Ui2bPQ7T0Lu39mhAsVhvA=\n    resource:\n      id: ingot-resource-tmc\n      loadBalanced: true\n      prefer-token-info: false\n      jwt:\n        key-uri: http://ingot-client-ac/oauth/token_key\n\ningot:\n  mq:\n    producerExchange: ingot.tmc\n', '7dfd33417409aa42d56b5c24c8104d2e', '2023-04-07 11:30:49', '2023-04-07 11:30:49', NULL, '172.88.0.1', '', 'shareducks', NULL, NULL, NULL, 'yaml', NULL, '');
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`, `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`, `encrypted_data_key`) VALUES (100, 'ingot-monitor.yml', 'DEFAULT_GROUP', 'spring:\n  cloud:\n    bus:\n      enabled: false\n  # 安全配置\n  security:\n    user:\n      name: admin\n      password: \'{noop}admin\'\n  boot:\n    admin:\n      ui:\n        resource-locations: \'classpath:/ui/,classpath:/static/\'\n        template-location: \'classpath:/ui/\'\n        title: \'Ingot 服务状态监控\'\n        brand: \'Ingot 服务状态监控\'\n\nsecurity:\n  oauth2:\n    client:\n      client-id: ${spring.application.name}\n      client-secret: FaRgB5EBFv7fTchC60GiM1ozOsbWxEScHy2+B+r1aKc=\n    resource:\n      id: ingot-resource-monitor', '6fcf4f38540dda633fcd05057409ccfa', '2023-04-07 11:30:49', '2023-04-07 11:30:49', NULL, '172.88.0.1', '', 'shareducks', NULL, NULL, NULL, 'yaml', NULL, '');
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`, `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`, `encrypted_data_key`) VALUES (101, 'merchant-service.yml', 'DEFAULT_GROUP', 'spring:\n  mvc:\n    date-format: yyyy-MM-dd HH:mm:ss\n  jackson:\n    joda-date-time-format: yyyy-MM-dd HH:mm:ss\n    date-format: yyyy-MM-dd HH:mm:ss\n    time-zone: GMT+8\n    default-property-inclusion: non_null\n  datasource:\n    url: jdbc:mysql://${MYSQL_HOST:ingot-db-mysql}:${MYSQL_PORT:3306}/rebate_db?useUnicode=true&characterEncoding=UTF8&useSSL=false&serverTimezone=GMT%2B8\n\nsecurity:\n  oauth2:\n    client:\n      client-id: ${spring.application.name}\n      client-secret: Ci/Djc19eqtGBqZYdaSAY1OhQNdLZNsRyJtuAECIzpE=\n    resource:\n      id: merchant-resource\n      loadBalanced: true\n      prefer-token-info: false\n      jwt:\n        key-uri: http://ingot-client-ac/oauth/token_key\n\ningot:\n  id:\n    redis:\n      enable: true\n  minio:\n    url: http://47.92.251.8:7900\n    accessKey: rebate\n    secretKey: wc.1331588182\n  job:\n    xxl:\n      admin:\n        addresses: http://${INGOT_TMC_HOST:ingot-task-manager-center}:8060\n      executor:\n        appname: ${spring.application.name}\n        ip:\n        port: 9880\n        logpath: /data/ingot/logs/${spring.application.name}/jobhandler\n        logretentiondays: -1\n      accessToken: IngotTmcAccessToken', '3d09d63908989e07ddb526d0e1322ca0', '2023-04-07 11:30:49', '2023-04-07 11:30:49', NULL, '172.88.0.1', '', 'shareducks', NULL, NULL, NULL, 'yaml', NULL, '');
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`, `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`, `encrypted_data_key`) VALUES (109, 'application-common.yml', 'DEFAULT_GROUP', 'jasypt:\n  encryptor:\n    password: ingot\n\nserver:\n  tomcat:\n    uri-encoding: UTF-8\n    remote-ip-header: x-forwarded-for\n  use-forward-headers: true\n\nmanagement:\n  endpoints:\n    web:\n      exposure:\n        include: \'*\'\n\nspring:\n  servlet:\n    multipart:\n      max-file-size: 100MB\n      max-request-size: 100MB\n  jackson:\n    date-format: yyyy-MM-dd HH:mm:ss\n    time-zone: GMT+8\n    default-property-inclusion: non_null\n  data:\n    redis:\n      database: 2\n      host: ${REDIS_HOST:ingot-db-redis}\n      port: ${REDIS_PORT:6379}\n      jedis:\n        pool:\n          max-active: 20\n  cloud:\n    sentinel:\n      eager: true\n      log:\n        dir: /ingot-data/sentinel/\n        switch-pid: true\n    bus:\n      id: ${spring.application.name}:${spring.cloud.client.ip-address}:${server.port}\n\nfeign:\n  sentinel:\n    enabled: true\n  okhttp:\n    enabled: true\n  httpclient:\n    enabled: false\n  client:\n    config:\n      default:\n        connectTimeout: 10000\n        readTimeout: 10000\n  compression:\n    request:\n      enabled: true\n    response:\n      enabled: true\n\nswagger:\n  title: Ingot Cloud Swagger API\n  license: Powered By Ingot\n  licenseUrl: https://secingot.com/\n  terms-of-service-url: https://secingot.com/\n  contact:\n    email: magician.of.technique@aliyun.com\n    url: https://wangchao.im\n  authorization:\n    name: Ingot OAuth\n    auth-regex: ^.*$\n    authorization-scope-list:\n      - scope: server\n        description: server all\n    token-url-list:\n      - http://${GATEWAY-HOST:ingot-gateway}:${GATEWAY-PORT:7980}/ac/oauth/token\n', 'b38a6b6b3df6a8f60a13624bad88f823', '2023-09-26 09:17:38', '2023-10-18 14:10:56', 'nacos', '172.88.0.1', '', 'tatashe', '', '', '', 'yaml', '', '');
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`, `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`, `encrypted_data_key`) VALUES (110, 'application-mysql.yml', 'DEFAULT_GROUP', 'spring:\n  datasource:\n    driver-class-name: com.mysql.cj.jdbc.Driver\n    filters: stat,wall,log4j\n    type: com.alibaba.druid.pool.DruidDataSource\n    druid:\n      initial-size: 10\n      min-idle: 10\n      max-active: 30\n      test-on-borrow: true\n      time-between-eviction-runs-millis: 60000\n      min-evictable-idle-time-millis: 300000\n      web-stat-filter:\n        exclusions: \'*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*\'\n      stat-view-servlet:\n        allow:\n        login-username: admin\n        login-password: admin\n      connection-init-sqls:\n      - SET NAMES utf8mb4\n\nmybatis:\n  configuration:\n    map-underscore-to-camel-case: true\n\nmybatis-plus:\n  mapper-locations: classpath:/mapper/*Mapper.xml,classpath*:sdk/mapper/*.xml\n  configuration:\n    default-enum-type-handler: com.baomidou.mybatisplus.core.handlers.CompositeEnumTypeHandler\n  global-config:\n    banner: false\n    db-config:\n      #logic-delete-field: delete_at  # 全局逻辑删除的实体字段名(since 3.3.0,配置后可以忽略不配置步骤2)\n      logic-delete-value: now() # 逻辑已删除值(默认为 1)\n      logic-not-delete-value: \"null\" # 逻辑未删除值(默认为 0)\n\n', 'c9c386bf0d6133aedef947a167a40ce2', '2023-09-26 09:17:38', '2023-09-26 09:17:38', NULL, '172.88.0.1', '', 'tatashe', '', NULL, NULL, 'yaml', NULL, '');
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`, `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`, `encrypted_data_key`) VALUES (111, 'tatashe-service-pms.yml', 'DEFAULT_GROUP', 'spring:\n  datasource:\n    url: jdbc:mysql://${MYSQL_HOST:ingot-db-mysql}:${MYSQL_PORT:3306}/tatashe_core?useUnicode=true&characterEncoding=UTF8&useSSL=false&serverTimezone=GMT%2B8\n  security:\n    oauth2:\n      resourceserver:\n        jwt:\n          issuer-uri: http://ingot-auth-server:5100\n\ningot:\n  security:\n    ignoreTenantValidateRoleCodeList:\n      - role_admin\n  tenant:\n    tables:\n      - sys_authority\n      - sys_dept\n      - sys_menu\n      - sys_role\n      - sys_role_user\n      - sys_role_group\n      - sys_user_dept\n      - app_role\n      - app_role_user\n  crypto:\n    secretKeys:\n      aes: ingotingotingot1\n  mybatis:\n    showSqlLog: true\n  minio:\n    url: http://ingot-cloud:5000\n    publicUrl: http://ingot-cloud:5000\n    accessKey: wnkZeaug4yV6ztKJ\n    secretKey: 6H3vEHUtxMfoQ8c6K6qmEKlDZpJ1BOCh\n  swagger:\n    title: ${spring.application.name}\n    description: 权限管理系统\n  job:\n    xxl:\n      admin:\n        addresses: http://${INGOT_TMC_HOST:ingot-task-manager-center}:8060\n      executor:\n        appname: ${spring.application.name}\n        ip:\n        port: 9800\n        logpath: /data/ingot/logs/${spring.application.name}/jobhandler\n        logretentiondays: -1\n      accessToken: IngotTmcAccessToken\n', '912476ffaffc1bf7768902d0c5b6ed4e', '2023-09-26 09:17:38', '2023-09-26 10:00:14', 'nacos', '172.88.0.1', '', 'tatashe', '', '', '', 'yaml', '', '');
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`, `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`, `encrypted_data_key`) VALUES (112, 'tatashe-service-gateway.yml', 'DEFAULT_GROUP', 'spring:\n  cloud:\n    gateway:\n      locator:\n        enabled: true\n      routes:\n      - id: tatashe-service-auth\n        uri: lb://tatashe-service-auth\n        predicates:\n        - Path=/auth/**\n        filters:\n        - StripPrefix=1\n        - TokenPasswordDecoderFilter\n      - id: tatashe-service-pms\n        uri: lb://tatashe-service-pms\n        predicates:\n        - Path=/pms/**\n        filters:\n        - StripPrefix=1\n        #限流过滤器\n        - name: RequestRateLimiter\n          args:\n            key-resolver: \'#{@remoteAddrKeyResolver}\'\n            # 每秒最大访问次数（放令牌桶的速率）\n            redis-rate-limiter.replenishRate: 50\n            # 令牌桶最大容量（令牌桶的大小）\n            redis-rate-limiter.burstCapacity: 100\n \ningot:\n  crypto:\n    secretKeys:\n      aes: ingotingotingot1\n  vc:\n    verifyUrls:\n      - image,/auth/oauth2/token,POST\n      - image,/auth/oauth2/pre_authorize,POST', '5dce2f019680788e6d981d870e4acb85', '2023-09-26 09:17:38', '2023-09-26 09:20:24', 'nacos', '172.88.0.1', '', 'tatashe', '', '', '', 'yaml', '', '');
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`, `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`, `encrypted_data_key`) VALUES (113, 'tatashe-service-auth.yml', 'DEFAULT_GROUP', 'spring:\n  mvc:\n    static-path-pattern: /static/**\n  thymeleaf:\n    cache: false\n    prefix: classpath:/templates/\n    suffix: .html\n    encoding: UTF-8\n    servlet:\n      content-type: text/html\n  datasource:\n    url: jdbc:mysql://${MYSQL_HOST:ingot-db-mysql}:${MYSQL_PORT:3306}/tatashe_core?useUnicode=true&characterEncoding=UTF8&useSSL=false&serverTimezone=GMT%2B8\n\ningot:\n  security:\n    oauth2:\n      auth:\n        loginFormUrl: https://login.ingotcloud.top/oauth2/challenge\n        issuer: http://ingot-auth-server:5100\n      resource:\n        publicUrls:\n          - /favicon.ico,GET\n          - /static/**,GET\n          - /logout,*\n  swagger:\n    title: ${spring.application.name}\n    description: 鉴权中心\n  job:\n    xxl:\n      admin:\n        addresses: http://${INGOT_TMC_HOST:ingot-task-manager-center}:8060\n      executor:\n        appname: ${spring.application.name}\n        ip:\n        port: 9900\n        logpath: /data/ingot/logs/${spring.application.name}/jobhandler\n        logretentiondays: -1\n      accessToken: IngotTmcAccessToken', '348a55d2a96e83de22ef17f3f0d1653c', '2023-09-26 09:17:38', '2023-09-26 09:18:03', 'nacos', '172.88.0.1', '', 'tatashe', '', '', '', 'yaml', '', '');
COMMIT;

-- ----------------------------
-- Table structure for config_info_aggr
-- ----------------------------
DROP TABLE IF EXISTS `config_info_aggr`;
CREATE TABLE `config_info_aggr` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `data_id` varchar(255) COLLATE utf8_bin NOT NULL COMMENT 'data_id',
  `group_id` varchar(255) COLLATE utf8_bin NOT NULL COMMENT 'group_id',
  `datum_id` varchar(255) COLLATE utf8_bin NOT NULL COMMENT 'datum_id',
  `content` longtext COLLATE utf8_bin NOT NULL COMMENT '内容',
  `gmt_modified` datetime NOT NULL COMMENT '修改时间',
  `app_name` varchar(128) COLLATE utf8_bin DEFAULT NULL,
  `tenant_id` varchar(128) COLLATE utf8_bin DEFAULT '' COMMENT '租户字段',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_configinfoaggr_datagrouptenantdatum` (`data_id`,`group_id`,`tenant_id`,`datum_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='增加租户字段';

-- ----------------------------
-- Records of config_info_aggr
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for config_info_beta
-- ----------------------------
DROP TABLE IF EXISTS `config_info_beta`;
CREATE TABLE `config_info_beta` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `data_id` varchar(255) COLLATE utf8_bin NOT NULL COMMENT 'data_id',
  `group_id` varchar(128) COLLATE utf8_bin NOT NULL COMMENT 'group_id',
  `app_name` varchar(128) COLLATE utf8_bin DEFAULT NULL COMMENT 'app_name',
  `content` longtext COLLATE utf8_bin NOT NULL COMMENT 'content',
  `beta_ips` varchar(1024) COLLATE utf8_bin DEFAULT NULL COMMENT 'betaIps',
  `md5` varchar(32) COLLATE utf8_bin DEFAULT NULL COMMENT 'md5',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `src_user` text COLLATE utf8_bin COMMENT 'source user',
  `src_ip` varchar(50) COLLATE utf8_bin DEFAULT NULL COMMENT 'source ip',
  `tenant_id` varchar(128) COLLATE utf8_bin DEFAULT '' COMMENT '租户字段',
  `encrypted_data_key` text COLLATE utf8_bin NOT NULL COMMENT '秘钥',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_configinfobeta_datagrouptenant` (`data_id`,`group_id`,`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='config_info_beta';

-- ----------------------------
-- Records of config_info_beta
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for config_info_tag
-- ----------------------------
DROP TABLE IF EXISTS `config_info_tag`;
CREATE TABLE `config_info_tag` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `data_id` varchar(255) COLLATE utf8_bin NOT NULL COMMENT 'data_id',
  `group_id` varchar(128) COLLATE utf8_bin NOT NULL COMMENT 'group_id',
  `tenant_id` varchar(128) COLLATE utf8_bin DEFAULT '' COMMENT 'tenant_id',
  `tag_id` varchar(128) COLLATE utf8_bin NOT NULL COMMENT 'tag_id',
  `app_name` varchar(128) COLLATE utf8_bin DEFAULT NULL COMMENT 'app_name',
  `content` longtext COLLATE utf8_bin NOT NULL COMMENT 'content',
  `md5` varchar(32) COLLATE utf8_bin DEFAULT NULL COMMENT 'md5',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `src_user` text COLLATE utf8_bin COMMENT 'source user',
  `src_ip` varchar(50) COLLATE utf8_bin DEFAULT NULL COMMENT 'source ip',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_configinfotag_datagrouptenanttag` (`data_id`,`group_id`,`tenant_id`,`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='config_info_tag';

-- ----------------------------
-- Records of config_info_tag
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for config_tags_relation
-- ----------------------------
DROP TABLE IF EXISTS `config_tags_relation`;
CREATE TABLE `config_tags_relation` (
  `id` bigint(20) NOT NULL COMMENT 'id',
  `tag_name` varchar(128) COLLATE utf8_bin NOT NULL COMMENT 'tag_name',
  `tag_type` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT 'tag_type',
  `data_id` varchar(255) COLLATE utf8_bin NOT NULL COMMENT 'data_id',
  `group_id` varchar(128) COLLATE utf8_bin NOT NULL COMMENT 'group_id',
  `tenant_id` varchar(128) COLLATE utf8_bin DEFAULT '' COMMENT 'tenant_id',
  `nid` bigint(20) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`nid`),
  UNIQUE KEY `uk_configtagrelation_configidtag` (`id`,`tag_name`,`tag_type`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='config_tag_relation';

-- ----------------------------
-- Records of config_tags_relation
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for group_capacity
-- ----------------------------
DROP TABLE IF EXISTS `group_capacity`;
CREATE TABLE `group_capacity` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_id` varchar(128) COLLATE utf8_bin NOT NULL DEFAULT '' COMMENT 'Group ID，空字符表示整个集群',
  `quota` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '配额，0表示使用默认值',
  `usage` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '使用量',
  `max_size` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '单个配置大小上限，单位为字节，0表示使用默认值',
  `max_aggr_count` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '聚合子配置最大个数，，0表示使用默认值',
  `max_aggr_size` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '单个聚合数据的子配置大小上限，单位为字节，0表示使用默认值',
  `max_history_count` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '最大变更历史数量',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_id` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='集群、各Group容量信息表';

-- ----------------------------
-- Records of group_capacity
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for his_config_info
-- ----------------------------
DROP TABLE IF EXISTS `his_config_info`;
CREATE TABLE `his_config_info` (
  `id` bigint(20) unsigned NOT NULL,
  `nid` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `data_id` varchar(255) COLLATE utf8_bin NOT NULL,
  `group_id` varchar(128) COLLATE utf8_bin NOT NULL,
  `app_name` varchar(128) COLLATE utf8_bin DEFAULT NULL COMMENT 'app_name',
  `content` longtext COLLATE utf8_bin NOT NULL,
  `md5` varchar(32) COLLATE utf8_bin DEFAULT NULL,
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `src_user` text COLLATE utf8_bin,
  `src_ip` varchar(50) COLLATE utf8_bin DEFAULT NULL,
  `op_type` char(10) COLLATE utf8_bin DEFAULT NULL,
  `tenant_id` varchar(128) COLLATE utf8_bin DEFAULT '' COMMENT '租户字段',
  `encrypted_data_key` text COLLATE utf8_bin NOT NULL COMMENT '秘钥',
  PRIMARY KEY (`nid`),
  KEY `idx_gmt_create` (`gmt_create`),
  KEY `idx_gmt_modified` (`gmt_modified`),
  KEY `idx_did` (`data_id`)
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='多租户改造';

-- ----------------------------
-- Records of his_config_info
-- ----------------------------
BEGIN;
INSERT INTO `his_config_info` (`id`, `nid`, `data_id`, `group_id`, `app_name`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`, `src_ip`, `op_type`, `tenant_id`, `encrypted_data_key`) VALUES (109, 34, 'application-common.yml', 'DEFAULT_GROUP', '', 'jasypt:\n  encryptor:\n    password: ingot\n\nserver:\n  tomcat:\n    uri-encoding: UTF-8\n    remote-ip-header: x-forwarded-for\n  use-forward-headers: true\n\nmanagement:\n  endpoints:\n    web:\n      exposure:\n        include: \'*\'\n\nspring:\n  servlet:\n    multipart:\n      max-file-size: 100MB\n      max-request-size: 100MB\n  jackson:\n    date-format: yyyy-MM-dd HH:mm:ss\n    time-zone: GMT+8\n    default-property-inclusion: non_null\n  data:\n    redis:\n      database: 0\n      host: ${REDIS_HOST:ingot-db-redis}\n      port: ${REDIS_PORT:6379}\n      jedis:\n        pool:\n          max-active: 20\n  cloud:\n    sentinel:\n      eager: true\n      log:\n        dir: /ingot-data/sentinel/\n        switch-pid: true\n    bus:\n      id: ${spring.application.name}:${spring.cloud.client.ip-address}:${server.port}\n\nfeign:\n  sentinel:\n    enabled: true\n  okhttp:\n    enabled: true\n  httpclient:\n    enabled: false\n  client:\n    config:\n      default:\n        connectTimeout: 10000\n        readTimeout: 10000\n  compression:\n    request:\n      enabled: true\n    response:\n      enabled: true\n\nswagger:\n  title: Ingot Cloud Swagger API\n  license: Powered By Ingot\n  licenseUrl: https://secingot.com/\n  terms-of-service-url: https://secingot.com/\n  contact:\n    email: magician.of.technique@aliyun.com\n    url: https://wangchao.im\n  authorization:\n    name: Ingot OAuth\n    auth-regex: ^.*$\n    authorization-scope-list:\n      - scope: server\n        description: server all\n    token-url-list:\n      - http://${GATEWAY-HOST:ingot-gateway}:${GATEWAY-PORT:7980}/ac/oauth/token\n', '2a7668e656cf3bae09d2b2635d0e7154', '2023-10-18 06:10:56', '2023-10-18 14:10:56', 'nacos', '172.88.0.1', 'U', 'tatashe', '');
COMMIT;

-- ----------------------------
-- Table structure for permissions
-- ----------------------------
DROP TABLE IF EXISTS `permissions`;
CREATE TABLE `permissions` (
  `role` varchar(50) NOT NULL,
  `resource` varchar(255) NOT NULL,
  `action` varchar(8) NOT NULL,
  UNIQUE KEY `uk_role_permission` (`role`,`resource`,`action`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of permissions
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for roles
-- ----------------------------
DROP TABLE IF EXISTS `roles`;
CREATE TABLE `roles` (
  `username` varchar(50) NOT NULL,
  `role` varchar(50) NOT NULL,
  UNIQUE KEY `idx_user_role` (`username`,`role`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of roles
-- ----------------------------
BEGIN;
INSERT INTO `roles` (`username`, `role`) VALUES ('nacos', 'ROLE_ADMIN');
COMMIT;

-- ----------------------------
-- Table structure for tenant_capacity
-- ----------------------------
DROP TABLE IF EXISTS `tenant_capacity`;
CREATE TABLE `tenant_capacity` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(128) COLLATE utf8_bin NOT NULL DEFAULT '' COMMENT 'Tenant ID',
  `quota` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '配额，0表示使用默认值',
  `usage` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '使用量',
  `max_size` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '单个配置大小上限，单位为字节，0表示使用默认值',
  `max_aggr_count` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '聚合子配置最大个数',
  `max_aggr_size` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '单个聚合数据的子配置大小上限，单位为字节，0表示使用默认值',
  `max_history_count` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '最大变更历史数量',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='租户容量信息表';

-- ----------------------------
-- Records of tenant_capacity
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for tenant_info
-- ----------------------------
DROP TABLE IF EXISTS `tenant_info`;
CREATE TABLE `tenant_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `kp` varchar(128) COLLATE utf8_bin NOT NULL COMMENT 'kp',
  `tenant_id` varchar(128) COLLATE utf8_bin DEFAULT '' COMMENT 'tenant_id',
  `tenant_name` varchar(128) COLLATE utf8_bin DEFAULT '' COMMENT 'tenant_name',
  `tenant_desc` varchar(256) COLLATE utf8_bin DEFAULT NULL COMMENT 'tenant_desc',
  `create_source` varchar(32) COLLATE utf8_bin DEFAULT NULL COMMENT 'create_source',
  `gmt_create` bigint(20) NOT NULL COMMENT '创建时间',
  `gmt_modified` bigint(20) NOT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_info_kptenantid` (`kp`,`tenant_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='tenant_info';

-- ----------------------------
-- Records of tenant_info
-- ----------------------------
BEGIN;
INSERT INTO `tenant_info` (`id`, `kp`, `tenant_id`, `tenant_name`, `tenant_desc`, `create_source`, `gmt_create`, `gmt_modified`) VALUES (1, '1', 'ingot', 'ingot', 'ingot', 'nacos', 1672366966203, 1672366966203);
INSERT INTO `tenant_info` (`id`, `kp`, `tenant_id`, `tenant_name`, `tenant_desc`, `create_source`, `gmt_create`, `gmt_modified`) VALUES (6, '1', 'hongya', 'hongya', 'hongya', 'nacos', 1679017720451, 1679017720451);
INSERT INTO `tenant_info` (`id`, `kp`, `tenant_id`, `tenant_name`, `tenant_desc`, `create_source`, `gmt_create`, `gmt_modified`) VALUES (7, '1', 'tonykancai', 'tonykancai', 'tonykancai', 'nacos', 1679017727825, 1679017727825);
INSERT INTO `tenant_info` (`id`, `kp`, `tenant_id`, `tenant_name`, `tenant_desc`, `create_source`, `gmt_create`, `gmt_modified`) VALUES (8, '1', 'mstx', 'mstx', 'mstx', 'nacos', 1679017735876, 1679017735876);
INSERT INTO `tenant_info` (`id`, `kp`, `tenant_id`, `tenant_name`, `tenant_desc`, `create_source`, `gmt_create`, `gmt_modified`) VALUES (9, '1', 'shareducks', 'shareducks', 'shareducks', 'nacos', 1680838241276, 1680838241276);
INSERT INTO `tenant_info` (`id`, `kp`, `tenant_id`, `tenant_name`, `tenant_desc`, `create_source`, `gmt_create`, `gmt_modified`) VALUES (10, '1', 'tatashe', 'tatashe', 'tatashe', 'nacos', 1695690119622, 1695690119622);
COMMIT;

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `username` varchar(50) NOT NULL,
  `password` varchar(500) NOT NULL,
  `enabled` tinyint(1) NOT NULL,
  PRIMARY KEY (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of users
-- ----------------------------
BEGIN;
INSERT INTO `users` (`username`, `password`, `enabled`) VALUES ('nacos', '$2a$10$EuWPZHzz32dJN7jexM34MOeYirDdFAZm2kuWj7VEOJhhZkDrxfvUu', 1);
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
