#!/usr/bin/env python3
"""
Download and export food-specific YOLO model to TensorFlow Lite format.
This script downloads a pre-trained food detection model and converts it for Android use.

Requirements:
    pip install ultralytics requests

Usage:
    python download_food_model.py
"""

import os
import requests
from ultralytics import YOLO
import shutil

def download_file(url, destination):
    """Download a file from URL to destination."""
    print(f"Downloading from {url}...")
    response = requests.get(url, stream=True)
    response.raise_for_status()
    
    with open(destination, 'wb') as f:
        for chunk in response.iter_content(chunk_size=8192):
            f.write(chunk)
    print(f"✓ Downloaded to {destination}")

def download_food_model():
    """Download the FoodVision YOLOv8 model."""
    
    # GitHub raw URL for the FoodVision model
    # This is the Nightey3s/FoodVision model with 55 food classes
    model_url = "https://github.com/Nightey3s/foodvision/raw/main/best.pt"
    
    model_path = "food_yolov8n.pt"
    
    if not os.path.exists(model_path):
        try:
            download_file(model_url, model_path)
        except Exception as e:
            print(f"✗ Error downloading model: {e}")
            print("\nAlternative: Download manually from:")
            print("https://github.com/Nightey3s/foodvision/raw/main/best.pt")
            return None
    else:
        print(f"✓ Model file already exists: {model_path}")
    
    return model_path

def export_to_tflite(model_path):
    """Export PyTorch model to TensorFlow Lite format."""
    
    print("\n" + "="*60)
    print("Exporting to TensorFlow Lite format...")
    print("="*60)
    
    try:
        # Load the model
        model = YOLO(model_path)
        
        # Export to TFLite
        # Using imgsz=640 for better accuracy (YOLOv8 default)
        model.export(format="tflite", imgsz=640, int8=False)
        
        # Find the exported file
        possible_paths = [
            "food_yolov8n_saved_model/food_yolov8n_float32.tflite",
            "food_yolov8n_float32.tflite",
            "best_saved_model/best_float32.tflite",
            "best_float32.tflite"
        ]
        
        for path in possible_paths:
            if os.path.exists(path):
                print(f"\n✓ Success! Model exported to: {path}")
                
                # Copy to Android project
                android_assets_path = "app/src/main/ml/food_yolov8n_float32.tflite"
                
                if os.path.exists("app/src/main/ml"):
                    shutil.copy2(path, android_assets_path)
                    print(f"✓ Copied to Android project: {android_assets_path}")
                else:
                    print(f"\n⚠ Please manually copy the file:")
                    print(f"   From: {path}")
                    print(f"   To: app/src/main/ml/food_yolov8n_float32.tflite")
                
                return path
        
        print("\n⚠ Export completed but file location unknown.")
        print("Check the current directory for .tflite files:")
        os.system("ls -la *.tflite 2>/dev/null || find . -name '*.tflite' -type f")
        
    except Exception as e:
        print(f"\n✗ Export failed: {e}")
        print("\nMake sure you have ultralytics installed:")
        print("   pip install ultralytics")
        raise

def main():
    """Main function to download and export food model."""
    
    print("="*60)
    print("Food YOLO Model Downloader & Exporter")
    print("="*60)
    print("\nThis will download a pre-trained food detection model")
    print("and convert it to TensorFlow Lite format for Android.")
    print("\nModel: FoodVision YOLOv8 (55 food classes)")
    print("Classes include: apple, banana, orange, broccoli, carrot,")
    print("pizza, cake, sandwich, fish, meat, and many more!")
    print("="*60 + "\n")
    
    # Download model
    model_path = download_food_model()
    
    if model_path:
        # Export to TFLite
        export_to_tflite(model_path)
        
        print("\n" + "="*60)
        print("Next Steps:")
        print("="*60)
        print("1. Update YoloDetector.kt to use the new model:")
        print('   const val DEFAULT_MODEL_NAME = "food_yolov8n_float32.tflite"')
        print("\n2. Update yolo_labels.txt with food classes")
        print("\n3. Rebuild the Android project")
        print("="*60)

if __name__ == "__main__":
    main()
