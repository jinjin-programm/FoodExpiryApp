#!/usr/bin/env python3
"""
Convert crawled_grocery dataset to YOLO format for annotation.

This script:
1. Creates the YOLO directory structure
2. Copies images to proper folders
3. Generates class names file
4. Creates placeholder label files for annotation

Usage:
    python convert_to_yolo_format.py

After running this, use an annotation tool to create bounding boxes.
"""

import os
import shutil
import random
from pathlib import Path

SOURCE_DATASET = r"D:\FoodExpiryDatasets\crawled_grocery"
OUTPUT_DATASET = r"D:\FoodExpiryDatasets\crawled_grocery_yolo"
TRAIN_SPLIT = 0.80
VAL_SPLIT = 0.10
TEST_SPLIT = 0.10

def get_class_names():
    """Get all class names from source directory."""
    class_names = []
    for item in sorted(os.listdir(SOURCE_DATASET)):
        item_path = os.path.join(SOURCE_DATASET, item)
        if os.path.isdir(item_path):
            class_names.append(item)
    return class_names

def create_yolo_structure():
    """Create YOLO directory structure."""
    splits = ['train', 'val', 'test']
    for split in splits:
        os.makedirs(os.path.join(OUTPUT_DATASET, split, 'images'), exist_ok=True)
        os.makedirs(os.path.join(OUTPUT_DATASET, split, 'labels'), exist_ok=True)
    print(f"[OK] Created YOLO structure at: {OUTPUT_DATASET}")

def collect_all_images(class_names):
    """Collect all images with their class index."""
    all_images = []
    valid_extensions = {'.jpg', '.jpeg', '.png', '.webp', '.bmp'}
    
    for class_idx, class_name in enumerate(class_names):
        class_path = os.path.join(SOURCE_DATASET, class_name)
        for file in os.listdir(class_path):
            ext = os.path.splitext(file)[1].lower()
            if ext in valid_extensions:
                src_path = os.path.join(class_path, file)
                all_images.append({
                    'src_path': src_path,
                    'class_idx': class_idx,
                    'class_name': class_name,
                    'filename': file
                })
    
    return all_images

def split_dataset(all_images):
    """Split dataset into train/val/test."""
    random.seed(42)
    random.shuffle(all_images)
    
    total = len(all_images)
    train_end = int(total * TRAIN_SPLIT)
    val_end = train_end + int(total * VAL_SPLIT)
    
    return {
        'train': all_images[:train_end],
        'val': all_images[train_end:val_end],
        'test': all_images[val_end:]
    }

def copy_images_to_splits(splits, class_names):
    """Copy images to YOLO structure and create placeholder labels."""
    class_image_counts = {name: {'train': 0, 'val': 0, 'test': 0} for name in class_names}
    
    for split_name, images in splits.items():
        print(f"\n[INFO] Processing {split_name}: {len(images)} images")
        
        for img_info in images:
            class_name = img_info['class_name']
            src_path = img_info['src_path']
            filename = img_info['filename']
            
            dest_img_dir = os.path.join(OUTPUT_DATASET, split_name, 'images')
            dest_lbl_dir = os.path.join(OUTPUT_DATASET, split_name, 'labels')
            
            new_filename = f"{class_name}_{filename}"
            dest_img_path = os.path.join(dest_img_dir, new_filename)
            
            shutil.copy2(src_path, dest_img_path)
            
            label_filename = os.path.splitext(new_filename)[0] + '.txt'
            label_path = os.path.join(dest_lbl_dir, label_filename)
            
            with open(label_path, 'w') as f:
                pass
            
            class_image_counts[class_name][split_name] += 1
    
    return class_image_counts

def create_data_yaml(class_names):
    """Create YOLO data.yaml file."""
    yaml_content = f"""# Crawled Grocery Dataset - YOLO Format
# 49 food categories for expiry detection

path: {OUTPUT_DATASET.replace(chr(92), '/')}
train: train/images
val: val/images
test: test/images

nc: {len(class_names)}
names:
"""
    for idx, name in enumerate(class_names):
        yaml_content += f"  {idx}: {name}\n"
    
    yaml_path = os.path.join(OUTPUT_DATASET, 'data.yaml')
    with open(yaml_path, 'w') as f:
        f.write(yaml_content)
    
    print(f"\n[OK] Created data.yaml with {len(class_names)} classes")
    return yaml_path

def create_classes_file(class_names):
    """Create classes.txt for annotation tools."""
    classes_path = os.path.join(OUTPUT_DATASET, 'classes.txt')
    with open(classes_path, 'w') as f:
        for name in class_names:
            f.write(name + '\n')
    print(f"[OK] Created classes.txt for annotation")

def print_statistics(splits, class_image_counts, class_names):
    """Print dataset statistics."""
    print("\n" + "="*60)
    print("DATASET STATISTICS")
    print("="*60)
    print(f"Total images: {sum(len(imgs) for imgs in splits.values())}")
    print(f"Total classes: {len(class_names)}")
    print(f"\nSplit distribution:")
    print(f"  Train: {len(splits['train'])} images ({TRAIN_SPLIT*100:.0f}%)")
    print(f"  Val:   {len(splits['val'])} images ({VAL_SPLIT*100:.0f}%)")
    print(f"  Test:  {len(splits['test'])} images ({TEST_SPLIT*100:.0f}%)")
    
    print(f"\nImages per class:")
    print("-"*60)
    print(f"{'Class':<25} {'Train':>8} {'Val':>8} {'Test':>8} {'Total':>8}")
    print("-"*60)
    
    for class_name in class_names:
        counts = class_image_counts[class_name]
        total = counts['train'] + counts['val'] + counts['test']
        print(f"{class_name:<25} {counts['train']:>8} {counts['val']:>8} {counts['test']:>8} {total:>8}")

def print_next_steps():
    """Print annotation instructions."""
    print("\n" + "="*60)
    print("NEXT STEPS: ANNOTATE YOUR DATA")
    print("="*60)
    print("""
Option 1: Use LabelImg (Desktop App)
-------------------------------------
  pip install labelimg
  labelimg D:/FoodExpiryDatasets/crawled_grocery_yolo/train/images
  # Load classes.txt from the dataset folder
  # Draw bounding boxes and save (YOLO format)

Option 2: Use Roboflow (Web - Recommended for accuracy)
-------------------------------------------------------
  1. Go to https://roboflow.com
  2. Create a free account
  3. Create new project -> Object Detection
  4. Upload images from train/images folder
  5. Annotate using their tools
  6. Export as "YOLOv8" format
  
Option 3: Use Label Studio (Desktop/Web)
----------------------------------------
  pip install label-studio
  label-studio start
  # Open http://localhost:8080

ANNOTATION TIPS:
- Draw tight bounding boxes around items
- Include all visible parts of the food
- For multiple items, create separate boxes
- Be consistent with labeling
""")

def main():
    print("="*60)
    print("CRAWLED GROCERY -> YOLO FORMAT CONVERTER")
    print("="*60)
    
    if not os.path.exists(SOURCE_DATASET):
        print(f"[ERROR] Source dataset not found: {SOURCE_DATASET}")
        return
    
    print(f"\nSource: {SOURCE_DATASET}")
    print(f"Output: {OUTPUT_DATASET}")
    
    class_names = get_class_names()
    print(f"\n[OK] Found {len(class_names)} classes")
    
    create_yolo_structure()
    
    print("\n[INFO] Collecting images...")
    all_images = collect_all_images(class_names)
    print(f"[OK] Found {len(all_images)} images")
    
    print("\n[INFO] Splitting dataset...")
    splits = split_dataset(all_images)
    
    print("\n[INFO] Copying images and creating label placeholders...")
    class_image_counts = copy_images_to_splits(splits, class_names)
    
    create_data_yaml(class_names)
    create_classes_file(class_names)
    
    print_statistics(splits, class_image_counts, class_names)
    print_next_steps()
    
    print("\n" + "="*60)
    print("CONVERSION COMPLETE!")
    print("="*60)
    print(f"\nDataset ready at: {OUTPUT_DATASET}")
    print("Images are copied and ready for annotation.")
    print("Label files are empty - you need to annotate them.\n")

if __name__ == "__main__":
    main()
