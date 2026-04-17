"""
Download representative food images for the FoodExpiryApp UI.
Each food gets 1 high-quality image saved to app/src/main/res/drawable/
Primary: Lorem Flickr (free, keyword-based, no API key needed)
Fallback: Pexels API (if PEXELS_API_KEY env var is set)
"""

import os
import time
import requests
from pathlib import Path
from PIL import Image
from io import BytesIO

SAVE_DIR = Path(__file__).parent / "app" / "src" / "main" / "res" / "drawable"
TARGET_SIZE = (256, 256)
TIMEOUT = 20
MAX_RETRIES = 3

FOOD_LIST = [
    ("apple", "red apple fruit fresh"),
    ("banana", "ripe banana fruit bunch"),
    ("beans", "canned beans kidney beans"),
    ("cake", "slice chocolate cake dessert"),
    ("candy", "colorful candy sweets"),
    ("cereal", "breakfast cereal bowl with milk"),
    ("chilli", "fresh red chilli peppers"),
    ("chips", "potato chips crispy snack bag"),
    ("chocolate", "chocolate bar dark"),
    ("coffee", "coffee beans roasted bag"),
    ("corn", "fresh sweet corn cob"),
    ("fish", "fresh raw fish fillet"),
    ("flour", "wheat flour bag baking"),
    ("grape", "fresh purple grapes bunch"),
    ("honey", "honey jar golden natural"),
    ("jam", "strawberry jam jar fruit preserve"),
    ("juice", "orange juice glass fresh"),
    ("lemon", "fresh yellow lemon fruit"),
    ("milk", "glass of milk white dairy"),
    ("nuts", "mixed nuts almonds walnuts"),
    ("oil", "olive oil bottle cooking"),
    ("orange", "fresh orange fruit citrus"),
    ("pasta", "spaghetti pasta dry italian"),
    ("pineapple", "fresh pineapple tropical fruit"),
    ("rice", "white rice grains bowl"),
    ("soda", "soda can carbonated drink"),
    ("spices", "colorful spice jars collection"),
    ("sugar", "white sugar bowl granulated"),
    ("tea", "tea bags green tea box"),
    ("tomato", "fresh red tomato vegetable"),
    ("tomato_sauce", "tomato sauce bottle ketchup red"),
    ("vinegar", "vinegar bottle balsamic"),
    ("water", "mineral water bottle clear"),
    ("watermelon", "fresh watermelon slice red green"),
    ("bread", "fresh bread loaf sliced wheat"),
    ("egg", "fresh eggs chicken white"),
    ("cheese", "cheese block cheddar yellow"),
    ("butter", "butter stick dairy yellow"),
    ("yogurt", "yogurt cup white dairy"),
    ("chicken", "raw chicken breast meat"),
    ("beef", "raw beef steak meat red"),
    ("pork", "raw pork chop meat"),
    ("salmon", "fresh salmon fillet raw"),
    ("shrimp", "fresh raw shrimp seafood"),
    ("carrot", "fresh orange carrots vegetables"),
    ("potato", "fresh potatoes brown"),
    ("onion", "fresh onion yellow brown"),
    ("garlic", "fresh garlic bulb white"),
    ("broccoli", "fresh broccoli green vegetable"),
    ("spinach", "fresh spinach leaves green"),
    ("lettuce", "fresh green lettuce salad"),
    ("cucumber", "fresh green cucumber vegetable"),
    ("mushroom", "fresh mushrooms white brown"),
    ("pepper", "bell pepper red green yellow"),
    ("strawberry", "fresh red strawberries fruit"),
    ("blueberry", "fresh blueberries fruit"),
    ("mango", "fresh ripe mango tropical"),
    ("kiwi", "fresh kiwi fruit green sliced"),
    ("avocado", "fresh ripe avocado green"),
    ("pear", "fresh green pear fruit"),
    ("peach", "fresh peach fruit yellow"),
    ("pizza", "pizza slice cheese pepperoni"),
    ("burger", "hamburger cheeseburger meal"),
    ("sandwich", "sandwich bread ham cheese"),
    ("soup", "hot soup bowl homemade"),
    ("salad", "fresh green salad bowl vegetables"),
    ("tofu", "tofu block white soy"),
    ("cream", "heavy cream carton dairy"),
    ("ice_cream", "ice cream cone scoop"),
    ("cookie", "chocolate chip cookies baked"),
    ("donut", "glazed donut pink sprinkles"),
    ("pretzel", "soft pretzel salted baked"),
    ("popcorn", "popcorn bowl butter salted"),
    ("cracker", "saltine crackers snack"),
    ("ketchup", "ketchup bottle tomato red"),
    ("mustard", "mustard jar yellow condiment"),
    ("mayonnaise", "mayonnaise jar condiment white"),
    ("soy_sauce", "soy sauce bottle asian"),
    ("peanut_butter", "peanut butter jar creamy"),
    ("oatmeal", "oatmeal breakfast bowl"),
    ("pancake", "pancakes stack maple syrup"),
    ("bagel", "fresh bagel bread sesame"),
    ("croissant", "butter croissant french pastry"),
    ("muffin", "blueberry muffin baked"),
    ("tortilla", "flour tortilla wrap flatbread"),
    ("cottage_cheese", "cottage cheese curd dairy"),
    ("cream_cheese", "cream cheese spread bagel"),
    ("coconut", "fresh coconut whole green"),
    ("pomegranate", "fresh pomegranate fruit red seeds"),
    ("cherry", "fresh red cherries fruit"),
    ("plum", "fresh purple plum fruit"),
    ("celery", "fresh celery stalks green"),
    ("zucchini", "fresh green zucchini vegetable"),
    ("eggplant", "fresh purple eggplant vegetable"),
    ("ginger", "fresh ginger root spice"),
    ("bacon", "crispy bacon strips fried"),
    ("sausage", "raw sausage links pork"),
    ("ham", "sliced ham deli meat"),
    ("turkey", "raw turkey meat poultry"),
]


def download_from_lorem_flickr(food_name: str, search_query: str) -> bytes | None:
    keyword = search_query.replace(" ", ",")
    for attempt in range(MAX_RETRIES):
        try:
            lock_val = hash(food_name) % 10000
            url = f"https://loremflickr.com/400/400/{keyword}?lock={abs(lock_val)}"
            resp = requests.get(
                url,
                timeout=TIMEOUT,
                allow_redirects=True,
                headers={"User-Agent": "Mozilla/5.0"},
            )
            if resp.status_code == 200 and len(resp.content) > 3000:
                content_type = resp.headers.get("content-type", "")
                if "image" in content_type:
                    return resp.content
        except Exception:
            pass
        time.sleep(1)
    return None


def download_from_pexels(query: str) -> bytes | None:
    pexels_key = os.environ.get("PEXELS_API_KEY", "")
    if not pexels_key:
        return None
    url = f"https://api.pexels.com/v1/search?query={query}&per_page=1&size=medium"
    headers = {"Authorization": pexels_key}
    try:
        resp = requests.get(url, headers=headers, timeout=TIMEOUT)
        if resp.status_code == 200:
            data = resp.json()
            if data.get("photos"):
                img_url = data["photos"][0]["src"]["medium"]
                img_resp = requests.get(img_url, timeout=TIMEOUT)
                if img_resp.status_code == 200 and len(img_resp.content) > 3000:
                    return img_resp.content
    except Exception:
        pass
    return None


def process_and_save(image_data: bytes, output_path: Path) -> bool:
    try:
        img = Image.open(BytesIO(image_data))
        if img.mode in ("RGBA", "P"):
            img = img.convert("RGB")
        elif img.mode != "RGB":
            img = img.convert("RGB")
        img = img.resize(TARGET_SIZE, Image.Resampling.LANCZOS)
        img.save(str(output_path), "JPEG", quality=85, optimize=True)
        return True
    except Exception as e:
        print(f"  Error processing image: {e}")
        return False


def download_food_image(food_name: str, search_query: str) -> tuple[str, bool]:
    safe_name = food_name.replace(" ", "_").lower()
    output_path = SAVE_DIR / f"food_{safe_name}.jpg"

    if output_path.exists() and output_path.stat().st_size > 3000:
        print(f"  SKIP {food_name} (already exists)")
        return (food_name, True)

    print(f"  Downloading: {food_name} ...", end=" ", flush=True)

    image_data = download_from_lorem_flickr(
        food_name, search_query
    ) or download_from_pexels(search_query)

    if image_data and process_and_save(image_data, output_path):
        size_kb = output_path.stat().st_size / 1024
        print(f"OK ({size_kb:.0f}KB)")
        return (food_name, True)

    print("FAILED")
    return (food_name, False)


def main():
    SAVE_DIR.mkdir(parents=True, exist_ok=True)

    print(f"Downloading {len(FOOD_LIST)} food images to {SAVE_DIR}")
    print("=" * 60)

    success_count = 0
    fail_count = 0
    failed_items = []

    for i, (name, query) in enumerate(FOOD_LIST):
        print(f"[{i + 1}/{len(FOOD_LIST)}]", end="")
        food_name, ok = download_food_image(name, query)
        if ok:
            success_count += 1
        else:
            fail_count += 1
            failed_items.append(food_name)
        time.sleep(0.3)

    print("=" * 60)
    print(f"Done: {success_count} success, {fail_count} failed")
    if failed_items:
        print(f"Failed items: {', '.join(failed_items)}")

    manifest_path = SAVE_DIR / "food_images_manifest.txt"
    with open(manifest_path, "w") as f:
        for name, _ in FOOD_LIST:
            safe_name = name.replace(" ", "_").lower()
            f.write(f"food_{safe_name}\n")
    print(f"Manifest written to {manifest_path}")


if __name__ == "__main__":
    main()
