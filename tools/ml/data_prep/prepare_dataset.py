import os
import shutil
import glob
from pathlib import Path

# ==========================================
# FoodExpiry Dataset Organizer Script
# ==========================================
# This script helps automatically copy and organize images from raw datasets
# (like Food-101 and Fruit-360) into your target 15 categories for training.
#
# PREREQUISITES:
# 1. Download Food-101 and Fruit-360 from Kaggle.
# 2. Extract them into D:\RawDatasets
#    So you should have:
#    D:\RawDatasets\food-101\images\...
#    D:\RawDatasets\fruits-360_dataset\fruits-360\Training\...

RAW_DIR = Path(r"D:\RawDatasets")
TARGET_DIR = Path(r"D:\FoodExpiryDatasets")

# Define our 15 target categories
CATEGORIES = [
    "Leafy Vegetables", "Root Vegetables", "Gourd Fruit Vegetables",
    "Hard Fruits", "Soft Fruits", "Tropical Fruits",
    "Fresh Meat", "Processed Meat", "Seafood",
    "Eggs", "Dairy Products", "Tofu Products",
    "Grains & Dry Goods", "Others"
]

# Create target directories
for cat in CATEGORIES:
    (TARGET_DIR / cat).mkdir(parents=True, exist_ok=True)

# Define mapping from Raw Dataset folders to our target categories.
# You can expand this mapping as you download more specific datasets.
MAPPING = {
    # --- FRUIT-360 MAPPINGS ---
    # Hard Fruits
    "Hard Fruits": [
        "fruits-360_dataset/fruits-360/Training/Apple*",
        "fruits-360_dataset/fruits-360/Training/Pear*",
        "fruits-360_dataset/fruits-360/Training/Peach*",
        "fruits-360_dataset/fruits-360/Training/Guava*"
    ],
    # Soft Fruits
    "Soft Fruits": [
        "fruits-360_dataset/fruits-360/Training/Strawberry*",
        "fruits-360_dataset/fruits-360/Training/Cherry*",
        "fruits-360_dataset/fruits-360/Training/Grape*",
        "fruits-360_dataset/fruits-360/Training/Blueberry*",
        "fruits-360_dataset/fruits-360/Training/Raspberry*"
    ],
    # Tropical Fruits
    "Tropical Fruits": [
        "fruits-360_dataset/fruits-360/Training/Banana*",
        "fruits-360_dataset/fruits-360/Training/Mango*",
        "fruits-360_dataset/fruits-360/Training/Pineapple*",
        "fruits-360_dataset/fruits-360/Training/Kiwi*",
        "fruits-360_dataset/fruits-360/Training/Papaya*"
    ],
    # Gourd Fruit Vegetables (from Fruit-360)
    "Gourd Fruit Vegetables": [
        "fruits-360_dataset/fruits-360/Training/Tomato*",
        "fruits-360_dataset/fruits-360/Training/Cucumber*",
        "fruits-360_dataset/fruits-360/Training/Pepper*",
        "fruits-360_dataset/fruits-360/Training/Eggplant*"
    ],
    # Root Vegetables (from Fruit-360)
    "Root Vegetables": [
        "fruits-360_dataset/fruits-360/Training/Potato*",
        "fruits-360_dataset/fruits-360/Training/Onion*",
        "fruits-360_dataset/fruits-360/Training/Beetroot*",
        "fruits-360_dataset/fruits-360/Training/Carrot*"
    ],

    # --- FOOD-101 MAPPINGS ---
    "Eggs": [
        "food-101/images/deviled_eggs",
        "food-101/images/eggs_benedict",
        "food-101/images/omelette",
        "food-101/images/huevos_rancheros"
    ],
    "Seafood": [
        "food-101/images/grilled_salmon",
        "food-101/images/lobster_bisque",
        "food-101/images/oysters",
        "food-101/images/shrimp_and_grits",
        "food-101/images/ceviche",
        "food-101/images/clam_chowder",
        "food-101/images/mussels",
        "food-101/images/scallops"
    ],
    "Fresh Meat": [
        "food-101/images/pork_chop",
        "food-101/images/steak",
        "food-101/images/filet_mignon",
        "food-101/images/beef_carpaccio"
    ],
    "Processed Meat": [
        "food-101/images/hot_dog",
        "food-101/images/hamburger",
        "food-101/images/pulled_pork_sandwich"
    ],
    "Dairy Products": [
        "food-101/images/cheese_plate",
        "food-101/images/ice_cream",
        "food-101/images/macaroni_and_cheese"
    ],
    "Tofu Products": [
        "food-101/images/miso_soup" # Often contains tofu, a temporary proxy
    ],
    "Grains & Dry Goods": [
        "food-101/images/fried_rice",
        "food-101/images/spaghetti_bolognese",
        "food-101/images/pad_thai",
        "food-101/images/ramen"
    ]
}

def collect_data():
    if not RAW_DIR.exists():
        print(f"Directory not found: {RAW_DIR}")
        print("Please create D:\\RawDatasets and extract the Kaggle datasets there.")
        return

    print(f"Starting data collection into {TARGET_DIR}...")
    
    total_copied = 0
    for target_cat, source_patterns in MAPPING.items():
        print(f"\nProcessing category: {target_cat}")
        target_path = TARGET_DIR / target_cat
        
        cat_copied = 0
        for pattern in source_patterns:
            # Resolve glob patterns (like Apple*)
            search_path = str(RAW_DIR / pattern)
            matching_dirs = glob.glob(search_path)
            
            for matched_dir in matching_dirs:
                if os.path.isdir(matched_dir):
                    files = list(Path(matched_dir).glob("*.jpg")) + list(Path(matched_dir).glob("*.jpeg")) + list(Path(matched_dir).glob("*.png"))
                    for file in files:
                        # Prevent duplicate file names by prefixing parent folder name
                        parent_name = file.parent.name
                        new_file_name = f"{parent_name}_{file.name}"
                        dest_file = target_path / new_file_name
                        
                        if not dest_file.exists():
                            shutil.copy2(file, dest_file)
                            cat_copied += 1
                            total_copied += 1
                            
                            if total_copied % 500 == 0:
                                print(f"  ...copied {total_copied} images so far...")

        print(f"  Copied {cat_copied} images to {target_cat}.")

    print(f"\nDone! Successfully copied {total_copied} images total.")
    print("For any missing categories (like raw Tofu or Leafy Vegetables), please download them manually or via Google Images and place them directly into D:\\FoodExpiryDatasets\\[Category].")

if __name__ == "__main__":
    collect_data()