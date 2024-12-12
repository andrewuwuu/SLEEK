# Gunakan base image Python yang ringan
FROM python:3.10-slim

# Set working directory dalam container
WORKDIR /app

# Salin file requirements.txt dan instal dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Salin seluruh project (kecuali file yang diabaikan oleh .dockerignore)
COPY . .

# Ekspos port 8080 untuk aplikasi FastAPI
EXPOSE 8080

# Jalankan aplikasi FastAPI dengan Uvicorn
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8080"]