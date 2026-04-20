#!/usr/bin/env python3
"""
Improved YOLO11 training script for crawled_grocery dataset.

Uses:
- YOLO11s (better accuracy than nano)
- 200 epochs (longer training)
- Better augmentation settings
- Automatic TFLite export

Usage:
    python train_improved.py                    # Use current dataset
    python train_improved.py --roboflow         # Use Roboflow-annotated dataset
"""

import os
import sys
import argparse
from pathlib import Path

PROJECT_DIR = Path(__file__).parent

DEFAULT_DATASET = Path(r"D:\FoodExpiryDatasets\crawled_grocery_yolo")
ROBOFLOW_DATASET = Path(r"D:\FoodExpiryDatasets\crawled_grocery_annotated")

PROJECT_NAME = "yolo11_crawled_improved"
MODEL_SIZE = "s"
EPOCHS = 200
BATCH_SIZE = 16
IMG_SIZE = 640
PATIENCE = 50

def find_dataset(use_roboflow=False):
    """Find the dataset to use for training."""
    if use_roboflow:
        if ROBOFLOW_DATASET.exists():
            data_yaml = ROBOFLOW_DATASET / "data.yaml"
            if data_yaml.exists():
                print(f"[OK] Using Roboflow dataset: {ROBOFLOW_DATASET}")
                return data_yaml
            else:
                print(f"[WARN] data.yaml not found in {ROBOFLOW_DATASET}")
    
    if DEFAULT_DATASET.exists():
        data_yaml = DEFAULT_DATASET / "data.yaml"
        if data_yaml.exists():
            print(f"[OK] Using default dataset: {DEFAULT_DATASET}")
            return data_yaml
    
    print("[ERROR] No valid dataset found!")
    return None

def check_environment():
    """Check if training environment is ready."""
    print("="*60)
    print("ENVIRONMENT CHECK")
    print("="*60)
    
    try:
        import torch
        print(f"[OK] PyTorch: {torch.__version__}")
        if torch.cuda.is_available():
            print(f"[OK] GPU: {torch.cuda.get_device_name(0)}")
        else:
            print("[INFO] Using CPU (slower but works)")
    except ImportError:
        print("[ERROR] PyTorch not installed!")
        print("Run: pip install torch torchvision")
        return False
    
    try:
        from ultralytics import YOLO
        import ultralytics
        print(f"[OK] Ultralytics: {ultralytics.__version__}")
    except ImportError:
        print("[ERROR] Ultralytics not installed!")
        print("Run: pip install ultralytics")
        return False
    
    return True

def train(data_yaml, model_size="s", epochs=200, resume=None):
    """Train the YOLO model."""
    from ultralytics import YOLO
    
    print("\n" + "="*60)
    print("TRAINING CONFIGURATION")
    print("="*60)
    print(f"Model:      YOLOv11{model_size}")
    print(f"Epochs:     {epochs}")
    print(f"Batch:      {BATCH_SIZE}")
    print(f"Image size: {IMG_SIZE}")
    print(f"Dataset:    {data_yaml}")
    print(f"Patience:   {PATIENCE}")
    print("="*60)
    
    if resume and os.path.exists(resume):
        print(f"\n[INFO] Resuming from: {resume}")
        model = YOLO(resume)
    else:
        print(f"\n[INFO] Loading pretrained YOLOv11{model_size}")
        model = YOLO(f"yolo11{model_size}.pt")
    
    print("[INFO] Starting training...")
    print("[INFO] This will take several hours (~4-8 hours on CPU)\n")
    
    results = model.train(
        data=str(data_yaml),
        epochs=epochs,
        imgsz=IMG_SIZE,
        batch=BATCH_SIZE,
        device="cpu",
        project=PROJECT_NAME,
        name="train",
        exist_ok=True,
        patience=PATIENCE,
        save_period=10,
        plots=True,
        verbose=True,
        workers=4,
        lr0=0.001,
        lrf=0.01,
        momentum=0.937,
        weight_decay=0.0005,
        warmup_epochs=3,
        warmup_momentum=0.8,
        warmup_bias_lr=0.1,
        box=7.5,
        cls=0.5,
        dfl=1.5,
        hsv_h=0.015,
        hsv_s=0.7,
        hsv_v=0.4,
        degrees=0.0,
        translate=0.1,
        scale=0.5,
        shear=0.0,
        perspective=0.0,
        flipud=0.0,
        fliplr=0.5,
        mosaic=1.0,
        mixup=0.0,
        copy_paste=0.0,
    )
    
    return model

def export_to_android(model):
    """Export model to TFLite and copy to Android project."""
    print("\n" + "="*60)
    print("EXPORTING TO TFLITE")
    print("="*60)
    
    try:
        tflite_path = model.export(format="tflite", imgsz=IMG_SIZE)
        print(f"[OK] Exported: {tflite_path}")
    except Exception as e:
        print(f"[ERROR] Export failed: {e}")
        tflite_path = None
    
    if tflite_path and os.path.exists(tflite_path):
        import shutil
        
        assets_dir = PROJECT_DIR / "app" / "src" / "main" / "assets"
        ml_dir = PROJECT_DIR / "app" / "src" / "main" / "ml"
        
        assets_dir.mkdir(parents=True, exist_ok=True)
        ml_dir.mkdir(parents=True, exist_ok=True)
        
        dest_name = "crawled_grocery_yolo11s.tflite"
        
        shutil.copy2(tflite_path, assets_dir / dest_name)
        shutil.copy2(tflite_path, ml_dir / dest_name)
        
        print(f"[OK] Copied to: {assets_dir / dest_name}")
        print(f"[OK] Copied to: {ml_dir / dest_name}")
        
        data_yaml = find_dataset(use_roboflow=False)
        if data_yaml:
            classes_src = data_yaml.parent / "classes.txt"
            labels_dst = assets_dir / "yolo_labels_crawled_grocery.txt"
            
            if classes_src.exists():
                shutil.copy2(classes_src, labels_dst)
                print(f"[OK] Updated labels: {labels_dst}")

def print_results():
    """Print training results summary."""
    print("\n" + "="*60)
    print("TRAINING COMPLETE!")
    print("="*60)
    print(f"""
Model saved to: {PROJECT_NAME}/train/weights/

To check training quality:
1. Open runs/detect/{PROJECT_NAME}/train/results.png
2. Look at mAP@50 - should be 70%+ for good model
3. Check confusion_matrix.png for class accuracy

To use in Android app:
1. Open Android Studio
2. Build -> Rebuild Project
3. Run the app

Model will detect 51 food categories with improved accuracy!
""")

def main():
    parser = argparse.ArgumentParser(description='Improved YOLO11 training')
    parser.add_argument('--roboflow', action='store_true',
                        help='Use Roboflow-annotated dataset')
    parser.add_argument('--model', type=str, default='s',
                        choices=['n', 's', 'm'],
                        help='Model size (n=nano, s=small, m=medium)')
    parser.add_argument('--epochs', type=int, default=200,
                        help='Number of training epochs')
    parser.add_argument('--resume', type=str, default=None,
                        help='Resume training from checkpoint')
    
    args = parser.parse_args()
    
    print("="*60)
    print("YOLO11 IMPROVED TRAINING")
    print("="*60)
    
    if not check_environment():
        sys.exit(1)
    
    data_yaml = find_dataset(args.roboflow)
    if data_yaml is None:
        print("\n[ERROR] No dataset found!")
        print("\nOptions:")
        print("1. Use current dataset: python train_improved.py")
        print("2. Annotate with Roboflow first, then: python train_improved.py --roboflow")
        sys.exit(1)
    
    model = train(data_yaml, args.model, args.epochs, args.resume)
    export_to_android(model)
    print_results()

if __name__ == "__main__":
    main()
