#!/usr/bin/env python3
"""
YOLO Annotation Helper - Verify and check annotations.

This script helps you:
1. Verify label files exist and are properly formatted
2. Visualize annotations on images
3. Check for missing or corrupt annotations

Usage:
    python verify_annotations.py          # Check all annotations
    python verify_annotations.py --visualize  # Show sample images with boxes
"""

import os
import argparse
from pathlib import Path

DATASET_PATH = r"D:\FoodExpiryDatasets\crawled_grocery_yolo"

def check_label_format(label_path, num_classes):
    """Check if a label file has valid YOLO format."""
    issues = []
    
    if not os.path.exists(label_path):
        return ["File missing"]
    
    if os.path.getsize(label_path) == 0:
        return ["Empty file"]
    
    with open(label_path, 'r') as f:
        lines = f.readlines()
    
    for line_num, line in enumerate(lines, 1):
        parts = line.strip().split()
        
        if len(parts) != 5:
            issues.append(f"Line {line_num}: Expected 5 values, got {len(parts)}")
            continue
        
        try:
            class_id = int(parts[0])
            x_center = float(parts[1])
            y_center = float(parts[2])
            width = float(parts[3])
            height = float(parts[4])
            
            if class_id < 0 or class_id >= num_classes:
                issues.append(f"Line {line_num}: Invalid class ID {class_id}")
            
            for val, name in [(x_center, 'x_center'), (y_center, 'y_center'), 
                             (width, 'width'), (height, 'height')]:
                if val < 0 or val > 1:
                    issues.append(f"Line {line_num}: {name}={val} out of range [0,1]")
        
        except ValueError as e:
            issues.append(f"Line {line_num}: Invalid number format - {e}")
    
    return issues

def verify_all_labels():
    """Verify all label files in the dataset."""
    print("="*60)
    print("VERIFYING YOLO ANNOTATIONS")
    print("="*60)
    
    data_yaml = os.path.join(DATASET_PATH, 'data.yaml')
    if not os.path.exists(data_yaml):
        print(f"[ERROR] data.yaml not found at {data_yaml}")
        print("Run convert_to_yolo_format.py first!")
        return
    
    with open(data_yaml, 'r') as f:
        content = f.read()
        import re
        nc_match = re.search(r'nc:\s*(\d+)', content)
        num_classes = int(nc_match.group(1)) if nc_match else 0
    
    stats = {'total': 0, 'empty': 0, 'valid': 0, 'issues': 0}
    issue_files = []
    
    for split in ['train', 'val', 'test']:
        labels_dir = os.path.join(DATASET_PATH, split, 'labels')
        
        if not os.path.exists(labels_dir):
            continue
        
        print(f"\n[{split.upper()}]")
        
        for label_file in os.listdir(labels_dir):
            if not label_file.endswith('.txt'):
                continue
            
            stats['total'] += 1
            label_path = os.path.join(labels_dir, label_file)
            issues = check_label_format(label_path, num_classes)
            
            if issues == ["Empty file"]:
                stats['empty'] += 1
            elif issues == ["File missing"]:
                stats['issues'] += 1
                issue_files.append((split, label_file, issues))
            elif issues:
                stats['issues'] += 1
                issue_files.append((split, label_file, issues))
            else:
                stats['valid'] += 1
    
    print("\n" + "="*60)
    print("SUMMARY")
    print("="*60)
    print(f"Total labels:  {stats['total']}")
    print(f"Valid labels:  {stats['valid']}")
    print(f"Empty labels:  {stats['empty']} (need annotation)")
    print(f"Issue labels:  {stats['issues']}")
    
    if issue_files:
        print(f"\nFirst 10 issues:")
        for split, file, issues in issue_files[:10]:
            print(f"  [{split}] {file}: {issues[0]}")
    
    if stats['empty'] > 0:
        print(f"\n[ACTION REQUIRED] {stats['empty']} labels need annotation!")
        print("Run annotation tool to draw bounding boxes.")

def visualize_samples(num_samples=5):
    """Visualize sample images with bounding boxes."""
    try:
        import cv2
        import numpy as np
    except ImportError:
        print("[ERROR] OpenCV not installed. Run: pip install opencv-python")
        return
    
    print("="*60)
    print("VISUALIZING ANNOTATIONS")
    print("="*60)
    
    data_yaml = os.path.join(DATASET_PATH, 'data.yaml')
    if not os.path.exists(data_yaml):
        print(f"[ERROR] data.yaml not found")
        return
    
    import yaml
    with open(data_yaml, 'r') as f:
        data = yaml.safe_load(f)
    
    class_names = data.get('names', {})
    if isinstance(class_names, dict):
        class_names = {int(k): v for k, v in class_names.items()}
    elif isinstance(class_names, list):
        class_names = {i: name for i, name in enumerate(class_names)}
    
    output_dir = os.path.join(DATASET_PATH, 'visualizations')
    os.makedirs(output_dir, exist_ok=True)
    
    for split in ['train', 'val', 'test']:
        images_dir = os.path.join(DATASET_PATH, split, 'images')
        labels_dir = os.path.join(DATASET_PATH, split, 'labels')
        
        if not os.path.exists(images_dir):
            continue
        
        images = [f for f in os.listdir(images_dir) 
                  if f.lower().endswith(('.jpg', '.jpeg', '.png'))]
        
        import random
        random.seed(42)
        samples = random.sample(images, min(num_samples, len(images)))
        
        for img_name in samples:
            img_path = os.path.join(images_dir, img_name)
            label_name = os.path.splitext(img_name)[0] + '.txt'
            label_path = os.path.join(labels_dir, label_name)
            
            img = cv2.imread(img_path)
            if img is None:
                continue
            
            h, w = img.shape[:2]
            
            if os.path.exists(label_path) and os.path.getsize(label_path) > 0:
                with open(label_path, 'r') as f:
                    lines = f.readlines()
                
                for line in lines:
                    parts = line.strip().split()
                    if len(parts) != 5:
                        continue
                    
                    class_id = int(parts[0])
                    x_center = float(parts[1]) * w
                    y_center = float(parts[2]) * h
                    box_w = float(parts[3]) * w
                    box_h = float(parts[4]) * h
                    
                    x1 = int(x_center - box_w / 2)
                    y1 = int(y_center - box_h / 2)
                    x2 = int(x_center + box_w / 2)
                    y2 = int(y_center + box_h / 2)
                    
                    color = (0, 255, 0)
                    cv2.rectangle(img, (x1, y1), (x2, y2), color, 2)
                    
                    class_name = class_names.get(class_id, f"class_{class_id}")
                    cv2.putText(img, class_name, (x1, y1 - 5),
                               cv2.FONT_HERSHEY_SIMPLEX, 0.5, color, 1)
            
            out_path = os.path.join(output_dir, f"{split}_{img_name}")
            cv2.imwrite(out_path, img)
            print(f"[OK] Saved: {out_path}")
    
    print(f"\n[OK] Visualizations saved to: {output_dir}")
    print("Open the images to verify your annotations are correct!")

def main():
    parser = argparse.ArgumentParser(description='Verify YOLO annotations')
    parser.add_argument('--visualize', '-v', action='store_true',
                       help='Create visualization images')
    parser.add_argument('--samples', '-n', type=int, default=5,
                       help='Number of samples per split to visualize')
    
    args = parser.parse_args()
    
    if not os.path.exists(DATASET_PATH):
        print(f"[ERROR] Dataset not found: {DATASET_PATH}")
        print("Run convert_to_yolo_format.py first!")
        return
    
    if args.visualize:
        visualize_samples(args.samples)
    else:
        verify_all_labels()

if __name__ == "__main__":
    main()
