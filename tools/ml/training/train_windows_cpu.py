#!/usr/bin/env python3
"""
Train YOLO11n on crawled_grocery dataset - Windows CPU version.

This script trains on Windows using CPU (works reliably).

Usage:
    python train_windows_cpu.py
"""

import os
import sys
from pathlib import Path

DATASET_PATH = Path(r"D:\FoodExpiryDatasets\crawled_grocery_yolo")
PROJECT_NAME = "yolo11_crawled_grocery"
MODEL_SIZE = "n"
EPOCHS = 50
BATCH_SIZE = 8
IMG_SIZE = 640

def check_environment():
    """Check if environment is ready."""
    print("="*60)
    print("ENVIRONMENT CHECK - Windows CPU Training")
    print("="*60)
    
    try:
        import torch
        print(f"[OK] PyTorch version: {torch.__version__}")
        if torch.cuda.is_available():
            print(f"[OK] CUDA available: {torch.cuda.get_device_name(0)}")
            print("[INFO] Will use GPU for faster training!")
            return "cuda"
        else:
            print("[INFO] Using CPU (slower but reliable)")
            return "cpu"
    except ImportError:
        print("[ERROR] PyTorch not installed!")
        return None
    
    try:
        from ultralytics import YOLO
        print("[OK] Ultralytics installed")
    except ImportError:
        print("[ERROR] Ultralytics not installed!")
        return None

def train_model():
    """Train YOLO model."""
    from ultralytics import YOLO
    
    device = check_environment()
    if device is None:
        return
    
    data_yaml = DATASET_PATH / "data.yaml"
    if not data_yaml.exists():
        print(f"[ERROR] data.yaml not found: {data_yaml}")
        return
    
    print("\n" + "="*60)
    print("TRAINING CONFIGURATION")
    print("="*60)
    print(f"Dataset: {DATASET_PATH}")
    print(f"Model:   YOLOv11{MODEL_SIZE}")
    print(f"Epochs:  {EPOCHS}")
    print(f"Batch:   {BATCH_SIZE}")
    print(f"Image:   {IMG_SIZE}")
    print(f"Device:  {device}")
    print("="*60)
    
    model = YOLO(f"yolo11{MODEL_SIZE}.pt")
    
    print("\n[INFO] Starting training...")
    print("[INFO] This will take several hours on CPU.")
    print("[INFO] Training progress will be displayed below.\n")
    
    results = model.train(
        data=str(data_yaml),
        epochs=EPOCHS,
        imgsz=IMG_SIZE,
        batch=BATCH_SIZE,
        device=device,
        project=PROJECT_NAME,
        name="train",
        exist_ok=True,
        patience=20,
        save_period=5,
        plots=True,
        verbose=True,
        workers=4,
    )
    
    print("\n" + "="*60)
    print("TRAINING COMPLETE!")
    print("="*60)
    
    print(f"\nModel saved to: {PROJECT_NAME}/train/weights/")
    print("  - best.pt  (best model)")
    print("  - last.pt  (final epoch)")
    
    return model

def export_model(model):
    """Export model to TFLite for Android."""
    print("\n" + "="*60)
    print("EXPORTING TO TFLITE")
    print("="*60)
    
    try:
        tflite_path = model.export(format="tflite", imgsz=IMG_SIZE)
        print(f"[OK] Exported: {tflite_path}")
        return tflite_path
    except Exception as e:
        print(f"[ERROR] Export failed: {e}")
        return None

def copy_to_android(tflite_path):
    """Copy model to Android project."""
    import shutil
    
    if not tflite_path or not os.path.exists(tflite_path):
        print("[WARN] No TFLite file to copy")
        return
    
    project_dir = Path(__file__).parent
    assets_dir = project_dir / "app" / "src" / "main" / "assets"
    ml_dir = project_dir / "app" / "src" / "main" / "ml"
    
    assets_dir.mkdir(parents=True, exist_ok=True)
    ml_dir.mkdir(parents=True, exist_ok=True)
    
    dest_assets = assets_dir / "crawled_grocery_yolo11n.tflite"
    dest_ml = ml_dir / "crawled_grocery_yolo11n.tflite"
    
    shutil.copy2(tflite_path, dest_assets)
    shutil.copy2(tflite_path, dest_ml)
    
    print(f"[OK] Copied to: {dest_assets}")
    print(f"[OK] Copied to: {dest_ml}")
    
    update_labels()

def update_labels():
    """Update yolo_labels.txt with new class names."""
    labels_src = DATASET_PATH / "classes.txt"
    project_dir = Path(__file__).parent
    labels_dst = project_dir / "app" / "src" / "main" / "assets" / "yolo_labels_crawled_grocery.txt"
    
    if labels_src.exists():
        import shutil
        shutil.copy2(labels_src, labels_dst)
        print(f"[OK] Updated labels: {labels_dst}")
        
        with open(labels_src, 'r') as f:
            classes = [line.strip() for line in f if line.strip()]
        print(f"[OK] {len(classes)} classes loaded")

def main():
    print("="*60)
    print("YOLO11 TRAINING - Windows")
    print("="*60)
    
    if not DATASET_PATH.exists():
        print(f"[ERROR] Dataset not found: {DATASET_PATH}")
        sys.exit(1)
    
    model = train_model()
    
    if model:
        tflite_path = export_model(model)
        copy_to_android(tflite_path)
    
    print("\n" + "="*60)
    print("ALL DONE!")
    print("="*60)
    print("""
Next steps:
1. Open Android Studio
2. Build -> Rebuild Project
3. Run the app
4. Test YOLO scanner with grocery items!
""")

if __name__ == "__main__":
    main()
