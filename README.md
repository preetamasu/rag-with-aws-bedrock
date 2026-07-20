# AWS Bedrock RAG

A Retrieval-Augmented Generation (RAG) API built with Spring Boot and Spring AI, using AWS Bedrock for embeddings and multi-model chat generation, with PostgreSQL + pgvector for vector storage.

Upload PDFs → they get chunked, embedded via Cohere on Bedrock, and stored in a vector database. Ask a question → the app retrieves the most relevant chunks and generates an answer using a Bedrock chat model of your choice (Claude, OpenAI's OSS models, Gemma, or DeepSeek — all routed through Bedrock's unified Converse API).

## Architecture

```
PDF documents
      │
      ▼
Tika reader → TokenTextSplitter (chunking)
      │
      ▼
Cohere Embed Multilingual v3 (via Bedrock)
      │
      ▼
PostgreSQL + pgvector (vector storage)

────────────────────────────────────────

User question
      │
      ▼
Similarity search (pgvector)
      │
      ▼
Retrieved context + question
      │
      ▼
Bedrock Converse API (Claude / OpenAI / Gemma / DeepSeek)
      │
      ▼
Answer
```

## Tech stack

- **Java 21**, **Spring Boot 4**, **Spring AI 2.0**
- **AWS Bedrock** — Cohere Embed Multilingual v3 for embeddings, Bedrock Converse API for chat generation (model-agnostic across providers)
- **PostgreSQL 16 + pgvector** — vector similarity search (cosine distance, HNSW index)
- **Docker Compose** — one-command local orchestration
- **Apache Tika** — PDF text extraction

## Supported chat models

Selectable per-request via the `model` query parameter, all served through Bedrock's Converse API:

| Key | Model |
|---|---|
| `claude` | Anthropic Claude Haiku 4.5 |
| `openai` | OpenAI GPT-OSS 120B |
| `google-gemma` | Google Gemma 3 27B |
| `deepseek` | DeepSeek V3.2 |

## Setup

### Prerequisites
- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- An AWS account with **Bedrock model access enabled** for the models above (AWS Console → Bedrock → Model access)

### Run it

1. Clone the repo:
   ```bash
   git clone <your-repo-url>
   cd aws-bedrock-rag
   ```

2. .env example
   AWS_ACCESS_KEY_ID=your_key
   AWS_SECRET_ACCESS_KEY=your_secret
   ```

3. Start everything:
   ```bash
   docker compose up --build
   ```

   This builds the Spring Boot app, starts a Postgres + pgvector container, waits for it to be healthy, then boots the app — which automatically ingests every PDF in `src/main/resources/pdf/` into the vector store on startup.

4. The API is live at `http://localhost:8080`.

> **Note:** running this app makes billed calls to AWS Bedrock (embeddings on ingestion, chat completions per question). Costs are incurred on whichever AWS account's credentials are supplied.

## API

### Ask a question

```http
POST /api/chat?model=claude
Content-Type: text/plain

What was mentioned about Fed rate cuts?
```

Returns a plain-text answer grounded in the ingested documents.

### Swagger / OpenAPI

Interactive API docs are available at `http://localhost:8080/swagger-ui.html` once the app is running.

## Adding your own documents

Drop additional PDFs into `src/main/resources/pdf/` before building — they'll be automatically discovered, chunked, embedded, and ingested on the next `docker compose up --build`.

## Local development (without Docker)

If you'd rather run the app directly (e.g. from your IDE) instead of the full Docker stack:

1. Start just the database: `docker compose up db`
2. Ensure `application.properties` points at `localhost:5332` (or whichever host port you've mapped)

## Notable implementation details

- **Model-agnostic chat routing** — a single `ChatModel` bean (Bedrock Converse) serves all four chat providers; the model is selected per-request via `ChatOptions`, not hardcoded per-provider beans.
- **Chunk sizing tuned to Cohere's 512-token embedding limit** — `TokenTextSplitter` is configured well under that ceiling so no chunk silently loses content to truncation during embedding.
- **Multi-stage Docker build** — the final image contains only a JRE + the compiled jar, not the full JDK/Maven toolchain used to build it.
