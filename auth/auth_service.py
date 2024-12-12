import json
import httpx
from firebase_admin import auth
from config.utils import get_secret
import os

api_key_data = json.loads(get_secret("api-key"))
FIREBASE_WEB_API_KEY = api_key_data["FIREBASE_WEB_API_KEY"]

FIREBASE_SIGNUP_URL = f"https://identitytoolkit.googleapis.com/v1/accounts:signUp?key={FIREBASE_WEB_API_KEY}"
FIREBASE_LOGIN_URL = f"https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key={FIREBASE_WEB_API_KEY}"
FIREBASE_REFRESH_TOKEN_URL = f"https://securetoken.googleapis.com/v1/token?key={FIREBASE_WEB_API_KEY}"

async def register_user(email: str, password: str):
    payload = {
        "email": email.strip(),
        "password": password.strip(),
        "returnSecureToken": True
    }
    async with httpx.AsyncClient() as client:
        response = await client.post(FIREBASE_SIGNUP_URL, json=payload)
        if response.status_code == 200:
            return response.json()
        else:
            error_message = response.json().get("error", {}).get("message", "Unknown error")
            raise ValueError(f"Registration failed: {error_message}")

async def login_user(email: str, password: str):
    payload = {
        "email": email.strip(),
        "password": password.strip(),
        "returnSecureToken": True
    }
    async with httpx.AsyncClient() as client:
        response = await client.post(FIREBASE_LOGIN_URL, json=payload)
        if response.status_code == 200:
            data = response.json()
            return {
                "idToken": data.get("idToken"),
                "refreshToken": data.get("refreshToken"),
                "expiresIn": data.get("expiresIn")
            }
        else:
            error_message = response.json().get("error", {}).get("message", "Unknown error")
            raise ValueError(f"Login failed: {error_message}")

async def refresh_user_token(refreshToken: str):
    payload = {
        "grant_type": "refresh_token",
        "refresh_token": refreshToken
    }
    
    async with httpx.AsyncClient() as client:
        response = await client.post(FIREBASE_REFRESH_TOKEN_URL, json=payload)
        
        if response.status_code == 200:
            data = response.json()
            return {
                "idToken": data.get("id_token"),
                "refreshToken": data.get("refresh_token"),
                "expiresIn": data.get("expires_in")
            }
        else:
            error_message = response.json().get("error", {}).get("message", "Unknown error")
            raise ValueError(f"Token refresh failed: {error_message}")

async def logout_user(idToken: str):
    """
    Logout pengguna dengan mencabut semua token refresh menggunakan Firebase Admin SDK.
    """
    try:
        # Verifikasi ID token untuk mendapatkan UID pengguna
        decoded_token = auth.verify_id_token(idToken, check_revoked=False)
        uid = decoded_token.get("uid")

        # Cabut semua refresh token untuk UID pengguna
        auth.revoke_refresh_tokens(uid)

        # Mendapatkan informasi pengguna setelah pencabutan
        user = auth.get_user(uid)
        revocation_second = user.tokens_valid_after_timestamp / 1000

        print(f"Tokens revoked at: {revocation_second}")  # Debugging

        return {"message": "Logout successful"}
    except auth.InvalidIdTokenError:
        raise ValueError("Invalid ID Token")
    except auth.RevokedIdTokenError:
        raise ValueError("Token already revoked")
    except Exception as e:
        raise Exception(f"An unexpected error occurred: {str(e)}")

def verify_id_token(id_token: str):
    try:
        return auth.verify_id_token(id_token)
    except Exception as e:
        raise ValueError(f"Token verification failed: {str(e)}")