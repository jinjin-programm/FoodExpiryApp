#!/usr/bin/env python3
"""
Train YOLO11 on crawled_grocery dataset - Configurable quality settings.

Usage:
    python train_configurable.py                    # Default: balanced
    python train_configurable.py --quality best     # Best quality
    python train_configurable.py --quality fast     # Quick test
    python train_configurable.py --epochs 200 --model yolo11s  # Custom
"""

import os
import sys
import argparse
from pathlib import Path

DATASET_PATH = Path(r"D:\FoodExpiryDatasets\crawled_grocery_yolo")

QUALITY_PRESETS = {
    "fast": {
        "epochs": 30,
        "imgsz": 416,
        "batch": 16,
        "model": "n",
        "patience": 10,
        "description": "Quick test (~30 min on CPU)"
    },
    "balanced": {
        "epochs": 100,
        "imgsz": 640,
        "batch": 8,
        "model": "n",
        "patience": 30,
        "description": "Good quality (~2-4 hours on CPU)"
    },
    "best": {
        "epochs": 150,
        "imgsz": 640,
        "batch": 8,
        "model": "s",
        "patience": 50,
        "description": "Best quality (~4-8 hours on CPU)"
    },
    "extreme": {
        "epochs": 300,
        "imgsz": 640,
        "batch": 4,
        "model": "m",
        "patience": 100,
        "description": "Maximum quality (~12-24 hours on CPU)"
    }
}

def train(args):
    """Train YOLO model."""
    from ultralytics import YOLO
    
    preset = QUALITY_PRESETS.get(args.quality, QUALITY_PRESETS["balanced"])
    
    epochs = args.epochs or preset["epochs"]
    imgsz = args.imgsz or preset["imgsz"]
    batch = args.batch or preset["batch"]
    model_size = args.model or preset["model"]
    patience = preset["patience"]
    
    print("="*60)
    print("YOLO11 TRAINING CONFIGURATION")
    print("="*60)
    print(f"Quality preset:    {args.quality}")
    print(f"Model:             YOLOv11{model_size}")
    print(f"Epochs:            {epochs}")
    print(f"Image size:        {imgsz}")
    print(f"Batch size:        {batch}")
    print(f"Early stopping:    {patience} epochs patience")
    print(f"Dataset:           {DATASET_PATH}")
    print(f"Classes:           51 food categories")
    print(f"Training images:   2,607")
    print(f"Validation images: 325")
    print("="*60)
    print(f"\nEstimated time: {preset['description']}")
    
    model = YOLO(f"yolo11{model_size}.pt")
    
    print("\n[INFO] Starting training...\n")
    
    results = model.train(
        data=str(DATASET_PATH / "data.yaml"),
        epochs=epochs,
        imgsz=imgsz,
        batch=batch,
        device="cpu",
        project="yolo11_crawled_grocery",
        name="train",
        exist_ok=True,
        patience=patience,
        save_period=10,
        plots=True,
        verbose=True,
        workers=4,
    )
    
    return model

def export_model(model, imgsz):
    """Export to TFLite and copy to Android."""
    import shutil
    
    print("\n" + "="*60)
    print("EXPORTING TO TFLITE")
    print("="*60)
    
    try:
        tflite_path = model.export(format="tflite", imgsz=imgsz)
        print(f"[OK] Exported: {tflite_path}")
    except Exception as e:
        print(f"[ERROR] Export failed: {e}")
        return
    
    project_dir = Path(__file__).parent
    assets_dir = project_dir / "app" / "src" / "main" / "assets"
    ml_dir = project_dir / "app" / "src" / "main" / "ml"
    
    assets_dir.mkdir(parents=True, exist_ok=True)
    ml_dir.mkdir(parents=True, exist_ok=True)
    
    dest_assets = assets_dir / "crawled_grocery_yolo11n.tflite"
    dest_ml = ml_dir / "crawled_grocery_yolo11n.tflite"
    
    if tflite_path and os.path.exists(tflite_path):
        shutil.copy2(tflite_path, dest_assets)
        shutil.copy2(tflite_path, dest_ml)
        print(f"[OK] Copied to: {dest_assets}")
        print(f"[OK] Copied to: {dest_ml}")
    
    labels_src = DATASET_PATH / "classes.txt"
    labels_dst = assets_dir / "yolo_labels_crawled_grocery.txt"
    
    if labels_src.exists():
        shutil.copy2(labels_src, labels_dst)
        print(f"[OK] Copied labels: {labels_dst}")

def main():
    parser = argparse.ArgumentParser(description='Train YOLO11 on crawled_grocery')
    
    parser.add_argument('--quality', '-q', type=str, default='balanced',
                        choices=['fast', 'balanced', 'best', 'extreme'],
                        help='Quality preset (default: balanced)')
    parser.add_argument('--epochs', '-e', type=int, default=None,
                        help='Override epochs')
    parser.add_argument('--imgsz', '-i', type=int, default=None,
                        help='Override image size')
    parser.add_argument('--batch', '-b', type=int, default=None,
                        help='Override batch size')
    parser.add_argument('--model', '-m', type=str, default=None,
                        choices=['n', 's', 'm', 'l', 'x'],
                        help='Override model size')
    
    args = parser.parse_args()
    
    if not DATASET_PATH.exists():
        print(f"[ERROR] Dataset not found: {DATASET_PATH}")
        sys.exit(1)
    
    preset = QUALITY_PRESETS[args.quality]
    imgsz = args.imgsz or preset["imgsz"]
    
    model = train(args)
    export_model(model, imgsz)
    
    print("\n" + "="*60)
    print("TRAINING COMPLETE!")
    print("="*60)
    print("""
Model saved to: runs/detect/yolo11_crawled_grocery/train/weights/

Next steps:
1. Open Android Studio
2. Build -> Rebuild Project
3. Run the app
4. Test with your grocery items!
""")

if __name__ == "__main__":
    main()
