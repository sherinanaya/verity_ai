# VerityAI — AI-Powered Compliance Intelligence

<p align="center">
  <strong>Turn policy documents into reviewable compliance insights.</strong>
</p>

VerityAI is a web-based compliance intelligence prototype that analyzes organizational policy PDFs, extracts candidate obligations, assigns suggested risk levels, and provides a workspace for human review and remediation tracking.

**Live application:** https://verity-ai-sooty.vercel.app/  
**Backend health check:** https://verityai-production-883e23ad.up.railway.app/api/health

> **Important:** VerityAI is a decision-support prototype. Extracted obligations, risk levels, rationales, and recommendations are AI-assisted suggestions—not legal advice, a compliance determination, or a certification. A qualified human reviewer must validate every finding against the original policy and the organization's context.

## Features

- **PDF policy upload:** Upload text-based PDF policy documents (up to 10 MB).
- **Text extraction:** Extract selectable text and page information from uploaded PDFs using Apache PDFBox.
- **AI-assisted obligation extraction:** Optionally use the OpenAI API to identify candidate compliance obligations in a structured format.
- **Rule-based fallback:** Continue to analyze documents when an OpenAI API key is not configured or AI extraction cannot be used.
- **Risk assessment:** Assign suggested Critical, High, Medium, or Low risk levels to extracted obligations.
- **Evidence context:** Display available source-page references, excerpts, and rationale to help reviewers trace findings to the source document.
- **Assessment persistence:** Save assessments and extracted obligations in MySQL so the workspace can retrieve previously analyzed findings.
- **Obligation review workflow:** Search and filter extracted obligations, inspect findings, and update review statuses.
- **Remediation workspace:** View outstanding findings and mark items as remediated.
- **CSV export:** Export the currently visible obligation list for offline review.
- **Dashboard overview:** View obligation totals, high/critical risk counts, review status, and risk distribution.
- **Responsive interface:** React-based workspace with Overview, Obligations, and Remediation sections.

## Technology Stack

| Layer | Technology |
|---|---|
| Frontend | React 18, JavaScript, Vite 6, Lucide React |
| Backend | Java 17, Spring Boot 3.4.5, Spring Web, Spring Data JPA, Bean Validation |
| Database | MySQL 8+ |
| PDF processing | Apache PDFBox 3.0.4 |
| AI integration | OpenAI API (optional), with rule-based fallback |
| Frontend hosting | Vercel |
| Backend hosting | Railway (Docker deployment) |
| Database hosting | Railway MySQL service with persistent volume |
| Build tools | Maven 3.9+, Node.js 20+, npm |

## Application Architecture

```text
                   ┌─────────────────────────────┐
                   │       User / Browser        │
                   └──────────────┬──────────────┘
                                  │
                                  ▼
                   ┌─────────────────────────────┐
                   │   React + Vite Frontend     │
                   │          Vercel             │
                   └──────────────┬──────────────┘
                                  │ HTTPS / REST API
                                  ▼
                   ┌─────────────────────────────┐
                   │   Spring Boot REST API     │
                   │          Railway           │
                   └───────┬─────────────┬───────┘
                           │             │
                 PDF text  │             │ AI-assisted extraction
                 extraction│             ▼
                           │      ┌──────────────────┐
                           │      │    OpenAI API    │
                           │      │   (optional)     │
                           │      └──────────────────┘
                           ▼
                   ┌─────────────────────────────┐
                   │   Analysis & Risk Logic     │
                   │  Rule-based fallback       │
                   └──────────────┬──────────────┘
                                  │
                                  ▼
                   ┌─────────────────────────────┐
                   │       Railway MySQL        │
                   │ Assessments & Obligations  │
                   └─────────────────────────────┘
```

## Repository Structure

```text
verity_ai/
├── backend/
│   ├── src/main/java/com/verityai/
│   │   ├── analysis/       # PDF analysis, AI extraction and fallback logic
│   │   ├── assessment/     # Assessment creation, history and retrieval
│   │   ├── common/         # Shared types, errors and health endpoint
│   │   ├── config/         # CORS and application configuration
│   │   ├── document/       # PDF text extraction using PDFBox
│   │   └── obligation/     # Obligation persistence and review APIs
│   ├── src/test/           # Backend unit and controller tests
│   ├── Dockerfile
│   ├── .env.example
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── api/            # Backend API client
│   │   ├── components/     # Upload, UI and obligation components
│   │   ├── context/        # Workspace state
│   │   ├── utils/          # Formatting helpers
│   │   ├── App.jsx         # Main dashboard and workspace views
│   │   └── styles.css      # Application styling
│   ├── .env.example
│   ├── vercel.json
│   ├── package.json
│   └── vite.config.js
├── .gitignore
└── README.md
```

## Run Locally

### Prerequisites

- Java 17
- Maven 3.9 or later
- Node.js 20 or later and npm
- MySQL 8 or later
- OpenAI API key (optional)

### 1. Clone the repository

```bash
git clone https://github.com/sherinanaya/verity_ai.git
cd verity_ai
```

### 2. Create a MySQL database

Start your local MySQL server and run:

```sql
CREATE DATABASE IF NOT EXISTS verityai;
CREATE USER IF NOT EXISTS 'verityai'@'localhost' IDENTIFIED BY 'verityai_local';
GRANT ALL PRIVILEGES ON verityai.* TO 'verityai'@'localhost';
FLUSH PRIVILEGES;
```

Use your own local database username and password if they differ from these examples.

### 3. Configure and start the backend

Open a terminal:

```bash
cd backend

export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export DB_URL='jdbc:mysql://localhost:3306/verityai?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
export DB_USERNAME='verityai'
export DB_PASSWORD='verityai_local'
export FRONTEND_URL='http://localhost:5173'

# Optional: enable OpenAI-powered extraction
export OPENAI_API_KEY='YOUR_OPENAI_API_KEY'
export OPENAI_MODEL='gpt-4o-mini'

mvn spring-boot:run
```

If `OPENAI_API_KEY` is omitted, the backend can use its rule-based extraction fallback. Do not commit API keys or passwords.

The backend runs at `http://localhost:8080`.

Health endpoint: `http://localhost:8080/api/health`

### 4. Configure and start the frontend

Open a second terminal from the repository root:

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

Set the local API URL in `frontend/.env`:

```env
VITE_API_URL=http://localhost:8080/api
```

Open `http://localhost:5173` in your browser.

## Environment Variables

### Backend

| Variable | Purpose | Local example |
|---|---|---|
| `PORT` | Port used by the web server | `8080` |
| `DB_URL` | MySQL JDBC connection URL | `jdbc:mysql://localhost:3306/verityai?...` |
| `DB_USERNAME` | MySQL username | `verityai` |
| `DB_PASSWORD` | MySQL password | `verityai_local` |
| `OPENAI_API_KEY` | Optional key for AI-assisted extraction | Configure as a secret |
| `OPENAI_MODEL` | OpenAI model identifier | `gpt-4o-mini` |
| `FRONTEND_URL` | Allowed frontend origin for CORS | `http://localhost:5173` |

### Frontend

| Variable | Purpose | Production value |
|---|---|---|
| `VITE_API_URL` | Backend API base URL | `https://verityai-production-883e23ad.up.railway.app/api` |

Only public configuration belongs in frontend variables. Never expose `OPENAI_API_KEY`, database credentials, or other secrets through a `VITE_` variable.

## API Overview

All endpoints are served from the backend base URL.

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/api/health` | Check backend health |
| `POST` | `/api/analyze` | Analyze a PDF (`multipart/form-data`, field name `file`) |
| `GET` | `/api/assessments` | List saved assessments |
| `GET` | `/api/assessments/{id}` | Retrieve assessment details |
| `GET` | `/api/assessments/{id}/obligations` | List obligations for one assessment |
| `GET` | `/api/obligations` | List obligations; supports `assessmentId`, `risk`, `status`, and `q` filters |
| `GET` | `/api/obligations/{id}` | Retrieve one obligation |
| `PUT` | `/api/obligations/{id}/status` | Update review status |
| `DELETE` | `/api/obligations/{id}` | Delete one obligation |

Example status update body:

```json
{
  "status": "CONFIRMED"
}
```

Supported review statuses include `NEEDS_REVIEW`, `CONFIRMED`, `NOT_APPLICABLE`, and `REMEDIATED`.

## Deployment

The frontend and backend are deployed independently and communicate through the backend API URL.

### Backend and database — Railway

- **Backend service:** `verity_ai`
- **Database service:** Railway MySQL
- **Backend public URL:** `https://verityai-production-883e23ad.up.railway.app`
- **Health endpoint:** `https://verityai-production-883e23ad.up.railway.app/api/health`
- **Backend root directory:** `backend`
- **Build:** Dockerfile in the `backend` directory
- **Application port:** `8080` (the application also reads Railway's `PORT` variable)

Configure the backend service with these environment variables:

```env
DB_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=${{MySQL.MYSQLUSER}}
DB_PASSWORD=${{MySQL.MYSQLPASSWORD}}
FRONTEND_URL=https://verity-ai-sooty.vercel.app
OPENAI_API_KEY=<set only if using OpenAI extraction>
OPENAI_MODEL=gpt-4o-mini
```

The `${{MySQL...}}` expressions are Railway service-reference variables. If your Railway MySQL service has a different service name, select its corresponding reference variables in Railway rather than copying the example name literally. Keep credentials in Railway's variable settings; never add them to Git.

### Frontend — Vercel

- **Project:** `verity-ai`
- **Production URL:** https://verity-ai-sooty.vercel.app/
- **GitHub repository:** `sherinanaya/verity_ai`
- **Production branch:** `main`
- **Root directory:** `frontend`
- **Framework preset:** Vite
- **Install command:** `npm install`
- **Build command:** `npm run build`
- **Output directory:** `dist`

Configure this Vercel environment variable for Production (and Preview if required):

```env
VITE_API_URL=https://verityai-production-883e23ad.up.railway.app/api
```

After changing environment variables, redeploy the corresponding service so the new values take effect.

## Validation Performed

- Backend health endpoint returned a successful JSON response: `{"service":"VerityAI API","status":"ok"}`.
- Railway showed the backend deployment as Active after the database connection configuration was corrected.
- Vercel reported the frontend deployment as **Deployed to Production**.
- The deployed dashboard and its main workspace flows were manually tested in the live application by the project owner.

## Limitations and Responsible Use

- The current PDF pipeline extracts selectable text; scanned/image-only PDFs may require OCR and are not guaranteed to work.
- Extraction can miss obligations or produce incorrect interpretations. Always compare findings with the original document.
- Risk levels and recommendations are suggestions, not legal or regulatory determinations.
- The rule-based fallback is not a substitute for an LLM or a qualified compliance professional.
- Before handling sensitive organizational documents at scale, add authentication, authorization, tenant isolation, audit logging, rate limiting, retention/deletion policies, stronger operational monitoring, and a security review.

## License

No license has been specified. Add a `LICENSE` file before distributing the project as open-source software.
