#!/usr/bin/env python3
"""
Auto-Annotate crawled_grocery dataset using YOLO.

This script uses a pre-trained YOLO model to automatically detect objects
and create bounding box annotations. The class is determined from the
folder name (images are organized by category).

Usage:
    python auto_annotate.py
    python auto_annotate.py --confidence 0.25
    python auto_annotate.py --split train
"""

import os
import sys
import argparse
from pathlib import Path
import time

DATASET_PATH = Path(r"D:\FoodExpiryDatasets\crawled_grocery_yolo")
CONFIDENCE_THRESHOLD = 0.25

def load_model():
    """Load YOLO model for detection."""
    try:
        from ultralytics import YOLO
    except ImportError:
        print("[ERROR] ultralytics not installed!")
        print("Run: pip install ultralytics")
        sys.exit(1)
    
    print("[INFO] Loading YOLO11n model...")
    model = YOLO("yolo11n.pt")
    print("[OK] Model loaded")
    return model

def get_class_mapping():
    """Load class names from data.yaml."""
    import yaml
    
    data_yaml = DATASET_PATH / "data.yaml"
    with open(data_yaml, 'r') as f:
        data = yaml.safe_load(f)
    
    class_names = data.get('names', {})
    if isinstance(class_names, dict):
        return {int(k): v for k, v in class_names.items()}
    elif isinstance(class_names, list):
        return {i: name for i, name in enumerate(class_names)}
    return {}

def get_class_id_from_filename(filename, class_mapping):
    """Extract class ID from filename (format: classname_originalname)."""
    class_name = filename.split('_')[0]
    
    for class_id, name in class_mapping.items():
        if name == class_name:
            return class_id, class_name
    
    for class_id, name in class_mapping.items():
        if class_name in name or name in class_name:
            return class_id, name
    
    return 0, class_name

def detect_and_annotate(model, image_path, class_id, confidence_threshold):
    """Run detection and create YOLO format annotation."""
    try:
        results = model(image_path, verbose=False, conf=confidence_threshold)[0]
        
        boxes = []
        for box in results.boxes:
            x1, y1, x2, y2 = box.xyxyn[0].tolist()
            
            x_center = (x1 + x2) / 2
            y_center = (y1 + y2) / 2
            width = x2 - x1
            height = y2 - y1
            
            boxes.append({
                'class_id': class_id,
                'x_center': x_center,
                'y_center': y_center,
                'width': width,
                'height': height,
                'confidence': float(box.conf[0])
            })
        
        return boxes
    except Exception as e:
        print(f"[WARN] Detection failed for {image_path}: {e}")
        return []

def create_whole_image_annotation(class_id):
    """Create annotation covering most of the image (fallback)."""
    return [{
        'class_id': class_id,
        'x_center': 0.5,
        'y_center': 0.5,
        'width': 0.8,
        'height': 0.8,
        'confidence': 0.0,
        'auto_generated': True
    }]

def save_yolo_label(label_path, boxes):
    """Save annotations in YOLO format."""
    with open(label_path, 'w') as f:
        for box in boxes:
            line = f"{box['class_id']} {box['x_center']:.6f} {box['y_center']:.6f} {box['width']:.6f} {box['height']:.6f}"
            f.write(line + '\n')

def process_split(model, split_name, class_mapping, confidence_threshold):
    """Process images in a split (train/val/test)."""
    images_dir = DATASET_PATH / split_name / "images"
    labels_dir = DATASET_PATH / split_name / "labels"
    
    if not images_dir.exists():
        print(f"[WARN] {split_name} images directory not found")
        return {'total': 0, 'detected': 0, 'fallback': 0}
    
    images = list(images_dir.glob("*.jpg")) + list(images_dir.glob("*.png"))
    
    stats = {'total': len(images), 'detected': 0, 'fallback': 0}
    low_confidence_images = []
    
    print(f"\n[INFO] Processing {split_name}: {len(images)} images")
    
    for i, img_path in enumerate(images, 1):
        if i % 100 == 0:
            print(f"       Progress: {i}/{len(images)} ({i*100//len(images)}%)")
        
        class_id, class_name = get_class_id_from_filename(img_path.stem, class_mapping)
        label_path = labels_dir / f"{img_path.stem}.txt"
        
        boxes = detect_and_annotate(model, str(img_path), class_id, confidence_threshold)
        
        valid_boxes = [b for b in boxes if b['confidence'] >= confidence_threshold]
        
        if valid_boxes:
            stats['detected'] += 1
            save_yolo_label(label_path, valid_boxes)
        else:
            fallback_boxes = create_whole_image_annotation(class_id)
            stats['fallback'] += 1
            save_yolo_label(label_path, fallback_boxes)
            
            low_confidence_images.append({
                'image': str(img_path),
                'class': class_name,
                'split': split_name
            })
    
    return stats, low_confidence_images

def run_auto_annotation(split=None, confidence=CONFIDENCE_THRESHOLD):
    """Run automatic annotation on the dataset."""
    print("="*60)
    print("AUTO-ANNOTATION: crawled_grocery dataset")
    print("="*60)
    
    start_time = time.time()
    
    model = load_model()
    class_mapping = get_class_mapping()
    print(f"[OK] Found {len(class_mapping)} classes")
    
    splits = [split] if split else ['train', 'val', 'test']
    total_stats = {'total': 0, 'detected': 0, 'fallback': 0}
    all_low_confidence = []
    
    for split_name in splits:
        stats, low_conf = process_split(model, split_name, class_mapping, confidence)
        total_stats['total'] += stats['total']
        total_stats['detected'] += stats['detected']
        total_stats['fallback'] += stats['fallback']
        all_low_confidence.extend(low_conf)
        
        print(f"\n[{split_name.upper()}] Results:")
        print(f"       Total:    {stats['total']}")
        print(f"       Detected: {stats['detected']} ({stats['detected']*100//max(stats['total'],1)}%)")
        print(f"       Fallback: {stats['fallback']} ({stats['fallback']*100//max(stats['total'],1)}%)")
    
    elapsed = time.time() - start_time
    
    print("\n" + "="*60)
    print("SUMMARY")
    print("="*60)
    print(f"Total images processed: {total_stats['total']}")
    print(f"Auto-detected boxes:    {total_stats['detected']} ({total_stats['detected']*100//max(total_stats['total'],1)}%)")
    print(f"Fallback (whole image): {total_stats['fallback']} ({total_stats['fallback']*100//max(total_stats['total'],1)}%)")
    print(f"Time elapsed: {elapsed:.1f} seconds")
    
    review_file = DATASET_PATH / "review_needed.txt"
    if all_low_confidence:
        with open(review_file, 'w') as f:
            for item in all_low_confidence:
                f.write(f"{item['split']},{item['class']},{item['image']}\n")
        print(f"\n[INFO] {len(all_low_confidence)} images need review")
        print(f"       See: {review_file}")
    
    print("\n" + "="*60)
    print("NEXT STEPS")
    print("="*60)
    print("""
1. Review annotations using LabelImg:
   pip install labelimg
   labelimg D:/FoodExpiryDatasets/crawled_grocery_yolo/train/images

2. Pay special attention to images in 'review_needed.txt'
   These had low detection confidence and used whole-image boxes.

3. Correct any wrong bounding boxes:
   - Adjust box size to fit the product tightly
   - Ensure correct class (should match folder name)

4. Run verification:
   python verify_annotations.py
""")

def main():
    parser = argparse.ArgumentParser(description='Auto-annotate dataset')
    parser.add_argument('--split', type=str, default=None, 
                        choices=['train', 'val', 'test'],
                        help='Only process specific split')
    parser.add_argument('--confidence', type=float, default=CONFIDENCE_THRESHOLD,
                        help='Detection confidence threshold')
    
    args = parser.parse_args()
    
    if not DATASET_PATH.exists():
        print(f"[ERROR] Dataset not found: {DATASET_PATH}")
        print("Run convert_to_yolo_format.py first!")
        sys.exit(1)
    
    run_auto_annotation(args.split, args.confidence)

if __name__ == "__main__":
    main()
