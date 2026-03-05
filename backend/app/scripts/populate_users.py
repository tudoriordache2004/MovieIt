# backend/app/scripts/seed_users.py
import sys
from pathlib import Path

backend_path = Path(__file__).resolve().parent.parent.parent
sys.path.insert(0, str(backend_path))

from sqlalchemy.orm import Session
from app.database import SessionLocal
from app.models.user import User
from app.routers.auth import hash_password  # folosește exact același bcrypt ca la register


USERS = [
    # username, email, password, role
    ("User1", "user1@gmail.com", "pass1234", "user"),
    ("User2", "user2@gmail.com", "pass1234", "user"),
    ("Mod1", "mod1@gmail.com", "pass1234", "mod"),
    ("Mod2", "mod2@gmail.com", "pass1234", "mod"),
    ("Admin1", "admin1@gmail.com", "pass1234", "admin"),
    ("Admin2", "admin2@gmail.com", "pass1234", "admin"),
]

def upsert_user(db: Session, username: str, email: str, password: str, role: str):
    existing = db.query(User).filter((User.email == email) | (User.username == username)).first()
    pwd_hash = hash_password(password)

    if existing:
        existing.email = email
        existing.username = username
        existing.password_hash = pwd_hash
        existing.role = role
        db.commit()
        db.refresh(existing)
        print(f"✓ Updated {username} ({role})")
        return

    u = User(email=email, username=username, password_hash=pwd_hash, role=role)
    db.add(u)
    db.commit()
    db.refresh(u)
    print(f"✓ Created {username} ({role})")

def main():
    db: Session = SessionLocal()
    try:
        for username, email, password, role in USERS:
            upsert_user(db, username, email, password, role)
    finally:
        db.close()

if __name__ == "__main__":
    main()