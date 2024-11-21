import json
import os
from fastapi import HTTPException
import httpx
from firebase_admin import auth

# Baca API Key dari file apiKey.json
current_dir = os.path.dirname(os.path.abspath(__file__))
api_key_path = os.path.join(current_dir, "apiKey.json")

with open(api_key_path, "r") as file:
    api_key_data = json.load(file)

FIREBASE_WEB_API_KEY = api_key_data["FIREBASE_WEB_API_KEY"]

# Firebase REST API endpoint
FIREBASE_SIGNUP_URL = f"https://identitytoolkit.googleapis.com/v1/accounts:signUp?key={FIREBASE_WEB_API_KEY}"
FIREBASE_LOGIN_URL = f"https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key={FIREBASE_WEB_API_KEY}"
FIREBASE_EMAIL_VERIFICATION_URL = f"https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key={FIREBASE_WEB_API_KEY}"

async def register_user(email: str, password: str):
    """
    Mendaftarkan pengguna baru menggunakan Firebase REST API.
    """
    payload = {
        "email": email,
        "password": password,
        "returnSecureToken": True
    }

    async with httpx.AsyncClient() as client:
        response = await client.post(FIREBASE_SIGNUP_URL, json=payload)

    if response.status_code == 200:
        data = response.json()
        return {
            "message": "User registered successfully",
            "idToken": data["idToken"],
            "refreshToken": data["refreshToken"],
            "expiresIn": data["expiresIn"]
        }
    else:
        error_message = response.json().get("error", {}).get("message", "Unknown error")
        raise HTTPException(status_code=400, detail=f"Registration failed: {error_message}")

async def login_user(email: str, password: str):
    """
    Login pengguna menggunakan Firebase REST API.
    """
    payload = {
        "email": email,
        "password": password,
        "returnSecureToken": True
    }

    async with httpx.AsyncClient() as client:
        response = await client.post(FIREBASE_LOGIN_URL, json=payload)

    if response.status_code == 200:
        data = response.json()
        return {
            "message": "Login successful",
            "idToken": data["idToken"],
            "refreshToken": data["refreshToken"],
            "expiresIn": data["expiresIn"]
        }
    else:
        error_message = response.json().get("error", {}).get("message", "Unknown error")
        raise HTTPException(status_code=401, detail=f"Login failed: {error_message}")

def verify_id_token(id_token: str):
    """
    Verifikasi Firebase ID Token menggunakan Firebase Admin SDK.
    """
    try:
        decoded_token = auth.verify_id_token(id_token)
        print(f"Decoded Token: {decoded_token}")
        return decoded_token
    except Exception as e:
        print(f"Error verifying token: {e}")
        raise HTTPException(status_code=401, detail=f"Token verification failed: {e}")
    
async def send_verification_email(id_token: str):
    """
    Mengirim email verifikasi ke pengguna menggunakan Firebase REST API.
    """
    payload = {
        "requestType": "VERIFY_EMAIL",
        "idToken": id_token
    }

    async with httpx.AsyncClient() as client:
        response = await client.post(FIREBASE_EMAIL_VERIFICATION_URL, json=payload)

    if response.status_code == 200:
        return {"message": "Verification email sent successfully."}
    else:
        error_message = response.json().get("error", {}).get("message", "Unknown error")
        raise HTTPException(status_code=400, detail=f"Failed to send verification email: {error_message}")

def check_email_verification(id_token: str):
    """
    Memeriksa apakah email pengguna sudah terverifikasi.
    """
    try:
        # Verifikasi token menggunakan Firebase Admin SDK
        decoded_token = auth.verify_id_token(id_token)
        email_verified = decoded_token.get("email_verified", False)
        if email_verified:
            return {"message": "Email has been verified."}
        else:
            return {"message": "Email has not been verified."}
    except Exception as e:
        raise HTTPException(status_code=401, detail=f"Error verifying email: {e}")