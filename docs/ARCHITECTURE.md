# OralDiagnosisAI — System Architecture & Engineering Guide

## 1. High-Level Architecture Overview

OralDiagnosisAI is an enterprise-grade medical diagnostics platform engineered for the automated classification of **Oral Squamous Cell Carcinoma (OSCC)** and **Normal** oral tissue using microscopic cytology images.

```
+-------------------------------------------------------------------------------+
|                               Doctor / Clinician                              |
+---------------------------------------+---------------------------------------+
                                        |
                 +----------------------+----------------------+
                 |                                             |
                 v                                             v
       +--------------------+                        +--------------------+
       |  Responsive Web    |                        |  Native Android    |
       |  Application (UI)  |                        |  Mobile App (Java) |
       +---------+----------+                        +---------+----------+
                 |                                             |
                 | REST API / Sessions                         | Direct Cloud /
                 |                                             | On-Device Edge
                 v                                             v
       +--------------------+                        +--------------------+
       |   Flask Backend    |                        | TFLite Inference   |
       |  (Python 3.12)     |                        | Manager (On-Device)|
       +---------+----------+                        +---------+----------+
                 |
      +----------+----------+
      |                     |
      v                     v
+------------+       +-------------+
|  Firebase  |       | SQLite DB / |
| Firestore  |       |  Fallback   |
+------------+       +-------------+
```

---

## 2. Deep Learning Model Contract

The core AI engine relies on a lightweight TensorFlow Lite flatbuffer (`oral_model.tflite`).

| Parameter | Specification |
|---|---|
| **Input Shape** | `(1, 224, 224, 3)` (Batch: 1, Height: 224, Width: 224, Channels: 3 RGB) |
| **Data Type** | 32-bit Floating Point (`np.float32`) |
| **Normalization** | Pixel scaling $X_{norm} = \frac{X}{255.0} \in [0.0, 1.0]$ |
| **Output Shape** | `(1, 2)` Softmax probability distribution |
| **Class 0** | `Normal` (Healthy tissue / Non-malignant epithelium) |
| **Class 1** | `OSCC` (Oral Squamous Cell Carcinoma) |

### Cytology Image Quality Guardrails
Before inference, images undergo deterministic microscopic stain validation:
1. **Resolution Bounds:** Minimum dimension $\ge 50 \times 50$ px.
2. **Color Contrast / Variance:** $\sigma_{pixels} \ge 5$ to reject blank or solid-color images.
3. **Stain Spectrum:** Rejection of green-dominant images (vegetation/landscapes) and enforcement of eosin/hematoxylin pink-purple saturation $\ge 5\%$.

---

## 3. Data Persistence & Dual-Mode Strategy

The system operates under a dual-mode persistence architecture:
1. **Cloud Mode (Production):** Google Cloud Firestore provides real-time document synchronization across web and mobile clients with Firestore Server Timestamps.
2. **Offline Fallback Mode (Development/Air-Gapped/Testing):** In-memory and SQLite-backed document store (`schema.sql` / `database.db`) providing zero-latency testing and offline resilience.

---

## 4. REST API Specification

### Authentication & Account Management
- `POST /api/signup` — Register new physician account.
- `POST /api/login` — Authenticate credentials and establish session.
- `POST /forgot-password` — Trigger password reset email link.

### Patient Clinical Records
- `GET /api/patients?user_id=<uid>` — List assigned patients.
- `POST /api/patients/add` — Create new patient record with clinical notes.
- `PUT /api/patients/<id>/edit` — Update existing patient details.
- `DELETE /api/patients/<id>/delete` — Remove patient and associated records.

### Diagnostic Inference & Reports
- `POST /api/diagnose` — Submit cytology image for TFLite inference.
- `GET /api/reports?user_id=<uid>` — Retrieve diagnostic history.
- `GET /api/dashboard?user_id=<uid>` — Aggregate clinical statistics.

### System Diagnostics & Monitoring
- `GET /api/health` — Liveness probe (HTTP 200).
- `GET /api/health/ready` — Readiness probe verifying database, model, and storage.
- `GET /api/system/status` — Platform version, engine details, and runtime configuration.

---

## 5. Security & Governance

1. **Authentication:** Enforced session checks on UI routes, Bearer/Session token validation on APIs.
2. **Cookie Security:** `HttpOnly`, `SameSite=Lax`, and conditional `Secure` flag in production.
3. **Input Sanitization:** Parameterized queries, `secure_filename` path traversal prevention, strict boundary validation.
4. **Audit Logging:** Access logging for clinical records ensuring accountability.
