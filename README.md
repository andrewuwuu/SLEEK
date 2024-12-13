# Cloud Computing Part in SLEEK
SLEEK or Smart Lifestyle Eating for Efficient Kalories is a mobile-based application designed to promote better health by offering personalized lifestyle habit guidance through advanced health data analysis. Our app provides daily meal recommendations tailored to individual preferences and nutritional needs.

## Techstack and GCP Features
We building the backend service using Python as programming language and FastAPI as framework. You can find the documentation for FastAPI in [here.](https://fastapi.tiangolo.com/)

For GCP Features that we utilize is listed as:
- Artifact Registry
This is where we push inage container that has been build from entire codebase.

- Cloud Run
This is where we hosted entire backend code that has been wrapped as docker image.

- Google Cloud Storage
This is where we store prediction model from Machine Learning team.

- Firebase
We utilize firebase to handle user logging and firestore database.

- Firestore
Firestore is database system that we use to store user's health data, prediction result from model, and generated meal plan from vertex.

- Vertex AI
We use pretrained model (Gemini 1.5 Pro) to generate meal plan with custom prompt from Machine Learning Team.

- Google Secret Manager
Secret Manager here is to save and retrieve safely firebase apiKey so that the key is not leaked via container image building.

## How to Deploy
To deploy this app to cloud run, there are several things that should be followed, which is:

### 1. Service Account Creation
You should create a dedicated service account that has permission of:

- Cloud Run Admin
- Cloud Run Invoker
- Environment and Storage Object User
- Firebase Admin
- Firestore Service Agent
- Secret Manager Secret Accesoer
- Vertex AI User

### 2. Docker Image Creation and Upload to Artifact Registry
Clone this whole repository, create changes on:
- PROJECT_ID = "[your-gcp-project-id]" on main.py line 31
- GCS_BUCKET_NAME = "[your-gcp-bucket-name]" on services/gcs_service.py line 5
-  init(project=project_id, location="[nearest-gemini-location-based-on-your-project") on services/text_generation_services line 135. For location list, kinda refer to [this.](https://cloud.google.com/gemini/docs/)
- api_key_data = json.loads(get_secret("[your-saved-api-key-name-on-secret-manager]")) on auth/auth_service.py line 7

After changes you can run specific command with gcloud SDK or cloud shell:

`docker build -t [your-artifact-repo-location]-docker.pkg.dev/[your-gcp-project-id]/[your-artifact-registry-repo-name]/[your-image-name]:[your-image-tag] .`

`docker push [your-artifact-repo-location]-docker.pkg.dev/ [your-artifact-repo-location]-docker.pkg.dev/[your-gcp-project-id]/[your-artifact-registry-repo-name]:[your-image-tag]`

These commands will create the image and push the created image to artifact registry repository on your project.

### 3. Connect GCP Project to Firebase and Firestore
This to ensure that we can link the GCP project with Firebase and Firestore both at the same time. To do this you can open

https://console.cloud.google.com/firebase

After that, you can select *Build with data* menu and then select get started on firestore card option. You can finish up the setup (including authentication setup) by reffering to [this documentation.](https://firebase.google.com/docs)

### 4. Cloud Run Deployment
To deploy this project (make sure you have completing listed steps before) to cloud  run, you can this specific command:

`gcloud run deploy [your-service-name] \
    --image [your-artifact-repo-location]-docker.pkg.dev/[your-gcp-project-id]/[your-artifact-registry-repo-name]:[your-image-tag] \
    --region [your-region] \
    --platform [your-platform] \
    --cpu [your-cpu] \
    --memory [your-memory] \
    --allow-unauthenticated \
    --service-account [your-service-account-email] \
    --set-env-vars GOOGLE_CLOUD_PROJECT=[your-gcp-project-id]`


## Other Part of This Project
1. Machine Learning
https://github.com/andrewuwuu/SLEEK/tree/Machine-Learning-Models

2. Mobile Development
https://github.com/andrewuwuu/SLEEK/tree/final-android-app
