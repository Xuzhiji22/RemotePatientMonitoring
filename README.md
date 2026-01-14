# Remote Patient Monitoring (RPM)

## Overview
This project implements a **Remote Patient Monitoring system** that displays simulated patient vital signs in real-time, raises visual alerts for abnormal values, and maintains **persistent historical records** locally and in the cloud.

---

## How to Run (Local UI)

**Requirements**
- Java **11**

**Run**
```bash
./gradlew run
```

Or run the main class directly in IntelliJ:
```
rpm.Main
```

This launches the **Swing-based clinician interface** with:
- Live plots of all vital signs and ECG  
- Warning / urgent visual alerts  
- Patient history and report generation  

---

## Cloud / Level 3 Verification

**Cloud base URL**
```
https://bioeng-rpm-app.impaas.uk
```

### Health Check
```
GET /health
```

Example:
```
https://bioeng-rpm-app.impaas.uk/health
```

Expected output confirms cloud mode and database connection:
```
mode=CLOUD
db=CONNECTED
```

---

### Latest Abnormal Events
```
GET /api/abnormal/latest
```

Example:
```
https://bioeng-rpm-app.impaas.uk/api/abnormal/latest?patientId=P001&limit=60
```

---

### Minute-Averaged History
```
GET /api/minutes/latest
```

Example:
```
https://bioeng-rpm-app.impaas.uk/api/minutes/latest?patientId=P001&limit=1000
```

---

## Notes for Assessment
- The system runs **fully locally** with automatic fallback if the cloud is unavailable  
- Cloud endpoints are **publicly accessible for verification**  
- No manual database setup is required  

---

## Code Structure (High-level)

```
rpm/
 ├── ui        Swing user interface
 ├── sim       Vital sign and ECG simulation
 ├── alert     Alert logic and thresholds
 ├── data      Local storage and aggregation
 ├── dao       Database access
 ├── web       Cloud API endpoints
 └── server    Cloud service bootstrap
```
