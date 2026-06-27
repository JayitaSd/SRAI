# SRAI - Spring Retrieval AI 🧠

---

<div align="center">

[![Java Version](https://img.shields.io/badge/Java-21-FF6B35?style=for-the-badge&logo=java&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.0--M7-00D084?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-ai)
[![Maven](https://img.shields.io/badge/Maven-3.9+-6C63FF?style=for-the-badge&logo=apache-maven&logoColor=white)](https://maven.apache.org/)
[![Google Gemini](https://img.shields.io/badge/Google%20Gemini-API-4285F4?style=for-the-badge&logo=google&logoColor=white)](https://ai.google.dev/)
[![Apache PDFBox](https://img.shields.io/badge/PDFBox-3.0.5-E74C3C?style=for-the-badge&logo=apache&logoColor=white)](https://pdfbox.apache.org/)
[![Apache POI](https://img.shields.io/badge/Apache%20POI-5.4.1-E74C3C?style=for-the-badge&logo=apache&logoColor=white)](https://poi.apache.org/)
[![Tess4J](https://img.shields.io/badge/Tess4J-5.11.0-2ECC71?style=for-the-badge&logoColor=white)](https://tess4j.sourceforge.net/)

</div>

---

> 🎯 Upload any document and ask questions — get answers grounded in the provided content, not in random outside knowledge.

A modern **Retrieval-Augmented Generation (RAG)** application that demonstrates how to build document-grounded AI assistants. This project combines **Spring Boot**, **Spring AI**, and **Google Gemini** to create a system where users upload documents through a web UI, which are then extracted, embedded, and queried using only the provided context.

---

## 📚 Table of Contents

- [Key Features](#-key-features)
- [What It Does](#-what-it-does)
- [How It Works](#-how-it-works)
- [Tech Stack](#-tech-stack)
- [Requirements](#-requirements)
- [Project Layout](#-project-layout)
- [Getting Started](#-getting-started)
- [API Endpoints](#-api-endpoints)
- [Tips & Best Practices](#-tips--best-practices)
- [Troubleshooting](#-troubleshooting)

---

## ✨ Key Features

- **Multi-Format Document Support:** Upload `.txt`, `.pdf`, `.doc`, `.docx`, `.png`, `.jpg`, `.jpeg` files directly through the browser
- **Local Text Extraction:** PDF text extracted via Apache PDFBox, Word documents via Apache POI — no Gemini tokens spent on extraction
- **OCR for Images:** PNG/JPG/JPEG files processed locally with Tess4J (Tesseract) again, no Gemini tokens used
- **Session-Based Architecture:** Each upload creates an isolated vector store per session with a 30-minute idle TTL and automatic cleanup
- **HyDE (Hypothetical Document Embeddings):** Generates a hypothetical answer to improve retrieval accuracy, with intelligent fallback to standard retrieval
- **Advanced Chunking:** Token-based splitting with configurable chunk sizes (750 tokens, min 200 chars)
- **Document-Grounded Answers:** Gemini responds only from your uploaded document, never from external knowledge
- **Built-in Web UI:** Two-panel interface served directly from Spring Boot - no separate frontend server needed
- **No External Vector Databases:** Uses Spring AI's `SimpleVectorStore` entirely in memory per session
- **File Validation:** Size limit (10MB), extension whitelist, and content-type checks on every upload

---

## 🚀 What It Does

```
┌─ User uploads document via browser
│
├─ Backend extracts text (PDFBox / POI / Tess4J OCR — no Gemini tokens)
│
├─ Text is cleaned, chunked, embedded → stored in a session-scoped VectorStore
│
├─ User asks a question in the chat UI
│
├─ Generate Hypothetical Answer (HyDE)
│
├─ Embed hypothetical document → Search session VectorStore
│
├─ Retrieve most relevant chunks
│
├─ Send (Original Question + Retrieved Chunks) → Gemini
│
└─ Final grounded answer displayed in chat
```

The frontend at `http://localhost:8080` lets you:

1. **Upload** any supported document (drag & drop or browse)
2. **Process** it  extraction, chunking, and embedding happen server-side
3. **Chat** ask questions and get answers grounded strictly in your document
4. **Session expires** after 30 minutes of inactivity just re-upload to start fresh

---

## 🧠 How It Works

### Pipeline Overview

![RAG Pipeline](src/main/resources/images/Pipeline.png)

### 1️⃣ Document Upload & Text Extraction

When a file is uploaded to `POST /api/upload`, the backend routes it to the correct extractor:

| File Type | Extractor                                             | Token Cost |
|-----------|-------------------------------------------------------|------------|
| `.txt` | `DocExtractService` - plain UTF-8 read                | None |
| `.pdf` | `DocExtractService` - Apache PDFBox `PDFTextStripper` | None |
| `.doc` | `DocExtractService` - Apache POI `HWPFDocument`       | None |
| `.docx` | `DocExtractService` - Apache POI `XWPFDocument`       | None |
| `.png` / `.jpg` / `.jpeg` | `OcrService` - Tess4J (Tesseract OCR)                 | None |

### 2️⃣ Preprocessing, Chunking & Embedding

After extraction:

1. **Preprocess:**  Normalize whitespace and trim using `TextPreprocessor`
2. **Chunk:**  Split into token-based chunks using `TokenTextSplitter`:
    - Chunk size: 750 tokens
    - Minimum chunk size: 200 characters
    - Minimum length to embed: 20 characters
3. **Embed:** Generate embeddings for each chunk using Gemini's embedding model
4. **Store:**  Load into a `SimpleVectorStore` scoped to this session's UUID

A `sessionId` is returned to the frontend and sent with every subsequent chat request.

### 3️⃣ HyDE Chat Flow

When you send a message to `POST /api/chat`:

```
User Question
     ↓
Generate Hypothetical Answer (HyDE) using Gemini
     ↓
Embed hypotheticalDoc → Search Session VectorStore → Get relevant chunks
     ↓
Send (Original Question + Retrieved Chunks) → Gemini → Final Answer
     ↓
If HyDE response invalid → Fallback: search with original question directly
```

**Step-by-step:**

1. **HyDE Generation:** Gemini generates a detailed hypothetical answer to understand what the answer might look like
2. **Semantic Search:** The hypothetical document is embedded and used to find the most semantically similar chunks from the session's vector store
3. **Relevance Validation:** The HyDE response is checked for quality (not empty, doesn't contain "I don't know", length > 25 chars)
4. **Fallback Mechanism:** If HyDE fails validation, the system falls back to standard retrieval using the original question
5. **Context Injection:** Retrieved chunks are passed as context alongside the original question
6. **Final Answer:** Gemini generates the response using only the retrieved context

The system prompt ensures the model:
- Answers only from the provided context
- Avoids using external knowledge
- Admits when the documents don't contain enough information

### 4️⃣ Session Management

- Each upload creates a new `SessionData` containing a `VectorStore` and chat history
- Sessions are stored in a `ConcurrentHashMap` inside `SessionStore`
- A scheduled task runs every 10 minutes to evict sessions idle for over 30 minutes
- Uploading a new document creates a fresh session old one is abandoned

---

## 🛠️ Tech Stack

- **Java 21**
- **Spring Boot 4.0.6**
- **Spring AI 2.0.0-M7**
- **Spring Web MVC**
- **Google GenAI / Gemini**
    - Chat model: `gemini-2.5-flash`
    - Embedding model: `gemini-embedding-001`
- **Apache PDFBox 3.0.5:** local PDF text extraction
- **Apache POI 5.4.1:** local `.doc` / `.docx` text extraction
- **Tess4J 5.11.0:** local OCR for image files (wraps Tesseract)
- **SimpleVectorStore:** in-memory per-session vector store
- **JS + CSS:** frontend UI served from `resources/static`

---

## ✅ Requirements

To run the app, you'll need:

- **JDK 21**
- **Maven** or the included Maven Wrapper
- A valid **Google Gemini API key**
- A valid **Google GenAI Project ID**
- **Tesseract OCR** installed locally (required for image/OCR support)
    - Windows: download installer from [UB Mannheim](https://github.com/UB-Mannheim/tesseract/wiki)
    - macOS: `brew install tesseract`
    - Linux: `apt install tesseract-ocr`
- `eng.traineddata` present in your Tesseract `tessdata` folder (included with the installer)
- Internet access for Gemini API calls (chat + embedding only)

---

## 🔧 Configuration

The app requires two environment variables for Google Gemini API access, and one property for Tesseract:

### Environment Variables

**On Windows (PowerShell):**

```powershell
$env:GEMINI_API_KEY = "your-api-key-here"
$env:GOOGLE_GENAI_PROJECT_ID = "your-project-id-here"
```

**On Windows (Command Prompt):**

```cmd
set GEMINI_API_KEY=your-api-key-here
set GOOGLE_GENAI_PROJECT_ID=your-project-id-here
```

**On macOS/Linux:**

```bash
export GEMINI_API_KEY=your-api-key-here
export GOOGLE_GENAI_PROJECT_ID=your-project-id-here
```

### application.properties

```properties
# Tesseract OCR
tess4j.datapath=C:/Program Files/Tesseract-OCR/tessdata
tess4j.language=eng
```

Update `tess4j.datapath` to match your Tesseract installation path. Use forward slashes even on Windows.

---

## 📁 Project Layout

```
SRAI/                                                   # Root directory
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/rag/                        # Application source code
│   │   │       ├── RagApplication.java                 # Spring Boot entry point
│   │   │       │
│   │   │       ├── controller/
│   │   │       │   ├── ModelController.java            # POST /api/chat — HyDE RAG logic
│   │   │       │   └── UploadController.java           # POST /api/upload — file intake & embedding
│   │   │       │
│   │   │       ├── model/
│   │   │       │   ├── SessionData.java                # Per-session VectorStore + chat history
│   │   │       │   └── ChatMessage.java                # Message value object (role, content, timestamp)
│   │   │       │
│   │   │       ├── service/
│   │   │       │   ├── DocExtractService.java          # Text extraction — txt, pdf, doc, docx
│   │   │       │   ├── OcrService.java                 # OCR extraction — png, jpg, jpeg via Tess4J
│   │   │       │   └── SessionStore.java               # ConcurrentHashMap session registry + TTL eviction
│   │   │       │
│   │   │       ├── config/
│   │   │       │   └── SessionCleanUpScheduler.java    # Scheduled idle session eviction (every 10 min)
│   │   │       │
│   │   │       └── util/
│   │   │           ├── TextPreprocessor.java           # Whitespace normalization & text cleaning
│   │   │           └── FileValidationUtil.java         # Extension + content-type + size validation
│   │   │
│   │   └── resources/
│   │       ├── application.properties                  # Spring Boot, Gemini & Tesseract configuration
│   │       ├── static/                                 # Frontend served by Spring Boot
│   │       │   ├── index.html                          # Two-panel RAG chat UI
│   │       │   ├── index.css                           # Dark theme styles
│   │       │   └── index.js                            # Upload + chat fetch logic
│   │       └── images/
│   │           └── pipeline_updated.png                # RAG pipeline architecture diagram
│   │
│   └── test/
│       └── java/
│           └── com/example/rag/
│               └── RagApplicationTests.java            # Application tests
│
├── pom.xml                                             # Maven dependencies & build config
├── mvnw & mvnw.cmd                                     # Maven Wrapper scripts
├── README.md                                           # Project documentation
└── .gitignore                                          # Git ignore rules
```

### Key Java Classes

| Class | Responsibility |
|-------|----------------|
| `RagApplication.java` | Spring Boot bootstrap |
| `UploadController.java` | Handles file upload, routes to extractor, chunks, embeds, creates session |
| `ModelController.java` | Handles chat — HyDE retrieval, fallback, session lookup |
| `DocExtractService.java` | Extracts text from `.txt`, `.pdf`, `.doc`, `.docx` locally |
| `OcrService.java` | Extracts text from images via Tess4J OCR |
| `SessionStore.java` | Thread-safe session registry with idle TTL eviction |
| `SessionData.java` | Holds per-session `VectorStore` and chat history |
| `FileValidationUtil.java` | Validates file size, extension, and content type |
| `TextPreprocessor.java` | Normalizes whitespace and cleans extracted text |
| `SessionCleanUpScheduler.java` | Scheduled task — evicts idle sessions every 10 minutes |

### Key Directories

| Directory | Purpose                                  |
|-----------|------------------------------------------|
| `src/main/java/.../controller/` | REST endpoints upload and chat           |
| `src/main/java/.../service/` | Text extraction services and session store |
| `src/main/java/.../model/` | Session and message value objects        |
| `src/main/java/.../config/` | Scheduler configuration                  |
| `src/main/java/.../util/` | File validation and text preprocessing   |
| `src/main/resources/static/` | Frontend HTML/CSS/JS served by Spring Boot |

---

## ▶️ Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/JayitaSd/SRAI.git
cd SRAI
```

### 2. Install Tesseract OCR

**Windows** — Download and run the installer from [UB Mannheim](https://github.com/UB-Mannheim/tesseract/wiki). Note the install path.

**macOS:**
```bash
brew install tesseract
```

**Linux:**
```bash
apt install tesseract-ocr
```

### 3. Configure application.properties

Update `src/main/resources/application.properties` with your Tesseract path:

```properties
tess4j.datapath=C:/Program Files/Tesseract-OCR/tessdata
tess4j.language=eng
```

### 4. Set your API credentials

**Windows (PowerShell):**
```powershell
$env:GEMINI_API_KEY = "your-api-key-here"
$env:GOOGLE_GENAI_PROJECT_ID = "your-project-id-here"
```

**Windows (Command Prompt):**
```cmd
set GEMINI_API_KEY=your-api-key-here
set GOOGLE_GENAI_PROJECT_ID=your-project-id-here
```

**macOS/Linux:**
```bash
export GEMINI_API_KEY=your-api-key-here
export GOOGLE_GENAI_PROJECT_ID=your-project-id-here
```

### 5. Run the application

**Using Maven Wrapper (all platforms):**

```bash
./mvnw spring-boot:run
```

**On Windows using the batch script:**

```powershell
mvnw.cmd spring-boot:run
```

### 6. Open the UI

Navigate to `http://localhost:8080` in your browser. You should see the SRAI document chat interface.

```
2026-05-27 10:15:23.123  INFO 12345 --- [main] com.example.rag.RagApplication : Started RagApplication in 5.234 seconds
```

---

## 🔌 API Endpoints

All endpoints are served at `http://localhost:8080`.

---

### `POST /api/upload` — Upload & process a document

Accepts a multipart file, extracts text, chunks it, generates embeddings, and returns a session ID.

**Request:**
```http
POST /api/upload
Content-Type: multipart/form-data

file: <your file>
```

**Supported file types:** `.txt`, `.pdf`, `.doc`, `.docx`, `.png`, `.jpg`, `.jpeg`
**Max file size:** 10MB

**Success response `200`:**
```json
{
  "sessionId": "f3a7c821-91b4-4d3e-b3f1-abc123def456",
  "status": "ready",
  "chunkCount": 42
}
```

**Error response `400`:**
```json
{
  "error": "File exceeds 10MB limit."
}
```

**Example (cURL):**
```bash
curl -X POST http://localhost:8080/api/upload \
  -F "file=@/path/to/your/document.pdf"
```

---

### `POST /api/chat` — Ask a question about your document

Accepts a `sessionId` and a `message`, runs HyDE retrieval against the session's vector store, and returns a grounded answer.

**Request:**
```http
POST /api/chat
Content-Type: application/json

{
  "sessionId": "f3a7c821-91b4-4d3e-b3f1-abc123def456",
  "message": "What are the key points in this document?"
}
```

**Success response `200`:**
```
Based on the provided document, the key points are: ...
```

**Error response `400`:**
```
Session not found or expired. Please re-upload your document.
```

**Example (cURL):**
```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"f3a7c821-91b4-4d3e-b3f1-abc123def456","message":"Summarize this document."}'
```

---

## 💡 Tips & Best Practices

### Understanding HyDE (Hypothetical Document Embeddings)

HyDE is an advanced retrieval technique that improves accuracy:

1. **Why HyDE?** Instead of embedding your question directly, HyDE generates what an ideal answer might look like, then searches based on that. Answers are semantically closer to other answers than questions are, so retrieval is more accurate.

2. **Fallback Mechanism:**  If the HyDE-generated response contains phrases like "I don't have enough information" or is too short, the system automatically falls back to standard retrieval using your original question.

3. **Configuration:** You can adjust HyDE behavior in `ModelController.java`:
    - Modify `hydePrompt` to change how hypothetical answers are generated
    - Adjust `topK` (currently 10) to retrieve more or fewer chunks
    - Change `similarityThreshold` (currently 0.55) for stricter or looser matching

### Text Preprocessing

The `TextPreprocessor` class handles document cleaning:

1. **Whitespace Normalization:** Collapses multiple spaces into single spaces
2. **Trimming:**  Removes leading/trailing whitespace

Minimal preprocessing is intentional aggressive character stripping (e.g. removing punctuation) can hurt embedding quality for named entities, dates, and numeric values.

### Document Chunking Strategy

The `TokenTextSplitter` in `UploadController` uses these settings:

- **Chunk Size**: 750 tokens balances context window size with specificity
- **Min Chunk Size**: 200 characters ensures chunks have meaningful content
- **Min Length to Embed**: 20 characters filters out very small fragments
- **Keep Separator**: `true` preserves chunk boundaries for better context

### OCR Quality

Tess4J accuracy depends on image quality:
- Clean, high-resolution scans work excellently
- Low-resolution or handwritten content may produce noisy output
- For best results, use images with at least 150 DPI and good contrast
- The `Estimating resolution as X` warning in the console is normal for images without embedded DPI metadata OCR still runs correctly

### Session Behaviour

- Each uploaded document creates a new isolated session
- Uploading a new file abandons the previous session (it will be evicted after 30 minutes idle)
- If the chat shows "Session not found or expired", just re-upload your document
- Session TTL and cleanup interval can be tuned in `SessionStore.java` and `SessionCleanUpScheduler.java`

---

## Troubleshooting

### Issue: "API key is not valid"

**Solution:**
- Verify that `GEMINI_API_KEY` environment variable is set correctly
- Check that your Google Gemini API key is valid and has the right permissions
- Restart your terminal/IDE after setting the environment variable

### Issue: "Project ID is not valid" or "Invalid project-id"

**Solution:**
- Verify that `GOOGLE_GENAI_PROJECT_ID` environment variable is set correctly
- Ensure the Project ID matches your Google Cloud project
- Check that the project has Gemini API enabled

### Issue: OCR not working / `UnsatisfiedLinkError: Unable to load library 'tesseract'`

**Solution:**
- Ensure Tesseract is installed (not just `eng.traineddata` — the full installer is required)
- Verify `tess4j.datapath` in `application.properties` points to the correct `tessdata` folder
- On Windows, use forward slashes in the path: `C:/Program Files/Tesseract-OCR/tessdata`
- Restart the application after changing the path

### Issue: `Estimating resolution as 135` in console

This is a Tesseract info message, not an error. It means the image has no embedded DPI metadata so Tesseract estimated it. OCR runs normally. Safe to ignore.

### Issue: Session not found in chat after upload

**Solution:**
- Ensure the upload completed successfully and returned a `sessionId` (check browser Network tab)
- Sessions expire after 30 minutes of inactivity — re-upload the document to start a new session
- Avoid refreshing the page after upload as the in-memory `sessionId` stored in JS state will be lost

### Issue: "No extractable text found in document"

**Solution:**
- For PDFs: ensure the PDF contains selectable text (not a scanned image — use a JPG/PNG instead for those)
- For images: ensure Tesseract is installed and the image has legible text
- For `.doc`/`.docx`: ensure the file is not password-protected

### Issue: "Port 8080 is already in use"

```bash
java -Dserver.port=8081 -jar target/RAG-0.0.1-SNAPSHOT.jar
```

Or add `server.port=8081` to `application.properties`.

### Issue: JNA native access warning in console

```
WARNING: java.lang.System::load has been called by com.sun.jna.Native
```

This is a Java 17+ module system warning from Tess4J's native loading — not an error. To silence it, add this VM option in your run configuration:

```
--enable-native-access=ALL-UNNAMED
```

---

## 🎯 In Short

SRAI is a compact, real-world RAG application built on Spring Boot and Spring AI. Users upload any supported document through a browser UI — text is extracted locally (no token cost), embedded via Gemini, and stored in a per-session vector store. Questions are answered using HyDE retrieval grounded strictly in the uploaded document. Use it as a foundation for your own document-grounded AI assistants.