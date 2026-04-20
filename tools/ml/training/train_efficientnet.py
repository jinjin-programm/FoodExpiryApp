import os
import tensorflow as tf
from tensorflow.keras.preprocessing import image_dataset_from_directory

# This script fine-tunes EfficientNet-Lite0 (or standard EfficientNetB0 as fallback)
# and exports an INT8 quantized TFLite model (~4MB) for Android NNAPI.
# 
# Data should be organized like:
# D:/FoodExpiryDatasets/
# ├── Leafy Vegetables/
# ├── Root Vegetables/
# ├── Gourd Fruit Vegetables/
# ├── Hard Fruits/
# ├── Soft Fruits/
# ├── Tropical Fruits/
# ├── Fresh Meat/
# ├── Processed Meat/
# ├── Seafood/
# ├── Eggs/
# ├── Dairy Products/
# ├── Tofu Products/
# ├── Grains & Dry Goods/
# └── Others/

DATA_DIR = r"D:\FoodExpiryDatasets_Classification"
BATCH_SIZE = 32
IMG_SIZE = (224, 224)
EPOCHS = 15

# 1. Load Data
train_ds = image_dataset_from_directory(
    DATA_DIR,
    validation_split=0.2,
    subset="training",
    seed=123,
    image_size=IMG_SIZE,
    batch_size=BATCH_SIZE
)

val_ds = image_dataset_from_directory(
    DATA_DIR,
    validation_split=0.2,
    subset="validation",
    seed=123,
    image_size=IMG_SIZE,
    batch_size=BATCH_SIZE
)

class_names = train_ds.class_names
print("Classes found:", class_names)
print("Note: Ensure Android FoodClassifier.kt maps these exact class names!")

# Pre-fetch for performance
AUTOTUNE = tf.data.AUTOTUNE
train_ds = train_ds.cache().prefetch(buffer_size=AUTOTUNE)
val_ds = val_ds.cache().prefetch(buffer_size=AUTOTUNE)

# 2. Build Model
# EfficientNetB0 from tf.keras is very close to Lite0 and natively supported.
# If strict Lite0 is needed, we would load from tfhub.dev/tensorflow/efficientnet/lite0/feature-vector/2
base_model = tf.keras.applications.EfficientNetB0(
    input_shape=IMG_SIZE + (3,),
    include_top=False,
    weights='imagenet'
)
base_model.trainable = False # Freeze base model initially

model = tf.keras.Sequential([
    tf.keras.layers.InputLayer(input_shape=IMG_SIZE + (3,)),
    # EfficientNetB0 expects inputs in [0, 255] unlike older models, but verify if using a custom hub model
    base_model,
    tf.keras.layers.GlobalAveragePooling2D(),
    tf.keras.layers.Dropout(0.2),
    tf.keras.layers.Dense(len(class_names), activation='softmax')
])

model.compile(
    optimizer=tf.keras.optimizers.Adam(),
    loss=tf.keras.losses.SparseCategoricalCrossentropy(),
    metrics=['accuracy']
)

# 3. Train Head First
print("Training classification head...")
model.fit(train_ds, validation_data=val_ds, epochs=5)

# 4. Fine-Tune (Unfreeze top layers)
base_model.trainable = True
# Freeze all but the last 20 layers
for layer in base_model.layers[:-20]:
    layer.trainable = False

model.compile(
    optimizer=tf.keras.optimizers.Adam(1e-5),  # Lower learning rate for fine-tuning
    loss=tf.keras.losses.SparseCategoricalCrossentropy(),
    metrics=['accuracy']
)

print("Fine-tuning base model...")
model.fit(train_ds, validation_data=val_ds, epochs=EPOCHS)

# 5. Convert to INT8 TFLite Model
print("Converting to INT8 TFLite...")
def representative_data_gen():
    # Provide a few batches of training data for calibration
    for input_value, _ in train_ds.take(100):
        yield [input_value]

converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
converter.representative_dataset = representative_data_gen
converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
converter.inference_input_type = tf.uint8  # or tf.int8
converter.inference_output_type = tf.uint8 # or tf.int8

tflite_quant_model = converter.convert()

# 6. Save Model
output_path = "food_efficientnet_lite0_int8.tflite"
with open(output_path, "wb") as f:
    f.write(tflite_quant_model)

print(f"Model saved to {output_path} (Size: {len(tflite_quant_model) / 1024 / 1024:.2f} MB)")
print("Done! Copy the .tflite file to your Android app's assets folder.")
