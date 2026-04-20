#!/usr/bin/env python3
"""
Prepare images for Roboflow annotation.

This script:
1. Creates a folder with images that need manual review
2. Copies corresponding label files for reference
3. Creates a zip file ready for Roboflow upload

The 655 images in review_needed.txt had low detection confidence
and need manual bounding box annotation.

Usage:
    python prepare_for_roboflow.py
"""

import os
import shutil
from pathlib import Path

DATASET_PATH = Path(r"D:\FoodExpiryDatasets\crawled_grocery_yolo")
REVIEW_FILE = DATASET_PATH / "review_needed.txt"
OUTPUT_DIR = Path(r"D:\FoodExpiryDatasets\roboflow_upload")

def main():
    print("="*60)
    print("PREPARING IMAGES FOR ROBOFLOW")
    print("="*60)
    
    if not REVIEW_FILE.exists():
        print(f"[ERROR] review_needed.txt not found: {REVIEW_FILE}")
        return
    
    images_dir = OUTPUT_DIR / "images"
    labels_dir = OUTPUT_DIR / "labels"
    images_dir.mkdir(parents=True, exist_ok=True)
    labels_dir.mkdir(parents=True, exist_ok=True)
    
    with open(REVIEW_FILE, 'r') as f:
        lines = f.readlines()
    
    print(f"[INFO] Found {len(lines)} images needing review")
    print(f"[INFO] Copying to: {OUTPUT_DIR}")
    
    copied = 0
    missing = 0
    
    for line in lines:
        parts = line.strip().split(',')
        if len(parts) < 3:
            continue
        
        split_name, class_name, img_path = parts[0], parts[1], parts[2]
        
        if not os.path.exists(img_path):
            missing += 1
            continue
        
        img_name = os.path.basename(img_path)
        dest_img = images_dir / img_name
        
        shutil.copy2(img_path, dest_img)
        
        label_name = os.path.splitext(img_name)[0] + '.txt'
        src_label = DATASET_PATH / split_name / "labels" / label_name
        dest_label = labels_dir / label_name
        
        if src_label.exists():
            shutil.copy2(src_label, dest_label)
        
        copied += 1
        
        if copied % 100 == 0:
            print(f"       Progress: {copied}/{len(lines)}")
    
    print(f"\n[OK] Copied {copied} images")
    if missing > 0:
        print(f"[WARN] {missing} images not found")
    
    print(f"\n[INFO] Images ready at: {images_dir}")
    print(f"[INFO] Labels ready at: {labels_dir}")
    
    classes_file = DATASET_PATH / "classes.txt"
    if classes_file.exists():
        shutil.copy2(classes_file, OUTPUT_DIR / "classes.txt")
        print(f"[OK] Copied classes.txt")
    
    print("\n" + "="*60)
    print("NEXT STEPS - ROBOFLOW UPLOAD")
    print("="*60)
    print("""
1. Go to https://roboflow.com and create a FREE account

2. Create a new project:
   - Project name: crawled_grocery
   - Project type: Object Detection (Bounding Box)
   - License: MIT

3. Upload images:
   - Click "Upload Images"
   - Drag & drop ALL images from:
     D:\\FoodExpiryDatasets\\roboflow_upload\\images
   - Or use the folder selector

4. Annotate:
   - Use Roboflow's smart annotation tools
   - Draw tight bounding boxes around food items
   - Label Assist can help speed up the process
   - For these images, the class is known from filename
     (e.g., "eggs_Eggs-000001.jpg" = eggs class)

5. After annotation, create a version:
   - Click "Generate New Version"
   - Split: 80% train, 10% valid, 10% test
   - Add augmentations (optional):
     * Flip: horizontal
     * Brightness: +/- 15%
     * Exposure: +/- 10%

6. Export dataset:
   - Click "Export Dataset"
   - Format: "YOLOv8"
   - Download as ZIP

7. Extract to:
   D:\\FoodExpiryDatasets\\crawled_grocery_annotated\\

8. Run training:
   python train_improved.py
""")

if __name__ == "__main__":
    main()
