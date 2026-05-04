import subprocess
import tempfile
import cv2
import numpy as np
from typing import List
from UPSKILL.UPSKILL.vehicle_axle_detection.config import DARKNET_PATH, YOLO_CFG, YOLO_WEIGHTS, YOLO_NAMES, CONFIDENCE_THRESHOLD, NMS_THRESHOLD
from UPSKILL.UPSKILL.vehicle_axle_detection.detector import DetectorInterface, DetectionResult

class DarknetDetector(DetectorInterface):
    def __init__(self):
        # Load class names
        with open(YOLO_NAMES, 'r') as f:
            self.class_names = [line.strip() for line in f.readlines()]

    def detect(self, image: np.ndarray) -> List[DetectionResult]:
        # Save image to temp file
        with tempfile.NamedTemporaryFile(suffix='.jpg', delete=False) as tmp:
            cv2.imwrite(tmp.name, image)
            image_path = tmp.name
        # Run Darknet detection
        cmd = [
            f"{DARKNET_PATH}/darknet", 'detect', YOLO_CFG, YOLO_WEIGHTS, image_path,
            '-thresh', str(CONFIDENCE_THRESHOLD), '-ext_output', '-dont_show'
        ]
        result = subprocess.run(cmd, capture_output=True, text=True)
        output = result.stdout
        # Parse output
        detections = self._parse_darknet_output(output)
        return detections

    def _parse_darknet_output(self, output: str) -> List[DetectionResult]:
        # Example output parsing (customize for your YOLO version)
        import re
        detections = []
        for line in output.splitlines():
            for class_name in self.class_names:
                if line.startswith(class_name):
                    # Example: wheel: 94%    (left_x:  123   top_y:  456   width:  78   height:  90)
                    m = re.search(rf'{class_name}: (\d+)%.*left_x: *(\d+).*top_y: *(\d+).*width: *(\d+).*height: *(\d+)', line)
                    if m:
                        conf = float(m.group(1)) / 100.0
                        x1 = int(m.group(2))
                        y1 = int(m.group(3))
                        w = int(m.group(4))
                        h = int(m.group(5))
                        bbox = [x1, y1, x1 + w, y1 + h]
                        detections.append(DetectionResult(class_name, conf, bbox))
        return detections
