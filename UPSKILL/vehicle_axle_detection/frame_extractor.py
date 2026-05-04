import cv2
import os
from typing import List
from UPSKILL.UPSKILL.vehicle_axle_detection.config import FRAME_INTERVAL

def extract_frames(video_path: str, output_dir: str, frame_interval: float = FRAME_INTERVAL) -> List[str]:
    cap = cv2.VideoCapture(video_path)
    fps = cap.get(cv2.CAP_PROP_FPS)
    frame_count = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    interval_frames = int(fps * frame_interval)
    frame_paths = []
    frame_num = 0
    saved_num = 0
    while cap.isOpened():
        ret, frame = cap.read()
        if not ret:
            break
        if frame_num % interval_frames == 0:
            frame_file = os.path.join(output_dir, f"frame_{saved_num:05d}.jpg")
            cv2.imwrite(frame_file, frame)
            frame_paths.append(frame_file)
            saved_num += 1
        frame_num += 1
    cap.release()
    return frame_paths
