import os
import shutil
import glob
from pathlib import Path
from bing_image_downloader import downloader

# ==========================================
# FoodExpiry Dataset Crawler and Organizer
# ==========================================

TARGET_DIR = Path(r"D:\FoodExpiryDatasets_Classification")

CATEGORIES = [
    "Leafy Vegetables", "Root Vegetables", "Gourd Fruit Vegetables",
    "Hard Fruits", "Soft Fruits", "Tropical Fruits",
    "Fresh Meat", "Processed Meat", "Seafood",
    "Eggs", "Dairy Products", "Tofu Products",
    "Grains & Dry Goods", "Others"
]

# Map specific search queries to target categories
SEARCH_MAPPINGS = {
    "Leafy Vegetables": ["Bok choy raw", "Spinach raw", "Lettuce raw", "Cabbage raw", "Kale raw"],
    "Root Vegetables": ["Raw Potato", "Raw Carrot", "Raw Radish", "Raw Turnip", "Raw Sweet Potato"],
    "Gourd Fruit Vegetables": ["Raw Tomato", "Raw Cucumber", "Raw Eggplant", "Raw Bell Pepper", "Raw Pumpkin"],
    "Hard Fruits": ["Apple fruit", "Pear fruit", "Guava fruit"],
    "Soft Fruits": ["Strawberry fruit", "Grapes", "Peach fruit", "Plum fruit", "Cherry fruit"],
    "Tropical Fruits": ["Banana fruit", "Mango fruit", "Pineapple fruit", "Papaya fruit"],
    "Fresh Meat": ["Raw beef meat", "Raw pork meat", "Raw chicken breast", "Raw lamb meat"],
    "Processed Meat": ["Hot dog sausages", "Bacon raw", "Ham slices", "Salami"],
    "Seafood": ["Raw fish fillet", "Raw salmon", "Raw shrimp", "Raw squid", "Raw scallops"],
    "Eggs": ["Raw chicken eggs", "Brown eggs", "White eggs in carton"],
    "Dairy Products": ["Milk carton", "Cheese block", "Yogurt container", "Butter block"],
    "Tofu Products": ["Raw tofu block", "Silken tofu", "Firm tofu raw"],
    "Grains & Dry Goods": ["Uncooked rice grains", "Uncooked pasta", "Oats dry", "Flour powder"],
    "Others": ["Condiment bottles", "Canned food", "Snack bags", "Juice box"]
}

# Directories to grab existing data from if available
EXISTING_DATA_DIR = Path(r"D:\FoodExpiryDatasets\crawled_grocery")

CRAWLED_TO_CATEGORIES = {
    "beef_meat": "Fresh Meat",
    "chicken_meat": "Fresh Meat",
    "duck_meat": "Fresh Meat",
    "lamb_meat": "Fresh Meat",
    "pork_meat": "Fresh Meat",
    "turkey_meat": "Fresh Meat",
    "cheese": "Dairy Products",
    "cream": "Dairy Products",
    "butter": "Dairy Products",
    "yogurt": "Dairy Products",
    "eggs": "Eggs",
    "cod_fish": "Seafood",
    "mackerel_fish": "Seafood",
    "salmon_fish": "Seafood",
    "sardines": "Seafood",
    "scallops": "Seafood",
    "shrimp": "Seafood",
    "tuna_fish": "Seafood",
    "crab_meat": "Seafood",
    "tofu": "Tofu Products",
    "baguette": "Grains & Dry Goods",
    "bagel": "Grains & Dry Goods",
    "croissant": "Grains & Dry Goods",
    "noodles": "Grains & Dry Goods",
    "oatmeal": "Grains & Dry Goods",
    "wheat_flour": "Grains & Dry Goods",
    "white_bread": "Grains & Dry Goods",
    "tortilla": "Grains & Dry Goods"
}

def create_dirs():
    for cat in CATEGORIES:
        (TARGET_DIR / cat).mkdir(parents=True, exist_ok=True)

def copy_existing_data():
    if not EXISTING_DATA_DIR.exists():
        print(f"Existing dataset dir not found: {EXISTING_DATA_DIR}")
        return
        
    print(f"Copying existing local data from {EXISTING_DATA_DIR}...")
    for source_folder, target_cat in CRAWLED_TO_CATEGORIES.items():
        src_path = EXISTING_DATA_DIR / source_folder
        if src_path.exists() and src_path.is_dir():
            target_path = TARGET_DIR / target_cat
            files = list(src_path.glob("*.*"))
            copied = 0
            for file in files:
                if file.suffix.lower() in [".jpg", ".jpeg", ".png"]:
                    dest_file = target_path / f"local_{source_folder}_{file.name}"
                    if not dest_file.exists():
                        shutil.copy2(file, dest_file)
                        copied += 1
            print(f"  -> Copied {copied} images from {source_folder} to {target_cat}")

def download_missing_data():
    print("Starting web crawler to download images via Bing...")
    # Set limit to a smaller number for faster execution during setup.
    # The user can increase this limit and re-run overnight for the full 35,000 dataset!
    limit_per_query = 15
    
    for category, queries in SEARCH_MAPPINGS.items():
        # Skip if we already have enough local data
        target_path = TARGET_DIR / category
        existing_count = len(list(target_path.glob("*.*")))
        if existing_count >= 50:
            print(f"Skipping {category} (already has {existing_count} images)")
            continue
        print(f"\nDownloading for category: {category}")
        for query in queries:
            print(f"  Searching: {query}")
            try:
                downloader.download(
                    query, 
                    limit=limit_per_query,  
                    output_dir=str(TARGET_DIR / "temp_downloads"), 
                    adult_filter_off=False, 
                    force_replace=False, 
                    timeout=60,
                    verbose=False
                )
                
                # Move them to the actual category folder and rename
                downloaded_dir = TARGET_DIR / "temp_downloads" / query
                if downloaded_dir.exists():
                    for file in downloaded_dir.glob("*.*"):
                        if file.suffix.lower() in [".jpg", ".jpeg", ".png"]:
                            dest_file = target_path / f"bing_{query.replace(' ', '_')}_{file.name}"
                            if not dest_file.exists():
                                shutil.move(str(file), str(dest_file))
            except Exception as e:
                print(f"  Failed to download {query}: {e}")

if __name__ == "__main__":
    create_dirs()
    copy_existing_data()
    download_missing_data()
    print("\nData organization complete! Check", TARGET_DIR)
