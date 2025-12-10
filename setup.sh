#!/usr/bin/env bash
set -euo pipefail

# Configurabil
PYTHON_BIN="${PYTHON_BIN:-python3}"
VENV_DIR="${VENV_DIR:-.venv}"

echo "[1/2] Creating virtualenv at ${VENV_DIR}"
${PYTHON_BIN} -m venv "${VENV_DIR}"

echo "[2/2] Activating and installing deps"
source "${VENV_DIR}/bin/activate"
pip install --upgrade pip
pip install fastapi uvicorn[standard] sqlalchemy psycopg2-binary alembic pydantic

echo "Done. Activate anytime with: source ${VENV_DIR}/bin/activate"