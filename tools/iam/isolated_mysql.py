"""仅运行本地已有镜像的无网络临时 MySQL；不连接已有库。"""
import subprocess
import time
import uuid


class IsolatedMysql:
    """上下文退出时清理容器；无端口、无卷、无网络、不拉镜像。"""
    def __init__(self, image='mysql:8.4'):
        self.image = image
        self.container = None

    def __enter__(self):
        self.container = subprocess.run(
            ['docker', 'run', '--detach', '--rm', '--pull=never', '--network=none',
             '--name', 'ingot-iam-schema-' + uuid.uuid4().hex,
             '--env', 'MYSQL_ALLOW_EMPTY_PASSWORD=yes', self.image,
             '--innodb-buffer-pool-size=64M', '--performance-schema=OFF', '--max-connections=20'],
            text=True, capture_output=True, check=True, timeout=30).stdout.strip()
        try:
            deadline = time.monotonic() + 60
            while time.monotonic() < deadline:
                ready = subprocess.run(['docker', 'exec', self.container, 'sh', '-c',
                                        'test "$(cat /proc/1/comm)" = mysqld && mysqladmin -uroot ping --silent'],
                                       capture_output=True, timeout=5)
                if ready.returncode == 0:
                    return self
                time.sleep(1)
            raise RuntimeError('Isolated MySQL startup timeout')
        except BaseException:
            self.__exit__(None, None, None)
            raise

    def sql(self, statement, database=None):
        command = ['docker', 'exec', '--interactive', self.container, 'mysql', '--user=root',
                   '--batch', '--raw', '--skip-column-names', '--default-character-set=utf8mb4']
        if database:
            command.append('--database=' + database)
        result = subprocess.run(command, input=statement, capture_output=True, text=True, timeout=90)
        if result.returncode:
            raise RuntimeError(result.stderr)
        return result.stdout

    def __exit__(self, *args):
        if self.container:
            subprocess.run(['docker', 'stop', '--time', '10', self.container],
                           capture_output=True, check=True, timeout=20)
            self.container = None
