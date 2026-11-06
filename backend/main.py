import io
import os
import base64
import tempfile
from typing import Optional, List, Dict, Any
from fastapi import FastAPI, File, UploadFile, Form, Query, HTTPException, Response
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, StreamingResponse, HTMLResponse
from fastapi import Request
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

@app.get("/", response_class=HTMLResponse)
def root(request: Request):
    # If client specifically asks for JSON (e.g. curl or API client), return JSON
    accept = request.headers.get("accept", "")
    if "application/json" in accept and "text/html" not in accept:
        return JSONResponse({
            "service": "PHOTO FRAME Backend Cloud Engine",
            "author": "MG Shankar Tech",
            "status": "Online & Operational",
            "version": "2.0.0",
            "platform": "Render Cloud Verified",
            "docs_url": "/docs",
            "health_check": "/api/health"
        })

    html_content = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>PHOTO FRAME - Cloud Engine Server</title>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=JetBrains+Mono:wght@400;600&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg-dark: #0B0E14;
            --surface-dark: #131823;
            --surface-card: #1A202E;
            --border-color: #263045;
            --cyan-glow: #00F5D4;
            --violet-glow: #7928CA;
            --purple-accent: #8E44AD;
            --text-primary: #F8FAFC;
            --text-secondary: #94A3B8;
            --font-main: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
            --font-mono: 'JetBrains Mono', monospace;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            background-color: var(--bg-dark);
            color: var(--text-primary);
            font-family: var(--font-main);
            min-height: 100vh;
            display: flex;
            flex-direction: column;
            overflow-x: hidden;
        }
        .header {
            background: linear-gradient(180deg, #131823 0%, rgba(19, 24, 35, 0.8) 100%);
            border-bottom: 1px solid var(--border-color);
            padding: 1.25rem 2rem;
            display: flex;
            align-items: center;
            justify-content: space-between;
            backdrop-filter: blur(12px);
            position: sticky;
            top: 0;
            z-index: 50;
        }
        .logo-group {
            display: flex;
            align-items: center;
            gap: 12px;
        }
        .logo-badge {
            width: 44px;
            height: 44px;
            border-radius: 12px;
            background: linear-gradient(135deg, #00F5D4 0%, #7928CA 100%);
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 22px;
            box-shadow: 0 0 20px rgba(0, 245, 212, 0.3);
        }
        .logo-title {
            font-size: 1.25rem;
            font-weight: 800;
            letter-spacing: -0.5px;
            background: linear-gradient(90deg, #FFF, #00F5D4);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }
        .logo-subtitle {
            font-size: 0.75rem;
            color: var(--text-secondary);
            font-weight: 500;
        }
        .status-pill {
            display: flex;
            align-items: center;
            gap: 8px;
            background: rgba(0, 245, 212, 0.08);
            border: 1px solid rgba(0, 245, 212, 0.3);
            border-radius: 999px;
            padding: 6px 14px;
            font-size: 0.8rem;
            font-weight: 600;
            color: var(--cyan-glow);
        }
        .dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background: var(--cyan-glow);
            box-shadow: 0 0 8px var(--cyan-glow);
            animation: pulse 2s infinite;
        }
        @keyframes pulse { 0% { opacity: 0.4; } 50% { opacity: 1; } 100% { opacity: 0.4; } }
        .container {
            max-width: 1100px;
            margin: 2rem auto;
            padding: 0 1.5rem;
            width: 100%;
            flex: 1;
        }
        .hero {
            text-align: center;
            margin-bottom: 2.5rem;
        }
        .hero h1 {
            font-size: 2.5rem;
            font-weight: 800;
            margin-bottom: 0.75rem;
            letter-spacing: -1px;
            background: linear-gradient(135deg, #FFFFFF 30%, #94A3B8 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }
        .hero p {
            color: var(--text-secondary);
            font-size: 1.05rem;
            max-width: 650px;
            margin: 0 auto 1.5rem;
            line-height: 1.6;
        }
        .pill-row {
            display: flex;
            justify-content: center;
            gap: 12px;
            flex-wrap: wrap;
        }
        .tag {
            background: var(--surface-card);
            border: 1px solid var(--border-color);
            padding: 6px 14px;
            border-radius: 8px;
            font-size: 0.8rem;
            font-family: var(--font-mono);
            color: #CBD5E1;
        }
        .main-card {
            background: var(--surface-dark);
            border: 1px solid var(--border-color);
            border-radius: 20px;
            padding: 2rem;
            box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4);
            margin-bottom: 2rem;
        }
        .card-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 1.5rem;
            border-bottom: 1px solid var(--border-color);
            padding-bottom: 1rem;
        }
        .card-title {
            font-size: 1.25rem;
            font-weight: 700;
            color: #FFF;
        }
        .grid-2 {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 2rem;
        }
        @media (max-width: 800px) {
            .grid-2 { grid-template-columns: 1fr; }
            .hero h1 { font-size: 2rem; }
        }
        .form-group {
            margin-bottom: 1.25rem;
        }
        label {
            display: block;
            font-size: 0.85rem;
            font-weight: 600;
            margin-bottom: 0.5rem;
            color: #E2E8F0;
        }
        .file-dropzone {
            border: 2px dashed var(--border-color);
            border-radius: 12px;
            padding: 2rem;
            text-align: center;
            cursor: pointer;
            transition: all 0.2s ease;
            background: rgba(26, 32, 46, 0.5);
        }
        .file-dropzone:hover {
            border-color: var(--cyan-glow);
            background: rgba(0, 245, 212, 0.04);
        }
        select, input[type="range"] {
            width: 100%;
        }
        select {
            background: var(--surface-card);
            border: 1px solid var(--border-color);
            color: var(--text-primary);
            padding: 10px 14px;
            border-radius: 10px;
            font-size: 0.9rem;
            font-family: var(--font-main);
            outline: none;
        }
        .slider-val {
            float: right;
            font-family: var(--font-mono);
            color: var(--cyan-glow);
            font-size: 0.8rem;
        }
        .btn-primary {
            background: linear-gradient(135deg, #00F5D4 0%, #7928CA 100%);
            color: #0B0E14;
            font-weight: 800;
            border: none;
            padding: 12px 24px;
            border-radius: 12px;
            font-size: 0.95rem;
            cursor: pointer;
            width: 100%;
            transition: transform 0.15s ease, box-shadow 0.15s ease;
            box-shadow: 0 4px 20px rgba(0, 245, 212, 0.3);
        }
        .btn-primary:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 25px rgba(0, 245, 212, 0.4);
        }
        .preview-box {
            background: var(--surface-card);
            border: 1px solid var(--border-color);
            border-radius: 14px;
            min-height: 280px;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            overflow: hidden;
            position: relative;
        }
        .preview-box img {
            max-width: 100%;
            max-height: 380px;
            object-fit: contain;
            display: block;
        }
        .metrics-bar {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 10px;
            margin-top: 1rem;
        }
        .metric-card {
            background: rgba(26, 32, 46, 0.7);
            border: 1px solid var(--border-color);
            border-radius: 10px;
            padding: 10px;
            text-align: center;
        }
        .metric-label { font-size: 0.75rem; color: var(--text-secondary); }
        .metric-value { font-size: 1.1rem; font-weight: 700; color: var(--cyan-glow); font-family: var(--font-mono); }
        .api-links {
            display: flex;
            gap: 12px;
            flex-wrap: wrap;
            margin-top: 2rem;
            justify-content: center;
        }
        .api-btn {
            background: var(--surface-card);
            border: 1px solid var(--border-color);
            color: var(--text-primary);
            text-decoration: none;
            padding: 10px 20px;
            border-radius: 10px;
            font-size: 0.85rem;
            font-weight: 600;
            display: inline-flex;
            align-items: center;
            gap: 8px;
            transition: all 0.2s ease;
        }
        .api-btn:hover {
            border-color: var(--cyan-glow);
            color: var(--cyan-glow);
        }
        footer {
            border-top: 1px solid var(--border-color);
            padding: 1.5rem;
            text-align: center;
            font-size: 0.8rem;
            color: var(--text-secondary);
        }
    </style>
</head>
<body>
    <header class="header">
        <div class="logo-group">
            <div class="logo-badge">📸</div>
            <div>
                <div class="logo-title">PHOTO FRAME</div>
                <div class="logo-subtitle">Cloud Processing Engine • MG Shankar Tech</div>
            </div>
        </div>
        <div class="status-pill">
            <span class="dot"></span>
            Render Live Cloud Ready
        </div>
    </header>

    <div class="container">
        <div class="hero">
            <h1>DSLR Frame Enhancement Web Server</h1>
            <p>FastAPI backend with OpenCV edge detection, LAB CLAHE micro-contrast, bilateral denoising, and cinematic profiles. Directly connects with the PHOTO FRAME Android App.</p>
            <div class="pill-row">
                <span class="tag">🚀 Render 1-Click Ready</span>
                <span class="tag">🐍 Python 3.11</span>
                <span class="tag">⚡ FastAPI + Uvicorn</span>
                <span class="tag">📷 OpenCV CLAHE</span>
                <span class="tag">🤖 Gemini Vision AI</span>
            </div>
        </div>

        <div class="main-card">
            <div class="card-header">
                <div class="card-title">✨ Test DSLR Enhancement Online</div>
                <span style="font-size:0.8rem; color:var(--text-secondary)">Real-time Image Processing Pipeline</span>
            </div>

            <div class="grid-2">
                <div>
                    <div class="form-group">
                        <label>Select Photo Frame / Image</label>
                        <div class="file-dropzone" onclick="document.getElementById('fileInput').click()">
                            <input type="file" id="fileInput" accept="image/*" style="display:none" onchange="previewSelectedImage(event)">
                            <div id="dropText">
                                <div style="font-size:28px; margin-bottom:6px">📁</div>
                                <strong>Click to choose image</strong>
                                <p style="font-size:0.75rem; color:var(--text-secondary); margin-top:4px">Supports JPEG, PNG, WebP</p>
                            </div>
                        </div>
                    </div>

                    <div class="form-group">
                        <label>DSLR Profile</label>
                        <select id="profileSelect">
                            <option value="DSLR_PRIME">DSLR Prime 50mm f/1.2 (Tack Sharp)</option>
                            <option value="STUDIO_VIVID">Studio Vivid (Rich Contrast & Saturation)</option>
                            <option value="CINEMATIC_TEAL_ORANGE">Cinematic Teal & Orange (Film Grade)</option>
                            <option value="PORTRAIT_GLOW">Portrait Luminous Glow (Soft Highlights)</option>
                            <option value="LEICA_MONOCHROME">Leica Monochrome (High-Art B&W)</option>
                            <option value="GOLDEN_HOUR">Golden Hour (Warm Sunlight Glow)</option>
                            <option value="NONE">Neutral Clean (Pure Sharpness & Dynamic Range)</option>
                        </select>
                    </div>

                    <div class="form-group">
                        <label>Unsharp Mask Clarity <span class="slider-val" id="sharpVal">0.50</span></label>
                        <input type="range" id="sharpSlider" min="0" max="1" step="0.05" value="0.5" oninput="document.getElementById('sharpVal').innerText = parseFloat(this.value).toFixed(2)">
                    </div>

                    <div class="form-group">
                        <label>CLAHE Micro-Contrast <span class="slider-val" id="claheVal">0.35</span></label>
                        <input type="range" id="claheSlider" min="0" max="1" step="0.05" value="0.35" oninput="document.getElementById('claheVal').innerText = parseFloat(this.value).toFixed(2)">
                    </div>

                    <div class="form-group">
                        <label>Prime Lens Vignette <span class="slider-val" id="vigVal">0.15</span></label>
                        <input type="range" id="vigSlider" min="0" max="1" step="0.05" value="0.15" oninput="document.getElementById('vigVal').innerText = parseFloat(this.value).toFixed(2)">
                    </div>

                    <button class="btn-primary" id="enhanceBtn" onclick="runEnhancement()">⚡ Process with DSLR Engine</button>
                </div>

                <div>
                    <label>Enhanced Result Preview</label>
                    <div class="preview-box" id="resultBox">
                        <span id="emptyState" style="color:var(--text-secondary); font-size:0.9rem">Select an image & click process</span>
                        <img id="enhancedImg" style="display:none" alt="Enhanced Preview">
                    </div>

                    <div class="metrics-bar" id="metricsRow" style="display:none">
                        <div class="metric-card">
                            <div class="metric-label">Input Sharpness</div>
                            <div class="metric-value" id="sharpBefore">--</div>
                        </div>
                        <div class="metric-card">
                            <div class="metric-label">Enhanced Sharpness</div>
                            <div class="metric-value" id="sharpAfter">--</div>
                        </div>
                        <div class="metric-card">
                            <div class="metric-label">Improvement</div>
                            <div class="metric-value" id="sharpImp" style="color:#00F5D4">--</div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="api-links">
            <a href="/docs" target="_blank" class="api-btn">📖 Interactive OpenAPI Docs (/docs)</a>
            <a href="/redoc" target="_blank" class="api-btn">📄 ReDoc Specification (/redoc)</a>
            <a href="/api/health" target="_blank" class="api-btn">🩺 Health Status API (/api/health)</a>
            <a href="/api/profiles" target="_blank" class="api-btn">🎨 Profiles List (/api/profiles)</a>
        </div>
    </div>

    <footer>
        PHOTO FRAME Cloud Engine • Developed by MG Shankar Tech • Certified for Render Deployment
    </footer>

    <script>
        let selectedFile = null;

        function previewSelectedImage(event) {
            const file = event.target.files[0];
            if (file) {
                selectedFile = file;
                document.getElementById('dropText').innerHTML = '<strong>' + file.name + '</strong><br><span style="font-size:0.75rem; color:#00F5D4">Ready for DSLR processing (' + (file.size / 1024).toFixed(1) + ' KB)</span>';
            }
        }

        async function runEnhancement() {
            if (!selectedFile) {
                alert("Please choose an image first!");
                return;
            }

            const btn = document.getElementById('enhanceBtn');
            const emptyState = document.getElementById('emptyState');
            const enhancedImg = document.getElementById('enhancedImg');
            const metricsRow = document.getElementById('metricsRow');

            btn.disabled = true;
            btn.innerText = "⏳ Enhancing frame...";
            emptyState.style.display = "block";
            emptyState.innerText = "Processing optical clarity, tone curve & color grading...";
            enhancedImg.style.display = "none";

            const formData = new FormData();
            formData.append("file", selectedFile);
            formData.append("profile", document.getElementById('profileSelect').value);
            formData.append("sharpness", document.getElementById('sharpSlider').value);
            formData.append("micro_contrast", document.getElementById('claheSlider').value);
            formData.append("vignette", document.getElementById('vigSlider').value);
            formData.append("output_format", "JPEG");

            try {
                const response = await fetch('/api/enhance/dslr', {
                    method: 'POST',
                    body: formData
                });

                if (!response.ok) {
                    throw new Error("Server returned status " + response.status);
                }

                const sBefore = response.headers.get("X-Sharpness-Before") || "--";
                const sAfter = response.headers.get("X-Sharpness-After") || "--";
                const sImp = response.headers.get("X-Sharpness-Improvement") || "--";

                const blob = await response.blob();
                const objectUrl = URL.createObjectURL(blob);

                enhancedImg.src = objectUrl;
                enhancedImg.style.display = "block";
                emptyState.style.display = "none";

                document.getElementById('sharpBefore').innerText = sBefore;
                document.getElementById('sharpAfter').innerText = sAfter;
                document.getElementById('sharpImp').innerText = sImp;
                metricsRow.style.display = "grid";

            } catch (err) {
                emptyState.innerText = "Error during enhancement: " + err.message;
            } finally {
                btn.disabled = false;
                btn.innerText = "⚡ Process with DSLR Engine";
            }
        }
    </script>
</body>
</html>
"""
    return HTMLResponse(content=html_content)

@app.get("/api/info")
def api_info():
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
