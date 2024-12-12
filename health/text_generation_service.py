import os
import json
import logging
from vertexai import init
from vertexai.preview.generative_models import GenerativeModel
from config.firebase_config import get_firestore_client
from datetime import datetime

def get_user_data(uid: str):
    """
    Fetches health data and prediction data from Firestore for a given user ID.
    """
    try:
        db = get_firestore_client()

        # Fetch health data
        logging.info("Fetching health data from Firestore...")
        health_data_doc = db.collection("healthData").where(field_path="uid", op_string="==", value=uid).stream()

        health_data = next(health_data_doc, None)
        if not health_data:
            raise ValueError("No health data found for the user.")
        health_data = health_data.to_dict()

        # Fetch prediction data
        logging.info("Fetching prediction data from Firestore...")
        user_prediction_doc = db.collection("userPrediction").document(uid).get()

        if not user_prediction_doc.exists:
            raise ValueError("No prediction data found for the user.")
        user_prediction = user_prediction_doc.to_dict()

        logging.info(f"Health Data: {health_data}, User Prediction: {user_prediction}")
        return health_data, user_prediction

    except Exception as e:
        logging.error(f"Error fetching user data: {e}")
        raise RuntimeError(f"Failed to fetch user data: {e}")

def generate_prompt(health_data: dict, user_prediction: dict) -> str:
    """
    Generates a text prompt based on user data and predictions using f-strings for inline formatting.
    """
    try:
        # Safely retrieve values and provide default fallbacks
        total_calories = int(float(user_prediction.get("predicted_bmr", 2000)))
        weight_category = str(user_prediction.get("weight_category", "Unknown")).strip() if user_prediction.get("weight_category") else "Unknown"
        dietary_restrictions = health_data.get("food_allergies", "None")
        
        if dietary_restrictions is None:
            dietary_restrictions = "None"
        elif isinstance(dietary_restrictions, list):
            dietary_restrictions = ", ".join(dietary_restrictions)
        dietary_restrictions = str(dietary_restrictions).strip()

        # Use f-strings for the prompt
        prompt = f"""
        You are a professional nutritionist specializing in personalized meal planning with a deep understanding 
        of diverse dietary needs, cultural sensitivities, and local cuisines. You excel at creating balanced meal plans 
        tailored to specific caloric and nutritional goals while considering individual preferences, dietary restrictions, 
        and allergies. Your expertise in Indonesian cuisine ensures your recommendations are practical, affordable, and 
        easily accessible. Your task is to generate 3 daily meal plan variations, each consisting of breakfast, lunch, 
        and dinner. Also, you have to customized the meal based on user's weight category whether is in underweight, ideal,
        overweight or obese to ensure that they have ideal daily nutrient intake.

        Nutritional Goals:
        - Total daily calorie intake: {total_calories} cal.
        - User's weight category: {weight_category}.
        - The calorie distribution among meals (breakfast, lunch, and dinner) should be dynamically determined.

        Dietary Restrictions:
        The user is allergic to {dietary_restrictions}, so avoid using this ingredient or any related items at any cost.
        Ensure the names of dishes do not reference the user allergy or imply its exclusion (e.g., "Bubur Ayam (tanpa ayam)", "Bubur Manis dengan Pisang dan Kacang Hijau (tanpa kacang)", "Ayam Goreng (tanpa ayam) dengan Nasi Putih dan Sayur Tumis",
        "Lontong Sayur Tanpa Telur dengan Ikan", "Mie Goreng Jawa (tanpa daging ayam)").
        Similarly, for any other specified allergies, avoid dish names that reference excluded ingredients or imply their absence.

        Focus on Indonesian Cuisine:
        - Meals must primarily feature local, affordable, and readily available ingredients.
        - Follow Indonesia's balanced nutrition principle (gizi seimbang) with a balance of carbohydrates, protein, vegetables, and fats.

        Output Requirements:
        - Use Bahasa Indonesia.
        - For the "ingredients" use a specific measurements, for example: 1 table spoon of sugar, 2 tea spoon of water, 1 bowl of rice, 1 plate of oil, 3 cups of milk.
        - For the "calories" use "~" symbol and use word "kalori", DO NOT USE the word "sekitar".
        - Present the 3 daily meal plans in strict JSON format:
        [
          {{
            "mealPlan": [
              {{
                "meal": "Breakfast",
                "dishName": "Dish name",
                "ingredients": ["Ingredient 1", "Ingredient 2"],
                "calories": "Dynamic"
              }},
              {{
                "meal": "Lunch",
                "dishName": "Dish name",
                "ingredients": ["Ingredient 1", "Ingredient 2"],
                "calories": "Dynamic"
              }},
              {{
                "meal": "Dinner",
                "dishName": "Dish name",
                "ingredients": ["Ingredient 1", "Ingredient 2"],
                "calories": "Dynamic"
              }}
            ]
          }},
          {{"mealPlan": [...]}},
          {{"mealPlan": [...]}},
        ]

        Ensure strict JSON formatting.
        """

        # Clean and log the generated prompt
        prompt = prompt.strip()
        logging.info(f"Generated Prompt: {prompt}")
        return prompt

    except Exception as e:
        logging.error(f"Error generating prompt: {e}")
        raise RuntimeError(f"Failed to generate prompt: {e}")

def generate_text_with_vertexai(prompt: str):
    """
    Generates text using Vertex AI Generative Models in the us-central1 region.
    """
    try:
        project_id = os.getenv("GOOGLE_CLOUD_PROJECT")
        if not project_id:
            raise RuntimeError("Environment variable 'GOOGLE_CLOUD_PROJECT' is not set.")

        # Initialize Vertex AI environment
        init(project=project_id, location="asia-southeast1")

        # Initialize the generative model
        generative_multimodal_model = GenerativeModel("gemini-1.5-pro-002")

        logging.info(f"Sending prompt to Vertex AI: {repr(prompt)}")
        response = generative_multimodal_model.generate_content([prompt])

        if not response or not hasattr(response, "text"):
            raise ValueError("Vertex AI response is empty or invalid")

        logging.info(f"Received raw response from Vertex AI: {repr(response.text)}")
        return response.text.strip()

    except Exception as e:
        logging.error(f"Error generating text with Vertex AI: {e}")
        raise RuntimeError(f"Failed to generate text: {e}")

def parse_meal_plan_response(raw_response: str):
    """
    Parses the raw response from Vertex AI and extracts the JSON content.
    
    Parameters:
        raw_response (str): The raw response string containing JSON.

    Returns:
        dict: The parsed JSON content.
    """
    try:
        # Log the raw response for debugging
        logging.info(f"Raw response received: {repr(raw_response)}")

        # Extract the JSON part from the response
        if "```json" in raw_response:
            json_part = raw_response.split("```json")[1].strip()
            if "```" in json_part:
                json_part = json_part.split("```")[0].strip()
        else:
            raise ValueError("No JSON content found in the response.")

        # Parse the extracted JSON content
        parsed_json = json.loads(json_part)
        logging.info(f"Parsed meal plan JSON: {parsed_json}")

        return parsed_json

    except Exception as e:
        logging.error(f"Failed to parse meal plan response: {e}")
        raise RuntimeError(f"Failed to parse meal plan response: {e}")

def save_separate_meal_plans_to_firestore(uid: str, meal_plans: list):
    """
    Save each meal plan variation to Firestore as a separate document with a timestamp.

    Parameters:
        uid (str): The user ID to associate with the meal plans.
        meal_plans (list): The parsed meal plans to save.
    """
    try:
        # Initialize Firestore client
        db = get_firestore_client()

        # Iterate through the meal plans and save each as a separate document
        for index, meal_plan in enumerate(meal_plans):
            # Generate a unique document ID using the UID and index
            document_id = f"{uid}_mealPlan_{index + 1}"

            # Prepare the document data with a timestamp
            document_data = {
                "mealPlan": meal_plan.get("mealPlan", []),
                "timestamp": datetime.utcnow().isoformat()  # Save timestamp in ISO 8601 format
            }

            # Save to Firestore under a collection named "mealPlans"
            db.collection("mealPlans").document(document_id).set(document_data)

            logging.info(f"Meal plan {index + 1} saved successfully for UID: {uid}")

        return {"message": f"{len(meal_plans)} meal plans saved successfully", "uid": uid}
    except Exception as e:
        logging.error(f"Error saving meal plans to Firestore: {e}")
        raise RuntimeError(f"Failed to save meal plans to Firestore: {e}")