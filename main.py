import os
import tempfile
import json
import logging
from fastapi import FastAPI, HTTPException, Header, Body
from fastapi.openapi.utils import get_openapi
from auth.auth_service import (
    register_user,
    login_user,
    verify_id_token,
    logout_user,
    refresh_user_token
)
from health.models import HealthData, UserPrediction
from health.prediction_service import save_user_prediction, get_user_health_data
from health.health_data_service import save_health_data, get_ordered_health_data
from services.model_service import predict_bmi_bmr
from services.gcs_service import download_models
from health.text_generation_service import (
    get_user_data, generate_prompt,
    generate_text_with_vertexai,
    parse_meal_plan_response,
    save_separate_meal_plans_to_firestore
)
import firebase_admin
from firebase_admin import credentials

# Inisialisasi aplikasi FastAPI
app = FastAPI()

PROJECT_ID = "sleek-backend"
logging.basicConfig(level=logging.INFO)

@app.on_event("startup")
async def startup_event():
    """
    Unduh model dari Google Cloud Storage sebelum aplikasi berjalan.
    """
    try:
        print("Downloading models...")
        download_models()
        print("Models downloaded successfully.")
    except Exception as e:
        raise RuntimeError(f"Failed to initialize models: {e}")

@app.post("/register")
async def register(email: str = Body(...), password: str = Body(...)):
    try:
        result = await register_user(email.strip(), password.strip())
        return result
    except HTTPException as e:
        raise e
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/login")
async def login(email: str = Body(...), password: str = Body(...)):
    try:
        result = await login_user(email.strip(), password.strip())
        return result
    except HTTPException as e:
        raise e
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/healthData")
async def submit_health_data(payload: dict = Body(...), authorization: str = Header(None)):
    if not authorization:
        raise HTTPException(status_code=401, detail="Authorization header missing")
    try:
        id_token = authorization.split("Bearer ")[-1]
        decoded_token = verify_id_token(id_token)
        uid = decoded_token["uid"]

        health_data = HealthData(**payload)
        save_health_data(uid, health_data)
        return {"message": "Health data saved successfully"}
    except HTTPException as e:
        raise e
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/predict")
async def predict(authorization: str = Header(None)):
    if not authorization:
        raise HTTPException(status_code=401, detail="Authorization header missing")
    try:
        # Verifikasi token
        id_token = authorization.split("Bearer ")[-1]
        decoded_token = verify_id_token(id_token)
        uid = decoded_token["uid"]

        # Ambil data kesehatan yang sudah difilter
        input_data = get_ordered_health_data(uid)
        print(f"Filtered and ordered input data: {input_data}")

        # Validasi dan lakukan prediksi
        result = predict_bmi_bmr(input_data)
        print(f"Prediction result: {result}")

        # Simpan hasil prediksi
        user_prediction = UserPrediction(**result)
        save_user_prediction(uid, user_prediction)

        return result
    except HTTPException as he:
        raise he
    except Exception as e:
        print(f"Unexpected error during prediction: {e}")
        raise HTTPException(status_code=500, detail="An error occurred during prediction.")

@app.get("/mealPlan")
async def generate_meal_plan(authorization: str = Header(None)):
    logging.info("Starting /mealPlan endpoint")

    if not authorization:
        logging.error("Authorization header missing")
        raise HTTPException(status_code=401, detail="Authorization header missing")

    try:
        # Extract the user ID from the authorization token
        id_token = authorization.split("Bearer ")[-1].strip()
        decoded_token = verify_id_token(id_token)
        uid = decoded_token.get("uid")

        if not uid:
            logging.error("Failed to decode UID from token")
            raise HTTPException(status_code=401, detail="Invalid authorization token")

        logging.info(f"Decoded UID: {uid}")

        # Fetch user data from Firestore
        health_data, user_prediction = get_user_data(uid)

        # Generate the prompt
        prompt = generate_prompt(health_data, user_prediction)

        # Generate text using Vertex AI
        raw_response = generate_text_with_vertexai(prompt)

        # Parse the raw response to extract meal plans
        meal_plans = parse_meal_plan_response(raw_response)

        # Save each meal plan variation as a separate document in Firestore
        save_separate_meal_plans_to_firestore(uid, meal_plans)

        # Return the parsed meal plans as a response
        return {"mealPlans": meal_plans}

    except ValueError as ve:
        logging.error(f"Value error: {ve}")
        raise HTTPException(status_code=400, detail=f"Value error: {ve}")
    except RuntimeError as re:
        logging.error(f"Runtime error: {re}")
        raise HTTPException(status_code=500, detail=f"Runtime error: {re}")
    except Exception as e:
        logging.error(f"Unexpected error: {e}")
        raise HTTPException(status_code=500, detail=f"Unexpected error: {e}")

@app.post("/refresh")
async def refresh_token(payload: dict = Body(...)):
    """
    Endpoint to refresh the user's ID token using the refresh token.
    """
    try:
        # Extract `refreshToken` from the payload and map it to `refresh_token`
        refresh_token = payload.get("refreshToken")
        if not refresh_token:
            raise HTTPException(status_code=422, detail="Field 'refreshToken' is required")

        # Call the token refresh logic
        refreshed_token = await refresh_user_token(refresh_token)

        return refreshed_token
    except ValueError as ve:
        logging.error(f"Value error: {ve}")
        raise HTTPException(status_code=400, detail=f"Value error: {ve}")
    except Exception as e:
        logging.error(f"Unexpected error during token refresh: {e}")
        raise HTTPException(status_code=500, detail=f"Unexpected error: {e}")

@app.post("/logout")
async def logout(payload: dict = Body(...)):
    """
    Endpoint untuk logout pengguna.
    """
    try:
        # Extract `idToken` from the payload
        id_token = payload.get("idToken")
        if not id_token:
            raise HTTPException(status_code=422, detail="Field 'idToken' is required")

        # Call the logout logic
        result = await logout_user(id_token.strip())

        return result
    except ValueError as e:
        logging.error(f"Value error during logout: {e}")
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        logging.error(f"Unexpected error during logout: {e}")
        raise HTTPException(status_code=500, detail=str(e))

def custom_openapi():
    if app.openapi_schema:
        return app.openapi_schema
    openapi_schema = get_openapi(
        title="Your API",
        version="1.0.0",
        description="This is a protected API that uses Firebase ID Tokens",
        routes=app.routes,
    )
    openapi_schema["components"]["securitySchemes"] = {
        "bearerAuth": {
            "type": "http",
            "scheme": "bearer",
            "bearerFormat": "JWT",
        }
    }
    for path in openapi_schema["paths"]:
        for method in openapi_schema["paths"][path]:
            openapi_schema["paths"][path][method]["security"] = [{"bearerAuth": []}]
    app.openapi_schema = openapi_schema
    return app.openapi_schema

app.openapi = custom_openapi
