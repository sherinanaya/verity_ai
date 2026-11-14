# VerityAI — AI-Powered Compliance Intelligence

VerityAI is a compliance-assessment prototype that analyzes organizational policy PDFs, extracts candidate obligations, suggests risk levels, and gives reviewers a workspace to validate findings and track remediation status.

> **Important:** VerityAI provides decision support only. Its findings and risk labels are suggestions, not legal advice, a compliance determination, or a certification. Every finding must be checked by a qualified human reviewer against the original policy and applicable requirements.

## Features

- **PDF policy ingestion:** Upload text-based PDF policies for text extraction and analysis.
- **LLM-assisted obligation extraction:** Uses the Groq Chat Completions API when `GROQ_API_KEY` is configured.
- **Rule-based fallback:** If Groq is not configured or the model response cannot be used, the application can fall back to rule-based candidate extraction.
- **Risk classification:** Candidate findings receive suggested risk levels (Critical, High, Medium, or Low).
- **Evidence and source references:** Findings include source-page and excerpt information where available, so reviewers can verify them against the document.
- **Assessment history:** Store and revisit document assessments and their extracted obligations.
- **Review workflow:** Search and filter obligations, inspect details, and update review status.
- **Remediation workspace:** Review obligations by their status and follow up on items that need attention.
- **CSV export:** Export obligation information from the workspace.

## Technology stack

| Layer | Technology |
|---|---|
| Frontend | React 18, Vite 6, JavaScript, Lucide React |
| Backend | Java 17, Spring Boot 3.4.5, Spring Web, Spring Data JPA |
| Database | MySQL 8+ |
| PDF processing | Apache PDFBox 3.x |
| LLM integration | Groq API using the OpenAI-compatible Chat Completions endpoint |
| Build tools | Maven 3.9+, npm |
| Frontend hosting | Vercel |
| Backend hosting | Render (Docker deployment) |

## Repository structure

```text
verity_ai/
├── backend/
│   ├── src/main/java/com/verityai/
│   │   ├── analysis/       # PDF analysis, Groq integration and fallback extraction
│   │   ├── assessment/     # Assessment creation, history and retrieval
│   │   ├── common/         # Shared types, errors and health endpoint
│   │   ├── config/         # CORS configuration
│   │   ├── document/       # PDF text extraction
│   │   └── obligation/     # Obligation persistence and review status APIs
│   ├── src/main/resources/application.properties
│   ├── src/test/           # Backend tests
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── api/            # Backend API client
│   │   ├── components/     # UI components
│   │   ├── context/        # Workspace state
│   │   └── utils/          # Formatting helpers
│   ├── .env.example
│   ├── vercel.json
│   └── package.json
├── .gitignore
└── README.md
```

## Prerequisites

- Java 17
- Maven 3.9 or later
- Node.js 20 or later and npm
- MySQL 8 or later
- A Groq API key for LLM-assisted extraction (optional; rule-based fallback is available)

## Run locally

Run the backend and frontend in separate terminal windows.

### 1. Create the database

Start your MySQL server, then create the database:

```sql
CREATE DATABASE IF NOT EXISTS verityai;
```

Create a dedicated local database user if needed:

```sql
CREATE USER IF NOT EXISTS 'verityai'@'localhost' IDENTIFIED BY 'verityai_local';
GRANT ALL PRIVILEGES ON verityai.* TO 'verityai'@'localhost';
FLUSH PRIVILEGES;
```

### 2. Configure and start the backend

```bash
cd backend

export DB_URL='jdbc:mysql://localhost:3306/verityai?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
export DB_USERNAME='verityai'
export DB_PASSWORD='verityai_local'
export FRONTEND_URL='http://localhost:5173'
export GROQ_API_KEY='YOUR_GROQ_API_KEY'
export GROQ_MODEL='openai/gpt-oss-20b'

mvn spring-boot:run
```

Replace `YOUR_GROQ_API_KEY` with your own Groq API key. Do not commit the key or paste it into source code. If you omit `GROQ_API_KEY`, the application uses its rule-based fallback when available.

The backend starts on `http://localhost:8080`.

Health endpoint: `http://localhost:8080/api/health`

### 3. Configure and start the frontend

Open a second terminal:

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

The local frontend API URL in `frontend/.env.example` is:

```env
VITE_API_URL=http://localhost:8080/api
```

Open `http://localhost:5173` in your browser.

## Environment variables

### Backend

| Variable | Purpose | Example / default |
|---|---|---|
| `PORT` | HTTP port | `8080` |
| `DB_URL` | MySQL JDBC connection URL | `jdbc:mysql://localhost:3306/verityai?...` |
| `DB_USERNAME` | MySQL username | `verityai` |
| `DB_PASSWORD` | MySQL password | Set locally; do not commit |
| `GROQ_API_KEY` | Groq API key for LLM extraction | Secret; configure in environment |
| `GROQ_MODEL` | Groq model identifier | `openai/gpt-oss-20b` |
| `FRONTEND_URL` | Allowed frontend origin for CORS | `http://localhost:5173` |

### Frontend

| Variable | Purpose | Local value |
|---|---|---|
| `VITE_API_URL` | Backend API base URL | `http://localhost:8080/api` |

Only public configuration should use the `VITE_` prefix. **Never put the Groq API key in a frontend environment variable**; browser-exposed variables are not secret.

## API overview

The backend exposes the following endpoints:

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/api/health` | Health check |
| `POST` | `/api/analyze` | Upload and analyze a PDF (`multipart/form-data`, field name `file`) |
| `GET` | `/api/assessments` | List assessments |
| `GET` | `/api/assessments/{id}` | Retrieve assessment details |
| `GET` | `/api/assessments/{id}/obligations` | List obligations for an assessment |
| `GET` | `/api/obligations` | List obligations; supports optional filters such as `assessmentId`, `risk`, `status`, and `q` |
| `GET` | `/api/obligations/{id}` | Retrieve one obligation |
| `PUT` | `/api/obligations/{id}/status` | Update an obligation's review status |
| `DELETE` | `/api/obligations/{id}` | Delete an obligation |

## Deployment

The frontend and backend are deployed separately. The frontend calls the backend using `VITE_API_URL`.

### Backend — Render

1. Push the repository to GitHub.
2. In Render, create a new **Web Service** and connect the GitHub repository.
3. Set the **Root Directory** to `backend`.
4. Select **Docker** as the runtime and use `Dockerfile` in the backend root.
5. Configure the backend environment variables: `PORT` (if required by the platform), `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `GROQ_API_KEY`, `GROQ_MODEL`, and `FRONTEND_URL`.
6. Use a hosted MySQL-compatible database reachable by the Render service. Put its connection details in `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`; do not use `localhost` for a separately hosted database.
7. Deploy and verify the service at `/api/health`.

### Frontend — Vercel

1. Import the GitHub repository into Vercel.
2. Set the **Root Directory** to `frontend`.
3. Select **Vite** as the framework preset.
4. Set the build command to `npm run build` and output directory to `dist` (Vercel usually detects these automatically).
5. Add the environment variable `VITE_API_URL` with the deployed backend URL, for example `https://YOUR-BACKEND.onrender.com/api`.
6. Deploy the frontend.
7. Set the backend's `FRONTEND_URL` to the deployed Vercel origin (for example, `https://YOUR-PROJECT.vercel.app`) and redeploy the backend if you changed it.

## Security notes

- Never commit `.env` files, API keys, database passwords, or access tokens.
- Keep `GROQ_API_KEY` on the backend only. Do not use a `VITE_GROQ_API_KEY` variable.
- Use `.env.example` files only for non-secret placeholders and documentation.
- Use a dedicated database account with only the permissions the application needs.
- Configure production CORS to allow only the deployed frontend origin.

## Current limitations

- PDF text extraction is designed for text-based PDFs. Scanned/image-only PDFs require OCR, which is not described as part of the current implementation.
- AI-generated findings can be incomplete or incorrect; verify each finding and source excerpt manually.
- The rule-based extractor is a fallback and is not an LLM.
- The prototype should be hardened before production use with authentication, authorization, tenant isolation, audit logging, rate limiting, file-retention controls, operational monitoring, and security review.

## License

No license has been specified yet. Add a `LICENSE` file before treating this repository as open source.
