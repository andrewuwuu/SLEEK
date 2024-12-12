import os
from google.cloud import storage

# Configure bucket and file names
GCS_BUCKET_NAME = "sleekstorage"
CLASSIFICATION_MODEL_BLOB = "classification_model_tf.h5"
REGRESSION_MODEL_BLOB = "bmr_regression_model.h5"

def download_model_from_gcs(bucket_name: str, source_blob_name: str, destination_file_name: str):
    """
    Downloads a file from Google Cloud Storage to a local directory.
    """
    try:
        # Use the default credentials from the Cloud Run service account
        client = storage.Client()  
        bucket = client.bucket(bucket_name)
        blob = bucket.blob(source_blob_name)

        # Download the file to the specified destination
        blob.download_to_filename(destination_file_name)
        print(f"Downloaded {source_blob_name} to {destination_file_name}.")
    except Exception as e:
        raise RuntimeError(f"Failed to download {source_blob_name}: {e}")

def download_models():
    """
    Downloads all models from GCS to a local directory.
    """
    # Ensure the assets directory exists
    os.makedirs("./assets", exist_ok=True)

    # Download models
    download_model_from_gcs(GCS_BUCKET_NAME, CLASSIFICATION_MODEL_BLOB, "./assets/classification_model_tf.h5")
    download_model_from_gcs(GCS_BUCKET_NAME, REGRESSION_MODEL_BLOB, "./assets/bmr_regression_model.h5")