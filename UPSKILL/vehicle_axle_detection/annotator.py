import cv2
from typing import List
from UPSKILL.UPSKILL.vehicle_axle_detection.detector import DetectionResult

def annotate_image(image, detections: List[DetectionResult], vehicle_bbox=None):
    annotated = image.copy()
    for det in detections:
        x1, y1, x2, y2 = det.bbox
        color = (0, 255, 0) if det.class_name == 'wheel' else (255, 0, 0)
        cv2.rectangle(annotated, (x1, y1), (x2, y2), color, 2)
        label = f"{det.class_name} {det.confidence:.2f}"
        cv2.putText(annotated, label, (x1, y1 - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.5, color, 2)
    if vehicle_bbox:
        x1, y1, x2, y2 = vehicle_bbox
        cv2.rectangle(annotated, (x1, y1), (x2, y2), (0, 0, 255), 2)
        cv2.putText(annotated, 'vehicle', (x1, y1 - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 0, 255), 2)
    return annotated
