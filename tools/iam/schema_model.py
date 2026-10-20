"""解析 manifest 的 CREATE TABLE，保留完整列定义；不连接数据库。"""
from dataclasses import dataclass
import re
from database_sources import sources, read_source

COMMENT = re.compile(r"\s+COMMENT\s*(?:=\s*)?'(?:''|\\.|[^'\\])*'", re.I)
CREATE = re.compile(r'CREATE TABLE\s+(?:IF NOT EXISTS\s+)?`?([a-z_]+)`?\s*\(', re.I)
COLUMN = re.compile(r'^`?([a-z_][a-z_0-9]*)`?\s+(?:boolean|bigint|int|tinyint|smallint|varchar|char|datetime|timestamp|json|text|longtext|mediumtext|bit|decimal|blob|varbinary)\b', re.I)


def closing(text, start):
    """寻找 SQL 括号配对，忽略字符串及反引号内的符号。"""
    depth, quote, i = 0, None, start
    while i < len(text):
        char = text[i]
        if quote:
            if char == '\\':
                i += 2
                continue
            if char == quote:
                if i + 1 < len(text) and text[i + 1] == quote:
                    i += 2
                    continue
                quote = None
        elif char in "'\"`":
            quote = char
        elif char == '(':
            depth += 1
        elif char == ')':
            depth -= 1
            if depth == 0:
                return i
        i += 1
    raise ValueError('Unbalanced SQL')


def parts(body):
    """按顶层逗号分割字段与约束，不拆分 JSON/生成表达式。"""
    result, start, i = [], 0, 0
    while i < len(body):
        char = body[i]
        if char == '(':
            i = closing(body, i)
        elif char in "'\"`":
            quote = char
            i += 1
            while i < len(body):
                if body[i] == '\\':
                    i += 2
                    continue
                if body[i] == quote:
                    if i + 1 < len(body) and body[i + 1] == quote:
                        i += 2
                        continue
                    break
                i += 1
        elif char == ',':
            result.append(body[start:i].strip())
            start = i + 1
        i += 1
    result.append(body[start:].strip())
    return result


def without_sql_comments(text):
    """权威脚本的行注释只用于排版，不属于列定义。"""
    return re.sub(r'(?m)^\s*--[^\n]*\n?', '', text)


def comment(definition):
    """返回 COMMENT 字符串的内容。"""
    match = COMMENT.search(definition)
    return match.group().split("'", 1)[1][:-1].replace("''", "'") if match else ''


def quote(value):
    """转义 SQL 字符串。"""
    return "'" + value.replace('\\', '\\\\').replace("'", "''") + "'"


@dataclass
class Table:
    """保留权威 CREATE 的列、约束和表选项。"""
    name: str
    source: object
    definitions: list
    options: str
    additional_constraints: list

    @property
    def columns(self):
        return {COLUMN.match(x).group(1): x for x in self.definitions if COLUMN.match(x)}

    @property
    def constraints(self):
        return [x for x in self.definitions if not COLUMN.match(x)] + self.additional_constraints


def tables():
    """按 manifest 顺序枚举 IAM 与框架共库表。"""
    result = []
    for path in sources('schemas') + sources('frameworkSchemas'):
        sql = without_sql_comments(read_source(path))
        for match in CREATE.finditer(sql):
            end = closing(sql, match.end() - 1)
            stop = sql.index(';', end)
            additional = re.findall(r'ALTER TABLE\s+`?' + re.escape(match.group(1)) + r'`?\s+ADD\s+([^;]+);', sql, re.I)
            result.append(Table(match.group(1), path, parts(sql[match.end():end]), sql[end + 1:stop], additional))
    return result
