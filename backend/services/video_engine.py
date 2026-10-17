import os
import cv2
import tempfile
import base64
from typing import List, Dict, Any, Optional
from .dslr_engine import compute_sharpness_score

def extract_video_frames(
    video_path: str,
    interval_ms: int = 1000,
    max_frames: int = 30,
    extract_thumbnails: bool = True
) -> Dict[str, Any]:
    """
    Extracts frames from a video file with millisecond accuracy and metadata.
    """
    cap = cv2.VideoCapture(video_path)
    if not cap.isOpened():
        raise ValueError("Could not open video file.")

    fps = cap.get(cv2.CAP_PROP_FPS) or 30.0
    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    duration_ms = int((total_frames / fps) * 1000) if fps > 0 else 0
    width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))

    extracted_frames: List[Dict[str, Any]] = []

    # Calculate step in milliseconds
    step_ms = max(50, interval_ms)
    current_time_ms = 0
    frame_idx = 0

    while current_time_ms <= duration_ms and len(extracted_frames) < max_frames:
        cap.set(cv2.CAP_PROP_POS_MSEC, current_time_ms)
        ret, frame_bgr = cap.read()
        if not ret:
            break

        sharpness = compute_sharpness_score(frame_bgr)
        
        # Thumbnail for fast preview transmission
        thumb_b64 = None
        if extract_thumbnails:
            # 320px width thumbnail
            scale = 320.0 / max(1, width)
            thumb_h = int(height * scale)
            thumb_bgr = cv2.resize(frame_bgr, (320, thumb_h), interpolation=cv2.INTER_AREA)
            _, thumb_enc = cv2.imencode(".jpg", thumb_bgr, [cv2.IMWRITE_JPEG_QUALITY, 80])
            thumb_b64 = base64.b64encode(thumb_enc.tobytes()).decode("utf-8")

        extracted_frames.append({
            "index": frame_idx,
            "timestamp_ms": current_time_ms,
            "timestamp_label": f"{current_time_ms // 1000}.{str(current_time_ms % 1000).zfill(3)}s",
            "sharpness_score": sharpness,
            "is_sharpest": False,
            "thumbnail_b64": thumb_b64
        })

        current_time_ms += step_ms
        frame_idx += 1

    cap.release()

    # Mark sharpest frame
    if extracted_frames:
        sharpest = max(extracted_frames, key=lambda f: f["sharpness_score"])
        sharpest["is_sharpest"] = True

    return {
        "video_info": {
            "duration_ms": duration_ms,
            "fps": round(fps, 2),
            "width": width,
            "height": height,
            "total_frames": total_frames
        },
        "extracted_count": len(extracted_frames),
        "frames": extracted_frames
    }
