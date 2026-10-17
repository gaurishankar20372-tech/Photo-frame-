# 🚀 PHOTO FRAME Backend - Render Deployment Guide (रेंडर पर डिप्लॉय करने की संपूर्ण गाइड)

This backend powers high-performance DSLR-grade photo enhancement, optical lens simulation (CLAHE, unsharp masking, bilateral denoising, cinematic LUTs), millisecond video frame extraction, and AI climax/incident detection for **PHOTO FRAME by MG Shankar Tech**.

---

## 🌟 Quick Overview (सरल परिचय)
Render (https://render.com) पर यह बैकएंड 100% फ्री में डिप्लॉय हो सकता है। आप इसे 2 तरीकों से डिप्लॉय कर सकते हैं:
1. **Method 1: 1-Click Blueprint Deploy (`render.yaml`) - सबसे आसान तरीका**
2. **Method 2: Manual Web Service Setup (मैन्युअल सेटअप)**

---

## 🛠️ Method 1: 1-Click Blueprint Deploy (रेंडर ब्लूप्रिंट - 1 क्लिक में)

1. अपने प्रोजेक्ट को GitHub पर पुश (Push) करें।
2. [Render Dashboard](https://dashboard.render.com/) में लॉगिन करें।
3. ऊपर दाईं ओर **"New +"** बटन पर क्लिक करें और **"Blueprint"** चुनें।
4. अपना GitHub रिपॉजिटरी सेलेक्ट करें।
5. Render स्वतः ही `render.yaml` फाइल को पहचान लेगा और सभी सेटिंग्स (Python 3.11, Build Command, Start Command) कॉन्फ़िगर कर देगा।
6. **"Apply"** पर क्लिक करें। 
7. 2-3 मिनट में आपका बैकएंड लाइव हो जाएगा और आपको एक URL मिलेगा, जैसे:
   `https://photoframe-backend-xxxx.onrender.com`

---

## 🔧 Method 2: Manual Web Service Setup (मैन्युअल सेटअप)

यदि आप ब्लूप्रिंट का उपयोग नहीं करना चाहते हैं, तो निम्न स्टेप्स फॉलो करें:

1. [Render Dashboard](https://dashboard.render.com/) खोलें और **"New +" -> "Web Service"** चुनें।
2. अपना GitHub रिपॉजिटरी चुनें।
3. सेटिंग्स इस प्रकार भरें:
   - **Name**: `photoframe-backend`
   - **Region**: Oregon (US West) या Singapore (Asia)
   - **Branch**: `main` (या आपकी एक्टिव ब्रांच)
   - **Root Directory**: `backend` (या खाली छोड़ें)
   - **Runtime**: `Python 3`
   - **Build Command**: 
     - अगर Root Directory `backend` है: `pip install -r requirements.txt`
     - अगर Root Directory खाली है: `pip install -r backend/requirements.txt`
   - **Start Command**: 
     - अगर Root Directory `backend` है: `uvicorn main:app --host 0.0.0.0 --port $PORT`
     - अगर Root Directory खाली है: `uvicorn backend.main:app --host 0.0.0.0 --port $PORT`
   - **Instance Type**: `Free`
4. **Environment Variables** जोड़ें:
   - `PYTHON_VERSION`: `3.11.2`
   - `GEMINI_API_KEY`: *(वैकल्पिक - यदि AI स्थिति विश्लेषण के लिए Gemini का उपयोग करना चाहते हैं)*
5. **"Create Web Service"** पर क्लिक करें!

---

## 📱 Android App में Render Backend कैसे जोड़ें?

1. जब Render पर बैकएंड डिप्लॉय हो जाए, तो अपना URL कॉपी करें:
   उदाहरण: `https://photoframe-backend.onrender.com`
2. **PHOTO FRAME** एंड्रॉइड ऐप खोलें।
3. सेटिंग्स (**Settings**) में जाएं।
4. **"Render Cloud Backend"** कार्ड देखें।
5. अपना Render URL पेस्ट करें।
6. **"Test Connection"** पर क्लिक करें। आपको 🟢 "Connected (OK)" और लेटेंसी (ms) दिखाई देगी।
7. **"Use Render Cloud for DSLR Enhancement"** चालू करें।
8. अब आपकी सभी वीडियो फ्रेम्स रेंडर क्लाउड सर्वर द्वारा अल्ट्रा-हाई क्वालिटी डीएसएलआर में एनहांस होंगी!

---

## 🧪 Available API Endpoints (उपलब्ध एंडपॉइंट्स)

- `GET /` : सर्वर स्थिति और जानकारी
- `GET /api/health` : हेल्थ चेक (Render Health Check)
- `GET /api/profiles` : उपलब्ध डीएसएलआर प्रोफाइल्स (DSLR Prime, Leica B&W, Studio Vivid, Cinematic Teal & Orange, Portrait Glow, Golden Hour)
- `POST /api/enhance/dslr` : इमेज फाइल अपलोड करके डीएसएलआर एनहांसमेंट
- `POST /api/enhance/dslr/json` : बेस64 इमेज एनहांसमेंट मोबाइल ऐप के लिए
- `POST /api/extract/frames` : वीडियो से मिलीसेकंड फ्रेम एक्सट्रैक्शन
- `POST /api/ai/situation-finder` : एआई सिचुएशन/क्लाइमेक्स मोमेंट फाइंडर
- `GET /docs` : इंटरएक्टिव स्वैगर एपीआई डॉक्स
