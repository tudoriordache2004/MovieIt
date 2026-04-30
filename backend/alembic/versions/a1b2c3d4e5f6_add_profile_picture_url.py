"""add profile_picture_url to users

Revision ID: a1b2c3d4e5f6
Revises: 10890036401e
Create Date: 2026-04-30 12:00:00.000000

"""
from typing import Sequence, Union
from alembic import op
import sqlalchemy as sa

revision: str = 'a1b2c3d4e5f6'
down_revision: Union[str, Sequence[str], None] = '10890036401e'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column("users", sa.Column("profile_picture_url", sa.String(length=500), nullable=True))


def downgrade() -> None:
    op.drop_column("users", "profile_picture_url")
