# Vehicle Axle Detection Proof-of-Concept

This Python application detects vehicles, wheels, axles, and related objects in images or videos using YOLO/Darknet. It is designed for weighbridge/WIM/Trafman environments as a supporting validation tool (not for legal metrology).

## Features
- Accepts input folder of images and/or videos
- Extracts frames from video at configurable intervals
- Runs object detection using Darknet (YOLO) or a pluggable interface
- Detects and stores bounding boxes for vehicle, truck, trailer, wheel, licence_plate
- Estimates axle count by grouping detected wheels
- Saves results to JSON, CSV, and Excel
- Annotates output images/frames
- Adds quality flags (low confidence, partial vehicle, occlusion, manual review)
- Modular, clean, and well-commented code

## Setup Instructions (WSL Ubuntu)

1. **Install Python 3.10+ and pip**
   ```sh
   sudo apt update
   sudo apt install python3 python3-pip python3-venv
   ```

2. **Clone or copy this repo**

3. **Install required Python packages**
   ```sh
   python3 -m venv venv
   source venv/bin/activate
   pip install -r requirements.txt
   ```

4. **Install OpenCV dependencies**
   ```sh
   sudo apt install libgl1-mesa-glx libglib2.0-0
   ```

5. **Install Darknet (YOLO)**
   - Follow instructions at https://github.com/AlexeyAB/darknet
   - Build with GPU/CUDA if available for best performance
   - Place your YOLO cfg, weights, and names files in a folder (e.g., `./model/`)

6. **Configure paths**
   - Edit `config.py` or set environment variables for:
     - `DARKNET_PATH` (path to Darknet binary)
     - `YOLO_CFG`, `YOLO_WEIGHTS`, `YOLO_NAMES` (model files)
     - `INPUT_FOLDER`, `OUTPUT_FOLDER`, `FRAME_INTERVAL` (as needed)

## Running the Tool

```sh
python main.py
```

- Place input images/videos in the input folder (default: `./input`)
- Annotated images/frames will be saved in `./output/annotated/`
- Results are saved as `results.json`, `results.csv`, and `results.xlsx` in the output folder

## Output Files
- **results.json**: List of detection events with bounding boxes, classes, confidence, axle count, and quality flags
- **results.csv**: Tabular version of the above
- **results.xlsx**: Excel workbook version
- **Annotated images**: Visual overlays of detections

## Output Schema
```
{
  "event_id": "uuid",
  "source_file": "...",
  "frame_number": 123,
  "timestamp_seconds": 4.92,
  "vehicle": {
    "detected": true,
    "class": "truck",
    "confidence": 0.91,
    "bbox": [x1, y1, x2, y2]
  },
  "detections": [
    {
      "class": "wheel",
      "confidence": 0.94,
      "bbox": [x1, y1, x2, y2]
    }
  ],
  "axle_count_estimate": 5,
  "quality_flags": {
    "low_confidence": false,
    "partial_vehicle": false,
    "occlusion_suspected": false,
    "manual_review_required": true
  }
}
```

## Notes
- This tool is for research and validation only. Not for legal metrology.
- You can swap out the detection engine by implementing the `DetectorInterface`.
- For custom models, update your YOLO config/weights/names files.
