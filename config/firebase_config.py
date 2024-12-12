import firebase_admin
from firebase_admin import credentials, firestore

# Initialize Firebase Admin SDK
if not firebase_admin._apps:
    # Use Application Default Credentials for authentication
    cred = credentials.ApplicationDefault()
    firebase_admin.initialize_app(cred)

# Firestore client
db = firestore.client()

# Function to get Firestore client
def get_firestore_client():
    
    return db