import tensorflow as tf
import pandas as pd
import numpy as np
from sklearn.preprocessing import LabelEncoder

# Global model variables
classification_model = None
regression_model = None

# Label encoder for BMI Category
label_encoder = LabelEncoder()
label_encoder.fit(["Underweight", "Ideal", "Overweight", "Obese"])

def get_classification_model():
    """
    Lazily loads the classification model.
    """
    global classification_model
    if classification_model is None:
        classification_model = tf.keras.models.load_model('./assets/classification_model_tf.h5')
        print("Classification model loaded.")
    return classification_model

def get_regression_model():
    """
    Lazily loads the regression model.
    """
    global regression_model
    if regression_model is None:
        regression_model = tf.keras.models.load_model('./assets/bmr_regression_model.h5')
        print("Regression model loaded.")
    return regression_model

def predict_bmi_bmr(input_data):
    """
    Predicts BMI category and BMR based on input data.
    """
    try:
        print(f"Input data for prediction: {input_data}")
        user_input = pd.DataFrame([input_data])

        if user_input.shape[1] != 4:
            raise ValueError(f"Invalid input dimensions. Expected 4 features, but received {user_input.shape[1]}.")

        classification_model = get_classification_model()
        regression_model = get_regression_model()

        classification_preds = classification_model.predict(user_input)
        predicted_category_idx = np.argmax(classification_preds, axis=1)[0]
        predicted_bmi_category = label_encoder.inverse_transform([predicted_category_idx])[0]

        bmr_prediction = regression_model.predict(user_input)[0][0]

        result = {
            "weight_category": predicted_bmi_category,
            "predicted_bmr": float(bmr_prediction)
        }
        print(f"Prediction result: {result}")
        return result

    except Exception as e:
        print(f"Error during prediction: {e}")
        return {"error": str(e)}