#!/usr/bin/env python3
"""
Download and export Roboflow Food YOLO model to TensorFlow Lite format.

This script downloads the "Food Imgae - YOLO" model from Roboflow Universe
which has 85 food classes including fish, meat, eggs, and more!

Requirements:
    pip install roboflow ultralytics

Usage:
    python download_roboflow_food_model.py
"""

import os
import sys

def download_from_roboflow():
    """Download the Food Imgae - YOLO model from Roboflow."""
    
    print("="*70)
    print("Roboflow Food Model Downloader")
    print("="*70)
    print("\nModel: Food Imgae - YOLO")
    print("Classes: 85 food items (fish, meat, eggs, dairy, fruits, veggies, etc.)")
    print("Source: https://universe.roboflow.com/food-image-classification/food-imgae-yolo")
    print("="*70)
    
    try:
        from roboflow import Roboflow
    except ImportError:
        print("\n✗ Error: roboflow package not installed")
        print("\nPlease install it:")
        print("   pip install roboflow")
        print("\nThen run this script again.")
        return False
    
    print("\n⚠ Note: You'll need a free Roboflow account to download models.")
    print("   Sign up at: https://universe.roboflow.com")
    print("\nYour API key can be found at: https://app.roboflow.com/settings/api")
    print()
    
    api_key = input("Enter your Roboflow API key: ").strip()
    
    if not api_key:
        print("\n✗ API key is required. Exiting.")
        return False
    
    try:
        print("\nConnecting to Roboflow...")
        rf = Roboflow(api_key=api_key)
        
        print("Accessing Food Imgae - YOLO project...")
        project = rf.workspace("food-image-classification").project("food-imgae-yolo")
        
        print("Getting latest model version...")
        model = project.version(1).model  # Version 1
        
        print("\n✓ Model accessed successfully!")
        print(f"Model classes: 85 food items")
        print("\nDownloading model weights...")
        
        # Download the model
        model_path = model.download("yolov8")
        print(f"✓ Model downloaded to: {model_path}")
        
        return model_path
        
    except Exception as e:
        print(f"\n✗ Error: {e}")
        print("\nTroubleshooting:")
        print("1. Make sure your API key is correct")
        print("2. Ensure you have internet connection")
        print("3. Check if the model is accessible from your account")
        return None

def export_to_tflite(model_path):
    """Export PyTorch model to TensorFlow Lite format."""
    
    print("\n" + "="*70)
    print("Exporting to TensorFlow Lite")
    print("="*70)
    
    try:
        from ultralytics import YOLO
    except ImportError:
        print("\n✗ Error: ultralytics package not installed")
        print("\nPlease install it:")
        print("   pip install ultralytics")
        return False
    
    try:
        # Find the weights file
        weights_path = os.path.join(model_path, "best.pt") if os.path.isdir(model_path) else model_path
        
        if not os.path.exists(weights_path):
            print(f"✗ Model weights not found at: {weights_path}")
            return False
        
        print(f"Loading model from: {weights_path}")
        model = YOLO(weights_path)
        
        print("Exporting to TFLite format (this may take a few minutes)...")
        model.export(format="tflite", imgsz=640)
        
        # Find the exported file
        base_name = os.path.splitext(os.path.basename(weights_path))[0]
        possible_paths = [
            f"{base_name}_saved_model/{base_name}_float32.tflite",
            f"{base_name}_float32.tflite",
            "best_saved_model/best_float32.tflite",
            "best_float32.tflite"
        ]
        
        for path in possible_paths:
            if os.path.exists(path):
                print(f"\n✓ Success! Model exported to: {path}")
                
                # Rename and copy to Android project
                android_path = "app/src/main/ml/food_yolov8_roboflow.tflite"
                
                if os.path.exists("app/src/main/ml"):
                    import shutil
                    shutil.copy2(path, android_path)
                    print(f"✓ Copied to: {android_path}")
                else:
                    print(f"\n⚠ Android ml folder not found")
                    print(f"   Please manually copy {path} to app/src/main/ml/food_yolov8_roboflow.tflite")
                
                return True
        
        print("\n⚠ Export completed but file not found in expected locations")
        print("Checking for .tflite files...")
        os.system("find . -name '*.tflite' -type f 2>/dev/null || echo 'No .tflite files found'")
        return False
        
    except Exception as e:
        print(f"\n✗ Export failed: {e}")
        return False

def main():
    """Main function."""
    
    # Download model
    model_path = download_from_roboflow()
    
    if not model_path:
        print("\n" + "="*70)
        print("Alternative: Manual Download")
        print("="*70)
        print("\nIf automatic download fails, you can manually:")
        print("1. Go to: https://universe.roboflow.com/food-image-classification/food-imgae-yolo")
        print("2. Sign up for free Roboflow account")
        print("3. Click 'Download Dataset' or 'Deploy Model'")
        print("4. Select 'YOLOv8' format")
        print("5. Download the model weights file")
        print("6. Run: python export_yolo11.py (modified for your model)")
        print("="*70)
        return
    
    # Export to TFLite
    if export_to_tflite(model_path):
        print("\n" + "="*70)
        print("Setup Complete!")
        print("="*70)
        print("\nNext steps:")
        print("1. Update YoloDetector.kt model name:")
        print('   const val DEFAULT_MODEL_NAME = "food_yolov8_roboflow.tflite"')
        print("\n2. Update yolo_labels.txt with the 85 class labels")
        print("\n3. Rebuild your Android project")
        print("="*70)
    else:
        print("\n✗ Export failed. Please check the errors above.")

if __name__ == "__main__":
    main()
