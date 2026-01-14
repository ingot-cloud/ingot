create user 'dev'@'%' identified by '123456';
grant all privileges on *.* to 'dev'@'%' with grant option;

create user 'nacos_dev'@'%' identified by '123456';
grant all privileges on ingot_nacos_config.* to 'nacos_dev'@'%' with grant option;

FLUSH PRIVILEGES;