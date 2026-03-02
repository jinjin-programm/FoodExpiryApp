#!/usr/bin/env python3
"""
Train YOLO11 on AMD RX 9070 XT using native ROCm on Windows.

Requirements (Python 3.12):
    pip install C:\\rocm_wheels\\rocm_sdk_core-7.2.0.dev0-py3-none-win_amd64.whl
    pip install C:\\rocm_wheels\\rocm_sdk_devel-7.2.0.dev0-py3-none-win_amd64.whl
    pip install C:\\rocm_wheels\\rocm_sdk_libraries_custom-7.2.0.dev0-py3-none-win_amd64.whl
    pip install C:\\rocm_wheels\\rocm-7.2.0.dev0.tar.gz
    pip install C:\\rocm_wheels\\torch-2.9.1+rocmsdk20260116-cp312-cp312-win_amd64.whl
    pip install C:\\rocm_wheels\\torchvision-0.24.1+rocmsdk20260116-cp312-cp312-win_amd64.whl
    pip install ultralytics

Usage:
    python3.12 train_rocm_win.py
    python3.12 train_rocm_win.py --epochs 150 --batch 32 --model yolo11s
    python3.12 train_rocm_win.py --resume runs/detect/train/weights/last.pt
"""

import os
import sys
import argparse
from pathlib import Path

# ── Fix: add Python Scripts dir to PATH so offload-arch.exe is found by ROCm ──
_scripts_dir = Path(sys.executable).parent / "Scripts"
if _scripts_dir.exists() and str(_scripts_dir) not in os.environ.get("PATH", ""):
    os.environ["PATH"] = str(_scripts_dir) + os.pathsep + os.environ.get("PATH", "")

# ── Configuration ─────────────────────────────────────────────────────────────
DATASET_PATH = Path(r"D:\FoodExpiryDatasets\crawled_grocery_yolo")
PROJECT_NAME = "yolo11_rocm"
DEFAULT_MODEL = "yolo11s"   # n=nano  s=small  m=medium  l=large  x=xlarge
DEFAULT_EPOCHS = 150
DEFAULT_BATCH = 32          # RX 9070 XT has 16 GB VRAM — can handle large batches
DEFAULT_IMGSZ = 640
DEFAULT_WORKERS = 4         # Windows has lower multiprocessing overhead limit


def check_rocm():
    """Verify ROCm PyTorch + AMD GPU are ready."""
    print("=" * 60)
    print("ROCm ENVIRONMENT CHECK")
    print("=" * 60)

    # Check Python version
    import platform
    print(f"[OK] Python {sys.version.split()[0]}")

    # Check torch
    try:
        import torch
        print(f"[OK] PyTorch:  {torch.__version__}")
        print(f"[OK] HIP:      {torch.version.hip}")
    except ImportError:
        print("[ERROR] PyTorch not installed!")
        print("  Run: pip install C:\\rocm_wheels\\torch-2.9.1+rocmsdk20260116-cp312-cp312-win_amd64.whl")
        return False

    # Check GPU
    if not torch.cuda.is_available():
        print("[ERROR] No ROCm GPU detected!")
        print("  Make sure the AMD ROCm SDK wheels are installed.")
        return False

    gpu_name = torch.cuda.get_device_name(0)
    vram_gb  = torch.cuda.get_device_properties(0).total_memory / 1e9
    print(f"[OK] GPU:      {gpu_name}")
    print(f"[OK] VRAM:     {vram_gb:.1f} GB")

    # Quick tensor test
    try:
        t = torch.randn(512, 512, device="cuda")
        _ = torch.mm(t, t)
        print("[OK] GPU compute test: PASSED")
    except Exception as e:
        print(f"[ERROR] GPU compute test failed: {e}")
        return False

    # Check ultralytics
    try:
        import ultralytics
        print(f"[OK] Ultralytics: {ultralytics.__version__}")
    except ImportError:
        print("[ERROR] Ultralytics not installed!")
        print("  Run: pip install ultralytics")
        return False

    # Check dataset
    data_yaml = DATASET_PATH / "data.yaml"
    if not data_yaml.exists():
        print(f"[ERROR] Dataset not found: {DATASET_PATH}")
        print("  Run convert_to_yolo_format.py first.")
        return False

    # Count annotated labels
    label_count = sum(
        1 for split in ("train", "val", "test")
        for p in (DATASET_PATH / split / "labels").glob("*.txt")
        if p.stat().st_size > 0
    )
    if label_count == 0:
        print("[ERROR] No annotations found in dataset!")
        return False

    print(f"[OK] Dataset:  {DATASET_PATH}")
    print(f"[OK] Labels:   {label_count} annotated files")
    print("=" * 60)
    return True


def train(args):
    """Run YOLO training on AMD GPU via ROCm."""
    from ultralytics import YOLO
    import torch

    model_name = f"{args.model}.pt"
    device     = "cuda"   # ROCm presents itself as CUDA to PyTorch

    print()
    print("=" * 60)
    print("TRAINING — AMD RX 9070 XT  (ROCm/HIP)")
    print("=" * 60)
    print(f"  Model:    {args.model}")
    print(f"  Epochs:   {args.epochs}")
    print(f"  Batch:    {args.batch}")
    print(f"  Img size: {args.imgsz}")
    print(f"  Device:   {device}  ({torch.cuda.get_device_name(0)})")
    print(f"  Workers:  {args.workers}")
    if args.resume:
        print(f"  Resume:   {args.resume}")
    print("=" * 60)

    # Load model (fresh or resume)
    if args.resume:
        print(f"\n[INFO] Resuming from {args.resume}")
        model = YOLO(args.resume)
    else:
        print(f"\n[INFO] Loading pretrained {model_name}")
        model = YOLO(model_name)

    # ── Train ──────────────────────────────────────────────────────────────────
    results = model.train(
        data        = str(DATASET_PATH / "data.yaml"),
        epochs      = args.epochs,
        imgsz       = args.imgsz,
        batch       = args.batch,
        device      = device,
        workers     = args.workers,
        project     = PROJECT_NAME,
        name        = "train",
        exist_ok    = True,
        patience    = 50,
        save_period = 10,
        plots       = True,
        verbose     = True,
        amp         = False,   # Disable AMP — avoids occasional HIP fp16 issues
        cos_lr      = True,    # Cosine LR schedule
        warmup_epochs = 3,
    )

    return model, results


def validate(model):
    """Validate the trained model."""
    print()
    print("=" * 60)
    print("VALIDATION")
    print("=" * 60)

    metrics = model.val(
        data  = str(DATASET_PATH / "data.yaml"),
        split = "val",
        plots = True,
    )

    print(f"\n  mAP@50:     {metrics.box.map50:.4f}")
    print(f"  mAP@50-95:  {metrics.box.map:.4f}")
    print(f"  Precision:  {metrics.box.mp:.4f}")
    print(f"  Recall:     {metrics.box.mr:.4f}")
    return metrics


def export(model, imgsz):
    """Export best weights to TFLite + ONNX for Android deployment."""
    print()
    print("=" * 60)
    print("EXPORT")
    print("=" * 60)

    exported = {}
    for fmt in ("onnx", "tflite"):
        print(f"\n[INFO] Exporting to {fmt.upper()}...")
        try:
            path = model.export(format=fmt, imgsz=imgsz)
            print(f"[OK]  Saved: {path}")
            exported[fmt] = path
        except Exception as e:
            print(f"[WARN] {fmt.upper()} export failed: {e}")

    return exported


def deploy_to_android(exported, model_name):
    """Copy exported TFLite model into Android assets."""
    import shutil

    tflite_path = exported.get("tflite")
    if not tflite_path or not Path(tflite_path).exists():
        print("[WARN] No TFLite model to deploy.")
        return

    project_root = Path(__file__).parent
    assets_dir   = project_root / "app" / "src" / "main" / "assets"
    ml_dir       = project_root / "app" / "src" / "main" / "ml"
    assets_dir.mkdir(parents=True, exist_ok=True)
    ml_dir.mkdir(parents=True, exist_ok=True)

    dest_name = f"{model_name}_rocm.tflite"
    for dst in (assets_dir / dest_name, ml_dir / dest_name):
        shutil.copy2(tflite_path, dst)
        print(f"[OK]  Deployed: {dst}")

    # Copy class labels
    for label_src in (DATASET_PATH / "classes.txt", DATASET_PATH / "data.yaml"):
        if label_src.exists():
            shutil.copy2(label_src, assets_dir / f"yolo_labels_{model_name}.txt")
            print(f"[OK]  Labels:   {assets_dir / f'yolo_labels_{model_name}.txt'}")
            break


def main():
    parser = argparse.ArgumentParser(
        description="Train YOLO11 on AMD RX 9070 XT via ROCm (Python 3.12)",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter,
    )
    parser.add_argument("--model",   default=DEFAULT_MODEL,
                        choices=["yolo11n","yolo11s","yolo11m","yolo11l","yolo11x"],
                        help="YOLO11 model size")
    parser.add_argument("--epochs",  type=int,   default=DEFAULT_EPOCHS)
    parser.add_argument("--batch",   type=int,   default=DEFAULT_BATCH)
    parser.add_argument("--imgsz",   type=int,   default=DEFAULT_IMGSZ)
    parser.add_argument("--workers", type=int,   default=DEFAULT_WORKERS)
    parser.add_argument("--resume",  type=str,   default=None,
                        help="Path to checkpoint .pt to resume from")
    parser.add_argument("--no-export", action="store_true",
                        help="Skip export step after training")
    parser.add_argument("--no-deploy", action="store_true",
                        help="Skip Android asset deployment")

    args = parser.parse_args()

    # ── Enforce Python 3.12 ────────────────────────────────────────────────────
    if sys.version_info < (3, 12):
        print(f"[ERROR] This script requires Python 3.12+ (ROCm wheels are cp312).")
        print(f"  Current: Python {sys.version}")
        print(f"  Run with: python3.12 {sys.argv[0]}")
        sys.exit(1)

    if not check_rocm():
        sys.exit(1)

    model, results = train(args)
    validate(model)

    if not args.no_export:
        exported = export(model, args.imgsz)
        if not args.no_deploy:
            deploy_to_android(exported, args.model)

    print()
    print("=" * 60)
    print("ALL DONE!")
    print("=" * 60)
    print(f"\n  Best weights: {PROJECT_NAME}/train/weights/best.pt")
    print(f"  Last weights: {PROJECT_NAME}/train/weights/last.pt")
    print(f"\n  Monitor training live:")
    print(f"    python monitor_yolo_training.py --path {PROJECT_NAME}/train")
    print()


if __name__ == "__main__":
    main()
