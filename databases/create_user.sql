-- ingot user
create user 'ingot_dev'@'%' identified by '123456';
grant all privileges on ingot_core.* to 'ingot_dev'@'%' with grant option;

-- nacos user
create user 'nacos_dev'@'%' identified by '123456';
grant all privileges on ingot_nacos_config.* to 'nacos_dev'@'%' with grant option;

