from fastapi import HTTPException
from config.firebase_config import get_firestore_client
from health.models import HealthData

def save_health_data(uid: str, input_data: HealthData):
    """
    Menyimpan data kesehatan ke koleksi 'healthData' di Firestore.
    Field 'uid' akan ditambahkan ke data yang disimpan.
    """
    try:
        # Gunakan Firestore client dari firebase_config
        db = get_firestore_client()
        collection_ref = db.collection("healthData")

        # Konversi input_data menjadi dictionary dan tambahkan 'uid'
        data_to_save = input_data.dict()
        data_to_save["uid"] = uid  # Tambahkan UID pengguna ke data yang akan disimpan

        # Simpan data ke Firestore
        doc_ref = collection_ref.document()  # Buat dokumen baru dengan ID unik
        doc_ref.set(data_to_save)

        return {"message": "Health data saved successfully"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error saving health data: {e}")

def get_ordered_health_data(uid: str) -> dict:
    """
    Mengambil data kesehatan dari Firestore untuk pengguna tertentu,
    mengecualikan 'food_allergies' dan memastikan atribut diurutkan.
    """
    try:
        # Inisialisasi klien Firestore
        db = get_firestore_client()
        collection_ref = db.collection("healthData")

        # Ambil dokumen berdasarkan UID
        query = collection_ref.where("uid", "==", uid).limit(1)
        docs = query.stream()

        health_data = None
        for doc in docs:
            health_data = doc.to_dict()
            break

        if not health_data:
            raise HTTPException(status_code=404, detail="Health data not found.")

        # Filter field yang tidak relevan
        allowed_fields = ["age", "gender", "height_cm", "weight_kg"]
        filtered_data = {key: health_data[key] for key in allowed_fields if key in health_data}
        print(f"Filtered health data: {filtered_data}")

        return filtered_data

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch and process health data: {e}")