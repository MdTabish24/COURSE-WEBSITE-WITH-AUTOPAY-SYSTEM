# Universal Skill Development Centre - Single-Page Checkout

Enterprise-grade checkout system built with **Java Spring Boot** - stable, version-proof, and production-ready.

## Why Java?

- **Zero version conflicts**: Java 17 LTS supported until 2029
- **Backward compatible**: Code written today runs 10 years later
- **Battle-tested**: Used by banks, governments, and Fortune 500 companies
- **No dependency hell**: Maven handles everything cleanly

## Tech Stack

- **Backend**: Java 17 + Spring Boot 3.2
- **Database**: PostgreSQL (rock-solid, 25+ years old)
- **Frontend**: Plain HTML/CSS/JS (no frameworks)
- **Email**: JavaMail API (built-in)
- **Excel**: Apache POI (industry standard since 2001)
- **PDF**: iText7 (enterprise-grade)
- **QR Codes**: ZXing (Google's library)

## Setup

### 1. Install Java 17
```bash
# Ubuntu/Debian
sudo apt install openjdk-17-jdk

# macOS
brew install openjdk@17

# Windows
# Download from: https://adoptium.net/
```

### 2. Install PostgreSQL
```bash
# Ubuntu/Debian
sudo apt install postgresql postgresql-contrib

# macOS
brew install postgresql

# Create database
sudo -u postgres psql
CREATE DATABASE safix_checkout;
\q
```

### 3. Configure Application
Edit `src/main/resources/application.properties`:
```properties
# Update these values
spring.datasource.password=your_postgres_password
spring.mail.username=your-email@gmail.com
spring.mail.password=your-gmail-app-password
upi.merchant.id=your-upi-id@bank
```

### 4. Run Application
```bash
# Build and run
./mvnw spring-boot:run

# Or build JAR and run
./mvnw clean package
java -jar target/elite-checkout-1.2.0.jar
```

Application runs on: http://localhost:8080

## Features

✅ Single-page checkout (no redirects)
✅ UPI deep links for mobile
✅ Dynamic QR code generation for desktop
✅ Screenshot upload for payment proof
✅ Auto-save to PostgreSQL database
✅ Auto-export to Excel file
✅ Auto-send PDF receipt via email
✅ Luxury UI (Gold on Black theme)

## How It Works

1. User fills form (Name, WhatsApp, Email)
2. User pays via UPI (mobile app or QR scan)
3. User uploads payment screenshot
4. System saves to database + Excel
5. System emails PDF receipt within 60 seconds

## Excel Export

All registrations are automatically saved to `registrations.xlsx` in the project root.

## Production Deployment

### Option 1: Traditional Server
```bash
# Build JAR
./mvnw clean package

# Run on server
java -jar target/elite-checkout-1.2.0.jar
```

### Option 2: Docker
```bash
docker build -t safix-checkout .
docker run -p 8080:8080 safix-checkout
```

### Option 3: Cloud (AWS/Azure/GCP)
Upload JAR to any cloud provider. Java runs everywhere.

## Why This Will Last Forever

- Java 17 LTS: Supported until 2029, backward compatible forever
- Spring Boot: Industry standard, won't break
- PostgreSQL: 25+ years old, still going strong
- Apache POI: Excel library since 2001
- No JavaScript frameworks: No React/Vue/Angular version hell

## License

Proprietary - Universal Skill Development Centre
