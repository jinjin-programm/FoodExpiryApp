#!/usr/bin/env python3
"""
Train YOLO11n on crawled_grocery dataset with AMD ROCm support.

This script:
1. Loads YOLO11n pre-trained weights
2. Trains on the annotated dataset
3. Validates and tests the model
4. Exports best model to various formats

Requirements (run in WSL2 with ROCm):
    pip install ultralytics

For AMD ROCm GPU support:
    pip3 install torch torchvision --index-url https://download.pytorch.org/whl/rocm6.0

Usage:
    # Train with default settings
    python train_yolo11_amd.py

    # Custom parameters
    python train_yolo11_amd.py --epochs 100 --batch 16 --imgsz 640

    # Resume training
    python train_yolo11_amd.py --resume runs/detect/train/weights/last.pt
"""

import os
import sys
import argparse
from pathlib import Path

DATASET_PATH = r"D:\FoodExpiryDatasets\crawled_grocery_yolo"
PROJECT_NAME = "yolo11_crawled_grocery"
MODEL_SIZE = "n"

def check_environment():
    """Check if the environment is ready for training."""
    print("="*60)
    print("ENVIRONMENT CHECK")
    print("="*60)
    
    try:
        import torch
        print(f"[OK] PyTorch version: {torch.__version__}")
        
        if torch.cuda.is_available():
            print(f"[OK] CUDA available: {torch.cuda.get_device_name(0)}")
            print(f"[OK] GPU Memory: {torch.cuda.get_device_properties(0).total_memory / 1e9:.1f} GB")
        else:
            print("[WARN] CUDA not available - will use CPU (slower)")
            print("       For AMD GPU: Install ROCm and PyTorch with ROCm support")
        
    except ImportError:
        print("[ERROR] PyTorch not installed!")
        print("Install: pip install torch torchvision")
        return False
    
    try:
        from ultralytics import YOLO
        import ultralytics
        print(f"[OK] Ultralytics version: {ultralytics.__version__}")
    except ImportError:
        print("[ERROR] Ultralytics not installed!")
        print("Install: pip install ultralytics")
        return False
    
    if not os.path.exists(DATASET_PATH):
        print(f"[ERROR] Dataset not found: {DATASET_PATH}")
        print("Run convert_to_yolo_format.py first!")
        return False
    
    data_yaml = os.path.join(DATASET_PATH, "data.yaml")
    if not os.path.exists(data_yaml):
        print(f"[ERROR] data.yaml not found: {data_yaml}")
        return False
    
    print(f"[OK] Dataset found: {DATASET_PATH}")
    
    labels_count = 0
    for split in ['train', 'val', 'test']:
        labels_dir = os.path.join(DATASET_PATH, split, 'labels')
        if os.path.exists(labels_dir):
            for f in os.listdir(labels_dir):
                if f.endswith('.txt') and os.path.getsize(os.path.join(labels_dir, f)) > 0:
                    labels_count += 1
    
    if labels_count == 0:
        print("[WARN] No annotations found!")
        print("       You need to annotate your data first.")
        print("       Use LabelImg, Roboflow, or Label Studio")
        return False
    
    print(f"[OK] Found {labels_count} annotated labels")
    
    return True

def train_model(args):
    """Train the YOLO model."""
    from ultralytics import YOLO
    
    print("\n" + "="*60)
    print("TRAINING CONFIGURATION")
    print("="*60)
    print(f"Model:       YOLOv11{MODEL_SIZE}")
    print(f"Dataset:     {DATASET_PATH}")
    print(f"Epochs:      {args.epochs}")
    print(f"Batch size:  {args.batch}")
    print(f"Image size:  {args.imgsz}")
    print(f"Device:      {args.device}")
    print(f"Workers:     {args.workers}")
    print("="*60)
    
    if args.resume:
        print(f"\n[INFO] Resuming from: {args.resume}")
        model = YOLO(args.resume)
    else:
        print(f"\n[INFO] Loading pretrained YOLOv11{MODEL_SIZE}")
        model = YOLO(f"yolo11{MODEL_SIZE}.pt")
    
    results = model.train(
        data=os.path.join(DATASET_PATH, "data.yaml"),
        epochs=args.epochs,
        imgsz=args.imgsz,
        batch=args.batch,
        device=args.device,
        workers=args.workers,
        project=PROJECT_NAME,
        name="train",
        exist_ok=True,
        patience=50,
        save_period=10,
        plots=True,
        verbose=True,
        amp=False,
    )
    
    return model, results

def validate_model(model):
    """Validate the trained model."""
    print("\n" + "="*60)
    print("VALIDATION")
    print("="*60)
    
    metrics = model.val(
        data=os.path.join(DATASET_PATH, "data.yaml"),
        split="val",
        plots=True,
    )
    
    print(f"\nmAP@50:    {metrics.box.map50:.4f}")
    print(f"mAP@50-95: {metrics.box.map:.4f}")
    
    return metrics

def export_model(model, export_formats=None):
    """Export model to different formats."""
    if export_formats is None:
        export_formats = ['tflite', 'onnx']
    
    print("\n" + "="*60)
    print("EXPORTING MODEL")
    print("="*60)
    
    exported_paths = []
    
    for fmt in export_formats:
        print(f"\n[INFO] Exporting to {fmt.upper()}...")
        try:
            path = model.export(format=fmt, imgsz=640)
            print(f"[OK] Exported to: {path}")
            exported_paths.append((fmt, path))
        except Exception as e:
            print(f"[ERROR] Failed to export {fmt}: {e}")
    
    return exported_paths

def print_next_steps(model, exported_paths):
    """Print instructions for next steps."""
    print("\n" + "="*60)
    print("TRAINING COMPLETE!")
    print("="*60)
    
    print(f"\nModel saved to: {PROJECT_NAME}/train/weights/")
    print(f"  - best.pt  (best checkpoint)")
    print(f"  - last.pt  (final checkpoint)")
    
    print(f"\nExported models:")
    for fmt, path in exported_paths:
        print(f"  - {fmt.upper()}: {path}")
    
    print("\nTo use in your Android app:")
    print("  1. Copy the TFLite model to:")
    print(f"     app/src/main/assets/yolo_crawled_grocery.tflite")
    print("  2. Update yolo_labels.txt with the 49 class names")
    print("  3. Rebuild and run the app")
    
    print("\nTo continue training or improve results:")
    print("  - Increase epochs for better accuracy")
    print("  - Add more annotated images")
    print("  - Try data augmentation")
    print("  - Use larger model (yolo11s, yolo11m)")

def main():
    parser = argparse.ArgumentParser(description='Train YOLO11 on crawled_grocery')
    parser.add_argument('--epochs', type=int, default=100, help='Training epochs')
    parser.add_argument('--batch', type=int, default=16, help='Batch size')
    parser.add_argument('--imgsz', type=int, default=640, help='Image size')
    parser.add_argument('--device', type=str, default='0', 
                        help='Device (0 for GPU, cpu for CPU)')
    parser.add_argument('--workers', type=int, default=8, help='Data loader workers')
    parser.add_argument('--resume', type=str, default=None, 
                        help='Resume from checkpoint')
    parser.add_argument('--export', nargs='+', default=['tflite', 'onnx'],
                        help='Export formats (tflite, onnx, torchscript)')
    
    args = parser.parse_args()
    
    if not check_environment():
        sys.exit(1)
    
    model, results = train_model(args)
    metrics = validate_model(model)
    exported_paths = export_model(model, args.export)
    print_next_steps(model, exported_paths)

if __name__ == "__main__":
    main()
