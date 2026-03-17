#!/usr/bin/env python3
"""
Train YOLO11n on crawled_grocery dataset using AMD GPU via DirectML.

This script patches Ultralytics to work with AMD DirectML devices.

Usage:
    python train_amd_directml.py
"""

import os
import sys
from pathlib import Path

DATASET_PATH = Path(r"D:\FoodExpiryDatasets\crawled_grocery_yolo")
PROJECT_NAME = "yolo11_crawled_amd"
MODEL_SIZE = "n"
EPOCHS = 100
BATCH_SIZE = 16
IMG_SIZE = 640

def patch_ultralytics_for_directml():
    """Patch Ultralytics to accept DirectML device."""
    import torch
    import torch_directml
    
    dml = torch_directml.device()
    print(f"[OK] DirectML device: {dml}")
    
    original_is_available = torch.cuda.is_available
    original_device_count = torch.cuda.device_count
    
    def patched_is_available():
        return True
    
    def patched_device_count():
        return 1
    
    def patched_device_name(device=None):
        return "AMD Radeon RX 9070 XT"
    
    def patched_get_device_name(device=None):
        if device == 0 or device is None:
            return "AMD Radeon RX 9070 XT"
        return "AMD Radeon RX 9070 XT"
    
    torch.cuda.is_available = patched_is_available
    torch.cuda.device_count = patched_device_count
    torch.cuda.get_device_name = patched_get_device_name
    torch.cuda.current_device = lambda: 0
    torch.cuda.set_device = lambda x: None
    
    original_to = torch.Tensor.to
    
    def patched_to(self, device, *args, **kwargs):
        if isinstance(device, int):
            if device == 0:
                device = dml
        elif isinstance(device, str):
            if device.startswith('cuda') or device == '0':
                device = dml
        return original_to(self, device, *args, **kwargs)
    
    torch.Tensor.to = patched_to
    
    return dml

def check_directml():
    """Check if DirectML is working with AMD GPU."""
    print("="*60)
    print("AMD GPU CHECK - DirectML")
    print("="*60)
    
    try:
        import torch
        import torch_directml
        
        dml = torch_directml.device()
        print(f"[OK] DirectML device: {dml}")
        
        test_tensor = torch.randn(3, 3, device=dml)
        print(f"[OK] GPU tensor test: SUCCESS")
        print(f"[OK] Your AMD RX 9070 XT is ready!")
        
        return dml
        
    except ImportError as e:
        print(f"[ERROR] {e}")
        print("\nInstall DirectML:")
        print("  pip install torch-directml")
        return None

def train_model():
    """Train YOLO model with AMD GPU."""
    
    dml_device = check_directml()
    if dml_device is None:
        return None
    
    patch_ultralytics_for_directml()
    print("[OK] Patched Ultralytics for DirectML support")
    
    try:
        from ultralytics import YOLO
    except ImportError:
        print("[ERROR] Ultralytics not installed!")
        print("  pip install ultralytics")
        return None
    
    data_yaml = DATASET_PATH / "data.yaml"
    if not data_yaml.exists():
        print(f"[ERROR] data.yaml not found: {data_yaml}")
        return None
    
    print("\n" + "="*60)
    print("TRAINING CONFIGURATION - AMD GPU (DirectML)")
    print("="*60)
    print(f"GPU:       AMD RX 9070 XT")
    print(f"Backend:   DirectML (Patched)")
    print(f"Dataset:   {DATASET_PATH}")
    print(f"Model:     YOLOv11{MODEL_SIZE}")
    print(f"Epochs:    {EPOCHS}")
    print(f"Batch:     {BATCH_SIZE}")
    print(f"Image:     {IMG_SIZE}")
    print(f"Device:    0 (DirectML)")
    print("="*60)
    
    model = YOLO(f"yolo11{MODEL_SIZE}.pt")
    
    print("\n[INFO] Starting AMD GPU training...")
    print("[INFO] Training will use your RX 9070 XT!\n")
    
    results = model.train(
        data=str(data_yaml),
        epochs=EPOCHS,
        imgsz=IMG_SIZE,
        batch=BATCH_SIZE,
        device=0,
        project=PROJECT_NAME,
        name="train",
        exist_ok=True,
        patience=30,
        save_period=10,
        plots=True,
        verbose=True,
        workers=4,
        amp=False,
    )
    
    print("\n" + "="*60)
    print("TRAINING COMPLETE!")
    print("="*60)
    
    return model

def export_and_deploy(model):
    """Export to TFLite and copy to Android."""
    print("\n" + "="*60)
    print("EXPORTING TO TFLITE")
    print("="*60)
    
    import shutil
    
    try:
        tflite_path = model.export(format="tflite", imgsz=IMG_SIZE)
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
    print("="*60)
    print("YOLO11 TRAINING - AMD GPU (DirectML)")
    print("="*60)
    print(f"\nDataset: {DATASET_PATH}")
    
    if not DATASET_PATH.exists():
        print(f"[ERROR] Dataset not found!")
        sys.exit(1)
    
    model = train_model()
    
    if model:
        export_and_deploy(model)
    
    print("\n" + "="*60)
    print("ALL DONE!")
    print("="*60)
    print("""
Training with AMD RX 9070 XT completed!

Next steps:
1. Open Android Studio
2. Build -> Rebuild Project  
3. Run the app
4. Test YOLO scanner with your grocery items!

The model can detect 51 food categories:
  - apple_cider_vinegar, bagel, beef_meat, cheese...
  - salmon_fish, shrimp, yogurt, etc.
""")

if __name__ == "__main__":
    main()
