# Food YOLO Model Setup Instructions

## Overview
This app now uses the **FoodVision YOLOv8 model** from GitHub, which is specifically trained to detect **55 types of fruits and vegetables**.

## Food Classes Supported
The model can detect:
- **Fruits**: avocados, green apples, green grapes, kiwifruit, limes, banana, date, peach, pear, blackberry, blueberry, cherry, plum, purple grapes, raspberry, red apple, red grape, strawberry, watermelon, apricot, grapefruit, lemon, mango, nectarine, orange, pineapple.
- **Vegetables**: asparagus, broccoli, cabbage, celery, cucumber, green beans, green capsicum, lettuce, peas, spinach, cauliflower, garlic, ginger, mushroom, onion, parsnip, potato, turnip, beetroot, eggplant, purple asparagus, radish, red cabbage, red capsicum, tomato, carrot, corn, pumpkin, sweet potato.

## Step 1: Download and Setup the Model

The setup is now automated. If you need to re-run it:
```bash
python setup_foodvision_model.py
```

This script:
1. Downloads the `best.pt` model from `Nightey3s/FoodVision` repository.
2. Exports it to TensorFlow Lite format (`foodvision_yolov8.tflite`).
3. Copies it to `app/src/main/ml/`.

## Step 2: Verify Files

Ensure these files exist:
```
app/src/main/ml/
└── foodvision_yolov8.tflite      (Food detection model)

app/src/main/assets/
└── yolo_labels.txt               (55 food class labels)
```

## Step 3: Build the App

1. In Android Studio: **Build** → **Rebuild Project**
2. Run the app on a device or emulator.

## Model Comparison

| Feature | Old (YOLO11n) | New (Food YOLOv8) |
|---------|--------------|-------------------|
| Classes | 80 (generic) | 55 (food-specific) |
| Fish Detection | ❌ No | ✅ Yes |
| Meat Detection | ❌ No | ✅ Yes |
| Dairy Detection | ❌ No | ✅ Yes |
| Model Size | ~10 MB | ~15 MB |
| Input Size | 640x640 | 640x640 |

## Troubleshooting

### Model Not Found
If you get "Model file not found" errors:
- Ensure the file is named exactly: `food_yolov8n_float32.tflite`
- Check it's in `app/src/main/ml/`
- Clean and rebuild: **Build** → **Clean Project**

### Download Fails
If the download script fails:
1. Download manually from: https://github.com/Nightey3s/foodvision/raw/main/best.pt
2. Run the export script:
   ```python
   from ultralytics import YOLO
   model = YOLO("best.pt")
   model.export(format="tflite", imgsz=640)
   ```
3. Copy the exported `.tflite` file to `app/src/main/ml/`

### Poor Detection
- Make sure good lighting
- Hold the camera steady
- Ensure the food item fills at least 30% of the frame
- Some items may need to be on a plain background

## Alternative Models

If the FoodVision model doesn't work well for your needs, other options:
1. **Roboflow Food Dataset** (85 classes): https://universe.roboflow.com/food-image-classification/food-imgae-yolo
2. **UEC Food 100** (Japanese food): https://github.com/bennycheung/Food100_YOLO_Tools
3. **Custom Training**: Train your own model using Roboflow or custom dataset

## Next Steps

1. Run the download script
2. Rebuild the project
3. Test with various food items
4. Enjoy detecting fish, meat, and 50+ other food items!
