"""diary entries

Revision ID: 59921fc454c0
Revises: f30fd768dd9c
Create Date: 2026-02-11 15:03:04.498686

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '59921fc454c0'
down_revision: Union[str, Sequence[str], None] = 'f30fd768dd9c'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "diary_entries",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("user_id", sa.Integer(), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("movie_id", sa.Integer(), sa.ForeignKey("movies.id", ondelete="CASCADE"), nullable=False),
        sa.Column("watched_on", sa.Date(), nullable=False),
        sa.Column("created_at", sa.TIMESTAMP(), server_default=sa.text("now()"), nullable=False),
    )

    op.create_index("ix_diary_entries_user_id", "diary_entries", ["user_id"])
    op.create_index("ix_diary_entries_movie_id", "diary_entries", ["movie_id"])
    op.create_index("ix_diary_entries_watched_on", "diary_entries", ["watched_on"])


def downgrade() -> None:
    """Downgrade schema."""
    pass
