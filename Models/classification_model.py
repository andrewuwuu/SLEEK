import tensorflow as tf
import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from sklearn.preprocessing import StandardScaler

data_path = '/data/bodyPerformance.csv'
data = pd.read_csv(data_path)

data = data[['age', 'gender', 'height_cm', 'weight_kg']]

data.dropna(inplace=True)

data['gender'] = data['gender'].map({'Male': 1, 'Female': 0})

height_q1 = data['height_cm'].quantile(0.25)
height_q3 = data['height_cm'].quantile(0.75)
height_iqr = height_q3 - height_q1
height_lower_bound = height_q1 - 1.5 * height_iqr
height_upper_bound = height_q3 + 1.5 * height_iqr

data = data[(data['height_cm'] >= height_lower_bound) & (data['height_cm'] <= height_upper_bound)]

weight_q1 = data['weight_kg'].quantile(0.25)
weight_q3 = data['weight_kg'].quantile(0.75)
weight_iqr = weight_q3 - weight_q1
weight_lower_bound = weight_q1 - 1.5 * weight_iqr
weight_upper_bound = weight_q3 + 1.5 * weight_iqr

data = data[(data['weight_kg'] >= weight_lower_bound) & (data['weight_kg'] <= weight_upper_bound)]

def calculate_bmi(row):
    height_m = row['height_cm'] / 100
    return row['weight_kg'] / (height_m ** 2)

data['BMI'] = data.apply(calculate_bmi, axis=1)

def categorize_bmi(bmi):
    if bmi < 18.5:
        return "Underweight"
    elif 18.5 <= bmi < 24.9:
        return "Ideal"
    elif 25 <= bmi < 29.9:
        return "Overweight"
    else:
        return "Obese"

data['BMI_Category'] = data['BMI'].apply(categorize_bmi)

label_encoder = LabelEncoder()
data['BMI_Category_Encoded'] = label_encoder.fit_transform(data['BMI_Category'])

X = data[['age', 'gender', 'height_cm', 'weight_kg']]
y = data['BMI_Category_Encoded']

scaler = StandardScaler()
X = scaler.fit_transform(X)

X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
X_train, X_val, y_train, y_val = train_test_split(X_train, y_train, test_size=0.25, random_state=42)

model = tf.keras.Sequential([
    tf.keras.layers.Dense(64, activation='relu', input_shape=(X_train.shape[1],), kernel_regularizer=tf.keras.regularizers.l2(0.01)),
    tf.keras.layers.Dense(32, activation='relu'),
    tf.keras.layers.Dense(16, activation='relu'),
    tf.keras.layers.Dense(4, activation='softmax')
])

model.compile(optimizer='adam',
              loss='sparse_categorical_crossentropy',
              metrics=['accuracy'])

history = model.fit(X_train, y_train,
                    validation_data=(X_val, y_val),
                    epochs=100,
                    batch_size=16)

test_loss, test_accuracy = model.evaluate(X_test, y_test)
print(f"Test Accuracy: {test_accuracy * 100:.2f}%")

model.save('./classification_model.h5')
