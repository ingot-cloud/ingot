"""Read frozen pre-021 role schemas so migration tests also work after committing 021.

The three changed schemas were captured from 5865e4cf on 2026-10-19.
Identity and auxiliary schemas use the current manifest sources.
"""
from pathlib import Path

LEGACY_SCHEMAS = Path(__file__).parent / 'fixtures' / 'pre_021'


def read_legacy_schema(path):
    """Return the frozen pre-021 schema where this change extends the structure."""
    legacy = LEGACY_SCHEMAS / path.name
    return (legacy if legacy.exists() else path).read_text()
