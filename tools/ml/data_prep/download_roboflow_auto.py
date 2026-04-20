#!/usr/bin/env python3
"""
Download Roboflow Food YOLO model with provided API key.
"""

import os
import sys

API_KEY = "HFxY9GmefUiq78t6pob1"

def download_model():
    """Download the Food Imgae - YOLO model from Roboflow."""
    
    print("="*70)
    print("Downloading Roboflow Food Model")
    print("="*70)
    
    try:
        from roboflow import Roboflow
    except ImportError:
        print("[ERROR] roboflow package not installed")
        print("Run: pip install roboflow")
        return None
    
    try:
        print("\nConnecting to Roboflow...")
        rf = Roboflow(api_key=API_KEY)
        
        print("Accessing Food Imgae - YOLO project...")
        project = rf.workspace("food-image-classification").project("food-imgae-yolo")
        
        print("Getting model version...")
        model = project.version(1).model
        
        print("\n[OK] Model accessed successfully!")
        print("Downloading model weights...")
        
        # Download the model (Roboflow only supports 'pt' format for direct download)
        print("Downloading in PyTorch format...")
        model_path = model.download("pt")
        print(f"[OK] Model downloaded to: {model_path}")
        
        return model_path
        
    except Exception as e:
        print(f"[ERROR] {e}")
        return None

def export_to_tflite(model_path):
    """Export to TensorFlow Lite."""
    
    print("\n" + "="*70)
    print("Exporting to TensorFlow Lite")
    print("="*70)
    
    try:
        from ultralytics import YOLO
    except ImportError:
        print("[ERROR] Error: ultralytics not installed")
        return False
    
    try:
        # Find weights file
        weights_path = os.path.join(model_path, "best.pt") if os.path.isdir(model_path) else model_path
        
        if not os.path.exists(weights_path):
            print(f"[ERROR] Model weights not found: {weights_path}")
            # Try to find best.pt in current directory
            for root, dirs, files in os.walk("."):
                if "best.pt" in files:
                    weights_path = os.path.join(root, "best.pt")
                    print(f"Found weights at: {weights_path}")
                    break
        
        if not os.path.exists(weights_path):
            print("[ERROR] Could not find model weights")
            return False
        
        print(f"Loading model from: {weights_path}")
        model = YOLO(weights_path)
        
        print("Exporting to TFLite (this may take 2-5 minutes)...")
        model.export(format="tflite", imgsz=640)
        
        # Find exported file
        base_name = os.path.splitext(os.path.basename(weights_path))[0]
        possible_paths = [
            f"{base_name}_saved_model/{base_name}_float32.tflite",
            f"{base_name}_float32.tflite",
            "best_saved_model/best_float32.tflite",
            "best_float32.tflite"
        ]
        
        for path in possible_paths:
            if os.path.exists(path):
                print(f"\n[OK] Success! Exported to: {path}")
                
                # Copy to Android project
                android_dir = "app/src/main/ml"
                android_path = f"{android_dir}/food_yolov8_roboflow.tflite"
                
                if os.path.exists(android_dir):
                    import shutil
                    shutil.copy2(path, android_path)
                    print(f"[OK] Copied to Android: {android_path}")
                    return True
                else:
                    print(f"\n[WARN] Android directory not found")
                    print(f"Please manually copy {path} to app/src/main/ml/food_yolov8_roboflow.tflite")
                    return True
        
        print("\n[WARN] Export completed but file not found")
        print("Searching for .tflite files...")
        for root, dirs, files in os.walk("."):
            for file in files:
                if file.endswith(".tflite"):
                    print(f"  Found: {os.path.join(root, file)}")
        return False
        
    except Exception as e:
        print(f"[ERROR] Export failed: {e}")
        import traceback
        traceback.print_exc()
        return False

def main():
    model_path = download_model()
    
    if model_path:
        success = export_to_tflite(model_path)
        
        if success:
            print("\n" + "="*70)
            print("[OK] Setup Complete!")
            print("="*70)
            print("\nNext steps:")
            print("1. Rebuild Android project: Build → Rebuild Project")
            print("2. Or run: ./gradlew assembleDebug")
            print("\nThe model can now detect 85 food classes including:")
            print("  [OK] Fish, Meat, Eggs")
            print("  [OK] Fruits (25 classes)")
            print("  [OK] Vegetables (19 classes)")
            print("  [OK] Dairy, Grains, and more!")
        else:
            print("\n[ERROR] Export failed. Check errors above.")
    else:
        print("\n[ERROR] Download failed. Check errors above.")

if __name__ == "__main__":
    main()
