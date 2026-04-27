# Smart Expense Tracker
### Java Swing + Spring Boot | Fraud Detection | PDF Reports | Charts

---

## Features

| Feature | Details |
|---|---|
| Income & Expenses | Add, edit, delete with categorization |
| Monthly Reports | Income vs Expense summary with charts |
| Fraud Detection | Z-score analysis, unusual hours, category spikes |
| Charts Dashboard | Pie (categories) + Bar (monthly trend) |
| PDF Export | Full monthly report with charts using iText 7 |

---

## Step-by-Step Setup in VS Code

### Step 1 — Prerequisites

Install the following before anything else:

1. **Java 17 JDK**
   - Download: https://adoptium.net/
   - Verify: `java -version` (should show 17+)

2. **Maven 3.8+**
   - Download: https://maven.apache.org/download.cgi
   - Verify: `mvn -version`

3. **VS Code**
   - Download: https://code.visualstudio.com/

---

### Step 2 — Install VS Code Extensions

Open VS Code → press `Ctrl+Shift+X` and install:

| Extension | Publisher | Purpose |
|---|---|---|
| Extension Pack for Java | Microsoft | Core Java support |
| Spring Boot Dashboard | Microsoft | Run/debug Spring Boot |
| Spring Boot Tools | VMware | Auto-complete for properties |
| Lombok Annotations Support | GabrielBB | Lombok @Data/@Builder etc. |

Or run in terminal:
```
code --install-extension vscjava.vscode-java-pack
code --install-extension vscjava.vscode-spring-boot-dashboard
code --install-extension vmware.vscode-spring-boot
code --install-extension GabrielBB.vscode-lombok
```

---

### Step 3 — Open Project

```bash
# Clone or copy project, then open in VS Code:
cd smart-expense-tracker
code .
```

VS Code will auto-detect the Maven project and start downloading dependencies (takes 2-5 minutes first time).

---

### Step 4 — Configure Lombok

1. Press `Ctrl+Shift+P`
2. Type: **"Java: Configure Java Runtime"**
3. Ensure Java 17 is selected

If you see Lombok errors:
1. Press `Ctrl+Shift+P` → "Java: Clean Language Server Workspace"
2. Restart VS Code

---

### Step 5 — Run the Application

**Option A — Spring Boot Dashboard (Recommended)**
1. Click the Spring Boot icon in the left sidebar (🌱)
2. Find `smart-expense-tracker`
3. Click the ▶ play button

**Option B — Run in Terminal**
```bash
mvn spring-boot:run
```

**Option C — VS Code Launch Config**
1. Press `F5`
2. Select "Run Expense Tracker"

---

### Step 6 — Using the Application

The Swing GUI window will open automatically. The Spring Boot server also starts at `http://localhost:8080`.

#### Tabs Overview:
- **📊 Dashboard** — KPI cards + pie chart + bar chart auto-load current month
- **💳 Transactions** — Add/edit/delete, filter by type, search by description
- **📈 Reports** — Select month/year, view breakdown, export PDF
- **🚨 Fraud Alerts** — View flagged transactions, resolve alerts, run manual scan

#### Adding a Transaction:
1. Go to **💳 Transactions** tab
2. Click **＋ Add Transaction**
3. Fill in Description, Amount, Type (Income/Expense), Category
4. Click **Save**
5. Fraud detection runs automatically on save

#### Exporting PDF Report:
1. Go to **📈 Reports** tab
2. Select year and month
3. Click **📄 Export PDF**
4. File saved to `./exports/` folder

#### H2 Database Console (optional):
Visit `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:file:./expense-tracker-db`
- Username: `sa`
- Password: `password`

---

## Project Structure

```
smart-expense-tracker/
├── .vscode/
│   ├── launch.json          ← VS Code run configurations
│   ├── settings.json        ← Java/formatting settings
│   └── extensions.json      ← Recommended extensions
│
├── src/main/java/com/expense/
│   ├── ExpenseTrackerApplication.java   ← Entry point
│   ├── model/
│   │   ├── Transaction.java             ← JPA Entity
│   │   ├── Category.java                ← JPA Entity
│   │   ├── FraudAlert.java              ← JPA Entity
│   │   └── TransactionType.java         ← Enum
│   ├── repository/
│   │   ├── TransactionRepository.java   ← Spring Data JPA
│   │   ├── CategoryRepository.java
│   │   └── FraudAlertRepository.java
│   ├── service/
│   │   ├── TransactionService.java      ← CRUD + business logic
│   │   ├── ReportService.java           ← Monthly analytics
│   │   ├── FraudDetectionService.java   ← Z-score + pattern analysis
│   │   └── PdfExportService.java        ← iText 7 PDF generation
│   ├── controller/
│   │   └── ExpenseController.java       ← REST API endpoints
│   ├── dto/
│   │   └── TransactionDTO.java          ← Request/Response DTO
│   ├── config/
│   │   ├── AppConfig.java               ← Spring beans
│   │   └── DataInitializer.java         ← Sample data seeder
│   └── ui/
│       ├── MainFrame.java               ← Main Swing window
│       └── panels/
│           ├── DashboardPanel.java      ← KPIs + JFreeChart
│           ├── TransactionPanel.java    ← CRUD table UI
│           ├── TransactionDialog.java   ← Add/Edit form dialog
│           ├── ReportPanel.java         ← Monthly reports + PDF
│           └── FraudPanel.java          ← Alerts + resolution
│
├── src/main/resources/
│   └── application.properties
│
└── pom.xml
```

---

## Fraud Detection Logic

The `FraudDetectionService` runs 3 checks on every EXPENSE transaction:

### 1. Large Amount (Z-Score)
```
z = (transaction_amount - historical_mean) / historical_std_dev
IF z > 2.5 → FLAGGED as LARGE_AMOUNT
Risk Score = min(10, z * 2)
```

### 2. Unusual Transaction Hour
```
IF transaction_hour is between 0:00 - 5:00 AM → FLAGGED as UNUSUAL_HOUR
Risk Score = 4.0
```

### 3. Category Spending Spike
```
IF this_month_category_total > (last_month_category_total × 3.0) → FLAGGED as CATEGORY_SPIKE
Risk Score = min(10, spike_ratio)
```

Thresholds are configurable in `application.properties`.

---

## REST API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/transactions` | All transactions |
| POST | `/api/transactions` | Add transaction |
| PUT | `/api/transactions/{id}` | Update transaction |
| DELETE | `/api/transactions/{id}` | Delete transaction |
| GET | `/api/reports/monthly?year=&month=` | Monthly summary |
| GET | `/api/reports/trend?months=6` | Expense trend |
| GET | `/api/reports/categories?year=&month=` | Category breakdown |
| POST | `/api/reports/export-pdf` | Generate PDF |
| GET | `/api/fraud/alerts` | Active fraud alerts |
| POST | `/api/fraud/alerts/{id}/resolve` | Resolve alert |
| POST | `/api/fraud/scan` | Run full scan |

---

## Common Issues

**Issue: Swing window doesn't open**
Fix: Make sure `-Djava.awt.headless=false` is in your JVM args (already set in `.vscode/launch.json`)

**Issue: Lombok red errors everywhere**
Fix: `Ctrl+Shift+P` → "Java: Clean Language Server Workspace" → restart VS Code

**Issue: H2 database locked**
Fix: Delete `expense-tracker-db.mv.db` file and restart — fresh database will be created

**Issue: PDF export fails**
Fix: Check that the `exports/` directory exists, or change path in `application.properties`

**Issue: Charts not showing**
Fix: Click the "↻ Refresh" button — charts load on demand

---

## Java 24 Upgrade Notes

### What Changed from Java 17

| Feature | Java 17 | Java 24 |
|---|---|---|
| `TransactionDTO` | Lombok `@Data @Builder` class | Native `record` |
| Lambda params | `(e) -> ...` | `(_ ) -> ...` unnamed var |
| Catch blocks | `catch (Exception e)` | `catch (Exception _)` unnamed |
| Filter logic | `if/else chains` | `switch` expressions |
| String format | `String.format(...)` | `"...".formatted(...)` |
| Local vars | Explicit types | `var` everywhere |
| Log messages | `String.format` | Text blocks `""" """` |
| Merge lambda | `(e1, e2) -> e1` | `(e1, _) -> e1` unnamed param |

### Java 24 Features Used

- **Records** (`TransactionDTO`) — immutable data carriers, auto-generate accessors, `equals`, `hashCode`, `toString`, and a compact canonical constructor for validation
- **Switch expressions** — exhaustive, no fall-through, used in `TransactionType.isDebit()`, fraud risk scoring, UI row coloring, and filter logic
- **Unnamed variables `_`** (preview in 22, standard in 24) — used in `catch (Exception _)`, `(e1, _) -> e1`, and event listeners `addActionListener(_ -> ...)`
- **Text blocks** — multi-line fraud alert descriptions in `FraudDetectionService`
- **`var` type inference** — used throughout services, controllers, and UI panels
- **`"...".formatted()`** — string formatting on instance instead of `String.format()`
- **Pattern matching `instanceof`** — available since Java 16, used in service layer
- **`List.of` / `SequencedCollection`** — `getFirst()` / `getLast()` on ordered lists
- **`Stream::toList()`** — shorthand for `collect(Collectors.toList())`

### Running with Preview Features

```bash
# Maven (preview flag baked into pom.xml)
mvn spring-boot:run

# Direct java command
java --enable-preview -jar target/smart-expense-tracker-1.0.0.jar

# VS Code — already configured in .vscode/launch.json
# Press F5 → "Run Expense Tracker (Java 24)"
```

### Install Java 24 JDK

```bash
# Temurin (recommended)
# https://adoptium.net/temurin/releases/?version=24

# Or via SDKMAN
sdk install java 24-tem
sdk use java 24-tem
java -version   # OpenJDK 24
```
