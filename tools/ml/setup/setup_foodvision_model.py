#!/usr/bin/env python3
"""
Download and export FoodVision YOLO model to TensorFlow Lite.

This model has 55 food classes including fish, meat, eggs, fruits, vegetables!
Source: https://github.com/Nightey3s/foodvision

Usage:
    python setup_foodvision_model.py
"""

import os
import sys
import urllib.request
import shutil

# Direct download URL for the model
MODEL_URL = "https://github.com/Nightey3s/foodvision/raw/main/best.pt"
MODEL_FILENAME = "foodvision_best.pt"

def download_model():
    """Download the FoodVision model from GitHub."""
    
    print("="*70)
    print("FoodVision YOLO Model Setup")
    print("="*70)
    print("\nModel: FoodVision YOLOv8 (55 food classes)")
    print("Source: https://github.com/Nightey3s/foodvision")
    print("\nClasses include:")
    print("  - Fish, Meat, Steak, Shrimp, Egg")
    print("  - Fruits: apple, banana, grapes, orange, etc.")
    print("  - Vegetables: broccoli, carrot, lettuce, etc.")
    print("  - Dairy: cheese, milk, yogurt")
    print("  - And more!")
    print("="*70)
    
    if os.path.exists(MODEL_FILENAME):
        print(f"\n[OK] Model already exists: {MODEL_FILENAME}")
        return MODEL_FILENAME
    
    print(f"\n[OK] Downloading from GitHub...")
    print(f"    URL: {MODEL_URL}")
    print(f"    This may take 1-2 minutes...", end="", flush=True)
    
    try:
        urllib.request.urlretrieve(MODEL_URL, MODEL_FILENAME)
        print(" Done!")
        print(f"[OK] Saved as: {MODEL_FILENAME}")
        return MODEL_FILENAME
    except Exception as e:
        print(f"\n[ERROR] Download failed: {e}")
        print("\nAlternative: Download manually from:")
        print(f"  {MODEL_URL}")
        print("\nThen place the file in this directory as 'foodvision_best.pt'")
        return None

def export_to_tflite(model_path):
    """Export to TensorFlow Lite format."""
    
    print("\n" + "="*70)
    print("Exporting to TensorFlow Lite")
    print("="*70)
    
    try:
        from ultralytics import YOLO
    except ImportError:
        print("[ERROR] ultralytics not installed!")
        print("Run: pip install ultralytics")
        return False
    
    print("[OK] Loading model...")
    try:
        model = YOLO(model_path)
    except Exception as e:
        print(f"[ERROR] Failed to load model: {e}")
        return False
    
    print("[OK] Exporting to TFLite...")
    print("    This will take 3-5 minutes. Please wait...", end="", flush=True)
    
    try:
        model.export(format="tflite", imgsz=640)
        print(" Done!")
    except Exception as e:
        print(f"\n[ERROR] Export failed: {e}")
        return False
    
    # Find the exported file
    print("\n[OK] Looking for exported file...")
    
    possible_paths = [
        "foodvision_best_saved_model/foodvision_best_float32.tflite",
        "foodvision_best_float32.tflite",
        "best_saved_model/best_float32.tflite",
        "best_float32.tflite"
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
        return False
    
    print(f"[OK] Found: {tflite_path}")
    
    # Copy to Android project
    android_dir = "app/src/main/ml"
    android_path = os.path.join(android_dir, "foodvision_yolov8.tflite")
    
    if not os.path.exists(android_dir):
        print(f"\n[OK] Creating directory: {android_dir}")
        os.makedirs(android_dir, exist_ok=True)
    
    print(f"\n[OK] Copying to Android project...")
    shutil.copy2(tflite_path, android_path)
    print(f"[OK] Saved to: {android_path}")
    
    return True

def main():
    # Download model
    model_path = download_model()
    
    if not model_path:
        print("\n[ERROR] Setup failed. Please download manually.")
        return
    
    # Export to TFLite
    success = export_to_tflite(model_path)
    
    if success:
        print("\n" + "="*70)
        print("SUCCESS! Setup Complete!")
        print("="*70)
        print("\n[OK] The FoodVision model is ready!")
        print("\nNext steps:")
        print("1. Open Android Studio")
        print("2. Build -> Rebuild Project")
        print("3. Run the app")
        print("\nYour app can now detect 55 food classes including:")
        print("  - Fish, Meat, Steak, Shrimp, Egg")
        print("  - 14 Fruits (apple, banana, grapes, etc.)")
        print("  - 16 Vegetables (broccoli, carrot, lettuce, etc.)")
        print("  - Dairy: cheese, milk, yogurt")
        print("  - Grains: bread, pasta, rice, pizza")
        print("  - Snacks: cake, cookie, doughnut, ice cream")
        print("="*70)
    else:
        print("\n[ERROR] Export failed. Check errors above.")
        print("\nYou can try running the export manually:")
        print(f"  python -c \"from ultralytics import YOLO; m = YOLO('{model_path}'); m.export(format='tflite', imgsz=640)\"")

if __name__ == "__main__":
    main()
