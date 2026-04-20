#!/usr/bin/env python3
"""
Export Roboflow Food Model to TensorFlow Lite

This script takes the downloaded best.pt file and exports it to TFLite format
for use in the Android app.

Usage:
    1. Download best.pt from Roboflow and place it in this folder
    2. Run: python export_roboflow_food.py
    3. The script will create food_yolov8_roboflow.tflite in app/src/main/ml/
"""

import os
import shutil
import sys

def main():
    print("="*70)
    print("Export Roboflow Food Model to TFLite")
    print("="*70)
    
    # Check for best.pt
    if not os.path.exists("best.pt"):
        print("\n[ERROR] best.pt not found!")
        print("\nPlease:")
        print("1. Go to: https://universe.roboflow.com/food-image-classification/food-imgae-yolo")
        print("2. Click 'Deploy' or 'Download'")
        print("3. Select 'YOLOv8' format")
        print("4. Download the best.pt file")
        print("5. Place best.pt in this directory")
        print("\nThen run this script again.")
        return
    
    print("\n[OK] Found best.pt")
    
    # Check for ultralytics
    try:
        from ultralytics import YOLO
    except ImportError:
        print("\n[ERROR] ultralytics not installed!")
        print("Run: pip install ultralytics")
        return
    
    print("[OK] Loading model...")
    model = YOLO("best.pt")
    
    print("\n[OK] Exporting to TensorFlow Lite...")
    print("    This will take 3-5 minutes. Please wait...")
    print("    Processing...", end="", flush=True)
    
    try:
        model.export(format="tflite", imgsz=640)
        print(" Done!")
    except Exception as e:
        print(f"\n[ERROR] Export failed: {e}")
        return
    
    # Find the exported file
    print("\n[OK] Looking for exported file...")
    
    possible_paths = [
        "best_saved_model/best_float32.tflite",
        "best_float32.tflite",
        "best.tflite"
    ]
    
    tflite_path = None
    for path in possible_paths:
        if os.path.exists(path):
            tflite_path = path
            break
    
    if not tflite_path:
        # Search for any .tflite file
        for root, dirs, files in os.walk("."):
            for file in files:
                if file.endswith(".tflite"):
                    tflite_path = os.path.join(root, file)
                    break
            if tflite_path:
                break
    
    if not tflite_path:
        print("[ERROR] Could not find exported .tflite file!")
        print("Files in current directory:")
        for f in os.listdir("."):
            print(f"  {f}")
        return
    
    print(f"[OK] Found: {tflite_path}")
    
    # Copy to Android project
    android_dir = "app/src/main/ml"
    android_path = os.path.join(android_dir, "food_yolov8_roboflow.tflite")
    
    if not os.path.exists(android_dir):
        print(f"\n[WARN] Creating directory: {android_dir}")
        os.makedirs(android_dir, exist_ok=True)
    
    print(f"\n[OK] Copying to Android project...")
    shutil.copy2(tflite_path, android_path)
    print(f"[OK] Saved to: {android_path}")
    
    # Success message
    print("\n" + "="*70)
    print("SUCCESS!")
    print("="*70)
    print("\nThe food model is ready!")
    print("\nNext steps:")
    print("1. Open Android Studio")
    print("2. Build -> Rebuild Project")
    print("3. Run the app")
    print("\nYour app can now detect 85 food classes including:")
    print("  - Fish, Meat, Eggs")
    print("  - 25+ Fruits")
    print("  - 19+ Vegetables")
    print("  - Dairy, Grains, and more!")
    print("="*70)

if __name__ == "__main__":
    main()
