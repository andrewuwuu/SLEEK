from fastapi import FastAPI, HTTPException, Depends, Header, Body
from auth.service import register_user, login_user, verify_id_token, send_verification_email, check_email_verification
from health.models import HealthData
from health.service import save_health_data
from fastapi.openapi.utils import get_openapi

app = FastAPI()

@app.post("/register")
async def register(email: str = Body(...), password: str = Body(...)):
    """
    Endpoint untuk registrasi pengguna.
    """
    try:
        result = await register_user(email, password)
        return result
    except HTTPException as e:
        raise e
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/login")
async def login(email: str = Body(...), password: str = Body(...)):
    """
    Endpoint untuk login pengguna.
    """
    try:
        result = await login_user(email, password)
        return result
    except HTTPException as e:
        raise e
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/protected")
async def protected_route(authorization: str = Header(None)):
    """
    Endpoint terproteksi yang memverifikasi Firebase ID Token.
    """
    if not authorization:
        raise HTTPException(status_code=401, detail="Authorization header missing")
    try:
        # Ekstrak ID Token dari header
        id_token = authorization.split("Bearer ")[-1]
        decoded_token = verify_id_token(id_token)
        return {"message": f"Hello, your UID is {decoded_token['uid']}"}
    except HTTPException as e:
        raise e
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/healthData")
async def submit_health_data(
    health_data: HealthData, 
    authorization: str = Header(None)
):
    """
    Endpoint untuk menyimpan data kesehatan pengguna.
    """
    if not authorization:
        raise HTTPException(status_code=401, detail="Authorization header missing")
    try:
        # Verifikasi token dan dapatkan UID pengguna
        id_token = authorization.split("Bearer ")[-1]
        decoded_token = verify_id_token(id_token)
        uid = decoded_token["uid"]

        # Simpan data kesehatan
        return save_health_data(uid, health_data)
    except HTTPException as e:
        raise e
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    
from fastapi.openapi.utils import get_openapi

@app.post("/send-email-verification")
async def send_email_verification_endpoint(body: dict = Body(...)):
    """
    Endpoint untuk mengirim email verifikasi ke pengguna.
    """
    id_token = body.get("id_token")  # Ambil id_token dari JSON object
    if not id_token:
        raise HTTPException(status_code=400, detail="Missing 'id_token'")
    return await send_verification_email(id_token)

@app.get("/check-email-verification")
async def check_email_verification_endpoint(authorization: str = Header(None)):
    """
    Endpoint untuk memeriksa apakah email pengguna sudah terverifikasi.
    """
    if not authorization:
        raise HTTPException(status_code=401, detail="Authorization header missing")
    try:
        # Ekstrak ID Token dari header
        id_token = authorization.split("Bearer ")[-1]
        return check_email_verification(id_token)
    except HTTPException as e:
        raise e
    except Exception as e:
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