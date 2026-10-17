import io
import os
import base64
import tempfile
from typing import Optional, List, Dict, Any
from fastapi import FastAPI, File, UploadFile, Form, Query, HTTPException, Response
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, StreamingResponse
from pydantic import BaseModel, Field

from .services.dslr_engine import (
    enhance_dslr_image,
    DslrProfile,
    compute_sharpness_score
)
from .services.video_engine import extract_video_frames
from .services.situation_engine import find_situation_moment

app = FastAPI(
    title="PHOTO FRAME Cloud Engine API",
    description="Professional DSLR-Grade Video to Photo Frame Extraction & AI Analysis Backend by MG Shankar Tech. Optimized for 1-click deployment on Render.",
    version="2.0.0"
)

# Enable CORS for Android App, Localhost, and Web clients
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ----------------- Models -----------------

class EnhanceRequest(BaseModel):
    image_base64: str = Field(..., description="Base64 encoded input image")
    profile: str = Field(default="DSLR_PRIME", description="DSLR color grading profile")
    sharpness: float = Field(default=0.5, ge=0.0, le=1.0, description="Unsharp mask strength (0.0 to 1.0)")
    micro_contrast: float = Field(default=0.35, ge=0.0, le=1.0, description="CLAHE midtone clarity boost")
    shadow_recovery: float = Field(default=0.25, ge=0.0, le=1.0, description="Shadow detail lift")
    vignette: float = Field(default=0.15, ge=0.0, le=1.0, description="Optical prime lens vignette")
    scale_factor: int = Field(default=1, description="Upscaling factor: 1 (original) or 2 (super-resolution)")
    output_format: str = Field(default="JPEG", description="Output format: JPEG or PNG")

class FrameData(BaseModel):
    index: int
    timestamp_ms: Optional[int] = 0
    sharpness_score: Optional[float] = 50.0
    thumbnail_b64: Optional[str] = None
    image_b64: Optional[str] = None

class SituationRequest(BaseModel):
    prompt: str = Field(..., description="Natural language situation/climax description")
    frames: List[FrameData] = Field(..., description="Candidate frames to evaluate")

# ----------------- Routes -----------------

@app.get("/")
def root():
    return {
        "service": "PHOTO FRAME Backend Cloud Engine",
        "author": "MG Shankar Tech",
        "status": "Online & Operational",
        "version": "2.0.0",
        "render_deployment": "Verified & Ready",
        "docs_url": "/docs",
        "health_check": "/api/health"
    }

@app.get("/health")
@app.get("/api/health")
def health_check():
    return {
        "status": "ok",
        "service": "PHOTO FRAME Backend",
        "platform": "Render Cloud Ready",
        "engine": "OpenCV + NumPy + FastAPI",
        "features": [
            "DSLR Optical Enhancement",
            "CLAHE Micro-Contrast",
            "Prime Lens Vignetting",
            "Video Frame Millisecond Extraction",
            "AI Climax Moment Finder"
        ]
    }

@app.get("/api/profiles")
def get_profiles():
    return {
        "profiles": [
            {
                "id": DslrProfile.DSLR_PRIME,
                "name": "DSLR Prime Lens",
                "description": "Tack-sharp 50mm f/1.2 optical clarity, natural skin tones, and rich dynamic range."
            },
            {
                "id": DslrProfile.STUDIO_VIVID,
                "name": "Studio Vivid",
                "description": "High-impact vibrant colors for action, sports, vehicles, and commercial photography."
            },
            {
                "id": DslrProfile.CINEMATIC_TEAL_ORANGE,
                "name": "Cinematic Teal & Orange",
                "description": "Hollywood blockbuster color grading with teal shadows and warm skin tone highlights."
            },
            {
                "id": DslrProfile.PORTRAIT_GLOW,
                "name": "Portrait Luminous Glow",
                "description": "Delicate highlight rolloff with ultra-sharp eye detail, ideal for close-up portraits."
            },
            {
                "id": DslrProfile.LEICA_MONOCHROME,
                "name": "Leica Monochrome",
                "description": "Fine-art architectural high-contrast black & white with smooth analog tonal gradient."
            },
            {
                "id": DslrProfile.GOLDEN_HOUR,
                "name": "Golden Hour",
                "description": "Warm, radiant afternoon sunlight aesthetic with lifted shadow warmth."
            },
            {
                "id": DslrProfile.NONE,
                "name": "Neutral Clean",
                "description": "Standard sharpness boost and dynamic range recovery without color grading."
            }
        ]
    }

@app.post("/api/enhance/dslr")
async def enhance_file(
    file: UploadFile = File(...),
    profile: str = Form(default="DSLR_PRIME"),
    sharpness: float = Form(default=0.5),
    micro_contrast: float = Form(default=0.35),
    shadow_recovery: float = Form(default=0.25),
    vignette: float = Form(default=0.15),
    scale_factor: int = Form(default=1),
    output_format: str = Form(default="JPEG")
):
    """
    Enhances an uploaded image file directly and returns the enhanced image stream.
    """
    content = await file.read()
    if not content:
        raise HTTPException(status_code=400, detail="Empty image uploaded.")

    try:
        enhanced_bytes, stats = enhance_dslr_image(
            image_bytes=content,
            profile=profile,
            sharpness=sharpness,
            micro_contrast=micro_contrast,
            shadow_recovery=shadow_recovery,
            vignette=vignette,
            scale_factor=scale_factor,
            output_format=output_format
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Enhancement error: {str(e)}")

    media_type = "image/png" if output_format.upper() == "PNG" else "image/jpeg"
    return Response(
        content=enhanced_bytes,
        media_type=media_type,
        headers={
            "X-Sharpness-Before": str(stats["metrics_before"]["sharpness_score"]),
            "X-Sharpness-After": str(stats["metrics_after"]["sharpness_score"]),
            "X-Sharpness-Improvement": stats["sharpness_improvement"],
            "X-Profile": profile
        }
    )

@app.post("/api/enhance/dslr/json")
async def enhance_json(req: EnhanceRequest):
    """
    Mobile JSON API: Accepts Base64 image and returns Base64 enhanced result with forensic metrics.
    """
    raw_b64 = req.image_base64
    if "," in raw_b64:
        raw_b64 = raw_b64.split(",", 1)[1]

    try:
        image_bytes = base64.b64decode(raw_b64)
    except Exception:
        raise HTTPException(status_code=400, detail="Invalid Base64 image payload.")

    try:
        enhanced_bytes, stats = enhance_dslr_image(
            image_bytes=image_bytes,
            profile=req.profile,
            sharpness=req.sharpness,
            micro_contrast=req.micro_contrast,
            shadow_recovery=req.shadow_recovery,
            vignette=req.vignette,
            scale_factor=req.scale_factor,
            output_format=req.output_format
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Enhancement error: {str(e)}")

    enhanced_b64 = base64.b64encode(enhanced_bytes).decode("utf-8")

    return {
        "status": "success",
        "profile": req.profile,
        "enhanced_image_base64": enhanced_b64,
        "metrics_before": stats["metrics_before"],
        "metrics_after": stats["metrics_after"],
        "sharpness_improvement": stats["sharpness_improvement"],
        "width": stats["width"],
        "height": stats["height"]
    }

@app.post("/api/extract/frames")
async def extract_frames(
    video: UploadFile = File(...),
    interval_ms: int = Form(default=1000),
    max_frames: int = Form(default=30)
):
    """
    Extracts frames from an uploaded video file with millisecond timestamps and sharpness scores.
    """
    with tempfile.NamedTemporaryFile(delete=False, suffix=".mp4") as tmp:
        content = await video.read()
        tmp.write(content)
        tmp_path = tmp.name

    try:
        result = extract_video_frames(
            video_path=tmp_path,
            interval_ms=interval_ms,
            max_frames=max_frames,
            extract_thumbnails=True
        )
        return {"status": "success", **result}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Video extraction error: {str(e)}")
    finally:
        if os.path.exists(tmp_path):
            try:
                os.remove(tmp_path)
            except Exception:
                pass

@app.post("/api/ai/situation-finder")
async def situation_finder(req: SituationRequest):
    """
    Finds the exact winning climax/incident frame matching the user prompt.
    """
    frames_dict = [f.model_dump() for f in req.frames]
    result = await find_situation_moment(
        situation_prompt=req.prompt,
        frames=frames_dict
    )
    return {
        "status": "success",
        "prompt": req.prompt,
        "result": result
    }
