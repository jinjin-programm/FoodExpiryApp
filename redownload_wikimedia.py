"""
Download food images from Wikimedia Commons (Wikipedia).
Reliable, free, single-food-item images.
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

FOODS_TO_FIX = [
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
]

WIKI_TITLES = {
    "milk": "File:A glass of milk.jpg",
    "cereal": "File:Corn flakes with milk.jpg",
    "chips": "File:Potato-chips.jpg",
    "fish": "File:Fishfillet.jpg",
    "flour": "File:Flour (1).jpg",
    "nuts": "File:Mixed nuts.jpg",
    "pasta": "File:Two raw spaghetti strings.jpg",
    "rice": "File:Rice grains (IRRI).jpg",
    "soda": "File:Cola32.png",
    "spices": "File:Spices in Gujarat (1).jpg",
    "sugar": "File:Sugar 2xmacro.jpg",
    "tea": "File:Gaiwan-Tea-Oolong.jpg",
    "tomato_sauce": "File:Tomato passata.jpg",
    "cheese": "File:Emmentaler 1.jpg",
    "butter": "File:Butter ketaren.jpg",
    "yogurt": "File:Yoghurt with berries (1).jpg",
    "salmon": "File:Salmon fillet.jpg",
    "onion": "File:Onion on white.jpg",
    "cream": "File:Heavy cream.jpg",
    "donut": "File:Chocolate donut.jpg",
    "pretzel": "File:Brezel mit butter.jpg",
    "mayonnaise": "File:Mayonnaise-1.jpg",
    "soy_sauce": "File:Soy sauce dishes.jpg",
    "bagel": "File:Bagel-01.jpg",
    "cottage_cheese": "File:Cottage cheese.jpg",
    "cream_cheese": "File:Schmalzler 2 range.jpg",
    "coconut": "File:Coconut.jpg",
    "celery": "File:Celery cross section.jpg",
    "eggplant": "File:Eggplant display.jpg",
    "bacon": "File:Bacon .jpg",
    "sausage": "File:Sausages.jpg",
    "turkey": "File:Turkey breast roast.jpg",
    "banana": "File:Banana and cross section.jpg",
    "juice": "File:Orange juice 1.jpg",
    "pineapple": "File:Pineapple with sword and crown.jpg",
    "strawberry": "File:Garden strawberry (Fragaria × ananassa) single.jpg",
    "tomato": "File:Tomato slice.svg",
    "watermelon": "File:Watermelon slice.jpg",
    "egg": "File:Egg white.jpg",
    "chicken": "File:Raw chicken.jpg",
    "broccoli": "File:Broccoli DSC00861.png",
    "spinach": "File:Spinach leaves.jpg",
    "lettuce": "File:Lactuca sativa leaf morphology.jpg",
    "cucumber": "File:Cucumber from below.jpg",
    "mushroom": "File:Champignon mushroom.jpg",
    "carrot": "File:Daucus carota 01 ies.jpg",
    "mango": "File:Mango fruits.jpg",
    "kiwi": "File:Kiwi aka.jpg",
    "pear": "File:Pear with stalk.jpg",
    "burger": "File:Cheeseburger.jpg",
    "sandwich": "File:Sandwich with eggs, tomato and cucumber.jpg",
    "soup": "File:Homemade chicken soup.JPG",
    "cracker": "File:Saltine crackers.jpg",
    "plum": "File:Plum on the branch.jpg",
    "ham": "File:Black forest ham.jpg",
}


def get_wiki_image_url(file_title: str) -> str | None:
    url = "https://commons.wikimedia.org/w/api.php"
    params = {
        "action": "query",
        "titles": file_title,
        "prop": "imageinfo",
        "iiprop": "url",
        "iiurlwidth": "400",
        "format": "json",
    }
    try:
        resp = requests.get(url, params=params, timeout=TIMEOUT)
        if resp.status_code == 200:
            data = resp.json()
            pages = data.get("query", {}).get("pages", {})
            for page_id, page_data in pages.items():
                imageinfo = page_data.get("imageinfo", [])
                if imageinfo:
                    thumb = imageinfo[0].get("thumburl")
                    orig = imageinfo[0].get("url")
                    return thumb or orig
    except Exception:
        pass
    return None


def search_wiki_image(food_name: str) -> str | None:
    url = "https://commons.wikimedia.org/w/api.php"
    params = {
        "action": "query",
        "list": "search",
        "srsearch": f"{food_name} food single item",
        "srnamespace": "6",
        "srlimit": "5",
        "format": "json",
    }
    try:
        resp = requests.get(url, params=params, timeout=TIMEOUT)
        if resp.status_code == 200:
            results = resp.json().get("query", {}).get("search", [])
            for result in results:
                title = result.get("title", "")
                img_url = get_wiki_image_url(title)
                if img_url:
                    return img_url
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


def download_food(food_name: str) -> bool:
    output_path = SAVE_DIR / f"food_{food_name}.jpg"

    img_url = None

    wiki_title = WIKI_TITLES.get(food_name)
    if wiki_title:
        img_url = get_wiki_image_url(wiki_title)

    if not img_url:
        img_url = search_wiki_image(food_name)

    if not img_url:
        print(f"  {food_name}: NO URL FOUND")
        return False

    try:
        resp = requests.get(
            img_url,
            timeout=TIMEOUT,
            headers={"User-Agent": "FoodExpiryApp/1.0 (educational project)"},
        )
        if resp.status_code == 200 and len(resp.content) > 3000:
            if process_and_save(resp.content, output_path):
                size_kb = output_path.stat().st_size / 1024
                print(f"  {food_name}: OK ({size_kb:.0f}KB) from {img_url[:60]}...")
                return True
    except Exception as e:
        print(f"  {food_name}: ERROR {e}")

    print(f"  {food_name}: FAILED")
    return False


def main():
    print(f"Downloading {len(FOODS_TO_FIX)} food images from Wikimedia Commons")
    print("=" * 60)

    success = 0
    fail = 0
    failed_items = []

    for i, name in enumerate(FOODS_TO_FIX):
        print(f"[{i + 1}/{len(FOODS_TO_FIX)}]", end="")
        if download_food(name):
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
