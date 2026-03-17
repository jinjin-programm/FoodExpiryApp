import os
import re

dataset_path = "D:/FoodExpiryDatasets/crawled_grocery"

def relabel_images():
    if not os.path.exists(dataset_path):
        print(f"Error: Path {dataset_path} does not exist.")
        return

    categories = [d for d in os.listdir(dataset_path) if os.path.isdir(os.path.join(dataset_path, d))]
    
    for category in categories:
        category_path = os.path.join(dataset_path, category)
        files = sorted(os.listdir(category_path))
        
        print(f"Processing category: {category} ({len(files)} files)")
        
        # Mapping to keep track of used numbers to avoid collisions
        for i, filename in enumerate(files):
            old_path = os.path.join(category_path, filename)
            
            if os.path.isdir(old_path):
                continue
                
            # Extract extension
            name_part, ext = os.path.splitext(filename)
            
            # Find numbers in the original filename
            numbers = re.findall(r'\d+', name_part)
            
            if numbers:
                # Use the last number found in the name (often the most relevant index)
                # Padding to 5 digits for consistency
                number_str = numbers[-1].zfill(5)
            else:
                # If no number, use sequence index
                number_str = str(i + 1).zfill(5)
            
            # Create new filename: Category-Number.ext
            # We use a placeholder first to avoid overwriting if the new name matches an existing one
            new_filename = f"{category.replace('_', ' ').title().replace(' ', '')}-{number_str}{ext}"
            new_path = os.path.join(category_path, new_filename)
            
            if old_path != new_path:
                try:
                    # If target exists, we might need a temporary name or handle it
                    if os.path.exists(new_path):
                        temp_path = os.path.join(category_path, f"TEMP_{new_filename}")
                        os.rename(old_path, temp_path)
                        os.rename(temp_path, new_path)
                    else:
                        os.rename(old_path, new_path)
                except Exception as e:
                    print(f"  Error renaming {filename}: {e}")

if __name__ == "__main__":
    relabel_images()
    print("Relabeling complete.")
