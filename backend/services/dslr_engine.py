import io
import math
from typing import Tuple, Dict, Any, Optional
import numpy as np
from PIL import Image, ImageOps, ImageEnhance
import cv2

class DslrProfile:
    NONE = "NONE"
    DSLR_PRIME = "DSLR_PRIME"
    STUDIO_VIVID = "STUDIO_VIVID"
    CINEMATIC_TEAL_ORANGE = "CINEMATIC_TEAL_ORANGE"
    PORTRAIT_GLOW = "PORTRAIT_GLOW"
    LEICA_MONOCHROME = "LEICA_MONOCHROME"
    GOLDEN_HOUR = "GOLDEN_HOUR"

def compute_sharpness_score(img_bgr: np.ndarray) -> float:
    """Computes Laplacian variance as an optical sharpness metric."""
    gray = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2GRAY)
    variance = cv2.Laplacian(gray, cv2.CV_64F).var()
    # Normalize score 0-100 range for human readability
    normalized = min(100.0, max(0.0, float(math.log1p(variance) * 11.5)))
    return round(normalized, 1)

def compute_image_metrics(img_bgr: np.ndarray) -> Dict[str, Any]:
    """Evaluates sharpness, exposure balance, contrast, and color richness."""
    gray = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2GRAY)
    sharpness = compute_sharpness_score(img_bgr)
    mean_val = float(np.mean(gray))
    contrast = float(np.std(gray))
    
    # Exposure classification
    if mean_val < 65:
        exposure = "Under-exposed"
    elif mean_val > 195:
        exposure = "Over-exposed"
    else:
        exposure = "Optimal"

    return {
        "sharpness_score": sharpness,
        "mean_brightness": round(mean_val, 1),
        "rms_contrast": round(contrast, 1),
        "exposure_status": exposure
    }

def apply_unsharp_mask(
    img_bgr: np.ndarray,
    amount: float = 0.5,
    radius: int = 1,
    threshold: int = 2
) -> np.ndarray:
    """Simulates prime-lens optical clarity via Gaussian unsharp masking."""
    if amount <= 0.02:
        return img_bgr
    
    blurred = cv2.GaussianBlur(img_bgr, (0, 0), sigmaX=radius)
    diff = cv2.addWeighted(img_bgr, 1.0 + amount, blurred, -amount, 0)
    
    if threshold > 0:
        low_contrast_mask = np.abs(img_bgr.astype(np.int16) - blurred.astype(np.int16)) < threshold
        diff[low_contrast_mask] = img_bgr[low_contrast_mask]
        
    return np.clip(diff, 0, 255).astype(np.uint8)

def apply_clahe_dynamic_range(
    img_bgr: np.ndarray,
    clip_limit: float = 2.0,
    grid_size: int = 8
) -> np.ndarray:
    """Applies Contrast Limited Adaptive Histogram Equalization on Luminance channel."""
    lab = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2LAB)
    l_channel, a_channel, b_channel = cv2.split(lab)
    
    clahe = cv2.createCLAHE(clipLimit=clip_limit, tileGridSize=(grid_size, grid_size))
    l_clahe = clahe.apply(l_channel)
    
    merged = cv2.merge([l_clahe, a_channel, b_channel])
    return cv2.cvtColor(merged, cv2.COLOR_LAB2BGR)

def apply_optical_vignette(img_bgr: np.ndarray, strength: float = 0.15) -> np.ndarray:
    """Simulates wide-aperture f/1.4 prime lens falloff vignette."""
    if strength <= 0.03:
        return img_bgr
    
    h, w = img_bgr.shape[:2]
    kernel_x = cv2.getGaussianKernel(w, w / 2.0)
    kernel_y = cv2.getGaussianKernel(h, h / 2.0)
    kernel = kernel_y * kernel_x.T
    
    mask = kernel / kernel.max()
    # Scale with vignette strength
    vignette = 1.0 - (1.0 - mask) * strength * 0.85
    vignette = np.dstack([vignette] * 3)
    
    result = img_bgr.astype(np.float32) * vignette
    return np.clip(result, 0, 255).astype(np.uint8)

def apply_dslr_color_grading(
    img_bgr: np.ndarray,
    profile: str,
    vibrance_boost: float = 0.15
) -> np.ndarray:
    """Applies cinematic and DSLR film-stock color calibration curves."""
    if profile == DslrProfile.LEICA_MONOCHROME:
        # High dynamic range black and white with fine tonal roll-off
        gray = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2GRAY)
        clahe = cv2.createCLAHE(clipLimit=2.2, tileGridSize=(8, 8))
        eq = clahe.apply(gray)
        # S-curve for deep blacks and crisp highlights
        lut = np.array([int(255 * (1.0 / (1.0 + math.exp(-0.035 * (x - 128))))) for x in range(256)], dtype=np.uint8)
        graded = cv2.LUT(eq, lut)
        return cv2.cvtColor(graded, cv2.COLOR_GRAY2BGR)

    img_float = img_bgr.astype(np.float32) / 255.0
    b, g, r = cv2.split(img_float)

    if profile == DslrProfile.CINEMATIC_TEAL_ORANGE:
        # Shadows push towards cyan/teal, highlights towards warm gold/orange
        luminance = 0.299 * r + 0.587 * g + 0.114 * b
        shadow_mask = np.clip(1.0 - luminance * 1.5, 0.0, 1.0)
        highlight_mask = np.clip(luminance * 1.3 - 0.3, 0.0, 1.0)
        
        b = np.clip(b + shadow_mask * 0.08 - highlight_mask * 0.06, 0.0, 1.0)
        g = np.clip(g + shadow_mask * 0.03, 0.0, 1.0)
        r = np.clip(r - shadow_mask * 0.04 + highlight_mask * 0.10, 0.0, 1.0)

    elif profile == DslrProfile.PORTRAIT_GLOW:
        # Warm, luminous skin tones with soft highlight diffusion
        r = np.clip(r * 1.04 + 0.02, 0.0, 1.0)
        g = np.clip(g * 1.01, 0.0, 1.0)
        b = np.clip(b * 0.96, 0.0, 1.0)

    elif profile == DslrProfile.STUDIO_VIVID:
        # Punchy, saturated commercial photography look
        hsv = cv2.cvtColor((img_float * 255).astype(np.uint8), cv2.COLOR_BGR2HSV).astype(np.float32)
        hsv[:, :, 1] = np.clip(hsv[:, :, 1] * (1.25 + vibrance_boost), 0, 255)
        hsv[:, :, 2] = np.clip(hsv[:, :, 2] * 1.05, 0, 255)
        return cv2.cvtColor(hsv.astype(np.uint8), cv2.COLOR_HSV2BGR)

    elif profile == DslrProfile.GOLDEN_HOUR:
        # Golden hour sunset warmth
        r = np.clip(r * 1.10 + 0.03, 0.0, 1.0)
        g = np.clip(g * 1.03 + 0.01, 0.0, 1.0)
        b = np.clip(b * 0.90, 0.0, 1.0)

    elif profile == DslrProfile.DSLR_PRIME:
        # Optical neutrality, rich contrast, slight vibrancy boost
        hsv = cv2.cvtColor((img_float * 255).astype(np.uint8), cv2.COLOR_BGR2HSV).astype(np.float32)
        hsv[:, :, 1] = np.clip(hsv[:, :, 1] * (1.12 + vibrance_boost), 0, 255)
        return cv2.cvtColor(hsv.astype(np.uint8), cv2.COLOR_HSV2BGR)

    merged = cv2.merge([b, g, r])
    return np.clip(merged * 255.0, 0, 255).astype(np.uint8)

def enhance_dslr_image(
    image_bytes: bytes,
    profile: str = DslrProfile.DSLR_PRIME,
    sharpness: float = 0.5,
    micro_contrast: float = 0.35,
    shadow_recovery: float = 0.25,
    vignette: float = 0.15,
    scale_factor: int = 1,
    output_format: str = "JPEG",
    jpeg_quality: int = 96
) -> Tuple[bytes, Dict[str, Any]]:
    """Complete DSLR enhancement pipeline."""
    # 1. Decode image
    nparr = np.frombuffer(image_bytes, np.uint8)
    img_bgr = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
    if img_bgr is None:
        raise ValueError("Could not decode image from byte buffer.")

    metrics_before = compute_image_metrics(img_bgr)

    # 2. Edge-preserving Denoising (Bilateral Filter)
    # Eliminates compression grain while preserving crisp edges
    denoised = cv2.bilateralFilter(img_bgr, d=5, sigmaColor=35, sigmaSpace=35)

    # 3. Dynamic Range & Micro-Contrast (CLAHE)
    clip_limit = 1.0 + (micro_contrast * 2.5)
    clahe_enhanced = apply_clahe_dynamic_range(denoised, clip_limit=clip_limit)

    # 4. Shadow recovery / Tone mapping
    if shadow_recovery > 0.05:
        # Gamma tone adjustment
        inv_gamma = 1.0 / (1.0 + shadow_recovery * 0.35)
        table = np.array([((i / 255.0) ** inv_gamma) * 255 for i in np.arange(0, 256)]).astype("uint8")
        clahe_enhanced = cv2.LUT(clahe_enhanced, table)

    # 5. Color Grading Profile
    graded = apply_dslr_color_grading(clahe_enhanced, profile=profile)

    # 6. Unsharp Masking (Optical Prime Sharpness)
    sharp_boost = apply_unsharp_mask(graded, amount=sharpness * 1.2, radius=1, threshold=2)

    # 7. Lens Vignette
    if vignette > 0.02 and profile != DslrProfile.NONE:
        final_img = apply_optical_vignette(sharp_boost, strength=vignette)
    else:
        final_img = sharp_boost

    # 8. High-Resolution Upscaling (if requested)
    if scale_factor in [2, 4]:
        new_w = final_img.shape[1] * scale_factor
        new_h = final_img.shape[0] * scale_factor
        final_img = cv2.resize(final_img, (new_w, new_h), interpolation=cv2.INTER_LANCZOS4)

    metrics_after = compute_image_metrics(final_img)

    # 9. Encode output
    if output_format.upper() == "PNG":
        success, encoded = cv2.imencode(".png", final_img, [cv2.IMWRITE_PNG_COMPRESSION, 4])
    else:
        success, encoded = cv2.imencode(".jpg", final_img, [cv2.IMWRITE_JPEG_QUALITY, jpeg_quality, cv2.IMWRITE_JPEG_OPTIMIZE, 1])

    if not success:
        raise RuntimeError("Failed to encode enhanced image.")

    stats = {
        "profile": profile,
        "width": int(final_img.shape[1]),
        "height": int(final_img.shape[0]),
        "scale_factor": scale_factor,
        "metrics_before": metrics_before,
        "metrics_after": metrics_after,
        "sharpness_improvement": f"+{round(max(0.0, metrics_after['sharpness_score'] - metrics_before['sharpness_score']), 1)} pts"
    }

    return encoded.tobytes(), stats
