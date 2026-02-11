"""add user role

Revision ID: f30fd768dd9c
Revises: 4e3242d38544
Create Date: 2026-02-11 13:39:53.472833

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'f30fd768dd9c'
down_revision: Union[str, Sequence[str], None] = '4e3242d38544'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column("users", sa.Column("role", sa.String(length=10), nullable=False, server_default="user"))

def downgrade() -> None:
    op.drop_column("users", "role")
