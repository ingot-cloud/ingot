/*
 Navicat Premium Data Transfer

 Source Server         : mysql-localhost
 Source Server Type    : MySQL
 Source Server Version : 50735
 Source Host           : localhost:3306
 Source Schema         : ingot_core

 Target Server Type    : MySQL
 Target Server Version : 50735
 File Encoding         : 65001

 Date: 07/04/2023 09:03:43
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for biz_leaf_alloc
-- ----------------------------
DROP TABLE IF EXISTS `biz_leaf_alloc`;
CREATE TABLE `biz_leaf_alloc` (
  `biz_tag` varchar(128) NOT NULL DEFAULT '',
  `max_id` bigint(20) NOT NULL DEFAULT '1',
  `step` int(11) NOT NULL,
  `description` varchar(256) DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`biz_tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for oauth2_authorization
-- ----------------------------
DROP TABLE IF EXISTS `oauth2_authorization`;
CREATE TABLE `oauth2_authorization` (
  `id` varchar(100) NOT NULL,
  `registered_client_id` varchar(100) NOT NULL,
  `principal_name` varchar(200) NOT NULL,
  `authorization_grant_type` varchar(100) NOT NULL,
  `authorized_scopes` varchar(1000) DEFAULT NULL,
  `attributes` varchar(4000) DEFAULT NULL,
  `state` varchar(500) DEFAULT NULL,
  `authorization_code_value` blob,
  `authorization_code_issued_at` datetime DEFAULT NULL,
  `authorization_code_expires_at` datetime DEFAULT NULL,
  `authorization_code_metadata` varchar(2000) DEFAULT NULL,
  `access_token_value` blob,
  `access_token_issued_at` datetime DEFAULT NULL,
  `access_token_expires_at` datetime DEFAULT NULL,
  `access_token_metadata` varchar(2000) DEFAULT NULL,
  `access_token_type` varchar(100) DEFAULT NULL,
  `access_token_scopes` varchar(1000) DEFAULT NULL,
  `oidc_id_token_value` blob,
  `oidc_id_token_issued_at` datetime DEFAULT NULL,
  `oidc_id_token_expires_at` datetime DEFAULT NULL,
  `oidc_id_token_metadata` varchar(2000) DEFAULT NULL,
  `refresh_token_value` blob,
  `refresh_token_issued_at` datetime DEFAULT NULL,
  `refresh_token_expires_at` datetime DEFAULT NULL,
  `refresh_token_metadata` varchar(2000) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of oauth2_authorization
-- ----------------------------
BEGIN;
INSERT INTO `oauth2_authorization` VALUES ('0360cd9f-f02d-4e33-9fd4-a73599694de3', 'ingot', 'admin', 'password', 'message.read,message.write', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"java.security.Principal\":{\"@class\":\"com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationToken\",\"authorities\":[\"java.util.Collections$UnmodifiableRandomAccessList\",[{\"@class\":\"com.ingot.framework.security.core.authority.ClientGrantedAuthority\",\"authority\":\"CLIENT_CLIENT_CLIENT_CLIENT_ingot\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"basic\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"role_admin\"}]],\"details\":null,\"authenticated\":true,\"principal\":{\"@class\":\"com.ingot.framework.security.core.userdetails.IngotUser\",\"username\":\"admin\",\"authorities\":[\"java.util.Collections$UnmodifiableSet\",[{\"@class\":\"com.ingot.framework.security.core.authority.ClientGrantedAuthority\",\"authority\":\"CLIENT_CLIENT_CLIENT_CLIENT_ingot\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"basic\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"role_admin\"}]],\"id\":1,\"deptId\":1,\"tenantId\":1,\"clientId\":\"ingot\",\"tokenAuthType\":\"0\"},\"credentials\":null,\"name\":\"admin\"}}', NULL, NULL, NULL, NULL, NULL, 0x65794A72615751694F6949784D6A41314E325A694E53316A4D5745334C54517A596A497459546C6C5A43316A4D444D335A6A45354D574D7A595459694C434A68624763694F694A53557A49314E694A392E65794A7A645749694F694A685A47317062694973496D46315A434936496D6C755A32393049697769626D4A6D496A6F784E6A63324F4441794E6A6B314C434A30595851694F6949774969776963324E76634755694F6C7369636D39735A5639685A47317062694973496D316C63334E685A325575636D56685A434973496D4A6863326C6A496977696257567A6332466E5A533533636D6C305A534A644C434A7063334D694F694A6F644852774F6938766157356E62335174595856306143317A5A584A325A5849364E5445774D434973496D6B694F6A4573496D526C634851694F6A4573496D5634634349364D5459334E6A67774F5467354E53776961574630496A6F784E6A63324F4441794E6A6B314C434A305A573568626E51694F6A46392E4978792D68446C4666416747676776757A653061474950384B6B6E7332793471706E4E5563392D784366484F664D2D3468445279477954324B304B434A564C5F5F347A73365562664733736448415473537A49416339494F66336A77455056633430674D6F65543176756668576331686742784749617572597351316A68667564536B716E4D596A394B792D45456A645F765030564C7143565235754C483375524C534D305152366965446D394F346569556A46444F5747473564734A706C6C6D526869714F79304B57345957365468737762554B67455F525944755F31546357673161355F5A377446507A2D3144324C465A6C43445848526636723476665A424A675A6F375842705772726974474D7A386B5751366C464B7039744E4E344C7130312D6F695A61623976424F2D6945645A57386A774B707361454A69346E785543543676685449355A705A7976476379335F4B7A41, '2023-02-19 18:31:35', '2023-02-19 20:31:35', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"metadata.token.claims\":{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"sub\":\"admin\",\"aud\":[\"java.util.Collections$SingletonList\",[\"ingot\"]],\"nbf\":[\"java.time.Instant\",1676802695.139000000],\"tat\":\"0\",\"scope\":[\"java.util.HashSet\",[\"role_admin\",\"message.read\",\"basic\",\"message.write\"]],\"iss\":[\"java.net.URL\",\"http://ingot-auth-server:5100\"],\"i\":[\"java.lang.Long\",1],\"dept\":[\"java.lang.Long\",1],\"exp\":[\"java.time.Instant\",1676809895.139000000],\"iat\":[\"java.time.Instant\",1676802695.139000000],\"tenant\":[\"java.lang.Long\",1]},\"metadata.token.invalidated\":false}', 'Bearer', 'message.read,message.write', NULL, NULL, NULL, NULL, 0x36727733726E6F7943447070696C484831597263775661795A6459624F67735370566A514F49734B66666E64757A71334C65785A386653796C30414D447464736F6C595778544D6D4C76336D4C794F7752464468444E4870346E72353857546A43306D46585066495A467370453442774A347A75595572774564747530496E74, '2023-02-19 18:31:35', '2023-02-26 18:31:35', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"metadata.token.invalidated\":false}');
INSERT INTO `oauth2_authorization` VALUES ('29cbe0e2-426f-4fb7-8624-9757b2862d37', 'ingot', 'admin', 'password', 'message.read,message.write', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"java.security.Principal\":{\"@class\":\"com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationToken\",\"authorities\":[\"java.util.Collections$UnmodifiableRandomAccessList\",[{\"@class\":\"com.ingot.framework.security.core.authority.ClientGrantedAuthority\",\"authority\":\"CLIENT_ingot\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"basic\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"role_admin\"}]],\"details\":null,\"authenticated\":true,\"principal\":{\"@class\":\"com.ingot.framework.security.core.userdetails.IngotUser\",\"username\":\"admin\",\"authorities\":[\"java.util.Collections$UnmodifiableSet\",[{\"@class\":\"com.ingot.framework.security.core.authority.ClientGrantedAuthority\",\"authority\":\"CLIENT_ingot\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"basic\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"role_admin\"}]],\"id\":1,\"deptId\":1,\"tenantId\":1,\"clientId\":\"ingot\",\"tokenAuthType\":\"0\"},\"credentials\":null,\"name\":\"admin\"}}', NULL, NULL, NULL, NULL, NULL, 0x65794A72615751694F694A6B4E5759794E446B325A4331685A574E694C54526D4D7A4D744F57497A4D5331684D7A49355A5746694D54426D5A6A59694C434A68624763694F694A53557A49314E694A392E65794A7A645749694F694A685A47317062694973496D46315A434936496D6C755A32393049697769626D4A6D496A6F784E6A63794D7A67354E7A59334C434A30595851694F6949774969776963324E76634755694F6C7369636D39735A5639685A47317062694973496D316C63334E685A325575636D56685A434973496D4A6863326C6A496977696257567A6332466E5A533533636D6C305A534A644C434A7063334D694F694A6F644852774F6938766157356E62335174595856306143317A5A584A325A5849364E5445774D434973496D6B694F6A4573496D526C634851694F6A4573496D5634634349364D5459334D6A4D354E6A6B324E79776961574630496A6F784E6A63794D7A67354E7A59334C434A305A573568626E51694F6A46392E4A4E48307A6C4B4871736C4B50474C6868684B706577574638474C6542775A313936426371693134392D556B4457306F4254764A794F71526F49552D6F2D30566D70344466526955783044584D464B726D4376474B766934313678796747764353584B457644306F2D6D50714C686C52575039384E744167414D667331716267364D623168316B45325F32585775665F674D4D742D39326F714D43373453633754766552444D4B423451784174704A637130416A4246594733315A72507A475F67396478614D50536854562D6C7748796F3059685F6B5479317561416B7A56384E666F334462494B576E315543415A7778664456504467316972497268627161567A554F6C4331656D3857496E463634396D5056385636396564686847546A52785365466F6143416149634232686E766A445F584959665F316364726E5837304D4969554432724F5A6F386C73396534485472304977, '2022-12-30 16:42:48', '2022-12-30 18:42:48', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"metadata.token.claims\":{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"sub\":\"admin\",\"aud\":[\"java.util.Collections$SingletonList\",[\"ingot\"]],\"nbf\":[\"java.time.Instant\",1672389767.897000000],\"tat\":\"0\",\"scope\":[\"java.util.HashSet\",[\"role_admin\",\"message.read\",\"basic\",\"message.write\"]],\"iss\":[\"java.net.URL\",\"http://ingot-auth-server:5100\"],\"i\":[\"java.lang.Long\",1],\"dept\":[\"java.lang.Long\",1],\"exp\":[\"java.time.Instant\",1672396967.897000000],\"iat\":[\"java.time.Instant\",1672389767.897000000],\"tenant\":[\"java.lang.Long\",1]},\"metadata.token.invalidated\":false}', 'Bearer', 'message.read,message.write', NULL, NULL, NULL, NULL, 0x6454664245746E6952797458313376512D56374C52756D75497A366F6462426175695355463944547065645A685A6C52432D64787645694B61557272422D4E3743503879785534524B7248504D687936734E767336695F6A5546516D6E4665615A4846724176575A55382D6F4B4C6D37534B584E4C5F50615730323044663877, '2022-12-30 16:42:48', '2023-01-06 16:42:48', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"metadata.token.invalidated\":false}');
INSERT INTO `oauth2_authorization` VALUES ('4210bd94-cf94-4885-b2a9-7dd7b9fe8a2c', 'ingot', 'admin', 'password', 'message.read,message.write', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"java.security.Principal\":{\"@class\":\"com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationToken\",\"authorities\":[\"java.util.Collections$UnmodifiableRandomAccessList\",[{\"@class\":\"com.ingot.framework.security.core.authority.ClientGrantedAuthority\",\"authority\":\"CLIENT_CLIENT_CLIENT_CLIENT_CLIENT_CLIENT_CLIENT_CLIENT_CLIENT_CLIENT_CLIENT_ingot\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"basic\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"role_admin\"}]],\"details\":null,\"authenticated\":true,\"principal\":{\"@class\":\"com.ingot.framework.security.core.userdetails.IngotUser\",\"username\":\"admin\",\"authorities\":[\"java.util.Collections$UnmodifiableSet\",[{\"@class\":\"com.ingot.framework.security.core.authority.ClientGrantedAuthority\",\"authority\":\"CLIENT_CLIENT_CLIENT_CLIENT_CLIENT_CLIENT_CLIENT_CLIENT_CLIENT_CLIENT_CLIENT_ingot\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"basic\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"role_admin\"}]],\"id\":1,\"deptId\":1,\"tenantId\":1,\"clientId\":\"ingot\",\"tokenAuthType\":\"0\"},\"credentials\":null,\"name\":\"admin\"}}', NULL, NULL, NULL, NULL, NULL, 0x65794A72615751694F6949784D6A41314E325A694E53316A4D5745334C54517A596A497459546C6C5A43316A4D444D335A6A45354D574D7A595459694C434A68624763694F694A53557A49314E694A392E65794A7A645749694F694A685A47317062694973496D46315A434936496D6C755A32393049697769626D4A6D496A6F784E6A63324E544D354F544D774C434A30595851694F6949774969776963324E76634755694F6C7369636D39735A5639685A47317062694973496D316C63334E685A325575636D56685A434973496D4A6863326C6A496977696257567A6332466E5A533533636D6C305A534A644C434A7063334D694F694A6F644852774F6938766157356E62335174595856306143317A5A584A325A5849364E5445774D434973496D6B694F6A4573496D526C634851694F6A4573496D5634634349364D5459334E6A55304E7A457A4D43776961574630496A6F784E6A63324E544D354F544D774C434A305A573568626E51694F6A46392E533858446B445F6631716A756950374749676E5A623553646A4E3976747467353557394E4230755F517631473675347571316B64437755547947666F6F5A725261585759632D717433476F4651576749536135723770446C42706A68452D70787A624C596A433955685571653068676A4E704D4A6C77777352555A7847395661324471344F4A6B3170304F77346353357457655464795F34464D754B70416B50343061387966435A5950394B6D4930745A4C575866716C6E6344747251414566522D5935697650456232424C30416261526B3233627348322D34434B355949664F436F6339726159734E426934784976786B666E53433031503856464446736E69567A4F4C716D4A43526557734B4F4779782D4633306153697647586F32617632725362524B7A77314E70745A646B576B646F366461626750334532386874595F6464436758764369535579376F6A617130697A7451, '2023-02-16 17:32:10', '2023-02-16 19:32:10', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"metadata.token.claims\":{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"sub\":\"admin\",\"aud\":[\"java.util.Collections$SingletonList\",[\"ingot\"]],\"nbf\":[\"java.time.Instant\",1676539930.043000000],\"tat\":\"0\",\"scope\":[\"java.util.HashSet\",[\"role_admin\",\"message.read\",\"basic\",\"message.write\"]],\"iss\":[\"java.net.URL\",\"http://ingot-auth-server:5100\"],\"i\":[\"java.lang.Long\",1],\"dept\":[\"java.lang.Long\",1],\"exp\":[\"java.time.Instant\",1676547130.043000000],\"iat\":[\"java.time.Instant\",1676539930.043000000],\"tenant\":[\"java.lang.Long\",1]},\"metadata.token.invalidated\":false}', 'Bearer', 'message.read,message.write', NULL, NULL, NULL, NULL, 0x7177613135754E63383378454373706A574E446F6C4F59655F6136765A727648674A684C566A376934476D6261727276314F6B6F4E3165486D6E6755504867707A66696A7A3279775871357A334B306C4C394D3067666834795A7A43774D546631516E3430425138476B4B4269414A54546B68737935633555454B377A6C555A, '2023-02-16 17:32:10', '2023-02-23 17:32:10', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"metadata.token.invalidated\":false}');
INSERT INTO `oauth2_authorization` VALUES ('43d443a0-330d-479b-89d3-d6303c8b5310', 'ingot', 'admin', 'password', 'message.read,message.write', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"java.security.Principal\":{\"@class\":\"com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationToken\",\"authorities\":[\"java.util.Collections$UnmodifiableRandomAccessList\",[{\"@class\":\"com.ingot.framework.security.core.authority.ClientGrantedAuthority\",\"authority\":\"CLIENT_ingot\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"basic\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"mstx-activity\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"mstx-admin\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"mstx-banner\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"mstx-log\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"mstx-user\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"role_admin\"}]],\"details\":null,\"authenticated\":true,\"principal\":{\"@class\":\"com.ingot.framework.security.core.userdetails.IngotUser\",\"username\":\"admin\",\"authorities\":[\"java.util.Collections$UnmodifiableSet\",[{\"@class\":\"com.ingot.framework.security.core.authority.ClientGrantedAuthority\",\"authority\":\"CLIENT_ingot\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"basic\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"mstx-activity\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"mstx-admin\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"mstx-banner\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"mstx-log\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"mstx-user\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"role_admin\"}]],\"id\":1,\"deptId\":1,\"tenantId\":1,\"clientId\":\"ingot\",\"tokenAuthType\":\"0\"},\"credentials\":null,\"name\":\"admin\"}}', NULL, NULL, NULL, NULL, NULL, 0x65794A72615751694F6949794D6D46694E47517A4F4331694F47526D4C5451344F546B74595745334D793169597A55334F4755784D6D566C5A6A41694C434A68624763694F694A53557A49314E694A392E65794A7A645749694F694A685A47317062694973496D46315A434936496D6C755A32393049697769626D4A6D496A6F784E6A67774D7A6B334E5445794C434A30595851694F6949774969776963324E76634755694F6C7369636D39735A5639685A47317062694973496D317A64486774596D4675626D5679496977696257567A6332466E5A5335795A57466B4969776962584E306543316859335270646D6C3065534973496D317A6448677459575274615734694C434A6959584E7059794973496D317A6448677464584E6C63694973496D317A644867746247396E496977696257567A6332466E5A533533636D6C305A534A644C434A7063334D694F694A6F644852774F6938766157356E62335174595856306143317A5A584A325A5849364E5445774D434973496D6B694F6A4573496D526C634851694F6A4573496D5634634349364D5459344D4451774E4463784D69776961574630496A6F784E6A67774D7A6B334E5445794C434A305A573568626E51694F6A46392E5737446F4651465A6E6F30755F75716132364A6A525951487A466F304B466463594F53632D55365452637530504564714869454B6A6D7943496B65576D77455351474B30497648706D55464F7372745A795F5531546763652D4B794E756556733161726E5679736732477270454F663356476B3253754961383572454A43775948526E6A6F3251537145426D786D4C344238646A5F364365387764645A3436434F586B64384F636D4F6A3459652D446E57577544645F614547754171786B576F2D7842514E6F6E4A6D6C663761574B445A7741646B6C4D76755A36664F6E74353837576B736C4843436E4C6975717543375137585677456E676D444D4D64617136314C4744594F39484E393875534736476C57596438576773693258325369736A66745530526C6A6A424B734572446E4958356B6A7749563779535475333251474171666C717A746C673939776765342D4339675451, '2023-04-02 09:05:13', '2023-04-02 11:05:13', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"metadata.token.claims\":{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"sub\":\"admin\",\"aud\":[\"java.util.Collections$SingletonList\",[\"ingot\"]],\"nbf\":[\"java.time.Instant\",1680397512.800000000],\"tat\":\"0\",\"scope\":[\"java.util.HashSet\",[\"role_admin\",\"mstx-banner\",\"message.read\",\"mstx-activity\",\"mstx-admin\",\"basic\",\"mstx-user\",\"mstx-log\",\"message.write\"]],\"iss\":[\"java.net.URL\",\"http://ingot-auth-server:5100\"],\"i\":[\"java.lang.Long\",1],\"dept\":[\"java.lang.Long\",1],\"exp\":[\"java.time.Instant\",1680404712.800000000],\"iat\":[\"java.time.Instant\",1680397512.800000000],\"tenant\":[\"java.lang.Long\",1]},\"metadata.token.invalidated\":false}', 'Bearer', 'message.read,message.write', NULL, NULL, NULL, NULL, 0x4B642D5F78757834584C747A6F6B6F706D77435051744B356A7A7356303031336E3045515446625A4F377032435034635171742D457261724F50515F5F36376149724F34466F53696F50395F734838514C454C6972514B4E38344F78372D4973446F4870663853656135645A78654E353274484577595A6866356C5174745854, '2023-04-02 09:05:13', '2023-04-09 09:05:13', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"metadata.token.invalidated\":false}');
INSERT INTO `oauth2_authorization` VALUES ('5037f947-434c-40f8-a4fe-4cfb2575d0dd', 'ingot', 'admin', 'password', 'message.read,message.write', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"java.security.Principal\":{\"@class\":\"com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationToken\",\"authorities\":[\"java.util.Collections$UnmodifiableRandomAccessList\",[{\"@class\":\"com.ingot.framework.security.core.authority.ClientGrantedAuthority\",\"authority\":\"CLIENT_ingot\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"basic\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"role_admin\"}]],\"details\":null,\"authenticated\":true,\"principal\":{\"@class\":\"com.ingot.framework.security.core.userdetails.IngotUser\",\"username\":\"admin\",\"authorities\":[\"java.util.Collections$UnmodifiableSet\",[{\"@class\":\"com.ingot.framework.security.core.authority.ClientGrantedAuthority\",\"authority\":\"CLIENT_ingot\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"basic\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"role_admin\"}]],\"id\":1,\"deptId\":1,\"tenantId\":1,\"clientId\":\"ingot\",\"tokenAuthType\":\"0\"},\"credentials\":null,\"name\":\"admin\"}}', NULL, NULL, NULL, NULL, NULL, 0x65794A72615751694F694A6B4E5759794E446B325A4331685A574E694C54526D4D7A4D744F57497A4D5331684D7A49355A5746694D54426D5A6A59694C434A68624763694F694A53557A49314E694A392E65794A7A645749694F694A685A47317062694973496D46315A434936496D6C755A32393049697769626D4A6D496A6F784E6A63794D7A67354D546B7A4C434A30595851694F6949774969776963324E76634755694F6C7369636D39735A5639685A47317062694973496D316C63334E685A325575636D56685A434973496D4A6863326C6A496977696257567A6332466E5A533533636D6C305A534A644C434A7063334D694F694A6F644852774F6938766157356E62335174595856306143317A5A584A325A5849364E5445774D434973496D6B694F6A4573496D526C634851694F6A4573496D5634634349364D5459334D6A4D354E6A4D354D79776961574630496A6F784E6A63794D7A67354D546B7A4C434A305A573568626E51694F6A46392E56744650434C6B7171793337512D73792D34622D6A3058627644655968554B5A633643774457744B2D5F72776C56774E50506159554363514379463576596F3977304970496E505F6E676737414D3972673432756D65616D54734D48627943474C475673335F474C4E59346B464F3542434C6959564664614D396B4A3935577265466D585F54704B7A6768365870324B697A59515539316B514665757A787A6657385A445474744D5065364478455931682D704E5F5F46546C485A7165556D5F527779676D4B50514645796E72565747626549315478477243374274584578416C3550795641376B325250733363305533656A4664306D7959554B4669333366754A4F4D746F5A58583273707239324A33634B556B467171396B4A4D494258526F4D50706E3448465655794B4C66626230356B762D42483949766949546B454B4D6E64353335784E4D346E564753595835304C553167, '2022-12-30 16:33:13', '2022-12-30 18:33:13', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"metadata.token.claims\":{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"sub\":\"admin\",\"aud\":[\"java.util.Collections$SingletonList\",[\"ingot\"]],\"nbf\":[\"java.time.Instant\",1672389193.214000000],\"tat\":\"0\",\"scope\":[\"java.util.HashSet\",[\"role_admin\",\"message.read\",\"basic\",\"message.write\"]],\"iss\":[\"java.net.URL\",\"http://ingot-auth-server:5100\"],\"i\":[\"java.lang.Long\",1],\"dept\":[\"java.lang.Long\",1],\"exp\":[\"java.time.Instant\",1672396393.214000000],\"iat\":[\"java.time.Instant\",1672389193.214000000],\"tenant\":[\"java.lang.Long\",1]},\"metadata.token.invalidated\":false}', 'Bearer', 'message.read,message.write', NULL, NULL, NULL, NULL, 0x6854356B61713069426854326F345A4D506C732D6B584B39366A655A4D59446973364E4262615739757A39314659514B4B6E72304C6232652D6B73636C44614C723434574F7959506665383747515462655F3241595359385A626376444B7641456A585A70726972446F564A4F7755394C5165695A725879536E4734524A6453, '2022-12-30 16:33:13', '2023-01-06 16:33:13', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"metadata.token.invalidated\":false}');
INSERT INTO `oauth2_authorization` VALUES ('6794f185-467d-4ea1-ab53-524d3a7267dc', 'ingot', 'admin', 'password', 'message.read,message.write', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"java.security.Principal\":{\"@class\":\"com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationToken\",\"authorities\":[\"java.util.Collections$UnmodifiableRandomAccessList\",[{\"@class\":\"com.ingot.framework.security.core.authority.ClientGrantedAuthority\",\"authority\":\"CLIENT_ingot\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"basic\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"role_admin\"}]],\"details\":null,\"authenticated\":true,\"principal\":{\"@class\":\"com.ingot.framework.security.core.userdetails.IngotUser\",\"username\":\"admin\",\"authorities\":[\"java.util.Collections$UnmodifiableSet\",[{\"@class\":\"com.ingot.framework.security.core.authority.ClientGrantedAuthority\",\"authority\":\"CLIENT_ingot\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"basic\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"role_admin\"}]],\"id\":1,\"deptId\":1,\"tenantId\":1,\"clientId\":\"ingot\",\"tokenAuthType\":\"0\"},\"credentials\":null,\"name\":\"admin\"}}', NULL, NULL, NULL, NULL, NULL, 0x65794A72615751694F694A6B4E5759794E446B325A4331685A574E694C54526D4D7A4D744F57497A4D5331684D7A49355A5746694D54426D5A6A59694C434A68624763694F694A53557A49314E694A392E65794A7A645749694F694A685A47317062694973496D46315A434936496D6C755A32393049697769626D4A6D496A6F784E6A63794D7A67354E6A45774C434A30595851694F6949774969776963324E76634755694F6C7369636D39735A5639685A47317062694973496D316C63334E685A325575636D56685A434973496D4A6863326C6A496977696257567A6332466E5A533533636D6C305A534A644C434A7063334D694F694A6F644852774F6938766157356E62335174595856306143317A5A584A325A5849364E5445774D434973496D6B694F6A4573496D526C634851694F6A4573496D5634634349364D5459334D6A4D354E6A67784D43776961574630496A6F784E6A63794D7A67354E6A45774C434A305A573568626E51694F6A46392E5859547034374A3165784E574F35625F5A4B4E357666666E686269644A51302D4E73694E4E3631686736766D4E676E487A545042767759523530736C516E6D386276383230547437434A6147336735393470585F745A623568554E48357A534679687570526B5333373932506164722D6A5F684A427076644553416F3446314736527A546B47397842594A36786D6C72443447624E315831385536426F684E623755396A6F3431694246725039485437685139726A7961614B5F736141633271614D744B476C4850356E527736774E512D2D6F654530366B6E4D5166452D3555564B7253306931445F3354424D6766476430586F57447268326C703479755455423455546662674D5F656E7A574369546B546B334137664F744C35656378414F7863564731776C386B636A6B4866414E4362653250786F59554658455750482D705A364E546455746D5736494E4F61435A5943637051, '2022-12-30 16:40:11', '2022-12-30 18:40:11', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"metadata.token.claims\":{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"sub\":\"admin\",\"aud\":[\"java.util.Collections$SingletonList\",[\"ingot\"]],\"nbf\":[\"java.time.Instant\",1672389610.861000000],\"tat\":\"0\",\"scope\":[\"java.util.HashSet\",[\"role_admin\",\"message.read\",\"basic\",\"message.write\"]],\"iss\":[\"java.net.URL\",\"http://ingot-auth-server:5100\"],\"i\":[\"java.lang.Long\",1],\"dept\":[\"java.lang.Long\",1],\"exp\":[\"java.time.Instant\",1672396810.861000000],\"iat\":[\"java.time.Instant\",1672389610.861000000],\"tenant\":[\"java.lang.Long\",1]},\"metadata.token.invalidated\":false}', 'Bearer', 'message.read,message.write', NULL, NULL, NULL, NULL, 0x437969704A3130433959514576555A743035474144443763714F5F3367536C4D73704437372D6B626A4A754E5534394D43794866684F486A6272305166436476346D59427957446D73765871695A466E47504C5A446F4C4E7247725643396A6856453748394D4B6A455538464A383939677155496B784572655642497A585350, '2022-12-30 16:40:11', '2023-01-06 16:40:11', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"metadata.token.invalidated\":false}');
INSERT INTO `oauth2_authorization` VALUES ('78391a25-9bbe-4aea-91b1-3bb8aebe2fef', 'ingot', 'admin', 'password', 'message.read,message.write', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"java.security.Principal\":{\"@class\":\"com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationToken\",\"authorities\":[\"java.util.Collections$UnmodifiableRandomAccessList\",[{\"@class\":\"com.ingot.framework.security.core.authority.ClientGrantedAuthority\",\"authority\":\"CLIENT_ingot\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"basic\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"role_admin\"}]],\"details\":null,\"authenticated\":true,\"principal\":{\"@class\":\"com.ingot.framework.security.core.userdetails.IngotUser\",\"username\":\"admin\",\"authorities\":[\"java.util.Collections$UnmodifiableSet\",[{\"@class\":\"com.ingot.framework.security.core.authority.ClientGrantedAuthority\",\"authority\":\"CLIENT_ingot\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"basic\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"role_admin\"}]],\"id\":1,\"deptId\":1,\"tenantId\":1,\"clientId\":\"ingot\",\"tokenAuthType\":\"0\"},\"credentials\":null,\"name\":\"admin\"}}', NULL, NULL, NULL, NULL, NULL, 0x65794A72615751694F694A6B4E5759794E446B325A4331685A574E694C54526D4D7A4D744F57497A4D5331684D7A49355A5746694D54426D5A6A59694C434A68624763694F694A53557A49314E694A392E65794A7A645749694F694A685A47317062694973496D46315A434936496D6C755A32393049697769626D4A6D496A6F784E6A63324D7A51784D7A6B774C434A30595851694F6949774969776963324E76634755694F6C7369636D39735A5639685A47317062694973496D316C63334E685A325575636D56685A434973496D4A6863326C6A496977696257567A6332466E5A533533636D6C305A534A644C434A7063334D694F694A6F644852774F6938766157356E62335174595856306143317A5A584A325A5849364E5445774D434973496D6B694F6A4573496D526C634851694F6A4573496D5634634349364D5459334E6A4D304F4455354D43776961574630496A6F784E6A63324D7A51784D7A6B774C434A305A573568626E51694F6A46392E6268705A377A766A4237566D39515844467850515A336B697068396E49615343677379544554453159465139315F5F5A324A746C644E464D52535654596A386635746D433850646F4D4B6C534B704C68787A4C386D3830484F6A712D67755448794A536B6269696C664F553156596F486233575166687342426530694D4846485A354467336F7354493353567857754857745377765A366950614A37734473636E374477566F586B794239646C303453366C41445F453675794E587A395F6C58797A43714130413753784E4F522D5633647633623279613568434841343063324B3562746C6B4551376F36346D6E51493044744C755F5355466E7A31536D476B5132364F4C7335704675305F7A384867507742516D3961695837796936326D6164334C6272653858505435614E6173706E34322D6C6B5955575F54584861784B436B546A78596E4A45696C7753556E65306351547467, '2023-02-14 10:23:10', '2023-02-14 12:23:10', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"metadata.token.claims\":{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"sub\":\"admin\",\"aud\":[\"java.util.Collections$SingletonList\",[\"ingot\"]],\"nbf\":[\"java.time.Instant\",1676341390.166000000],\"tat\":\"0\",\"scope\":[\"java.util.HashSet\",[\"role_admin\",\"message.read\",\"basic\",\"message.write\"]],\"iss\":[\"java.net.URL\",\"http://ingot-auth-server:5100\"],\"i\":[\"java.lang.Long\",1],\"dept\":[\"java.lang.Long\",1],\"exp\":[\"java.time.Instant\",1676348590.166000000],\"iat\":[\"java.time.Instant\",1676341390.166000000],\"tenant\":[\"java.lang.Long\",1]},\"metadata.token.invalidated\":false}', 'Bearer', 'message.read,message.write', NULL, NULL, NULL, NULL, 0x3452666557747543776453334E5A7172355F645958544679776E4E4276646B6C6A7472745A7A53526D554F436D33334265686C4D7461735158746C724D6349354B744C714A534E47467057794C444D59585778597373654B3535625777707666686E355A697063325062574E64516278746C45506646744E71746E6D42314E67, '2023-02-14 10:23:10', '2023-02-21 10:23:10', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"metadata.token.invalidated\":false}');
INSERT INTO `oauth2_authorization` VALUES ('797f49d6-9c57-4810-9ea1-4a1396e4728e', 'ingot', 'admin', 'password', 'message.read,message.write', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"java.security.Principal\":{\"@class\":\"com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationToken\",\"authorities\":[\"java.util.Collections$UnmodifiableRandomAccessList\",[{\"@class\":\"com.ingot.framework.security.core.authority.ClientGrantedAuthority\",\"authority\":\"CLIENT_ingot\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"basic\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"role_admin\"}]],\"details\":null,\"authenticated\":true,\"principal\":{\"@class\":\"com.ingot.framework.security.core.userdetails.IngotUser\",\"username\":\"admin\",\"authorities\":[\"java.util.Collections$UnmodifiableSet\",[{\"@class\":\"com.ingot.framework.security.core.authority.ClientGrantedAuthority\",\"authority\":\"CLIENT_ingot\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"basic\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"role_admin\"}]],\"id\":1,\"deptId\":1,\"tenantId\":1,\"clientId\":\"ingot\",\"tokenAuthType\":\"0\"},\"credentials\":null,\"name\":\"admin\"}}', NULL, NULL, NULL, NULL, NULL, 0x65794A72615751694F694934595467794E7A5A6C596931695A575A6B4C5451355A6A4974596A4E6B4F43316D4D57517959574D355A5751304F5759694C434A68624763694F694A53557A49314E694A392E65794A7A645749694F694A685A47317062694973496D46315A434936496D6C755A32393049697769626D4A6D496A6F784E6A63334D7A67794E6A6B344C434A30595851694F6949774969776963324E76634755694F6C7369636D39735A5639685A47317062694973496D316C63334E685A325575636D56685A434973496D4A6863326C6A496977696257567A6332466E5A533533636D6C305A534A644C434A7063334D694F694A6F644852774F6938766157356E62335174595856306143317A5A584A325A5849364E5445774D434973496D6B694F6A4573496D526C634851694F6A4573496D5634634349364D5459334E7A4D344F5467354F43776961574630496A6F784E6A63334D7A67794E6A6B344C434A305A573568626E51694F6A46392E755571742D5F463169336A52306F356F433653386A77446432576C59327364314342306B5F764945553530376262646D324171314348584559545136446D304B7764664F5958466E4F77454B413371485076706B5059733256505F7A45755358326445495A4568664567545A35734767322D794C44646F6567474D566A396858793759475A47473252754F376A3173475979342D334B5032457678484A35647341683652756E6D5F504656783634565449737375675F4D5F52476935615A4838577A7670653436465F614D665A6C5247644C49775268754464784B4B705343767633766F6B53513937365A586C524369544C587872554C464E7654346F44565352794B4D6F537064396648784D4E576A72725F66754554503257745A51756A4E70654C775F3236424E5733544E635A50426E76344E664C45516F5851566E58635174474568715442732D374437615A5A734756497151, '2023-02-26 11:38:19', '2023-02-26 13:38:19', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"metadata.token.claims\":{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"sub\":\"admin\",\"aud\":[\"java.util.Collections$SingletonList\",[\"ingot\"]],\"nbf\":[\"java.time.Instant\",1677382698.769000000],\"tat\":\"0\",\"scope\":[\"java.util.HashSet\",[\"role_admin\",\"message.read\",\"basic\",\"message.write\"]],\"iss\":[\"java.net.URL\",\"http://ingot-auth-server:5100\"],\"i\":[\"java.lang.Long\",1],\"dept\":[\"java.lang.Long\",1],\"exp\":[\"java.time.Instant\",1677389898.769000000],\"iat\":[\"java.time.Instant\",1677382698.769000000],\"tenant\":[\"java.lang.Long\",1]},\"metadata.token.invalidated\":false}', 'Bearer', 'message.read,message.write', NULL, NULL, NULL, NULL, 0x484F53674932574445575A6F6F543564757776553730586377696D6E78514B4A59423031394F784544664C6E4569784469754F58596D3876687878646B3364486E556E5A31583675485773775A517A6358486C412D51334A4F732D7461344D63483765774954786B5149766377584F693735566E4B4F6B3571517A5871695659, '2023-02-26 11:38:19', '2023-03-05 11:38:19', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"metadata.token.invalidated\":false}');
INSERT INTO `oauth2_authorization` VALUES ('b39e4652-c9e5-4ac2-ba7c-ee80b79e4e0f', 'ingot', 'admin', 'password', 'message.read,message.write', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"java.security.Principal\":{\"@class\":\"com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationToken\",\"authorities\":[\"java.util.Collections$UnmodifiableRandomAccessList\",[{\"@class\":\"com.ingot.framework.security.core.authority.ClientGrantedAuthority\",\"authority\":\"CLIENT_ingot\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"basic\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"role_admin\"}]],\"details\":null,\"authenticated\":true,\"principal\":{\"@class\":\"com.ingot.framework.security.core.userdetails.IngotUser\",\"username\":\"admin\",\"authorities\":[\"java.util.Collections$UnmodifiableSet\",[{\"@class\":\"com.ingot.framework.security.core.authority.ClientGrantedAuthority\",\"authority\":\"CLIENT_ingot\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"basic\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"role_admin\"}]],\"id\":1,\"deptId\":1,\"tenantId\":1,\"clientId\":\"ingot\",\"tokenAuthType\":\"0\"},\"credentials\":null,\"name\":\"admin\"}}', NULL, NULL, NULL, NULL, NULL, 0x65794A72615751694F694934595467794E7A5A6C596931695A575A6B4C5451355A6A4974596A4E6B4F43316D4D57517959574D355A5751304F5759694C434A68624763694F694A53557A49314E694A392E65794A7A645749694F694A685A47317062694973496D46315A434936496D6C755A32393049697769626D4A6D496A6F784E6A63334D5445334D7A49354C434A30595851694F6949774969776963324E76634755694F6C7369636D39735A5639685A47317062694973496D316C63334E685A325575636D56685A434973496D4A6863326C6A496977696257567A6332466E5A533533636D6C305A534A644C434A7063334D694F694A6F644852774F6938766157356E62335174595856306143317A5A584A325A5849364E5445774D434973496D6B694F6A4573496D526C634851694F6A4573496D5634634349364D5459334E7A45794E4455794F53776961574630496A6F784E6A63334D5445334D7A49354C434A305A573568626E51694F6A46392E6146447A6E307755307036724D37784B42727A307461545A3674356C324C583570456438545968343442694938794C5751355458635137304836485964597663564D666871393339756A3751714657456C31493739416A6A70374C592D373356666F3542686945303842304553385A43786D78767337597732495646423478587A544976776A364241727937477637414B782D4E684B3052774D305275714A6B306A666F3444533771623553514E3441583065356F39543648425768586A63486A42546847356E556578546E4C497568715945347351594936796479714E473346484E38446D4871523334443870474B4D45716E366D616B337467325A4E446E4A5174674A586570564C4F533645655076766258784D47646F4F6A54707067494C644F765152616F5F556B67477578326B36557A36375A46672D64665671754D534F5A6B6B7137385165735649565475327647637867, '2023-02-23 09:55:29', '2023-02-23 11:55:29', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"metadata.token.claims\":{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"sub\":\"admin\",\"aud\":[\"java.util.Collections$SingletonList\",[\"ingot\"]],\"nbf\":[\"java.time.Instant\",1677117329.326000000],\"tat\":\"0\",\"scope\":[\"java.util.HashSet\",[\"role_admin\",\"message.read\",\"basic\",\"message.write\"]],\"iss\":[\"java.net.URL\",\"http://ingot-auth-server:5100\"],\"i\":[\"java.lang.Long\",1],\"dept\":[\"java.lang.Long\",1],\"exp\":[\"java.time.Instant\",1677124529.326000000],\"iat\":[\"java.time.Instant\",1677117329.326000000],\"tenant\":[\"java.lang.Long\",1]},\"metadata.token.invalidated\":false}', 'Bearer', 'message.read,message.write', NULL, NULL, NULL, NULL, 0x5A316E4676487859554B4E586A345734586F6F314E5572722D4E714F743964415143464B5F6A71566A494C3046476B47635179736538396B765978686F557A567361307139396731514270376F62477A366854486C63565643476A5F4A66566753585F6D4732767267334D56493035777858786C4D436149315A794C6B41576F, '2023-02-23 09:55:29', '2023-03-02 09:55:29', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"metadata.token.invalidated\":false}');
INSERT INTO `oauth2_authorization` VALUES ('c14579f8-1a79-40ce-a385-476f44f3ddbc', 'ingot', 'admin', 'password', 'message.read,message.write', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"java.security.Principal\":{\"@class\":\"com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationToken\",\"authorities\":[\"java.util.Collections$UnmodifiableRandomAccessList\",[{\"@class\":\"com.ingot.framework.security.core.authority.ClientGrantedAuthority\",\"authority\":\"CLIENT_ingot\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"basic\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"role_admin\"}]],\"details\":null,\"authenticated\":true,\"principal\":{\"@class\":\"com.ingot.framework.security.core.userdetails.IngotUser\",\"username\":\"admin\",\"authorities\":[\"java.util.Collections$UnmodifiableSet\",[{\"@class\":\"com.ingot.framework.security.core.authority.ClientGrantedAuthority\",\"authority\":\"CLIENT_ingot\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"basic\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"role_admin\"}]],\"id\":1,\"deptId\":1,\"tenantId\":1,\"clientId\":\"ingot\",\"tokenAuthType\":\"0\"},\"credentials\":null,\"name\":\"admin\"}}', NULL, NULL, NULL, NULL, NULL, 0x65794A72615751694F694934595467794E7A5A6C596931695A575A6B4C5451355A6A4974596A4E6B4F43316D4D57517959574D355A5751304F5759694C434A68624763694F694A53557A49314E694A392E65794A7A645749694F694A685A47317062694973496D46315A434936496D6C755A32393049697769626D4A6D496A6F784E6A63334D5451304E4441344C434A30595851694F6949774969776963324E76634755694F6C7369636D39735A5639685A47317062694973496D316C63334E685A325575636D56685A434973496D4A6863326C6A496977696257567A6332466E5A533533636D6C305A534A644C434A7063334D694F694A6F644852774F6938766157356E62335174595856306143317A5A584A325A5849364E5445774D434973496D6B694F6A4573496D526C634851694F6A4573496D5634634349364D5459334E7A45314D5459774F43776961574630496A6F784E6A63334D5451304E4441344C434A305A573568626E51694F6A46392E494A56374A4359794B5A38523679395933755A594D69703167724D473068774C774A37577055706B586C626A446F4741533939594346366D566C4F49475772336F39762D723562706C535A7461757875556A6C42536468482D5968706A38496465794A47654775442D7344616D363835536B5567702D366D7A63414B49442D346B32465271576D3372423047504E4F49725967676A6835496B3834564F786C33484A7734477968336F49736D3461452D56324E6872572D767175504E4A567371634975474F4F4D2D4D307546683463686378694C3871336D67466654675879776C50554F476269477345396E45716872703630356E6F534E786348502D496A694B43657A437753426C6B546F3136524D4C42706F6B4C535A3744524E48393366577A4B622D657A6D4A755A443641536E5F2D54444A717479696E4661315647356B387459614B4D56714C45796D4872556E7A524F4441, '2023-02-23 17:26:48', '2023-02-23 19:26:48', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"metadata.token.claims\":{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"sub\":\"admin\",\"aud\":[\"java.util.Collections$SingletonList\",[\"ingot\"]],\"nbf\":[\"java.time.Instant\",1677144408.115000000],\"tat\":\"0\",\"scope\":[\"java.util.HashSet\",[\"role_admin\",\"message.read\",\"basic\",\"message.write\"]],\"iss\":[\"java.net.URL\",\"http://ingot-auth-server:5100\"],\"i\":[\"java.lang.Long\",1],\"dept\":[\"java.lang.Long\",1],\"exp\":[\"java.time.Instant\",1677151608.115000000],\"iat\":[\"java.time.Instant\",1677144408.115000000],\"tenant\":[\"java.lang.Long\",1]},\"metadata.token.invalidated\":false}', 'Bearer', 'message.read,message.write', NULL, NULL, NULL, NULL, 0x4F434C4E38616365794175636F68725A39394362517838545A746B502D623958363161675A6D727A5A4C49443267626A616F6A4F6E7178304E54506A6F535A744E5567344B4258566F3073485A62365A77797139784865354B36544E66486155795470346B64434C75686A304B44497141356D62616537326D6831455946544D, '2023-02-23 17:26:48', '2023-03-02 17:26:48', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"metadata.token.invalidated\":false}');
INSERT INTO `oauth2_authorization` VALUES ('cff066b1-cade-4288-b7c6-8c70c4b7491e', 'ingot', 'admin', 'password', 'message.read,message.write', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"java.security.Principal\":{\"@class\":\"com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationToken\",\"authorities\":[\"java.util.Collections$UnmodifiableRandomAccessList\",[{\"@class\":\"com.ingot.framework.security.core.authority.ClientGrantedAuthority\",\"authority\":\"CLIENT_ingot\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"basic\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"mstx-activity\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"mstx-admin\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"mstx-banner\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"mstx-log\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"mstx-user\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"role_admin\"}]],\"details\":null,\"authenticated\":true,\"principal\":{\"@class\":\"com.ingot.framework.security.core.userdetails.IngotUser\",\"username\":\"admin\",\"authorities\":[\"java.util.Collections$UnmodifiableSet\",[{\"@class\":\"com.ingot.framework.security.core.authority.ClientGrantedAuthority\",\"authority\":\"CLIENT_ingot\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"basic\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"mstx-activity\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"mstx-admin\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"mstx-banner\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"mstx-log\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"mstx-user\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"role_admin\"}]],\"id\":1,\"deptId\":1,\"tenantId\":1,\"clientId\":\"ingot\",\"tokenAuthType\":\"0\"},\"credentials\":null,\"name\":\"admin\"}}', NULL, NULL, NULL, NULL, NULL, 0x65794A72615751694F6949794D6D46694E47517A4F4331694F47526D4C5451344F546B74595745334D793169597A55334F4755784D6D566C5A6A41694C434A68624763694F694A53557A49314E694A392E65794A7A645749694F694A685A47317062694973496D46315A434936496D6C755A32393049697769626D4A6D496A6F784E6A67774D7A6B334E5441774C434A30595851694F6949774969776963324E76634755694F6C7369636D39735A5639685A47317062694973496D317A64486774596D4675626D5679496977696257567A6332466E5A5335795A57466B4969776962584E306543316859335270646D6C3065534973496D317A6448677459575274615734694C434A6959584E7059794973496D317A6448677464584E6C63694973496D317A644867746247396E496977696257567A6332466E5A533533636D6C305A534A644C434A7063334D694F694A6F644852774F6938766157356E62335174595856306143317A5A584A325A5849364E5445774D434973496D6B694F6A4573496D526C634851694F6A4573496D5634634349364D5459344D4451774E4463774D43776961574630496A6F784E6A67774D7A6B334E5441774C434A305A573568626E51694F6A46392E67514F454A766849546344364636586353656571534257632D5175554F694361334378564650715F6574516A58574574444F79703736336E47684F7A5330414D6D3168644A4D6F513230745F2D303957507766563870684A772D626446384672743232324C49464B474B5554704B787676393778736B6D644C53697333687A625775674F7764656A344C38726E4D70747A416A616341357649616F38645750524A555F584D47657243316971455772303449334E53685F476B4C496834746B6958546D684161536F46646354666C6B632D7478516F4D6C34674462313644383155704D58696D72784455747742426D50667876493567773568665638624E57416631654A59363736686F314D4D6F744558675F61726D6436735546634D725631494A5553393079393730512D57356E3479714C5A6857345A664E744D766E6C364A7256364945324231794F446F767A305F5A30347867, '2023-04-02 09:05:00', '2023-04-02 11:05:00', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"metadata.token.claims\":{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"sub\":\"admin\",\"aud\":[\"java.util.Collections$SingletonList\",[\"ingot\"]],\"nbf\":[\"java.time.Instant\",1680397500.127000000],\"tat\":\"0\",\"scope\":[\"java.util.HashSet\",[\"role_admin\",\"mstx-banner\",\"message.read\",\"mstx-activity\",\"mstx-admin\",\"basic\",\"mstx-user\",\"mstx-log\",\"message.write\"]],\"iss\":[\"java.net.URL\",\"http://ingot-auth-server:5100\"],\"i\":[\"java.lang.Long\",1],\"dept\":[\"java.lang.Long\",1],\"exp\":[\"java.time.Instant\",1680404700.127000000],\"iat\":[\"java.time.Instant\",1680397500.127000000],\"tenant\":[\"java.lang.Long\",1]},\"metadata.token.invalidated\":false}', 'Bearer', 'message.read,message.write', NULL, NULL, NULL, NULL, 0x415F6A42347156777A5F545F5A6238564D3371744753314E4B35674F2D637A7649344B3077696E503566437A7250746D7079714E454B795A6F4E35617A70775A454736797254726537714B367A4A2D48473236334238725F54686131732D456867547438544B5532486D52697A597773475358517168632D4C6C2D33386E3264, '2023-04-02 09:05:00', '2023-04-09 09:05:00', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"metadata.token.invalidated\":false}');
INSERT INTO `oauth2_authorization` VALUES ('dd6e26b9-abbb-4f2a-8faf-1c7e5aa9563f', 'ingot', 'admin', 'password', 'message.read,message.write', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"java.security.Principal\":{\"@class\":\"com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationToken\",\"authorities\":[\"java.util.Collections$UnmodifiableRandomAccessList\",[{\"@class\":\"com.ingot.framework.security.core.authority.ClientGrantedAuthority\",\"authority\":\"CLIENT_ingot\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"basic\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"role_admin\"}]],\"details\":null,\"authenticated\":true,\"principal\":{\"@class\":\"com.ingot.framework.security.core.userdetails.IngotUser\",\"username\":\"admin\",\"authorities\":[\"java.util.Collections$UnmodifiableSet\",[{\"@class\":\"com.ingot.framework.security.core.authority.ClientGrantedAuthority\",\"authority\":\"CLIENT_ingot\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"basic\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"role_admin\"}]],\"id\":1,\"deptId\":1,\"tenantId\":1,\"clientId\":\"ingot\",\"tokenAuthType\":\"0\"},\"credentials\":null,\"name\":\"admin\"}}', NULL, NULL, NULL, NULL, NULL, 0x65794A72615751694F694A6B4E5759794E446B325A4331685A574E694C54526D4D7A4D744F57497A4D5331684D7A49355A5746694D54426D5A6A59694C434A68624763694F694A53557A49314E694A392E65794A7A645749694F694A685A47317062694973496D46315A434936496D6C755A32393049697769626D4A6D496A6F784E6A63794D7A6B774D6A67334C434A30595851694F6949774969776963324E76634755694F6C7369636D39735A5639685A47317062694973496D316C63334E685A325575636D56685A434973496D4A6863326C6A496977696257567A6332466E5A533533636D6C305A534A644C434A7063334D694F694A6F644852774F6938766157356E62335174595856306143317A5A584A325A5849364E5445774D434973496D6B694F6A4573496D526C634851694F6A4573496D5634634349364D5459334D6A4D354D444D774E79776961574630496A6F784E6A63794D7A6B774D6A67334C434A305A573568626E51694F6A46392E42524B4A635143636C4E716A57526852693377654E594341645873727759497874505745316334576B57335F7456496E3559694249726463506E416B3633563371474D5A587A6B677A4172763756745168396B4E6D59446731476F4F324D7674464C3578355F7643416633453543584A5F623051774B68643477364B466E3058445967517358436C4F757174506877506863714B775F613849666131656938656D5659576A67775A5162637459676F6B7A4A3165627A685F63516E41766F766158507337596C30566B45377477432D6C614B654E43413177323368326457537A6B445F32645664784D6C2D6251727452564A353341435749327849553169454E6F4C6C7456796862787863716B58424F6B51365A56754C53696934484648764D48506E7A53527068693363784250725566446F4C7039335064576B5F6A35773772762D6B4B78537A6F666A4B33365853725F616F3277, '2022-12-30 16:51:27', '2022-12-30 16:51:47', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"metadata.token.claims\":{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"sub\":\"admin\",\"aud\":[\"java.util.Collections$SingletonList\",[\"ingot\"]],\"nbf\":[\"java.time.Instant\",1672390287.202000000],\"tat\":\"0\",\"scope\":[\"java.util.HashSet\",[\"role_admin\",\"message.read\",\"basic\",\"message.write\"]],\"iss\":[\"java.net.URL\",\"http://ingot-auth-server:5100\"],\"i\":[\"java.lang.Long\",1],\"dept\":[\"java.lang.Long\",1],\"exp\":[\"java.time.Instant\",1672390307.202000000],\"iat\":[\"java.time.Instant\",1672390287.202000000],\"tenant\":[\"java.lang.Long\",1]},\"metadata.token.invalidated\":false}', 'Bearer', 'message.read,message.write', NULL, NULL, NULL, NULL, 0x5330772D684A4637304E4946656E5F5338757A2D6F5A364B4D4C66586E794941735241624858677A734436413139577445363831394670372D4634784B484D436D6C57626A4E4D7935695F325852703831517349794F7A776439646B526C4F5A78473571506C38615636374A3858735A6866715F52316A4F5A586C6B4E733669, '2022-12-30 16:51:27', '2023-01-06 16:51:27', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"metadata.token.invalidated\":false}');
INSERT INTO `oauth2_authorization` VALUES ('e73db3a1-f9ad-47a8-9a12-fb3774a50722', 'ingot', 'admin', 'password', 'message.read,message.write', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"java.security.Principal\":{\"@class\":\"com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationToken\",\"authorities\":[\"java.util.Collections$UnmodifiableRandomAccessList\",[{\"@class\":\"com.ingot.framework.security.core.authority.ClientGrantedAuthority\",\"authority\":\"CLIENT_ingot\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"basic\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"role_admin\"}]],\"details\":null,\"authenticated\":true,\"principal\":{\"@class\":\"com.ingot.framework.security.core.userdetails.IngotUser\",\"username\":\"admin\",\"authorities\":[\"java.util.Collections$UnmodifiableSet\",[{\"@class\":\"com.ingot.framework.security.core.authority.ClientGrantedAuthority\",\"authority\":\"CLIENT_ingot\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"basic\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"role_admin\"}]],\"id\":1,\"deptId\":1,\"tenantId\":1,\"clientId\":\"ingot\",\"tokenAuthType\":\"0\"},\"credentials\":null,\"name\":\"admin\"}}', NULL, NULL, NULL, NULL, NULL, 0x65794A72615751694F694A6B4E5759794E446B325A4331685A574E694C54526D4D7A4D744F57497A4D5331684D7A49355A5746694D54426D5A6A59694C434A68624763694F694A53557A49314E694A392E65794A7A645749694F694A685A47317062694973496D46315A434936496D6C755A32393049697769626D4A6D496A6F784E6A63794D7A6B774D5463354C434A30595851694F6949774969776963324E76634755694F6C7369636D39735A5639685A47317062694973496D316C63334E685A325575636D56685A434973496D4A6863326C6A496977696257567A6332466E5A533533636D6C305A534A644C434A7063334D694F694A6F644852774F6938766157356E62335174595856306143317A5A584A325A5849364E5445774D434973496D6B694F6A4573496D526C634851694F6A4573496D5634634349364D5459334D6A4D354D4445354F53776961574630496A6F784E6A63794D7A6B774D5463354C434A305A573568626E51694F6A46392E4B336565755374376D3243792D476F36354742454B77564968384673326E6F49446F79325730694737353448315569386C6A325866787435736C5334476F425831774F6151704E6C58796550364B5A63773734413072434A71626559584E646E754A487373765033686E52645950747469486B686D3265434D666D536B6E757348616939464277366E57796861514A44746771344C577474496C3569665F54457A7A614F50364F504A323953576C394F392D53735335306747474B45316E32676C522D6C4F326556614D65773977306D42554F2D31695462366E64695A39703265314366537A704579347861744C4D6D733549374741465573643755676530624263676D364F314271584A57766641675F756E6D792D666C5565365132414245666F5A6B64465855324E65703339715338532D79534674794952703532424E57457256565A7A54786E3673616D535346786451354341, '2022-12-30 16:49:40', '2022-12-30 16:50:00', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"metadata.token.claims\":{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"sub\":\"admin\",\"aud\":[\"java.util.Collections$SingletonList\",[\"ingot\"]],\"nbf\":[\"java.time.Instant\",1672390179.680000000],\"tat\":\"0\",\"scope\":[\"java.util.HashSet\",[\"role_admin\",\"message.read\",\"basic\",\"message.write\"]],\"iss\":[\"java.net.URL\",\"http://ingot-auth-server:5100\"],\"i\":[\"java.lang.Long\",1],\"dept\":[\"java.lang.Long\",1],\"exp\":[\"java.time.Instant\",1672390199.680000000],\"iat\":[\"java.time.Instant\",1672390179.680000000],\"tenant\":[\"java.lang.Long\",1]},\"metadata.token.invalidated\":false}', 'Bearer', 'message.read,message.write', NULL, NULL, NULL, NULL, 0x42455443464B5F48594C303950436356354546684636784E3878674D586B3877744E35587375577436354744556F72584E58516270765843767273564F3659386A4468525668794B7333445462746637454C4E6B6544345F462D6E7276517435535278673030794F396C725A64324D5065724C3061344449307671426E344933, '2022-12-30 16:49:40', '2023-01-06 16:49:40', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"metadata.token.invalidated\":false}');
INSERT INTO `oauth2_authorization` VALUES ('ebc6b2c7-2eb2-4acf-a5af-fdcd98ea1adc', 'ingot', 'admin', 'password', 'message.read,message.write', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"java.security.Principal\":{\"@class\":\"com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationToken\",\"authorities\":[\"java.util.Collections$UnmodifiableRandomAccessList\",[{\"@class\":\"com.ingot.framework.security.core.authority.ClientGrantedAuthority\",\"authority\":\"CLIENT_CLIENT_CLIENT_CLIENT_CLIENT_ingot\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"basic\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"role_admin\"}]],\"details\":null,\"authenticated\":true,\"principal\":{\"@class\":\"com.ingot.framework.security.core.userdetails.IngotUser\",\"username\":\"admin\",\"authorities\":[\"java.util.Collections$UnmodifiableSet\",[{\"@class\":\"com.ingot.framework.security.core.authority.ClientGrantedAuthority\",\"authority\":\"CLIENT_CLIENT_CLIENT_CLIENT_CLIENT_ingot\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"basic\"},{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"authority\":\"role_admin\"}]],\"id\":1,\"deptId\":1,\"tenantId\":1,\"clientId\":\"ingot\",\"tokenAuthType\":\"0\"},\"credentials\":null,\"name\":\"admin\"}}', NULL, NULL, NULL, NULL, NULL, 0x65794A72615751694F694A6B4E5759794E446B325A4331685A574E694C54526D4D7A4D744F57497A4D5331684D7A49355A5746694D54426D5A6A59694C434A68624763694F694A53557A49314E694A392E65794A7A645749694F694A685A47317062694973496D46315A434936496D6C755A32393049697769626D4A6D496A6F784E6A63794E4441774E5441324C434A30595851694F6949774969776963324E76634755694F6C7369636D39735A5639685A47317062694973496D316C63334E685A325575636D56685A434973496D4A6863326C6A496977696257567A6332466E5A533533636D6C305A534A644C434A7063334D694F694A6F644852774F6938766157356E62335174595856306143317A5A584A325A5849364E5445774D434973496D6B694F6A4573496D526C634851694F6A4573496D5634634349364D5459334D6A51774E7A63774E69776961574630496A6F784E6A63794E4441774E5441324C434A305A573568626E51694F6A46392E68546A6D31702D5536644A4257536C79336B757955597572334445484445536379614445507849457A486555753230776358784C5358485A7953557767496F6932634D4156466679386A396D4D347444504F385450516C64362D32324B5F704F79654946494645684B734841687241446939534147613876477A724E65756634564F496A43384E5647466F4A59547652577469486653477167742D436E54505844687436436B5749524A4135634137723831716E6C5A693763514A5A714A66394A71305F795953434C34655F4A68576A6B6146436F4F6862467450696B6F345075455F42745069524F3041495867626C6157777A5F4F71414E316833363453565531584D514875376E457A5053537131586B76694838592D554A64314C356772367166356376695366547678537A3671514A66336D563258774B6C687750396B6D484C46484766677A37766230654A594A3351372D77, '2022-12-30 19:41:46', '2022-12-30 21:41:46', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"metadata.token.claims\":{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"sub\":\"admin\",\"aud\":[\"java.util.Collections$SingletonList\",[\"ingot\"]],\"nbf\":[\"java.time.Instant\",1672400506.425000000],\"tat\":\"0\",\"scope\":[\"java.util.HashSet\",[\"role_admin\",\"message.read\",\"basic\",\"message.write\"]],\"iss\":[\"java.net.URL\",\"http://ingot-auth-server:5100\"],\"i\":[\"java.lang.Long\",1],\"dept\":[\"java.lang.Long\",1],\"exp\":[\"java.time.Instant\",1672407706.425000000],\"iat\":[\"java.time.Instant\",1672400506.425000000],\"tenant\":[\"java.lang.Long\",1]},\"metadata.token.invalidated\":false}', 'Bearer', 'message.read,message.write', NULL, NULL, NULL, NULL, 0x6F526C746C4B71733442334451507A6B755A6562587866793535584E484C5F54717847746C536B3330353774415F36576659394B716A7868497A6546526A375F4F5158786F6A3230525461362D73567849543876436A76346F6E695946717176362D6873484E454A3275676E704C494E584E384A54736B596346543254376A79, '2022-12-30 19:41:46', '2023-01-06 19:41:46', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"metadata.token.invalidated\":false}');
COMMIT;

-- ----------------------------
-- Table structure for oauth2_authorization_consent
-- ----------------------------
DROP TABLE IF EXISTS `oauth2_authorization_consent`;
CREATE TABLE `oauth2_authorization_consent` (
  `registered_client_id` varchar(100) NOT NULL,
  `principal_name` varchar(200) NOT NULL,
  `authorities` varchar(1000) NOT NULL,
  PRIMARY KEY (`registered_client_id`,`principal_name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of oauth2_authorization_consent
-- ----------------------------
BEGIN;
INSERT INTO `oauth2_authorization_consent` VALUES ('ingot', 'admin', 'SCOPE_message.read');
COMMIT;

-- ----------------------------
-- Table structure for oauth2_registered_client
-- ----------------------------
DROP TABLE IF EXISTS `oauth2_registered_client`;
CREATE TABLE `oauth2_registered_client` (
  `id` varchar(100) NOT NULL COMMENT 'ID',
  `client_id` varchar(100) NOT NULL COMMENT '客户端ID',
  `client_id_issued_at` datetime NOT NULL COMMENT 'client id 发布时间',
  `client_secret` varchar(200) CHARACTER SET utf8 DEFAULT NULL COMMENT '客户端秘钥',
  `client_secret_expires_at` datetime DEFAULT NULL COMMENT '秘钥过期时间',
  `client_name` varchar(200) CHARACTER SET utf8 NOT NULL COMMENT '客户端名称',
  `client_authentication_methods` varchar(1000) CHARACTER SET utf8 NOT NULL DEFAULT ',' COMMENT '客户端认证方法',
  `authorization_grant_types` varchar(1000) CHARACTER SET utf8 NOT NULL COMMENT '客户端可以使用的授权类型',
  `redirect_uris` varchar(1000) CHARACTER SET utf8 DEFAULT NULL COMMENT '重定向URL',
  `scopes` varchar(1000) CHARACTER SET utf8 NOT NULL COMMENT '客户端的访问范围',
  `client_settings` varchar(2000) CHARACTER SET utf8 NOT NULL COMMENT '客户端设置',
  `token_settings` varchar(2000) CHARACTER SET utf8 NOT NULL COMMENT 'token设置',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`),
  KEY `clientId` (`client_id`) USING BTREE COMMENT '客户端ID',
  KEY `clientName` (`client_name`) USING BTREE COMMENT '客户端名称',
  KEY `clientIdIssuedAt` (`client_id_issued_at`) USING BTREE COMMENT '发布时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of oauth2_registered_client
-- ----------------------------
BEGIN;
INSERT INTO `oauth2_registered_client` VALUES ('ingot', 'ingot', '2020-11-20 15:57:29', '{noop}ingot', NULL, 'Ingot管理平台', 'client_secret_basic', 'client_credentials,password,authorization_code,refresh_token', 'https://ingotcloud.com', 'message.read,message.write', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"settings.client.require-authorization-consent\":true,\"ingot.settings.client.status\":\"0\",\"settings.client.require-proof-key\":false}', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"settings.token.reuse-refresh-tokens\":false,\"settings.token.id-token-signature-algorithm\":[\"org.springframework.security.oauth2.jose.jws.SignatureAlgorithm\",\"RS256\"],\"settings.token.access-token-time-to-live\":[\"java.time.Duration\",7200.000000000],\"ingot.settings.token.auth-type\":\"0\",\"settings.token.access-token-format\":{\"@class\":\"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat\",\"value\":\"self-contained\"},\"settings.token.refresh-token-time-to-live\":[\"java.time.Duration\",604800.000000000],\"settings.token.authorization-code-time-to-live\":[\"java.time.Duration\",300.000000000]}', '2023-02-16 15:13:40', NULL);
COMMIT;

-- ----------------------------
-- Table structure for sys_authority
-- ----------------------------
DROP TABLE IF EXISTS `sys_authority`;
CREATE TABLE `sys_authority` (
  `id` bigint(20) unsigned NOT NULL COMMENT 'ID',
  `tenant_id` bigint(20) unsigned NOT NULL COMMENT '租户ID',
  `pid` bigint(20) unsigned NOT NULL DEFAULT '0' COMMENT '父ID',
  `name` varchar(32) NOT NULL COMMENT '权限名称',
  `code` varchar(128) NOT NULL COMMENT '权限编码',
  `status` char(1) CHARACTER SET utf8 NOT NULL DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `remark` varchar(255) CHARACTER SET utf8 DEFAULT '' COMMENT '备注',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `tenant-code` (`tenant_id`,`code`) USING BTREE COMMENT '编码'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of sys_authority
-- ----------------------------
BEGIN;
INSERT INTO `sys_authority` VALUES (782647310861250562, 1, 0, '基础管理', 'basic', '0', '', '2022-12-18 16:27:01', NULL, NULL);
INSERT INTO `sys_authority` VALUES (782649986701373442, 1, 782647310861250562, '账户管理', 'basic.user', '0', '', '2022-12-18 16:37:39', '2023-02-14 09:25:39', NULL);
INSERT INTO `sys_authority` VALUES (782650056679141377, 1, 782647310861250562, '部门管理', 'basic.dept', '0', '', '2022-12-18 16:37:56', NULL, NULL);
INSERT INTO `sys_authority` VALUES (782650095858135041, 1, 782647310861250562, '角色管理', 'basic.role', '0', '', '2022-12-18 16:38:05', NULL, NULL);
INSERT INTO `sys_authority` VALUES (782650139818635265, 1, 782647310861250562, '租户管理', 'basic.tenant', '0', '', '2022-12-18 16:38:15', NULL, NULL);
INSERT INTO `sys_authority` VALUES (782650179794546690, 1, 782647310861250562, '菜单管理', 'basic.menu', '0', '', '2022-12-18 16:38:25', '2023-02-16 17:38:10', NULL);
INSERT INTO `sys_authority` VALUES (782650237604638722, 1, 782647310861250562, '权限管理', 'basic.authority', '0', '', '2022-12-18 16:38:39', '2022-12-26 21:10:14', NULL);
INSERT INTO `sys_authority` VALUES (782650307704041474, 1, 782647310861250562, '客户端管理', 'basic.client', '0', '', '2022-12-18 16:38:55', '2022-12-27 12:19:35', NULL);
INSERT INTO `sys_authority` VALUES (805471516535599106, 1, 782647310861250562, '社交管理', 'basic.social', '0', '', '2023-02-19 16:02:16', NULL, NULL);
INSERT INTO `sys_authority` VALUES (807933676733902849, 1, 782649986701373442, '读', 'basic.user.read', '0', '', '2023-02-26 11:06:00', NULL, NULL);
INSERT INTO `sys_authority` VALUES (807933724267950081, 1, 782649986701373442, '写', 'basic.user.write', '0', '', '2023-02-26 11:06:12', NULL, NULL);
INSERT INTO `sys_authority` VALUES (807933768714989570, 1, 782650056679141377, '读', 'basic.dept.read', '0', '', '2023-02-26 11:06:22', NULL, NULL);
INSERT INTO `sys_authority` VALUES (807933824746696705, 1, 782650056679141377, '写', 'basic.dept.write', '0', '', '2023-02-26 11:06:36', NULL, NULL);
INSERT INTO `sys_authority` VALUES (807933868778500097, 1, 782650095858135041, '读', 'basic.role.read', '0', '', '2023-02-26 11:06:46', NULL, NULL);
INSERT INTO `sys_authority` VALUES (807933911300354049, 1, 782650095858135041, '写', 'basic.role.write', '0', '', '2023-02-26 11:06:56', NULL, NULL);
INSERT INTO `sys_authority` VALUES (807933967919263745, 1, 782650139818635265, '读', 'basic.tenant.read', '0', '', '2023-02-26 11:07:10', NULL, NULL);
INSERT INTO `sys_authority` VALUES (807934009086357505, 1, 782650139818635265, '写', 'basic.tenant.write', '0', '', '2023-02-26 11:07:20', NULL, NULL);
INSERT INTO `sys_authority` VALUES (807934048638644225, 1, 782650179794546690, '读', 'basic.menu.read', '0', '', '2023-02-26 11:07:29', NULL, NULL);
INSERT INTO `sys_authority` VALUES (807934078883770370, 1, 782650179794546690, '写', 'basic.menu.write', '0', '', '2023-02-26 11:07:36', NULL, NULL);
INSERT INTO `sys_authority` VALUES (807934112199127042, 1, 782650307704041474, '读', 'basic.client.read', '0', '', '2023-02-26 11:07:44', NULL, NULL);
INSERT INTO `sys_authority` VALUES (807934139076227073, 1, 782650307704041474, '写', 'basic.client.write', '0', '', '2023-02-26 11:07:51', NULL, NULL);
INSERT INTO `sys_authority` VALUES (807934166997708802, 1, 805471516535599106, '读', 'basic.social.read', '0', '', '2023-02-26 11:07:57', NULL, NULL);
INSERT INTO `sys_authority` VALUES (807934191819599874, 1, 805471516535599106, '写', 'basic.social.write', '0', '', '2023-02-26 11:08:03', NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
  `id` bigint(20) unsigned NOT NULL COMMENT 'ID',
  `tenant_id` bigint(20) unsigned NOT NULL COMMENT '租户ID',
  `pid` bigint(20) unsigned NOT NULL DEFAULT '0' COMMENT '父ID',
  `name` varchar(50) NOT NULL COMMENT '部门名称',
  `scope` char(1) NOT NULL DEFAULT '0' COMMENT '部门角色范围, 0:当前部门，1:当前部门和直接子部门',
  `sort` int(11) NOT NULL DEFAULT '999' COMMENT '排序',
  `status` char(1) CHARACTER SET utf8 NOT NULL DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `tenant` (`tenant_id`) USING BTREE COMMENT '租户'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of sys_dept
-- ----------------------------
BEGIN;
INSERT INTO `sys_dept` VALUES (1, 1, 0, 'Ingot管理平台', '0', 1, '0', '2022-12-17 15:50:07', '2022-12-20 12:58:43', NULL);
INSERT INTO `sys_dept` VALUES (782235460805898241, 1, 0, '测试部门', '0', 10, '0', '2022-12-17 13:10:28', '2022-12-20 12:59:10', NULL);
INSERT INTO `sys_dept` VALUES (783319588905201665, 1, 782235460805898241, '测试子部门', '0', 10, '0', '2022-12-20 12:58:24', '2022-12-20 12:59:15', NULL);
COMMIT;

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id` bigint(20) unsigned NOT NULL COMMENT 'ID',
  `tenant_id` bigint(20) unsigned NOT NULL COMMENT '租户ID',
  `pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '父ID',
  `name` varchar(32) NOT NULL COMMENT '菜单名称',
  `menu_type` char(1) NOT NULL COMMENT '菜单类型',
  `path` varchar(128) NOT NULL COMMENT '菜单url',
  `authority_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '权限ID',
  `custom_view_path` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否自定义视图路径',
  `view_path` varchar(128) DEFAULT NULL COMMENT '视图路径',
  `route_name` varchar(32) DEFAULT NULL COMMENT '命名路由',
  `redirect` varchar(128) DEFAULT NULL COMMENT '重定向',
  `icon` varchar(64) DEFAULT NULL COMMENT '图标',
  `sort` int(11) NOT NULL DEFAULT '999' COMMENT '排序',
  `is_cache` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否缓存',
  `hidden` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否隐藏',
  `hide_breadcrumb` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否隐藏面包屑',
  `props` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否匹配props',
  `status` char(1) CHARACTER SET utf8 NOT NULL DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `tenant-code` (`tenant_id`,`route_name`) USING BTREE COMMENT '编码'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
BEGIN;
INSERT INTO `sys_menu` VALUES (782579756306313217, 1, 0, '基础管理', '0', '/basic', 782647310861250562, 1, '@/layouts/InAppLayout.vue', NULL, '/basic/user', 'ingot:basic', 10, 0, 0, 0, 0, '0', '2022-12-18 11:58:35', '2022-12-27 20:46:33', NULL);
INSERT INTO `sys_menu` VALUES (782582623704494082, 1, 782579756306313217, '账户管理', '0', '/basic/user', 807933676733902849, 1, '@/layouts/InSimpleLayout.vue', NULL, '/basic/user/home', NULL, 10, 0, 0, 0, 0, '0', '2022-12-18 12:09:58', '2023-02-26 11:34:54', NULL);
INSERT INTO `sys_menu` VALUES (782583360945696769, 1, 782579756306313217, '部门管理', '1', '/basic/dept', 807933768714989570, 0, '@/pages/basic/dept/IndexPage.vue', NULL, NULL, NULL, 12, 0, 0, 0, 0, '0', '2022-12-18 12:12:54', '2023-02-26 11:34:47', NULL);
INSERT INTO `sys_menu` VALUES (782583497235410945, 1, 782579756306313217, '角色管理', '0', '/basic/role', 807933868778500097, 1, '@/layouts/InSimpleLayout.vue', NULL, '/basic/role/home', NULL, 14, 0, 0, 0, 0, '0', '2022-12-18 12:13:27', '2023-02-26 11:33:43', NULL);
INSERT INTO `sys_menu` VALUES (782583633701285889, 1, 782583497235410945, '角色管理', '1', '/basic/role/home', 807933868778500097, 0, '@/pages/basic/role/home/IndexPage.vue', NULL, NULL, NULL, 10, 0, 0, 0, 0, '0', '2022-12-18 12:13:59', '2023-02-26 11:33:51', NULL);
INSERT INTO `sys_menu` VALUES (782584241699205121, 1, 782579756306313217, '租户管理', '1', '/basic/tenant', 807933967919263745, 0, '@/pages/basic/tenant/IndexPage.vue', NULL, NULL, NULL, 16, 0, 0, 0, 0, '0', '2022-12-18 12:16:24', '2023-02-26 11:34:38', NULL);
INSERT INTO `sys_menu` VALUES (782584370229456897, 1, 782579756306313217, '菜单管理', '1', '/basic/menu', 807934048638644225, 0, '@/pages/basic/menu/IndexPage.vue', NULL, NULL, NULL, 18, 0, 0, 0, 0, '0', '2022-12-18 12:16:55', '2023-02-26 11:34:24', NULL);
INSERT INTO `sys_menu` VALUES (782586637955411969, 1, 782579756306313217, '权限管理', '1', '/basic/authority', 782650237604638722, 0, '@/pages/basic/authority/IndexPage.vue', NULL, NULL, NULL, 20, 0, 0, 0, 0, '0', '2022-12-18 12:25:55', '2022-12-27 20:48:59', NULL);
INSERT INTO `sys_menu` VALUES (782587038901514241, 1, 782579756306313217, '客户端管理', '0', '/basic/client', 807934112199127042, 1, '@/layouts/InSimpleLayout.vue', NULL, '/basic/client/home', NULL, 22, 0, 0, 0, 0, '0', '2022-12-18 12:27:31', '2023-02-26 11:34:06', NULL);
INSERT INTO `sys_menu` VALUES (782587211480346625, 1, 782587038901514241, '客户端列表', '1', '/basic/client/home', 807933676733902849, 0, '@/pages/basic/client/home/IndexPage.vue', NULL, NULL, NULL, 10, 0, 0, 0, 0, '0', '2022-12-18 12:28:12', '2023-02-26 11:26:39', NULL);
INSERT INTO `sys_menu` VALUES (782661688801144834, 1, 782587038901514241, '编辑客户端', '1', '/basic/client/details/:id', 807934139076227073, 0, '@/pages/basic/client/details/IndexPage.vue', NULL, NULL, NULL, 20, 0, 1, 0, 1, '0', '2022-12-18 17:24:09', '2023-02-26 11:25:36', NULL);
INSERT INTO `sys_menu` VALUES (784784987182116866, 1, 782582623704494082, '账户列表', '1', '/basic/user/home', 807933676733902849, 0, '@/pages/basic/user/home/IndexPage.vue', NULL, NULL, NULL, 10, 0, 0, 0, 0, '0', '2022-12-24 14:01:23', '2023-02-26 11:26:21', NULL);
INSERT INTO `sys_menu` VALUES (785924904339681281, 1, 782582623704494082, '账户详情', '1', '/basic/user/details/:id', 807933724267950081, 0, '@/pages/basic/user/details/IndexPage.vue', NULL, NULL, NULL, 20, 0, 1, 0, 1, '0', '2022-12-27 17:31:00', '2023-02-26 11:24:42', NULL);
INSERT INTO `sys_menu` VALUES (785927747020828673, 1, 782583497235410945, '绑定部门', '0', '/basic/role/binddept/:id', 807933911300354049, 0, '@/pages/basic/role/binddept/IndexPage.vue', NULL, NULL, NULL, 20, 0, 1, 0, 1, '0', '2022-12-27 17:42:18', '2023-02-26 11:25:02', NULL);
INSERT INTO `sys_menu` VALUES (785927849261182978, 1, 782583497235410945, '绑定权限', '0', '/basic/role/bindauthority/:id', 807933911300354049, 0, '@/pages/basic/role/bindauthority/IndexPage.vue', NULL, NULL, NULL, 30, 0, 1, 0, 1, '0', '2022-12-27 17:42:42', '2023-02-26 11:25:10', NULL);
INSERT INTO `sys_menu` VALUES (785927957314842626, 1, 782583497235410945, '绑定客户端', '0', '/basic/role/bindclient/:id', 807933911300354049, 0, '@/pages/basic/role/bindclient/IndexPage.vue', NULL, NULL, NULL, 40, 0, 1, 0, 1, '0', '2022-12-27 17:43:08', '2023-02-26 11:25:15', NULL);
INSERT INTO `sys_menu` VALUES (805472038554480642, 1, 782579756306313217, '社交管理', '1', '/basic/social', 807934166997708802, 0, '@/pages/basic/social/IndexPage.vue', NULL, NULL, NULL, 30, 0, 0, 0, 0, '0', '2023-02-19 16:04:20', '2023-02-26 11:34:13', NULL);
COMMIT;

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` bigint(20) unsigned NOT NULL COMMENT 'ID',
  `tenant_id` bigint(20) unsigned NOT NULL COMMENT '租户',
  `name` varchar(50) CHARACTER SET utf8 NOT NULL DEFAULT '' COMMENT '角色名称',
  `code` varchar(50) CHARACTER SET utf8 NOT NULL DEFAULT '' COMMENT '角色编码',
  `type` char(1) NOT NULL COMMENT '角色类型',
  `status` char(1) CHARACTER SET utf8 NOT NULL DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `remark` varchar(300) CHARACTER SET utf8 DEFAULT '' COMMENT '备注',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `tenant-code` (`tenant_id`,`code`) USING BTREE COMMENT '编码'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
BEGIN;
INSERT INTO `sys_role` VALUES (1, 1, '超级管理员', 'role_admin', '0', '0', '超级管理员', '2021-01-03 11:07:59', '2022-12-17 18:57:32', NULL);
INSERT INTO `sys_role` VALUES (2, 1, '管理员', 'role_manager', '0', '0', '管理员', '2021-06-23 09:28:19', '2023-02-16 18:10:59', NULL);
INSERT INTO `sys_role` VALUES (3, 1, '用户', 'role_user', '0', '0', '用户', '2021-06-23 09:28:33', '2021-06-23 14:34:07', NULL);
COMMIT;

-- ----------------------------
-- Table structure for sys_role_authority
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_authority`;
CREATE TABLE `sys_role_authority` (
  `role_id` bigint(20) unsigned NOT NULL COMMENT '角色ID',
  `authority_id` bigint(20) unsigned NOT NULL COMMENT '权限ID',
  PRIMARY KEY (`role_id`,`authority_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of sys_role_authority
-- ----------------------------
BEGIN;
INSERT INTO `sys_role_authority` VALUES (1, 782647310861250562);
INSERT INTO `sys_role_authority` VALUES (2, 807933676733902849);
INSERT INTO `sys_role_authority` VALUES (2, 807933768714989570);
INSERT INTO `sys_role_authority` VALUES (2, 807933967919263745);
COMMIT;

-- ----------------------------
-- Table structure for sys_role_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_dept`;
CREATE TABLE `sys_role_dept` (
  `role_id` bigint(20) unsigned NOT NULL COMMENT '角色ID',
  `dept_id` bigint(20) unsigned NOT NULL COMMENT '部门ID',
  PRIMARY KEY (`role_id`,`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for sys_role_oauth_client
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_oauth_client`;
CREATE TABLE `sys_role_oauth_client` (
  `role_id` bigint(20) unsigned NOT NULL COMMENT '角色ID',
  `client_id` varchar(100) NOT NULL COMMENT '客户端ID',
  PRIMARY KEY (`role_id`,`client_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of sys_role_oauth_client
-- ----------------------------
BEGIN;
INSERT INTO `sys_role_oauth_client` VALUES (1, 'ingot');
INSERT INTO `sys_role_oauth_client` VALUES (2, 'ingot');
COMMIT;

-- ----------------------------
-- Table structure for sys_role_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_user`;
CREATE TABLE `sys_role_user` (
  `role_id` bigint(20) unsigned NOT NULL COMMENT '角色ID',
  `user_id` bigint(20) unsigned NOT NULL COMMENT '用户ID',
  PRIMARY KEY (`role_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of sys_role_user
-- ----------------------------
BEGIN;
INSERT INTO `sys_role_user` VALUES (1, 1);
COMMIT;

-- ----------------------------
-- Table structure for sys_social_details
-- ----------------------------
DROP TABLE IF EXISTS `sys_social_details`;
CREATE TABLE `sys_social_details` (
  `id` bigint(20) unsigned NOT NULL COMMENT 'ID',
  `tenant_id` bigint(20) unsigned NOT NULL COMMENT '租户ID',
  `app_id` varchar(64) NOT NULL COMMENT 'App ID',
  `app_secret` varchar(64) DEFAULT NULL COMMENT 'App Secret',
  `redirect_url` varchar(128) DEFAULT NULL COMMENT '重定向地址',
  `name` varchar(20) DEFAULT NULL COMMENT '社交名称',
  `type` varchar(20) DEFAULT NULL COMMENT '类型',
  `status` char(1) CHARACTER SET utf8 DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for sys_tenant
-- ----------------------------
DROP TABLE IF EXISTS `sys_tenant`;
CREATE TABLE `sys_tenant` (
  `id` bigint(20) unsigned NOT NULL COMMENT 'ID',
  `name` varchar(255) NOT NULL COMMENT '租户名称',
  `code` varchar(64) NOT NULL COMMENT '租户编号',
  `start_at` datetime DEFAULT NULL COMMENT '开始日期',
  `end_at` datetime DEFAULT NULL COMMENT '结束日期',
  `status` char(1) CHARACTER SET utf8 NOT NULL DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of sys_tenant
-- ----------------------------
BEGIN;
INSERT INTO `sys_tenant` VALUES (1, 'Ingot管理平台', 'ingot', NULL, NULL, '0', '2021-01-06 13:48:26', '2023-02-16 17:58:46', NULL);
COMMIT;

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint(20) unsigned NOT NULL COMMENT 'ID',
  `tenant_id` bigint(20) unsigned NOT NULL COMMENT '所属租户',
  `dept_id` bigint(20) unsigned NOT NULL DEFAULT '0' COMMENT '部门ID',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '用户名',
  `password` varchar(300) CHARACTER SET utf8 NOT NULL COMMENT '密码',
  `init_pwd` tinyint(1) NOT NULL DEFAULT '1' COMMENT '初始化密码标识',
  `nickname` varchar(32) CHARACTER SET utf8 DEFAULT NULL COMMENT '昵称',
  `phone` varchar(32) CHARACTER SET utf8 DEFAULT NULL COMMENT '手机号',
  `email` varchar(64) CHARACTER SET utf8 DEFAULT NULL COMMENT '邮件地址',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
  `status` char(1) CHARACTER SET utf8 NOT NULL DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `tenant-username` (`tenant_id`,`username`) USING BTREE COMMENT '用户名',
  KEY `tenant-phone` (`tenant_id`,`phone`) USING BTREE COMMENT '手机号',
  KEY `tenant-email` (`tenant_id`,`email`) USING BTREE COMMENT '邮箱'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
BEGIN;
INSERT INTO `sys_user` VALUES (1, 1, 1, 'admin', '{bcrypt}$2a$10$un6vyGjHLthh007s5qtHFe56doYCkA8BeEAkJZxQG67pPHjN75B76', 0, '超级管理员', '88888888888', 'admin@ingot.com', NULL, '0', '2021-01-03 11:02:46', '2022-12-21 17:35:38', NULL);
COMMIT;

-- ----------------------------
-- Table structure for sys_user_social
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_social`;
CREATE TABLE `sys_user_social` (
  `id` bigint(20) NOT NULL COMMENT 'ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `type` varchar(20) NOT NULL COMMENT '渠道类型',
  `unique_id` varchar(32) NOT NULL COMMENT '渠道唯一ID',
  `bind_at` datetime NOT NULL COMMENT '绑定时间',
  PRIMARY KEY (`id`),
  KEY `unique-type-user` (`unique_id`,`type`,`user_id`) USING BTREE COMMENT '渠道用户索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
