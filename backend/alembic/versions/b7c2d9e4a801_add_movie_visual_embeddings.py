"""add movie visual embeddings

Revision ID: b7c2d9e4a801
Revises: 5c9350d8a590
Create Date: 2026-05-02 14:13:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from pgvector.sqlalchemy import Vector


# revision identifiers, used by Alembic.
revision: str = "b7c2d9e4a801"
down_revision: Union[str, Sequence[str], None] = "5c9350d8a590"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    op.execute("CREATE EXTENSION IF NOT EXISTS vector")

    op.create_table(
        "movie_visual_embeddings",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("movie_id", sa.Integer(), nullable=False),
        sa.Column("poster_url", sa.Text(), nullable=False),
        sa.Column("embedding", Vector(512), nullable=False),
        sa.Column("updated_at", sa.DateTime(), nullable=True),
        sa.ForeignKeyConstraint(["movie_id"], ["movies.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("movie_id"),
    )
    op.create_index(op.f("ix_movie_visual_embeddings_id"), "movie_visual_embeddings", ["id"], unique=False)
    op.create_index(op.f("ix_movie_visual_embeddings_movie_id"), "movie_visual_embeddings", ["movie_id"], unique=False)
    op.execute(
        "CREATE INDEX ix_movie_visual_embeddings_embedding_hnsw "
        "ON movie_visual_embeddings "
        "USING hnsw (embedding vector_cosine_ops)"
    )


def downgrade() -> None:
    """Downgrade schema."""
    op.execute("DROP INDEX IF EXISTS ix_movie_visual_embeddings_embedding_hnsw")
    op.drop_index(op.f("ix_movie_visual_embeddings_movie_id"), table_name="movie_visual_embeddings")
    op.drop_index(op.f("ix_movie_visual_embeddings_id"), table_name="movie_visual_embeddings")
    op.drop_table("movie_visual_embeddings")
