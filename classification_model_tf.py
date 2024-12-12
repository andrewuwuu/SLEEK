import tensorflow as tf
import pandas as pd
from sklearn.model_selection import train_test_split
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from sklearn.preprocessing import StandardScaler

# Generate data with 5,000 samples
data = {
    "age": np.random.randint(15, 81, 10000),  # Age range from 15 to 80
    "gender": np.random.choice([0, 1], 10000),  # Gender: 0 for female, 1 for male
    "height_cm": np.random.randint(150, 200, 10000),  # Height in cm
    "weight_kg": np.random.randint(50, 100, 10000),  # Weight in kg
}

# Calculate BMI
def calculate_bmi(row):
    # Tinggi badan dari cm ke meter
    height_m = row['height_cm'] / 100
    # Rumus BMI
    return row['weight_kg'] / (height_m ** 2)

# Convert data to DataFrame and calculate BMI
df = pd.DataFrame(data)
df['BMI'] = df.apply(calculate_bmi, axis=1)

# Optional: Tambahkan kolom kategori BMI
def categorize_bmi(bmi):
    if bmi < 18.5:
        return "Underweight"
    elif 18.5 <= bmi < 24.9:
        return "Ideal"
    elif 25 <= bmi < 29.9:
        return "Overweight"
    else:
        return "Obese"

df['BMI_Category'] = df['BMI'].apply(categorize_bmi)

# Tampilkan DataFrame
print(df)

label_encoder = LabelEncoder()
df['BMI_Category_Encoded'] = label_encoder.fit_transform(df['BMI_Category'])

# Use encoded BMI categories as the target variable
X = df[['age', 'gender', 'height_cm', 'weight_kg']]
y = df['BMI_Category_Encoded']  # Changed to encoded categories

# Split into train, validation, and test sets
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
X_train, X_val, y_train, y_val = train_test_split(X_train, y_train, test_size=0.25, random_state=42)

model = tf.keras.Sequential([
    tf.keras.layers.Dense(64, activation='relu', input_shape=(X_train.shape[1],), kernel_regularizer=tf.keras.regularizers.l2(0.01)),
    tf.keras.layers.Dense(32, activation='relu'),
    tf.keras.layers.Dense(16, activation='relu'),
    tf.keras.layers.Dense(4, activation='softmax')  # 4 classes for BMI categories
])

model.compile(optimizer='adam',
              loss='sparse_categorical_crossentropy',
              metrics=['accuracy'])

history = model.fit(X_train, y_train,
                    validation_data=(X_val, y_val),
                    epochs=100,
                    batch_size=16)

# Evaluate the model (no changes needed here)
test_loss, test_accuracy = model.evaluate(X_test, y_test)
print(f"Test Accuracy: {test_accuracy * 100:.2f}%")

model.save('./classification_model_tf.h5')
