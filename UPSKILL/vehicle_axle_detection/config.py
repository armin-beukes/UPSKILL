import os
from pathlib import Path

# Configuration for the vehicle axle detection system

# Input/output folders
INPUT_FOLDER = os.getenv('INPUT_FOLDER', './input')
OUTPUT_FOLDER = os.getenv('OUTPUT_FOLDER', './output')
ANNOTATED_FOLDER = os.getenv('ANNOTATED_FOLDER', './output/annotated')

# Frame extraction
FRAME_INTERVAL = float(os.getenv('FRAME_INTERVAL', 0.5))  # seconds between frames

# Model paths (do not hard-code, set via env or config file)
DARKNET_PATH = os.getenv('DARKNET_PATH', '/opt/darknet')
YOLO_CFG = os.getenv('YOLO_CFG', './model/yolov4.cfg')
YOLO_WEIGHTS = os.getenv('YOLO_WEIGHTS', './model/yolov4.weights')
YOLO_NAMES = os.getenv('YOLO_NAMES', './model/obj.names')

# Detection thresholds
CONFIDENCE_THRESHOLD = float(os.getenv('CONFIDENCE_THRESHOLD', 0.5))
NMS_THRESHOLD = float(os.getenv('NMS_THRESHOLD', 0.4))

# Output formats
EXPORT_JSON = True
EXPORT_CSV = True
EXPORT_XLSX = True

# Classes of interest
CLASSES = ['vehicle', 'truck', 'trailer', 'wheel', 'licence_plate']

# Utility
def ensure_dirs():
    Path(OUTPUT_FOLDER).mkdir(parents=True, exist_ok=True)
    Path(ANNOTATED_FOLDER).mkdir(parents=True, exist_ok=True)

ensure_dirs()
