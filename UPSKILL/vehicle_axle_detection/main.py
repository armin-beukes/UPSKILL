import os
import sys
import uuid
import cv2
import glob
import numpy as np
from datetime import datetime
from UPSKILL.UPSKILL.vehicle_axle_detection.config import INPUT_FOLDER, OUTPUT_FOLDER, ANNOTATED_FOLDER, CLASSES
from UPSKILL.UPSKILL.vehicle_axle_detection.config import EXPORT_JSON, EXPORT_CSV, EXPORT_XLSX
from UPSKILL.UPSKILL.vehicle_axle_detection.detector import DetectorInterface, DetectionResult
from UPSKILL.UPSKILL.vehicle_axle_detection.darknet_detector import DarknetDetector
from UPSKILL.UPSKILL.vehicle_axle_detection.frame_extractor import extract_frames
from UPSKILL.UPSKILL.vehicle_axle_detection.axle_counter import group_wheels_by_vehicle, estimate_axle_count
from UPSKILL.UPSKILL.vehicle_axle_detection.annotator import annotate_image
from UPSKILL.UPSKILL.vehicle_axle_detection.exporters import export_json, export_csv, export_xlsx

import logging
logging.basicConfig(level=logging.INFO)

def process_image(detector, image_path, frame_number=0, timestamp=0.0):
    image = cv2.imread(image_path)
    detections = detector.detect(image)
    vehicle = next((d for d in detections if d.class_name in ['vehicle', 'truck', 'trailer']), None)
    wheels = [d for d in detections if d.class_name == 'wheel']
    wheel_groups = group_wheels_by_vehicle(detections)
    axle_count = estimate_axle_count(wheel_groups)
    quality_flags = {
        'low_confidence': any(d.confidence < 0.5 for d in detections),
        'partial_vehicle': vehicle is None,
        'occlusion_suspected': False,  # Placeholder
        'manual_review_required': False  # Placeholder
    }
    event_id = str(uuid.uuid4())
    result = {
        'event_id': event_id,
        'source_file': image_path,
        'frame_number': frame_number,
        'timestamp_seconds': timestamp,
        'vehicle': {
            'detected': vehicle is not None,
            'class': vehicle.class_name if vehicle else None,
            'confidence': vehicle.confidence if vehicle else None,
            'bbox': vehicle.bbox if vehicle else None
        },
        'detections': [d.to_dict() for d in detections],
        'axle_count_estimate': axle_count,
        'quality_flags': quality_flags
    }
    # Annotate and save image
    annotated = annotate_image(image, detections, vehicle.bbox if vehicle else None)
    out_img = os.path.join(ANNOTATED_FOLDER, f"{os.path.basename(image_path)}")
    cv2.imwrite(out_img, annotated)
    return result

def main():
    detector = DarknetDetector()
    results = []
    # Process images
    image_files = glob.glob(os.path.join(INPUT_FOLDER, '*.jpg')) + glob.glob(os.path.join(INPUT_FOLDER, '*.png'))
    for img_path in image_files:
        logging.info(f"Processing image: {img_path}")
        result = process_image(detector, img_path)
        results.append(result)
    # Process videos
    video_files = glob.glob(os.path.join(INPUT_FOLDER, '*.mp4')) + glob.glob(os.path.join(INPUT_FOLDER, '*.avi'))
    for vid_path in video_files:
        logging.info(f"Processing video: {vid_path}")
        frame_dir = os.path.join(OUTPUT_FOLDER, f"frames_{os.path.splitext(os.path.basename(vid_path))[0]}")
        os.makedirs(frame_dir, exist_ok=True)
        frame_paths = extract_frames(vid_path, frame_dir)
        for i, frame_path in enumerate(frame_paths):
            timestamp = i  # Placeholder, can be improved
            result = process_image(detector, frame_path, frame_number=i, timestamp=timestamp)
            results.append(result)
    # Export results
    if EXPORT_JSON:
        export_json(results, os.path.join(OUTPUT_FOLDER, 'results.json'))
    if EXPORT_CSV:
        export_csv(results, os.path.join(OUTPUT_FOLDER, 'results.csv'))
    if EXPORT_XLSX:
        export_xlsx(results, os.path.join(OUTPUT_FOLDER, 'results.xlsx'))
    logging.info("Processing complete.")

if __name__ == '__main__':
    main()
