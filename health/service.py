from fastapi import HTTPException
from config.firebaseconf import get_firestore_client
from health.models import HealthData

def save_health_data(uid: str, health_data: HealthData):
    """
    Menyimpan data kesehatan pengguna ke Firestore.
    """
    try:
        db = get_firestore_client()
        collection_ref = db.collection("healthData")
        doc_ref = collection_ref.document(uid)  # Gunakan UID pengguna sebagai ID dokumen
        doc_ref.set(health_data.dict())
        return {"message": "Health data submitted successfully"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error saving health data: {e}")