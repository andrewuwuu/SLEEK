import tensorflow as tf
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler

data_path = '/data/bodyPerformance.csv'
data = pd.read_csv(data_path)
data = data[['age', 'gender', 'height_cm', 'weight_kg']]

def calculate_bmr(row):
    if row['gender'] == 1:
        return 88.362 + (13.397 * row['weight_kg']) + (4.799 * row['height_cm']) - (5.677 * row['age'])
    else:
        return 447.593 + (9.247 * row['weight_kg']) + (3.098 * row['height_cm']) - (4.330 * row['age'])

data['BMR'] = data.apply(calculate_bmr, axis=1)

X = data[['age', 'gender', 'height_cm', 'weight_kg']]
y = data['BMR']

scaler = StandardScaler()
X = scaler.fit_transform(X)

X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
X_train, X_val, y_train, y_val = train_test_split(X_train, y_train, test_size=0.25, random_state=42)

model = tf.keras.Sequential([
    tf.keras.layers.Dense(64, activation='relu', input_shape=(X_train.shape[1],), kernel_regularizer=tf.keras.regularizers.l2(0.01)),
    tf.keras.layers.Dense(32, activation='relu'),
    tf.keras.layers.Dense(16, activation='relu'),
    tf.keras.layers.Dense(1)
])

model.compile(optimizer='adam', loss=tf.keras.losses.MeanSquaredError(), metrics=[tf.keras.metrics.MeanAbsoluteError()])

history = model.fit(
    X_train, y_train,
    validation_data=(X_val, y_val),
    epochs=100,
    batch_size=32,
    verbose=1
)

test_loss, test_mae = model.evaluate(X_test, y_test, verbose=1)
print(f"Test MAE: {test_mae}")

model.save('bmr_regression_model.keras')
