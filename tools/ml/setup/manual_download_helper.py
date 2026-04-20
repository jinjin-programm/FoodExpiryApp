#!/usr/bin/env python3
"""
Manual download helper for Roboflow Food Model.
Since the API download isn't working, here are manual steps.
"""

print("="*70)
print("Manual Download Instructions for Roboflow Food Model")
print("="*70)

print("""
The automatic API download isn't working for this model.
Please follow these manual steps:

STEP 1: Visit the Model Page
   https://universe.roboflow.com/food-image-classification/food-imgae-yolo

STEP 2: Download the Model
   1. Click on the "Deploy" or "Download" button
   2. Select "YOLOv8" format
   3. Choose "Get weights file (best.pt)"
   4. Download the best.pt file

STEP 3: Export to TensorFlow Lite
   Place the downloaded best.pt file in this directory, then run:
   
   python export_food_model.py

   This will create the TFLite file for Android.

ALTERNATIVE: Direct Model Access
   Some Roboflow models allow direct download. Check if there's a 
   "Download Dataset" button and download the full dataset with model weights.

""")

# Create a simple export script
export_script = '''#!/usr/bin/env python3
"""
Export the downloaded best.pt model to TFLite format.
Place best.pt in the same directory as this script, then run it.
"""

from ultralytics import YOLO
import os
import shutil

print("Loading model from best.pt...")
model = YOLO("best.pt")

print("\\nExporting to TensorFlow Lite (this will take 3-5 minutes)...")
model.export(format="tflite", imgsz=640)

# Find the exported file
possible_paths = [
    "best_saved_model/best_float32.tflite",
    "best_float32.tflite"
]

for path in possible_paths:
    if os.path.exists(path):
        print(f"\\n[OK] Exported to: {path}")
        
        # Copy to Android project
        android_dir = "app/src/main/ml"
        android_path = f"{android_dir}/food_yolov8_roboflow.tflite"
        
        if os.path.exists(android_dir):
            shutil.copy2(path, android_path)
            print(f"[OK] Copied to Android: {android_path}")
            print("\\nDone! Rebuild your Android project.")
        else:
            print(f"\\nPlease manually copy {path} to app/src/main/ml/food_yolov8_roboflow.tflite")
        break
else:
    print("\\n[WARN] Could not find exported file automatically")
    print("Check current directory for .tflite files")
'''

with open('export_food_model.py', 'w') as f:
    f.write(export_script)

print("[OK] Created export_food_model.py for when you have the model file")
print("="*70)
