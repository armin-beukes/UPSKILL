from abc import ABC, abstractmethod
from typing import List, Dict, Any
import numpy as np

class DetectionResult:
    def __init__(self, class_name: str, confidence: float, bbox: list):
        self.class_name = class_name
        self.confidence = confidence
        self.bbox = bbox  # [x1, y1, x2, y2]

    def to_dict(self):
        return {
            'class': self.class_name,
            'confidence': self.confidence,
            'bbox': self.bbox
        }

class DetectorInterface(ABC):
    @abstractmethod
    def detect(self, image: np.ndarray) -> List[DetectionResult]:
        """Run detection on an image and return a list of DetectionResult objects."""
        pass
