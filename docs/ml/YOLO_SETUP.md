# YOLO Model Setup Instructions

## Overview
The YOLO food detection feature requires a TensorFlow Lite model file (`yolo11n_float32.tflite`) to be placed in the assets folder.

## Step 1: Export the Model

Since the YOLO11 model cannot be directly downloaded, you need to export it from PyTorch format to TensorFlow Lite format.

### Prerequisites

1. **Install Python** (3.8 or higher) from https://www.python.org/
2. **Install Ultralytics**:
   ```bash
   pip install ultralytics
   ```

### Export the Model

1. Open a terminal/command prompt
2. Navigate to the project root directory:
   ```bash
   cd C:\Users\jinjin\AndroidStudioProjects\FoodExpiryAPP
   ```
3. Run the export script:
   ```bash
   python export_yolo11.py
   ```

This will:
- Download the YOLO11n PyTorch model (~5.4 MB)
- Convert it to TensorFlow Lite format
- Create the file `yolo11n_saved_model/yolo11n_float32.tflite` (~10-15 MB)

### Alternative: Manual Export

If the script doesn't work, you can export manually:

```python
from ultralytics import YOLO

# Load model
model = YOLO("yolo11n.pt")

# Export to TFLite
model.export(format="tflite", imgsz=640)
```

## Step 2: Copy Model to Assets

After successful export, copy the model file:

### On Windows:
```cmd
copy yolo11n_saved_model\yolo11n_float32.tflite app\src\main\assets\
```

### On macOS/Linux:
```bash
cp yolo11n_saved_model/yolo11n_float32.tflite app/src/main/assets/
```

### Or manually:
- Copy `yolo11n_saved_model/yolo11n_float32.tflite` 
- Paste into `FoodExpiryAPP/app/src/main/assets/`

## Step 3: Verify Installation

The assets folder should now contain:
```
app/src/main/assets/
├── yolo_labels.txt          (already exists)
└── yolo11n_float32.tflite   (your exported model)
```

## Step 4: Rebuild the App

1. In Android Studio: **Build** → **Rebuild Project**
2. Or run from terminal:
   ```bash
   ./gradlew assembleDebug
   ```

## Troubleshooting

### Model File Not Found
If you get "Model file not found" errors:
- Ensure the file is named exactly: `yolo11n_float32.tflite`
- Check it's in `app/src/main/assets/`
- Clean and rebuild: **Build** → **Clean Project**

### Export Fails
If the export script fails:
1. Check Python version: `python --version` (need 3.8+)
2. Update pip: `pip install --upgrade pip`
3. Reinstall ultralytics: `pip install --upgrade ultralytics`
4. Check disk space (need ~500MB free)

### Out of Memory
If export runs out of memory:
```python
# Use smaller image size
model.export(format="tflite", imgsz=320)  # instead of 640
```

### Model Compatibility
The YoloDetector class is designed for YOLO11 models. If you export a different YOLO version:
- Update `MODEL_NAME` in `YoloDetector.kt`
- Adjust `INPUT_SIZE` if needed (YOLO11 uses 640)
- Update `yolo_labels.txt` with correct COCO labels

## Alternative: Using YOLOv5

If YOLO11 export doesn't work, you can use YOLOv5:

```python
from ultralytics import YOLO

model = YOLO("yolov5n.pt")  # Note: yolov5n not yolo11n
model.export(format="tflite", imgsz=640)
```

Then update in `YoloDetector.kt`:
```kotlin
private const val MODEL_NAME = "yolov5n_float32.tflite"
```

## File Sizes

- `yolo11n.pt` (PyTorch): ~5.4 MB (auto-downloaded during export)
- `yolo11n_float32.tflite`: ~10-15 MB (generated)
- `yolo11n_saved_model/` folder: ~50 MB (can be deleted after copying .tflite)

## Next Steps

After setting up the model:
1. Run the app on a device with camera
2. Tap the camera button in the inventory screen
3. Point camera at food items (apple, banana, pizza, etc.)
4. The app will detect and suggest adding items to inventory

## Support

For export issues, see:
- Ultralytics docs: https://docs.ultralytics.com/integrations/tflite/
- GitHub issues: https://github.com/ultralytics/ultralytics/issues
