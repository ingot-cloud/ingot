create user 'dev'@'%' identified by '123456';
grant all privileges on *.* to 'dev'@'%' with grant option;
FLUSH PRIVILEGES;