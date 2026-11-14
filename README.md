# VerityAI — AI-assisted Compliance Intelligence

VerityAI is a local-first prototype that extracts candidate compliance obligations from policy PDFs, suggests risk levels, and provides a human review workflow. It is an assistive tool, not legal advice or a compliance certification system. Every finding must be reviewed by a qualified person.

## Project structure

- `frontend/`: React 18 + Vite single-page application.
- `backend/`: Java 17+ + Spring Boot 3.4.5 REST API.
- MySQL: stores assessments, extracted obligations, and review statuses.
- Apache PDFBox: extracts selectable text from PDFs.
- OpenAI Chat Completions API: optional structured extraction. If `OPENAI_API_KEY` is blank or the AI request fails, the API uses its rule-based fallback.

## Prerequisites (macOS)

- Node.js 20+ and npm.
- Java 17 or newer. Java 22 is also suitable for this project.
- Maven 3.9+.
- MySQL Server 8+ running locally.

Check your tools:

```bash
node -v
npm -v
java -version
mvn -version
mysql --version
```

If Maven is missing and you use Homebrew:

```bash
brew install maven
```

If Homebrew is not installed, install it from https://brew.sh, reopen Terminal, then run the command above. If MySQL Server is not installed, install and start it with:

```bash
brew install mysql
brew services start mysql
```

The `mysql` command-line client alone is not enough; a MySQL server must be running and accepting connections.

## Run locally

Open **three Terminal tabs/windows**: one for MySQL (if it is not already running), one for the backend, and one for the frontend.

### 1. Create the database and a local database user

Start MySQL first. Then run:

```bash
mysql -u root -p
```

Enter your MySQL root password. At the MySQL prompt, run:

```sql
CREATE DATABASE IF NOT EXISTS verityai;
CREATE USER IF NOT EXISTS 'verityai'@'localhost' IDENTIFIED BY 'verityai_local';
GRANT ALL PRIVILEGES ON verityai.* TO 'verityai'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

If your local MySQL root account does not use a password, use `mysql -u root` instead of `mysql -u root -p`.

### 2. Start the Spring Boot backend

From the project root:

```bash
cd backend
export DB_URL='jdbc:mysql://localhost:3306/verityai?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
export DB_USERNAME='verityai'
export DB_PASSWORD='verityai_local'
export FRONTEND_URL='http://localhost:5173'
```

Optional: enable OpenAI extraction. Leave this unset to use the built-in rule-based extractor:

```bash
export OPENAI_API_KEY='your_openai_api_key'
export OPENAI_MODEL='gpt-4o-mini'
```

Start the API:

```bash
mvn spring-boot:run
```

Wait until Spring Boot reports that it has started. Verify in a browser:

- Health endpoint: http://localhost:8080/api/health
- Obligations endpoint: http://localhost:8080/api/obligations

The obligations endpoint should initially return `[]` if the database is empty.

### 3. Start the React frontend

Open a **new Terminal tab/window**. From the project root:

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

Open the local Vite URL printed by the terminal, normally:

http://localhost:5173

Keep both backend and frontend terminal processes running while using the application.

## Main workflow

1. Open the VerityAI frontend.
2. Upload a text-based PDF (maximum 10 MB).
3. Select **Analyze document**.
4. Review the candidate obligations, risk suggestions, source pages, and evidence excerpts.
5. Change each finding's review status to `NEEDS_REVIEW`, `CONFIRMED`, `NOT_APPLICABLE`, or `REMEDIATED`.
6. Use the Remediation view to review suggested corrective actions.

Scanned/image-only PDFs are not supported because OCR has not been added. The rule-based fallback is a demo extractor, not a substitute for an AI model or a compliance expert.

## API endpoints

- `GET /api/health` — API health check.
- `POST /api/analyze` — multipart form upload; field name is `file`.
- `GET /api/obligations` — list findings; optional query parameters: `assessmentId`, `risk`, `status`, `q`.
- `GET /api/obligations/{id}` — get one finding.
- `PUT /api/obligations/{id}/status` — update review status. JSON body: `{ "status": "CONFIRMED" }`.
- `DELETE /api/obligations/{id}` — delete one finding.
- `GET /api/assessments` — list uploaded assessments.
- `GET /api/assessments/{id}` — assessment and its obligations.
- `GET /api/assessments/{id}/obligations` — obligations for one assessment.

## Configuration

Backend environment variables:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `OPENAI_API_KEY` (optional), `OPENAI_MODEL` (optional; default `gpt-4o-mini`)
- `FRONTEND_URL` (default `http://localhost:5173`)
- `PORT` (default `8080`)

Frontend environment variable:

- `VITE_API_URL` (default `http://localhost:8080/api`)

Do not commit `.env` files, API keys, or database passwords.

## Deployment notes

Keep `frontend/` and `backend/` as separate deploy roots. The frontend can be deployed to Vercel (root `frontend`, build `npm run build`, output `dist`). The backend Dockerfile is in `backend/`; deploy it as a Docker web service and provide the environment variables above. Use a managed external MySQL-compatible database; do not configure a hosted service to connect to `localhost`.

## Known prototype limitations

- No authentication, tenant isolation, or role-based access control.
- No OCR for scanned PDFs.
- Remediation is currently a recommendation/status view, not a separate task-management entity with owners and due dates.
- No audit trail, rate limiting, or production-grade file retention controls.
- `spring.jpa.hibernate.ddl-auto=update` is convenient for local prototyping; use versioned database migrations before production.
