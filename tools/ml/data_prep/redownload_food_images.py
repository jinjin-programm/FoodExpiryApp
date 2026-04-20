"""
Re-download failed food images using Foodish API + Lorem Flickr with retry + better queries.
Only downloads images that were flagged as WRONG or MIXED by vision analysis.
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

NEEDS_REDOWNLOAD = {
    "WRONG": [
        "milk",
        "cereal",
        "chips",
        "fish",
        "flour",
        "nuts",
        "pasta",
        "rice",
        "soda",
        "spices",
        "sugar",
        "tea",
        "tomato_sauce",
        "cheese",
        "butter",
        "yogurt",
        "salmon",
        "onion",
        "cream",
        "donut",
        "pretzel",
        "mayonnaise",
        "soy_sauce",
        "bagel",
        "cottage_cheese",
        "cream_cheese",
        "coconut",
        "celery",
        "eggplant",
        "bacon",
        "sausage",
        "turkey",
    ],
    "MIXED": [
        "banana",
        "juice",
        "pineapple",
        "strawberry",
        "tomato",
        "watermelon",
        "egg",
        "chicken",
        "broccoli",
        "spinach",
        "lettuce",
        "cucumber",
        "mushroom",
        "carrot",
        "mango",
        "kiwi",
        "pear",
        "burger",
        "sandwich",
        "soup",
        "cracker",
        "plum",
        "ham",
    ],
}

SEARCH_QUERIES = {
    "milk": ["glass of white milk only", "milk glass dairy white"],
    "cereal": ["breakfast cereal flakes bowl", "corn flakes cereal"],
    "chips": ["potato chips single bag", "crisps potato chips snack"],
    "fish": ["raw fish fillet fresh", "salmon fish fillet raw"],
    "flour": ["wheat flour white powder bag", "all purpose flour bag"],
    "nuts": ["mixed nuts almonds walnuts bowl", "almond nuts single"],
    "pasta": ["spaghetti pasta dry uncooked", "penne pasta raw dry"],
    "rice": ["white rice grains uncooked", "basmati rice raw grains"],
    "soda": ["cola soda can drink", "carbonated soda can"],
    "spices": ["spice jars colorful", "ground spices turmeric paprika"],
    "sugar": ["white sugar granulated bowl", "sugar cubes white"],
    "tea": ["green tea leaves dry", "tea bags box"],
    "tomato_sauce": ["tomato sauce pasta jar", "passata tomato puree"],
    "cheese": ["cheddar cheese block yellow", "cheese wedge yellow"],
    "butter": ["butter stick yellow dairy", "butter block yellow"],
    "yogurt": ["yogurt cup white plain", "greek yogurt white cup"],
    "salmon": ["salmon fillet raw fresh", "fresh salmon steak raw"],
    "onion": ["yellow onion whole single", "brown onion single"],
    "cream": ["heavy cream carton pouring", "whipping cream white liquid"],
    "donut": ["glazed donut single pink", "ring donut sweet"],
    "pretzel": ["soft pretzel salted single", "pretzel bread salted"],
    "mayonnaise": ["mayonnaise jar white", "mayo jar condiment"],
    "soy_sauce": ["soy sauce bottle dark", "kikkoman soy sauce bottle"],
    "bagel": ["plain bagel single bread", "sesame bagel bread"],
    "cottage_cheese": ["cottage cheese bowl white curd", "cottage cheese carton"],
    "cream_cheese": ["cream cheese spread white tub", "philadelphia cream cheese"],
    "coconut": ["whole brown coconut", "fresh coconut halved"],
    "celery": ["celery stalks fresh green", "celery bunch fresh"],
    "eggplant": ["purple eggplant single whole", "aubergine purple fresh"],
    "bacon": ["bacon strips raw pork", "crispy bacon strips fried"],
    "sausage": ["pork sausage links raw", "sausage rolls meat"],
    "turkey": ["raw turkey breast meat", "turkey meat raw poultry"],
    "banana": ["single ripe banana yellow", "banana fruit yellow fresh"],
    "juice": ["orange juice glass single", "fresh orange juice glass"],
    "pineapple": ["whole pineapple fresh tropical", "fresh pineapple fruit single"],
    "strawberry": ["single strawberry red fresh", "fresh red strawberries close"],
    "tomato": ["single red tomato fresh", "ripe red tomato vegetable"],
    "watermelon": ["watermelon slice red fresh", "watermelon half fruit"],
    "egg": ["fresh white eggs single", "chicken egg single white"],
    "chicken": ["raw chicken breast meat", "fresh chicken thigh raw"],
    "broccoli": ["fresh broccoli head green", "single broccoli vegetable"],
    "spinach": ["fresh spinach leaves green", "baby spinach leaves green"],
    "lettuce": ["fresh green lettuce head", "iceberg lettuce whole"],
    "cucumber": ["fresh green cucumber whole", "single cucumber vegetable"],
    "mushroom": ["fresh white mushroom single", "button mushrooms fresh"],
    "carrot": ["fresh orange carrot single", "raw carrots orange vegetable"],
    "mango": ["ripe yellow mango single fruit", "fresh mango tropical"],
    "kiwi": ["kiwi fruit halved green", "single kiwi fruit brown"],
    "pear": ["green pear fruit single", "fresh pear whole"],
    "burger": ["single hamburger cheeseburger", "beef burger single"],
    "sandwich": ["club sandwich single", "ham cheese sandwich single"],
    "soup": ["hot soup bowl single", "tomato soup bowl"],
    "cracker": ["saltine crackers single", "wheat crackers snack"],
    "plum": ["purple plum fruit single", "fresh plum dark red"],
    "ham": ["sliced ham deli meat", "cooked ham slices"],
}


def try_lorem_flickr(query: str, lock_val: int) -> bytes | None:
    keyword = query.replace(" ", ",")
    url = f"https://loremflickr.com/400/400/{keyword}?lock={lock_val}"
    try:
        resp = requests.get(
            url,
            timeout=TIMEOUT,
            allow_redirects=True,
            headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"},
        )
        if resp.status_code == 200 and len(resp.content) > 3000:
            ct = resp.headers.get("content-type", "")
            if "image" in ct:
                return resp.content
    except Exception:
        pass
    return None


def try_foodish(food_name: str) -> bytes | None:
    try:
        resp = requests.get("https://foodish-api.com/api/", timeout=TIMEOUT)
        if resp.status_code == 200:
            img_url = resp.json().get("image")
            if img_url:
                img_resp = requests.get(img_url, timeout=TIMEOUT)
                if img_resp.status_code == 200 and len(img_resp.content) > 5000:
                    return img_resp.content
    except Exception:
        pass
    return None


def process_and_save(image_data: bytes, output_path: Path) -> bool:
    try:
        img = Image.open(BytesIO(image_data))
        if img.mode != "RGB":
            img = img.convert("RGB")
        img = img.resize(TARGET_SIZE, Image.Resampling.LANCZOS)
        img.save(str(output_path), "JPEG", quality=85, optimize=True)
        return True
    except Exception:
        return False


def is_likely_food_statue(image_data: bytes) -> bool:
    try:
        img = Image.open(BytesIO(image_data))
        w, h = img.size
        pixels = list(img.getdata())
        total = len(pixels)
        gray_count = 0
        green_count = 0
        for r, g, b in pixels[: min(1000, total)]:
            if abs(r - g) < 20 and abs(g - b) < 20 and r > 100:
                gray_count += 1
            if g > r + 20 and g > b + 20:
                green_count += 1
        gray_ratio = gray_count / min(1000, total)
        if gray_ratio > 0.6:
            return True
        return False
    except Exception:
        return False


def download_food_image(food_name: str) -> bool:
    output_path = SAVE_DIR / f"food_{food_name}.jpg"
    queries = SEARCH_QUERIES.get(food_name, [food_name])

    best_data = None
    best_score = -1

    for query in queries:
        for lock_val in range(1, 50):
            data = try_lorem_flickr(query, lock_val)
            if data and not is_likely_food_statue(data):
                score = len(data)
                if score > best_score:
                    best_score = score
                    best_data = data
                break
            time.sleep(0.1)
        if best_data:
            break

    if best_data and process_and_save(best_data, output_path):
        size_kb = output_path.stat().st_size / 1024
        print(f"  {food_name}: OK ({size_kb:.0f}KB) query='{query}'")
        return True

    print(f"  {food_name}: FAILED")
    return False


def main():
    all_bad = NEEDS_REDOWNLOAD["WRONG"] + NEEDS_REDOWNLOAD["MIXED"]
    print(f"Re-downloading {len(all_bad)} bad food images...")
    print(f"  WRONG: {len(NEEDS_REDOWNLOAD['WRONG'])}")
    print(f"  MIXED: {len(NEEDS_REDOWNLOAD['MIXED'])}")
    print("=" * 60)

    success = 0
    fail = 0
    failed_items = []

    for i, name in enumerate(all_bad):
        print(f"[{i + 1}/{len(all_bad)}]", end="")
        if download_food_image(name):
            success += 1
        else:
            fail += 1
            failed_items.append(name)
        time.sleep(0.3)

    print("=" * 60)
    print(f"Done: {success} success, {fail} failed")
    if failed_items:
        print(f"Failed: {', '.join(failed_items)}")


if __name__ == "__main__":
    main()
