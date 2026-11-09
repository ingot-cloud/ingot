"""Read frozen pre-021 role schemas so migration tests also work after committing 021.

The three changed schemas and their compatible bootstrap are captured from 5865e4cf.
Identity and auxiliary schemas use the current manifest sources.
"""
from pathlib import Path
from tools.iam.database_sources import legacy_schema_files, sources, read_source
from tools.iam.historical_schema import historical_names

LEGACY_SCHEMAS = Path(__file__).parent / 'fixtures' / 'pre_021'


def read_legacy_schema(path):
    """Return the frozen pre-021 schema where this change extends the structure."""
    legacy = LEGACY_SCHEMAS / path.name
    if legacy.exists():
        return legacy.read_text()
    content = historical_names(path.read_text())
    if path.name == '005_auxiliary.sql':
        content += '\n' + '\n'.join(historical_names(read_source(source))
                                      for source in sources('frameworkSchemas'))
        # 旧环境仍带历史套餐事实；新项目默认初始化不再创建该表。
        content += '\n' + next(source for source in legacy_schema_files()
                              if source.name == '003_tenant_plan_record.sql').read_text()
    return content
