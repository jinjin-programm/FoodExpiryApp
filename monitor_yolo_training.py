#!/usr/bin/env python3
"""
YOLO Training Progress Monitor
Shows training metrics updated every second.

Usage:
    python monitor_yolo_training.py
    python monitor_yolo_training.py --path yolo11_crawled_grocery/train
    python monitor_yolo_training.py --path runs/detect/train
"""

import os
import sys
import time
import argparse
import pandas as pd
from pathlib import Path
from datetime import datetime


DEFAULT_PATHS = [
    "yolo11_crawled_grocery/train",
    "runs/detect/train",
    "runs/train",
]

COLUMNS_TO_DISPLAY = [
    "epoch",
    "train/box_loss",
    "train/cls_loss",
    "train/dfl_loss",
    "metrics/precision(B)",
    "metrics/recall(B)",
    "metrics/mAP50(B)",
    "metrics/mAP50-95(B)",
    "val/box_loss",
    "val/cls_loss",
]

def find_results_file(base_path):
    """Find the results.csv file in the training directory."""
    results_path = os.path.join(base_path, "results.csv")
    if os.path.exists(results_path):
        return results_path
    
    for root, dirs, files in os.walk(base_path):
        if "results.csv" in files:
            return os.path.join(root, "results.csv")
    
    return None

def find_training_dir():
    """Auto-detect the training directory."""
    for path in DEFAULT_PATHS:
        if os.path.exists(path):
            results = find_results_file(path)
            if results:
                return path, results
    
    search_dirs = [".", "runs", "yolo11_crawled_grocery"]
    for search_dir in search_dirs:
        if os.path.exists(search_dir):
            for root, dirs, files in os.walk(search_dir):
                if "results.csv" in files:
                    return os.path.dirname(root), os.path.join(root, "results.csv")
    
    return None, None

def get_latest_epoch(results_df):
    """Get the latest completed epoch."""
    if results_df is not None and len(results_df) > 0:
        return int(results_df.iloc[-1]["epoch"])
    return 0

def format_metric(value, metric_name):
    """Format metric value for display."""
    if pd.isna(value):
        return "N/A"
    if "loss" in metric_name.lower():
        return f"{value:.4f}"
    elif "map" in metric_name.lower():
        return f"{value:.4f}"
    elif "precision" in metric_name.lower() or "recall" in metric_name.lower():
        return f"{value:.4f}"
    else:
        return f"{value:.4f}"

def display_progress(results_df, epoch, total_epochs):
    """Display the training progress."""
    os.system('cls' if os.name == 'nt' else 'clear')
    
    print("=" * 80)
    print(f"YOLO Training Progress Monitor")
    print(f"Updated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 80)
    
    if results_df is None or len(results_df) == 0:
        print("\nWaiting for training to start...")
        print("Make sure YOLO training is running in another terminal.")
        return
    
    latest = results_df.iloc[-1]
    
    print(f"\nEpoch: {int(latest.get('epoch', 0)) + 1}/{total_epochs} ({100 * (int(latest.get('epoch', 0)) + 1) / total_epochs:.1f}%)")
    print("-" * 80)
    
    print("\n📊 Training Metrics:")
    for col in ["train/box_loss", "train/cls_loss", "train/dfl_loss"]:
        if col in results_df.columns:
            val = latest.get(col, 0)
            print(f"  {col:20s}: {format_metric(val, col)}")
    
    print("\n📈 Validation Metrics:")
    for col in ["metrics/precision(B)", "metrics/recall(B)", "metrics/mAP50(B)", "metrics/mAP50-95(B)"]:
        if col in results_df.columns:
            val = latest.get(col, 0)
            print(f"  {col:20s}: {format_metric(val, col)}")
    
    print("\n🔍 Validation Loss:")
    for col in ["val/box_loss", "val/cls_loss"]:
        if col in results_df.columns:
            val = latest.get(col, 0)
            print(f"  {col:20s}: {format_metric(val, col)}")
    
    print("\n" + "-" * 80)
    print("History (last 5 epochs):")
    print("-" * 80)
    
    recent = results_df.tail(5)
    header = "Epoch  | mAP50    | mAP50-95 | Precision| Recall   | Box Loss"
    print(header)
    print("-" * len(header))
    
    for _, row in recent.iterrows():
        ep = int(row.get('epoch', 0)) + 1
        map50 = format_metric(row.get('metrics/mAP50(B)', 0), 'map')
        map50_95 = format_metric(row.get('metrics/mAP50-95(B)', 0), 'map')
        prec = format_metric(row.get('metrics/precision(B)', 0), 'precision')
        rec = format_metric(row.get('metrics/recall(B)', 0), 'recall')
        box_loss = format_metric(row.get('train/box_loss', 0), 'loss')
        print(f"{ep:5d}  | {map50:7s}  | {map50_95:8s} | {prec:7s}  | {rec:7s}  | {box_loss}")
    
    print("=" * 80)

def get_total_epochs(train_args):
    """Try to get total epochs from training args or default."""
    args_file = os.path.join(os.path.dirname(train_args) if train_args else ".", "args.yaml")
    if os.path.exists(args_file):
        try:
            import yaml
            with open(args_file, 'r') as f:
                args = yaml.safe_load(f)
                return args.get('epochs', 100)
        except:
            pass
    return 100

def monitor_training(train_path, interval=1):
    """Monitor training progress with specified interval."""
    train_dir = train_path
    if not train_dir:
        print("Training directory not found!")
        print("Please specify the training path with --path option.")
        sys.exit(1)
    
    print(f"Monitoring: {train_dir}")
    print("Press Ctrl+C to stop\n")
    
    results_file = find_results_file(train_dir)
    total_epochs = get_total_epochs(train_dir)
    last_epoch = -1
    
    while True:
        try:
            results_file = find_results_file(train_dir)
            
            if results_file and os.path.exists(results_file):
                try:
                    results_df = pd.read_csv(results_file)
                    results_df.columns = results_df.columns.str.strip()
                    
                    current_epoch = get_latest_epoch(results_df)
                    
                    if current_epoch != last_epoch:
                        display_progress(results_df, current_epoch, total_epochs)
                        last_epoch = current_epoch
                    
                    if current_epoch >= total_epochs - 1:
                        print("\n✅ Training completed!")
                        break
                        
                except Exception as e:
                    print(f"Error reading results: {e}")
            else:
                os.system('cls' if os.name == 'nt' else 'clear')
                print("Waiting for training to start...")
                print(f"Checked path: {train_dir}")
                print("\nMake sure YOLO training is running.")
            
            time.sleep(interval)
            
        except KeyboardInterrupt:
            print("\n\nMonitoring stopped.")
            break

def main():
    parser = argparse.ArgumentParser(description='Monitor YOLO training progress')
    parser.add_argument('--path', type=str, default=None, 
                        help='Training directory path (auto-detected if not provided)')
    parser.add_argument('--interval', type=int, default=1,
                        help='Update interval in seconds (default: 1)')
    
    args = parser.parse_args()
    
    if args.path:
        train_path = args.path
    else:
        train_path, _ = find_training_dir()
    
    monitor_training(train_path, args.interval)

if __name__ == "__main__":
    main()
