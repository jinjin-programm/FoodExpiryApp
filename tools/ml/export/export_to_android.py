#!/usr/bin/env python3
"""
Export trained YOLO11 model to TFLite and copy to Android project.

This script:
1. Loads the best trained model
2. Exports to TFLite format (float32 and float16)
3. Copies models to Android assets
4. Updates the labels file

Usage:
    python export_to_android.py
    python export_to_android.py --model yolo11_crawled_grocery/train/weights/best.pt
"""

import os
import shutil
import argparse
from pathlib import Path

PROJECT_DIR = Path(__file__).parent
ANDROID_ASSETS = PROJECT_DIR / "app" / "src" / "main" / "assets"
ANDROID_ML = PROJECT_DIR / "app" / "src" / "main" / "ml"
DATASET_PATH = r"D:\FoodExpiryDatasets\crawled_grocery_yolo"

def export_to_tflite(model_path, output_dir):
    """Export model to TFLite format."""
    from ultralytics import YOLO
    
    print(f"\n[INFO] Loading model: {model_path}")
    model = YOLO(model_path)
    
    os.makedirs(output_dir, exist_ok=True)
    
    print("[INFO] Exporting to TFLite (float32)...")
    tflite_path = model.export(format="tflite", imgsz=640)
    print(f"[OK] Exported: {tflite_path}")
    
    return tflite_path

def copy_to_android(tflite_path, model_name="crawled_grocery"):
    """Copy TFLite model to Android project."""
    if not os.path.exists(tflite_path):
        print(f"[ERROR] TFLite file not found: {tflite_path}")
        return None
    
    android_dest = ANDROID_ASSETS / f"yolo_{model_name}_float32.tflite"
    
    ANDROID_ASSETS.mkdir(parents=True, exist_ok=True)
    
    shutil.copy2(tflite_path, android_dest)
    print(f"[OK] Copied to: {android_dest}")
    
    ml_dest = ANDROID_ML / f"yolo_{model_name}.tflite"
    ANDROID_ML.mkdir(parents=True, exist_ok=True)
    shutil.copy2(tflite_path, ml_dest)
    print(f"[OK] Copied to: {ml_dest}")
    
    return android_dest

def update_labels_file(dataset_path):
    """Update yolo_labels.txt with the new class names."""
    import yaml
    
    data_yaml = os.path.join(dataset_path, "data.yaml")
    
    if not os.path.exists(data_yaml):
        print(f"[WARN] data.yaml not found: {data_yaml}")
        return
    
    with open(data_yaml, 'r') as f:
        data = yaml.safe_load(f)
    
    class_names = data.get('names', {})
    
    if isinstance(class_names, dict):
        names_list = [class_names[i] for i in sorted(class_names.keys())]
    elif isinstance(class_names, list):
        names_list = class_names
    else:
        print(f"[WARN] Could not parse class names")
        return
    
    labels_file = ANDROID_ASSETS / "yolo_labels_crawled_grocery.txt"
    
    with open(labels_file, 'w') as f:
        for name in names_list:
            f.write(name + '\n')
    
    print(f"[OK] Updated labels: {labels_file}")
    print(f"     {len(names_list)} classes: {', '.join(names_list[:5])}...")
    
    return names_list

def print_integration_guide(names_list):
    """Print instructions for integrating the model."""
    print("\n" + "="*60)
    print("ANDROID INTEGRATION GUIDE")
    print("="*60)
    
    print("""
1. Update YoloScanner.kt to use the new model:
   
   private val MODEL_FILE = "yolo_crawled_grocery_float32.tflite"
   private val LABELS_FILE = "yolo_labels_crawled_grocery.txt"

2. The model can detect these items:
""")
    if names_list:
        for i in range(0, len(names_list), 5):
            batch = names_list[i:i+5]
            print(f"   {', '.join(batch)}")
    
    print("""
3. Build and run the app:
   - In Android Studio: Build -> Rebuild Project
   - Run on device or emulator

4. Test the scanner:
   - Open the app
   - Navigate to YOLO Scanner
   - Point camera at grocery items
   - Check if detection works correctly
""")

def main():
    parser = argparse.ArgumentParser(description='Export YOLO11 to Android')
    parser.add_argument('--model', type=str, 
                        default='yolo11_crawled_grocery/train/weights/best.pt',
                        help='Path to trained model')
    parser.add_argument('--name', type=str, default='crawled_grocery',
                        help='Model name for output files')
    
    args = parser.parse_args()
    
    print("="*60)
    print("YOLO11 -> ANDROID EXPORT")
    print("="*60)
    
    model_path = args.model
    if not os.path.exists(model_path):
        print(f"[ERROR] Model not found: {model_path}")
        print("\nLooking for alternative paths...")
        
        alternatives = [
            "yolo11_crawled_grocery/train/weights/best.pt",
            "runs/detect/train/weights/best.pt",
            "yolo11n.pt",
        ]
        
        for alt in alternatives:
            if os.path.exists(alt):
                print(f"[OK] Found: {alt}")
                model_path = alt
                break
        else:
            print("[ERROR] No model found. Train the model first!")
            print("Run: python train_yolo11_amd.py")
            return
    
    output_dir = PROJECT_DIR / "exported_models"
    tflite_path = export_to_tflite(model_path, output_dir)
    
    if tflite_path and os.path.exists(tflite_path):
        android_path = copy_to_android(tflite_path, args.name)
    
    names_list = update_labels_file(DATASET_PATH)
    
    print_integration_guide(names_list or [])
    
    print("\n" + "="*60)
    print("EXPORT COMPLETE!")
    print("="*60)

if __name__ == "__main__":
    main()
