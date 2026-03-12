# ⚕ AI-Powered Smart Health Diagnosis and Appointment System

A full-stack, production-ready web application that enables patients to enter symptoms, receive AI-powered disease predictions using a trained machine learning model, and book appointments with specialist doctors.

---

## 🧬 Tech Stack

| Layer        | Technology                                 |
|--------------|--------------------------------------------|
| Frontend     | HTML5, CSS3, JavaScript, Bootstrap 5, Chart.js |
| Backend API  | Java 17 + Spring Boot 3.2 + Spring Security |
| Auth         | JWT (jjwt) + BCrypt password hashing       |
| AI/ML Model  | Python 3.11 + Flask + scikit-learn          |
| Database     | MySQL 8                                    |
| ORM          | Spring Data JPA / Hibernate                |

---

## 🗂 Project Structure

```
smart-health-system/
├── frontend/
│   ├── index.html               # Landing page
│   ├── login.html               # Login
│   ├── register.html            # Register
│   ├── patient-dashboard.html   # Patient portal
│   ├── doctor-dashboard.html    # Doctor portal
│   ├── admin-dashboard.html     # Admin portal
│   ├── style.css                # Global dark-luxury stylesheet
│   ├── script.js                # Core API & tab logic
│   ├── patient.js               # Patient dashboard logic
│   ├── doctor.js                # Doctor dashboard logic
│   └── admin.js                 # Admin dashboard + Chart.js
│
├── backend-java/
│   ├── pom.xml                  # Maven dependencies
│   └── src/main/java/com/smarthealth/
│       ├── SmartHealthApplication.java
│       ├── models/              # User, Doctor, Prediction, Appointment, Symptom
│       ├── repositories/        # Spring Data JPA repos
│       ├── dto/                 # Request/response DTOs
│       ├── controllers/         # AuthController, PatientController, DoctorController, AdminController
│       ├── services/            # PredictionService (calls Python AI)
│       ├── security/            # JwtUtils, JwtAuthFilter, SecurityConfig
│       └── resources/
│           └── application.properties
│
├── ai-model-python/
│   ├── train_model.py           # Train Random Forest on symptom data
│   ├── predict_api.py           # Flask REST API
│   ├── requirements.txt
│   └── model/                   # Generated after training
│       ├── disease_model.pkl
│       ├── label_encoder.pkl
│       ├── symptoms_list.json
│       └── disease_specialty.json
│
└── database/
    └── schema.sql               # Full MySQL schema + seed data
```

---

## ✨ Features

### 1. User Authentication
- Register / Login with JWT tokens
- Role-based access: **Patient**, **Doctor**, **Admin**
- BCrypt password hashing
- Protected routes per role

### 2. Patient Dashboard
- ✅ Select symptoms from 30 clinical options
- ✅ AI predicts disease with confidence score
- ✅ View prediction history
- ✅ Book doctor appointments by specialty
- ✅ Track appointment status

### 3. Doctor Dashboard
- ✅ View all appointments
- ✅ Approve / Reject / Complete appointments
- ✅ Add doctor notes
- ✅ Manage profile (specialization, schedule, fee)

### 4. Admin Dashboard
- ✅ User management (activate/deactivate)
- ✅ Doctor verification
- ✅ All appointments overview
- ✅ Analytics with Chart.js (top diseases, appointment stats)

### 5. AI Symptom Checker
- Random Forest model trained on 300+ symptom-disease records
- 22 disease predictions supported
- Returns confidence score + alternative diagnoses
- Maps disease to recommended specialist

### 6. Appointment System
- Filter doctors by specialization
- Date & time slot booking
- Status tracking: PENDING → APPROVED/REJECTED → COMPLETED

---

## 🚀 Setup Instructions

### Prerequisites

- Java 17+
- Maven 3.9+
- MySQL 8.0+
- Python 3.10+
- Node.js (optional, for serving frontend)

---

### Step 1: Database Setup

```sql
-- Open MySQL Workbench or phpMyAdmin
-- Run the full schema file:
source /path/to/smart-health-system/database/schema.sql;
```

This creates the `smart_health_db` database with all tables and seeds:
- Admin user: `admin@smarthealth.com` / `Admin@123`
- 30 master symptoms

---

### Step 2: AI Model Setup

```bash
cd smart-health-system/ai-model-python

# Install Python dependencies
pip install -r requirements.txt

# Train the model (generates model/ directory)
python train_model.py

# Start Flask prediction API on port 5000
python predict_api.py
```

**Verify AI service:**
```bash
curl http://localhost:5000/health
# → {"status":"ok","model_loaded":true,"version":"1.0"}

curl -X POST http://localhost:5000/predict-disease \
  -H "Content-Type: application/json" \
  -d '{"symptoms":["Fever","Cough","Headache","Fatigue"]}'
```

---

### Step 3: Backend Setup

Edit `backend-java/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/smart_health_db?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD

ai.service.url=http://localhost:5000
```

```bash
cd smart-health-system/backend-java

# Build and run
mvn clean install
mvn spring-boot:run
```

Backend runs on `http://localhost:8080/api`

---

### Step 4: Frontend Setup

No build step needed — pure HTML/CSS/JS.

```bash
cd smart-health-system/frontend

# Option A: Python simple server
python -m http.server 3000

# Option B: VS Code Live Server (recommended)
# Install "Live Server" extension, right-click index.html → Open with Live Server

# Option C: Node http-server
npx http-server -p 3000
```

Open `http://localhost:3000`

---

### Step 5: Demo Accounts

Create demo patients and doctors by registering via the frontend, or insert directly:

```sql
-- Demo patient (password: Demo@1234)
INSERT INTO users (full_name, email, password, role, is_active)
VALUES ('John Patient', 'patient@demo.com',
  '$2a$10$Nv5SxTMCqaQVwWh5aFpVdOpJ8MaV7pNx7CbxQ1JcZxV6kZqCWJWEy', 'PATIENT', TRUE);

-- Demo doctor user (password: Demo@1234)
INSERT INTO users (full_name, email, password, role, is_active)
VALUES ('Dr. Priya Sharma', 'doctor@demo.com',
  '$2a$10$Nv5SxTMCqaQVwWh5aFpVdOpJ8MaV7pNx7CbxQ1JcZxV6kZqCWJWEy', 'DOCTOR', TRUE);

-- Link doctor profile (after getting user ID)
INSERT INTO doctors (user_id, specialization, hospital_name, years_experience, is_verified)
SELECT id, 'General Physician', 'City Hospital', 10, TRUE
FROM users WHERE email = 'doctor@demo.com';
```

---

## 🌐 API Reference

### Auth
| Method | Endpoint              | Body                          | Auth |
|--------|-----------------------|-------------------------------|------|
| POST   | `/auth/register`      | fullName, email, password, role | —  |
| POST   | `/auth/login`         | email, password               | —    |
| GET    | `/auth/me`            | —                             | JWT  |

### Patient
| Method | Endpoint                      | Description               |
|--------|-------------------------------|---------------------------|
| POST   | `/patient/addSymptoms`        | AI prediction             |
| GET    | `/patient/predictions`        | Prediction history        |
| POST   | `/patient/bookAppointment`    | Book appointment          |
| GET    | `/patient/appointments`       | My appointments           |
| GET    | `/patient/doctors?specialty=` | Find doctors              |

### Doctor
| Method | Endpoint                        | Description              |
|--------|---------------------------------|--------------------------|
| GET    | `/doctor/appointments`          | View all appointments    |
| PUT    | `/doctor/updateAppointment/{id}`| Approve/Reject/Complete  |
| GET    | `/doctor/profile`               | Get profile              |
| PUT    | `/doctor/profile`               | Update profile           |

### Admin
| Method | Endpoint                             | Description           |
|--------|--------------------------------------|-----------------------|
| GET    | `/admin/manageUsers`                 | All users             |
| PUT    | `/admin/manageUsers/{id}/toggle`     | Activate/Deactivate   |
| GET    | `/admin/manageDoctors`               | All doctors           |
| PUT    | `/admin/manageDoctors/{id}/verify`   | Verify doctor         |
| GET    | `/admin/manageAppointments`          | All appointments      |
| GET    | `/admin/analytics`                   | System analytics      |

### AI Service (Flask)
| Method | Endpoint            | Description              |
|--------|---------------------|--------------------------|
| GET    | `/health`           | Health check             |
| GET    | `/symptoms`         | List supported symptoms  |
| POST   | `/predict-disease`  | Predict disease          |

---

## 🔐 Security

- JWT Bearer tokens (24h expiry)
- BCrypt(10) password hashing
- Role-based endpoint guards (`@PreAuthorize`)
- CORS configured for specific origins
- Input validation with Bean Validation API
- MySQL parameterized queries via JPA (SQL injection protection)

---

## 🤖 AI Model Details

- **Algorithm**: Random Forest Classifier (200 trees)
- **Input**: 30 binary symptom features
- **Output**: Disease name + probability scores
- **Training set**: 300+ augmented records across 22 diseases
- **Diseases detected**: Common Cold, Influenza, COVID-19, Pneumonia, Diabetes Type 2, Hypertension, Heart Disease, Dengue, Malaria, Typhoid, Gastroenteritis, Migraine, Arthritis, and more
- **Specialty mapping**: Each disease maps to the recommended medical specialist

---

## ☁️ Deployment

### Frontend → Netlify
```bash
# Drag-and-drop the frontend/ folder to netlify.com
# Or use Netlify CLI:
npx netlify deploy --prod --dir=frontend
```

### Backend → Render
1. Push backend-java to GitHub
2. Create a new Web Service on render.com
3. Set build command: `mvn clean package -DskipTests`
4. Set start command: `java -jar target/smart-health-backend-1.0.0.jar`
5. Add environment variables for DB and JWT

### AI Model → Railway / Render
```bash
# Procfile for Railway/Render:
echo "web: gunicorn predict_api:app" > ai-model-python/Procfile
```

### Database → PlanetScale / Railway MySQL
Update `application.properties` with cloud DB URL.

---

## 🗄️ Database Schema

| Table           | Key Columns                                           |
|-----------------|-------------------------------------------------------|
| users           | id, full_name, email, password, role, is_active       |
| doctors         | id, user_id, specialization, license_number, verified |
| symptoms        | id, name, body_part, severity                         |
| predictions     | id, patient_id, symptom_names, predicted_disease, confidence |
| appointments    | id, patient_id, doctor_id, date, time, status         |
| medical_history | id, patient_id, doctor_id, diagnosis, prescription    |

---

## 📸 Screenshots

> After running the app:
> - `localhost:3000` → Landing page
> - `localhost:3000/login.html` → Auth
> - `localhost:3000/patient-dashboard.html` → Patient portal
> - `localhost:3000/doctor-dashboard.html` → Doctor portal
> - `localhost:3000/admin-dashboard.html` → Admin analytics

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

MIT License — free to use, modify, and distribute.

---

## 👨‍💻 Author

Built with ❤️ as a full-stack AI health platform demonstration.
Stack: Java Spring Boot · Python Flask · scikit-learn · MySQL · Bootstrap 5
