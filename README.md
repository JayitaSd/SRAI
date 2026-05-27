# 🏀 NBA RAG Assistant

[![Java Version](https://img.shields.io/badge/Java-21-blue?logo=java)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen?logo=spring-boot)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.9+-orange?logo=apache-maven)](https://maven.apache.org/)
[![Google Gemini](https://img.shields.io/badge/Google%20Gemini-API-yellow?logo=google)](https://ai.google.dev/)

> 🎯 Ask questions about NBA content and get answers grounded in the provided document — not in random outside knowledge.

A modern **Retrieval-Augmented Generation (RAG)** application that demonstrates how to build document-grounded AI assistants. This project combines **Spring Boot**, **Spring AI**, and **Google Gemini** to create a system that retrieves relevant document chunks and answers questions using only the provided context.

---

## 📚 Table of Contents

- [Key Features](#key-features)
- [How It Works](#-how-it-works)
- [Tech Stack](#-tech-stack)
- [Getting Started](#-getting-started)
- [API Reference](#api-reference)
- [Project Structure](#-project-layout)
- [Troubleshooting](#troubleshooting)
- [License](#license)

---

## ✨ Key Features

- **Document-Grounded Answers** — Gemini responds only from your provided documents, not from general knowledge
- **Local Vector Store** — Embeddings are cached locally in `vectorstore.json`, so no recomputation on every startup
- **Production-Ready Code** — Clean Spring Boot config, dependency injection, and best practices
- **Easy to Extend** — Simple to add more documents or swap embeddings/chat models
- **No External Dependencies** — Uses only Spring AI and Google Gemini APIs

## 🚀 What it does

```
┌─ User asks question
│
├─ App embeds the question
│
├─ Vector store retrieves similar chunks
│
├─ Chunks sent as context to Gemini
│
└─ Gemini responds using only the context
```

The endpoint at `GET /rag/models` accepts a message parameter and:

1. translates your question into a vector embedding
2. searches the vector store for matching document chunks
3. passes those chunks as context to Gemini
4. returns Gemini's grounded response

## 🧠 How it works

### 1️⃣ Document ingestion on startup

When the app starts, `RagConfig` checks whether a persisted vector store exists:

| Scenario | Action |
|----------|--------|
| Vector store exists | Load it immediately from `vectorstore.json` |
| Vector store missing | Read `nba.txt` → tokenize → embed → persist |

If the vector store is being built for the first time:

1. Read the NBA document from `src/main/resources/data/nba.txt`
2. Split it into smaller, overlapping chunks using token-based splitting
3. Generate embeddings for each chunk using Gemini's embedding model
4. Store the embeddings locally in `vectorstore.json`

### 2️⃣ Question answering flow

When you call `GET /rag/models?message=your_question`:

1. **Embed** — Your question is converted to a vector
2. **Retrieve** — The vector store finds the k most similar document chunks
3. **Augment** — Those chunks are injected into the prompt as context
4. **Generate** — Gemini reads the context and answers your question

The system prompt ensures the model:
- answers only from the provided context
- avoids using external knowledge
- admits when the documents don't contain enough information

## 🛠️ Tech stack

- **Java 21**
- **Spring Boot 4.0.6**
- **Spring AI 2.0.0-M7**
- **Spring Web MVC**
- **Google GenAI / Gemini**
  - chat model: `gemini-2.5-flash`
  - embedding model: `gemini-embedding-001`
- **SimpleVectorStore** for local persistence

## 📁 Project layout

- `src/main/java/com/example/rag/RagApplication.java` — application entry point
- `src/main/java/com/example/rag/RagConfig.java` — document loading and vector store setup
- `src/main/java/com/example/rag/ModelController.java` — REST endpoint for questions
- `src/main/resources/data/nba.txt` — source document used for retrieval
- `src/main/resources/data/vectorstore.json` — saved vector store data

## ✅ Requirements

To run the app, you'll need:

- **JDK 21**
- **Maven** or the included Maven Wrapper
- a valid **Google Gemini API key**
- internet access for Gemini API calls

## 🔧 Configuration

The app expects the Gemini API key in the environment variable below:

```bash
GEMINI_API_KEY
```

This is used in `src/main/resources/application.properties` for both chat and embedding requests.

## ▶️ Getting Started

### 1. Clone or extract the project

```bash
cd C:\JAYITA\PROJECTS\RAG
```

### 2. Set your API key

**On Windows (PowerShell):**

```powershell
$env:GEMINI_API_KEY = "your-api-key-here"
```

**On Windows (Command Prompt):**

```cmd
set GEMINI_API_KEY=your-api-key-here
```

**On macOS/Linux:**

```bash
export GEMINI_API_KEY=your-api-key-here
```

### 3. Run the application

**Using Maven Wrapper (all platforms):**

```bash
./mvnw spring-boot:run
```

**Or on Windows using the batch script:**

```powershell
mvnw.cmd spring-boot:run
```

### 4. Verify it's running

You should see output like:

```
2026-05-27 10:15:23.123  INFO 12345 --- [main] com.example.rag.RagApplication          : Started RagApplication in 5.234 seconds
```

The app will be available at `http://localhost:8080`

---

## API Reference

### Endpoint: Ask a question

**Request:**
```http
GET /rag/models?message=your+question+here
```

**Parameters:**

| Name | Type | Required | Description |
|------|------|----------|-------------|
| message | string | Yes | Your question about the NBA content |

**Example:**
```bash
curl "http://localhost:8080/rag/models?message=Who%20are%20the%20NBA%20champions%20mentioned%20in%20the%20document%3F"
```

**Response:**
```
A text response grounded in the NBA document, or a message stating that the documents do not contain enough information.
```

**Example response:**
```
The document mentions [NBA champions' information]. Based on the provided context...
```

---

## 💡 Tips & Best Practices

- **Vector Store Caching** — The vector store is persisted locally, so the app loads embeddings from disk on subsequent startups. This is much faster than regenerating them.
- **Updating the Document** — If you modify `nba.txt`, you must delete the `vectorstore.json` file so the app regenerates embeddings from the new content on the next startup.
- **Extending the Project** — You can easily add more documents by modifying `RagConfig.java` to load multiple text files and combine them in the vector store.
- **Model Tuning** — Adjust the Gemini model or the max output tokens in `application.properties` to customize the response behavior.

## Troubleshooting

### Issue: "API key is not valid"

**Solution:**
- Verify that `GEMINI_API_KEY` environment variable is set correctly
- Check that your Google Gemini API key is valid and has the right permissions
- Restart your terminal/IDE after setting the environment variable

### Issue: "Vector store file not found" or embeddings aren't loading

**Solution:**
- Ensure `nba.txt` exists in `src/main/resources/data/`
- Delete `vectorstore.json` to force regeneration
- Check that the app has write permissions to the `src/main/resources/data/` directory

### Issue: Slow first startup

**Cause:** The first run generates embeddings for all document chunks, which takes time depending on document size and API latency.

**Solution:** This is normal. Subsequent runs will be much faster because embeddings are cached.

### Issue: "Port 8080 is already in use"

**Solution:**
```bash
# Change the port in application.properties or via environment variable:
java -Dserver.port=8081 -jar target/RAG-0.0.1-SNAPSHOT.jar
```

---

## 🎯 In short

This project is a compact, real-world example of how to build a document-grounded AI assistant with Spring Boot, Spring AI, and Gemini.

## License

No license has been specified yet.
