-- 回退下载中心任务表；会丢弃任务记录，对象存储中的文件需按 bucket 前缀另行清理。
DROP TABLE IF EXISTS iam_download_task;
