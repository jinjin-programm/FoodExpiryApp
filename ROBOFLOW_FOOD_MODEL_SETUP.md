# Roboflow Food YOLO Model Setup

## Overview
This app now uses the **Roboflow "Food Imgae - YOLO" model** with **85 food classes** including fish, meat, eggs, and many more!

**Model Source:** https://universe.roboflow.com/food-image-classification/food-imgae-yolo

## Food Classes Supported (85 total)

### Fruits (25 classes)
Apple, Apple -Green-, Apple -Red-, Avocado, Banana, Blackberries, Blueberries, Cantaloupe, Clementine, Galia, Grapes, Honeydew, Kiwi, Lemon, Nectarine, Orange, Oranges, Peach, Pear, Pineapple, Plum, Pomegranate, Raspberries, Strawberries, Strawberry, Watermelon

### Vegetables (19 classes)
Artichoke, Asparagus, Beans, Bell Pepper, Broccoli, Brussel Sprouts, Cabbage, Carrots, Cauliflower, Corn, Cucumber, Eggplant, Garlic, Lettuce, Mushroom, Mushrooms, Spinach, Squash, Tomatoes, Zucchini

### Proteins/Meat/Fish (7 classes)
Canned Fish, Egg, Eggs, Fish, Meat, Meat -Red-, Tofu

### Dairy (5 classes)
Butter, Cheese, Creamer, Milk, Yogurt

### Grains & Baked Goods (7 classes)
Bread, Cereal, Cerealbox, Grains, Noodles, Oats, Rice

### Beverages (5 classes)
Coffee, Drinks, Juice, Soda, Water

### Prepared Foods & Snacks (11 classes)
Boxed Food, Canned Food, Jar, Peanut Butter, Pickled Food -Jar-, Popcorn, Potatoes -Package-, Salad, Snack

### Condiments & Sauces (4 classes)
Condiment, Sauce, Seasoning -Thin Package-

### Non-food (2 classes)
Book, Detergent

## Step 1: Download the Model

### Option A: Automatic Download (Recommended)

Run the download script:
```bash
python download_roboflow_food_model.py
```

This will:
1. Prompt for your Roboflow API key
2. Download the model from Roboflow Universe
3. Export it to TensorFlow Lite format
4. Copy it to `app/src/main/ml/food_yolov8_roboflow.tflite`

**To get your API key:**
1. Create a free account at https://universe.roboflow.com
2. Go to https://app.roboflow.com/settings/api
3. Copy your API key

### Option B: Manual Download

1. Go to: https://universe.roboflow.com/food-image-classification/food-imgae-yolo
2. Sign up for free Roboflow account
3. Click "Download Dataset" or "Deploy Model"
4. Select "YOLOv8" format
5. Download the model weights (best.pt file)
6. Export to TFLite:
   ```python
   from ultralytics import YOLO
   model = YOLO("best.pt")
   model.export(format="tflite", imgsz=640)
   ```
7. Copy the exported `.tflite` file to `app/src/main/ml/food_yolov8_roboflow.tflite`

## Step 2: Verify Files

Ensure these files exist:
```
app/src/main/ml/
└── food_yolov8_roboflow.tflite   (85-class food detection model)

app/src/main/assets/
└── yolo_labels.txt               (85 food class labels)
```

## Step 3: Build the App

1. In Android Studio: **Build** → **Rebuild Project**
2. Or run: `./gradlew assembleDebug`

## Model Comparison

| Feature | Old (YOLO11n) | New (Roboflow Food) |
|---------|--------------|---------------------|
| Classes | 80 (generic) | 85 (food-specific) |
| Fish Detection | ❌ No | ✅ Yes |
| Meat Detection | ❌ No | ✅ Yes |
| Egg Detection | ❌ No | ✅ Yes |
| Dairy Detection | ❌ No | ✅ Yes |
| Model Source | COCO Dataset | Roboflow Universe |
| Input Size | 640x640 | 640x640 |

## Troubleshooting

### Model Not Found
If you get "Model file not found" errors:
- Ensure the file is named exactly: `food_yolov8_roboflow.tflite`
- Check it's in `app/src/main/ml/`
- Clean and rebuild: **Build** → **Clean Project**

### API Key Issues
If the download script fails:
- Verify your API key is correct
- Check your internet connection
- Ensure you have access to the model on Roboflow

### Export Fails
If export to TFLite fails:
```bash
pip install --upgrade ultralytics
pip install --upgrade roboflow
```

### Poor Detection
- Ensure good lighting
- Hold camera steady
- Fill at least 30% of frame with food item
- Use plain background when possible

## Requirements

```bash
pip install roboflow ultralytics
```

## Next Steps

1. Get your free Roboflow API key
2. Run the download script
3. Rebuild the project
4. Test detecting fish, meat, eggs, and 80+ other foods!

Enjoy your food-specific YOLO model! 🐟🥩🥚
