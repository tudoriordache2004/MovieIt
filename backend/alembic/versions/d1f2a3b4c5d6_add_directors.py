"""add directors

Revision ID: d1f2a3b4c5d6
Revises: 8c4aaa49a47f
Create Date: 2026-05-03 21:42:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = "d1f2a3b4c5d6"
down_revision: Union[str, Sequence[str], None] = "8c4aaa49a47f"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "directors",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("tmdb_id", sa.Integer(), nullable=False),
        sa.Column("name", sa.String(length=255), nullable=False),
        sa.Column("biography", sa.Text(), nullable=True),
        sa.Column("profile_url", sa.Text(), nullable=True),
        sa.Column("birthday", sa.Date(), nullable=True),
        sa.Column("deathday", sa.Date(), nullable=True),
        sa.Column("place_of_birth", sa.String(length=255), nullable=True),
        sa.Column("created_at", sa.TIMESTAMP(), nullable=True),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("tmdb_id"),
    )
    op.create_index(op.f("ix_directors_id"), "directors", ["id"], unique=False)
    op.create_index(op.f("ix_directors_tmdb_id"), "directors", ["tmdb_id"], unique=False)
    op.create_index(op.f("ix_directors_name"), "directors", ["name"], unique=False)

    op.create_table(
        "movie_directors",
        sa.Column("movie_id", sa.Integer(), nullable=False),
        sa.Column("director_id", sa.Integer(), nullable=False),
        sa.ForeignKeyConstraint(["movie_id"], ["movies.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["director_id"], ["directors.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("movie_id", "director_id"),
    )


def downgrade() -> None:
    op.drop_table("movie_directors")
    op.drop_index(op.f("ix_directors_name"), table_name="directors")
    op.drop_index(op.f("ix_directors_tmdb_id"), table_name="directors")
    op.drop_index(op.f("ix_directors_id"), table_name="directors")
    op.drop_table("directors")