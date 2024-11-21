import firebase_admin
from firebase_admin import credentials, firestore
import os

# Path ke file Service Account JSON
service_account_path = os.path.join(os.path.dirname(__file__), "C:\\Users\\Pongo\\backend\\serviceAcc.json")


if not firebase_admin._apps:
    cred = credentials.Certificate(service_account_path)
    firebase_admin.initialize_app(cred)

# Firestore client
db = firestore.client()

# Fungsi untuk mendapatkan Firestore client
def get_firestore_client():
    return db