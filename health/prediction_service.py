from fastapi import HTTPException
from config.firebase_config import get_firestore_client
from health.models import UserPrediction, HealthData

def save_user_prediction(uid: str, user_prediction: UserPrediction):
    """
    Menyimpan hasil prediksi pengguna ke koleksi 'userPrediction' di Firestore.
    """
    try:
        db = get_firestore_client()
        collection_ref = db.collection("userPrediction")
        doc_ref = collection_ref.document(uid)  # Gunakan UID pengguna sebagai ID dokumen
        doc_ref.set(user_prediction.dict())  # Konversi Pydantic model ke dictionary
        return {"message": "User prediction saved successfully"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error saving user prediction: {e}")

def get_user_health_data(uid: str):
    """
    Mengambil data kesehatan pengguna dari koleksi 'healthData' di Firestore.
    """
    try:
        db = get_firestore_client()
        health_data_collection = db.collection("healthData").where("uid", "==", uid).stream()
        
        # Ambil data pertama
        health_data_list = [doc.to_dict() for doc in health_data_collection]
        if not health_data_list:
            raise HTTPException(status_code=404, detail="No health data found for this user.")
        
        return health_data_list[0]  # Kembalikan data pertama
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error retrieving health data: {e}")