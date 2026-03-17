# YOLO Food Scanner - Technical Documentation

This document explains how YOLO object detection works in the FoodExpiryApp for detecting food items via camera.

---

## Overview

The YOLO scanner uses TensorFlow Lite to run a custom-trained YOLO26n model on-device for real-time food detection. When a user opens the scanner, the camera captures frames, passes them through the model, and displays bounding boxes around detected food items.

---

## Project Structure

### Kotlin Files (Android App)

```
app/src/main/java/com/example/foodexpiryapp/presentation/ui/yolo/
├── YoloScanFragment.kt          # Main scanner UI + camera handling
├── YoloDetector.kt             # TensorFlow Lite model loading & inference
└── DetectionOverlayView.kt     # Draws bounding boxes on camera preview
```

### Asset Files

```
app/src/main/assets/
├── food_yolo26n_float32.tflite  # ✅ TRAINED FOOD MODEL (PRIMARY - 11 classes)
├── yolo26n_float32.tflite        # COCO pretrained (fallback - 80 classes)
├── food_categories.txt          # 34 food labels for your model
├── coco_labels.txt              # 80 COCO labels
└── yolo_labels.txt              # Legacy labels
```

### Layout & Navigation

```
app/src/main/res/
├── layout/fragment_yolo_scan.xml  # Scanner screen layout
└── navigation/nav_graph.xml       # Contains navigation_yolo_scan
```

---

## How Detection Works

### 1. Camera Capture (YoloScanFragment.kt)

```
Camera Frame → ImageAnalysis → YUV Format → Bitmap (640x640)
```

- Uses CameraX with `ImageAnalysis` 
- Frame rate: ~1 detection per 800ms (to save processing)
- Image converted from YUV_420_888 → NV21 → Bitmap

### 2. Model Inference (YoloDetector.kt)

```
Bitmap (640x640) → TensorFlow Lite → [1, 300, 6] Output Array
```

**Input:** 640x640 normalized image (values 0-1)

**Output Format per detection [1, 300, 6]:**
```
[x1, y1, x2, y2, confidence, class_id]
- x1, y1, x2, y2: Bounding box coordinates (absolute pixels in 640x640 space)
- confidence: Detection confidence (0.0 - 1.0)
- class_id: Index of the detected class label
```

### 3. Post-Processing

- Filter by confidence threshold: **0.35** (35%)
- Map `class_id` → label string using label file
- Optional NMS (Non-Maximum Suppression) for legacy models

### 4. Display Results (DetectionOverlayView.kt)

- Scale coordinates from 640x640 to actual view size
- Draw bounding boxes with color coding:
  - **Green:** Confidence > 80%
  - **Yellow:** Confidence > 60%
  - **Red:** Confidence ≤ 60%
- Show label + confidence % text

---

## Food Labels (34 Classes)

Your trained model can detect these food items:

| Category | Items |
|----------|-------|
| **FRUITS** | APPLE, BANANA, GRAPE, LEMON, ORANGE, PINEAPPLE, WATERMELON |
| **VEGETABLES** | BEANS, CHILLI, CORN, TOMATO |
| **MEAT** | FISH |
| **DAIRY** | MILK |
| **GRAINS** | CEREAL, FLOUR, PASTA, RICE |
| **CONDIMENTS** | HONEY, JAM, OIL, SPICES, SUGAR, TOMATO_SAUCE, VINEGAR |
| **SNACKS** | CAKE, CANDY, CHIPS, CHOCOLATE, NUTS |
| **BEVERAGES** | COFFEE, JUICE, SODA, TEA, WATER |

---

## Configuration Parameters

| Parameter | Value | Location |
|-----------|-------|----------|
| Input Size | 640x640 | YoloDetector.kt:62 |
| Max Detections | 300 | YoloDetector.kt:65 |
| Confidence Threshold | 0.35 (35%) | YoloDetector.kt:68 |
| High Confidence | 0.50 (50%) | YoloScanFragment.kt:226 |
| Detection Interval | 800ms | YoloScanFragment.kt:37 |

---

## Testing the Scanner

### Manual Testing Steps

1. **Open the app** on Android device/emulator
2. **Navigate to YOLO Scanner** (from Inventory screen)
3. **Point camera** at a food item
4. **Wait for detection** - bounding boxes should appear
5. **Check confidence** - if ≥50%, "Use Detection" button appears
6. **Tap button** to add detected food to inventory

### Expected Behavior

| Scenario | Expected Result |
|----------|-----------------|
| Good lighting, clear food view | Green/yellow box, high confidence |
| Poor lighting | Red box, low confidence |
| No food in frame | "Scanning... point camera at an object" |
| Food not in trained classes | May show wrong label or low confidence |

### Test Food Items

Best test items (from your 34 classes):
- 🍎 Apple
- 🍌 Banana
- 🍊 Orange
- 🍅 Tomato
- 🥛 Milk
- 🍫 Chocolate
- 🍪 Cake

---

## Troubleshooting

### Model Not Loading
- Check `food_yolo26n_float32.tflite` exists in `app/src/main/assets/`
- Check file name matches exactly (case-sensitive)

### No Detections
- Ensure good lighting
- Hold camera steady
- Point at single food item
- Check model matches label file

### Low Confidence
- This is normal for edge cases
- The model was trained on your custom dataset
- Results depend on training data quality

### App Crashes
- Check Camera permission is granted
- Check TensorFlow Lite dependency in build.gradle

---

## Model Training (Reference)

If you need to retrain or improve the model:

1. **Collect images** - Take photos of food items
2. **Annotate** - Use tools like Roboflow to label bounding boxes
3. **Train** - Use Ultralytics YOLO26n
4. **Export** - Convert to TFLite format
5. **Deploy** - Replace `food_yolo26n_float32.tflite` in assets

Training scripts available in project root:
- `train_yolo26.py` - Main training script
- `export_yolo26.py` - Export to TFLite
- `prepare_training_data.py` - Data preparation

---

## Code Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        YoloScanFragment                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │ CameraX     │  │ FrameAnalyzer │  │ UI Updates          │  │
│  │ Preview     │──│ (800ms)      │──│ (bounding boxes)    │  │
│  └──────────────┘  └──────┬───────┘  └──────────────────────┘  │
└───────────────────────────┼────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                        YoloDetector                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │ Load TFLite │  │ preprocess   │  │ parseDetections     │  │
│  │ Model       │──│ (640x640)    │──│ (filter by conf)    │  │
│  └──────────────┘  └──────────────┘  └──────────────────────┘  │
│                            │                                    │
│                            ▼                                    │
│                    ┌──────────────┐                             │
│                    │ Detection    │                             │
│                    │ Result List  │                             │
│                    └──────────────┘                             │
└───────────────────────────┬────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    DetectionOverlayView                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │ Scale coords │  │ Draw boxes   │  │ Draw labels + %     │  │
│  │ (640→view)  │──│ (color coded)│──│ (on camera preview) │  │
│  └──────────────┘  └──────────────┘  └──────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Key Files Reference

| File | Purpose | Lines |
|------|---------|-------|
| YoloScanFragment.kt:156-188 | Frame analysis loop | 33 |
| YoloDetector.kt:244-272 | Main detect() function | 29 |
| YoloDetector.kt:308-359 | Parse detections | 52 |
| DetectionOverlayView.kt:56-107 | Draw bounding boxes | 52 |

---

## Dependencies

```gradle
// TensorFlow Lite
implementation 'org.tensorflow:tensorflow-lite:2.14.0'
implementation 'org.tensorflow:tensorflow-lite-support:0.4.4'

// CameraX
implementation 'androidx.camera:camera-core:1.3.1'
implementation 'androidx.camera:camera-camera2:1.3.1'
implementation 'androidx.camera:camera-lifecycle:1.3.1'
implementation 'androidx.camera:camera-view:1.3.1'
```

---

*Last Updated: February 2026*
*Maintained by: jinjin-programm*
