import numpy as np
from typing import List, Dict, Any
from UPSKILL.UPSKILL.vehicle_axle_detection.detector import DetectionResult

def group_wheels_by_vehicle(detections: List[DetectionResult]) -> List[List[DetectionResult]]:
    # Placeholder: group wheels by proximity (simple clustering)
    # In production, use a better clustering algorithm
    wheels = [d for d in detections if d.class_name == 'wheel']
    if not wheels:
        return []
    # Simple: treat all wheels as one group
    return [wheels]

def estimate_axle_count(wheel_groups: List[List[DetectionResult]]) -> int:
    # Each axle has 2 wheels (simplified)
    if not wheel_groups:
        return 0
    wheels = wheel_groups[0]
    return max(1, len(wheels) // 2)
