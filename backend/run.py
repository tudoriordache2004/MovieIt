import uvicorn

if __name__ == "__main__":
    # Setez ip-ul la 0.0.0.0 pt localhost ca sa fie accesat de Waydroid
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)