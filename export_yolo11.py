#!/usr/bin/env python3
"""
Export YOLO11n model to TensorFlow Lite format.
Run this script to generate yolo11n_float32.tflite

Requirements:
    pip install ultralytics

Usage:
    python export_yolo11.py
"""

from ultralytics import YOLO
import os

def export_yolo11_tflite():
    """Download and export YOLO11n to TFLite format."""
    
    print("Downloading YOLO11n model...")
    # Load the YOLO11n model (downloads automatically)
    model = YOLO("yolo11n.pt")
    
    print("Exporting to TensorFlow Lite format...")
    # Export to TFLite format
    # This creates yolo11n_float32.tflite in the current directory
    model.export(format="tflite", imgsz=640)
    
    # The export creates a directory with the model
    tflite_path = "yolo11n_saved_model/yolo11n_float32.tflite"
    
    if os.path.exists(tflite_path):
        print(f"\n✓ Success! Model exported to: {tflite_path}")
        print(f"\nNext steps:")
        print(f"1. Copy the file to your Android project:")
        print(f"   cp {tflite_path} /path/to/FoodExpiryAPP/app/src/main/assets/yolo11n_float32.tflite")
        print(f"\n2. Or manually copy the file from: yolo11n_saved_model/yolo11n_float32.tflite")
    else:
        # Check for alternative paths
        alternative_paths = [
            "yolo11n_float32.tflite",
            "yolo11n.tflite",
        ]
        
        for path in alternative_paths:
            if os.path.exists(path):
                print(f"\n✓ Success! Model exported to: {path}")
                print(f"\nNext steps:")
                print(f"1. Copy the file to your Android project:")
                print(f"   cp {path} /path/to/FoodExpiryAPP/app/src/main/assets/yolo11n_float32.tflite")
                return
        
        print("\n⚠ Model export completed but file location unknown.")
        print("Check the current directory for .tflite files:")
        os.system("ls -la *.tflite 2>/dev/null || find . -name '*.tflite' -type f")

if __name__ == "__main__":
    try:
        export_yolo11_tflite()
    except Exception as e:
        print(f"\n✗ Error: {e}")
        print("\nMake sure you have ultralytics installed:")
        print("   pip install ultralytics")
        exit(1)
