# 🧠 SmartScheduler — Intelligent Appointment Optimization System

---

## 1. 🎯 Purpose

**SmartScheduler** is an intelligent appointment optimization system designed to **maximize clinic slot utilization**, **minimize doctor idle time**, and **improve patient satisfaction**.

It automatically detects **cancelled appointment slots**, identifies the most eligible patients to fill them using a **rules-driven and AI-assisted prioritization engine**, and notifies patients via **Rasa (conversational AI)** and **Twilio (WhatsApp/SMS)**. The system integrates with **Zoho Booking** for external synchronization.

---

## 2. 🧩 High-Level Flow

### 🏥 Actors

- **Patient** — Receives appointment and waitlist notifications, responds via WhatsApp/SMS.
- **Twilio** — Manages WhatsApp/SMS communication between system and patients.
- **Notification Service** — Applies rules, prioritization, and manages communication logic.
- **Appointment Service** — Updates and maintains appointment states; publishes slot events.
- **Rasa Chatbot** — Conversational interface that processes patient replies (YES/NO).
- **Zoho Booking** — External booking platform and source of truth for appointment data.

---

## 3. ⚙️ Core Workflow

### 🩺 Step 1: Appointment Cancellation
1. A patient cancels or misses an appointment.
2. **Appointment Service** updates the database and publishes a **slot cancelled event**.

---

### 📢 Step 2: Notification Trigger
1. **Notification Service** listens for cancellation events.
2. It retrieves details about the cancelled slot and the associated doctor.

---

### 🧮 Step 3: Candidate Identification
The system determines who should be notified next, in order of preference:

1. **Waitlist patients** for the same doctor/day.
2. **Patients with later same-day appointments** (offered earlier time).
3. If none are available, the slot is left **open in Zoho Booking** for new bookings.

---

### 🤖 Step 4: Priority Calculation
Each candidate’s **priority** is computed using both **rules-based** and **AI-based** scoring.

---

## 4.1 🧩 Rules Engine (Deterministic)
Business rules applied:

- Prefer **VIP** patients (long-term or loyal customers).
- Exclude patients who have **opted out** within `optOutDuration`.
- Deprioritize patients who have missed more than `maxConsecutiveMisses`.
- Consider **responsiveness rate** (`notifications responded / notifications sent`).

Rules are configurable through application properties.

---

## 4.2 🤖 AI-Based Patient Priority Engine (Predictive)

**Implementation:** `AiBasedPatientPriorityEngine` integrates with an external LightGBM model service (via Feign client).

**Model Input Features:**
- `isVip`
- `severityLevel`
- `totalNotificationsResponded`
- `totalNotificationsSent`
- `bookingHistory` (recent appointments)

**Model Output:**
- `score` (0.0–1.0): likelihood that the patient will accept the offered slot.

**Model Input Class Example:**
```java
@Data
public class PatientPriority {
    private Long id;
    private Boolean isVip;
    private Integer severityLevel;
    private Integer totalNotificationsResponded;
    private Integer totalNotificationsSent;
    private List<LocalDateTime> bookingHistory;
    private Double score;
}
```

The AI score augments rule-based weighting to produce a **final composite rank** used for sequential notification.

---

## 5. 💬 Notification Flow

1. **Notification Service** generates ranked list (rules + AI).
2. **Twilio** sends WhatsApp/SMS messages with slot details.
3. **Rasa** processes replies and returns intents (e.g., `/confirm_slot_offer`, `/deny_slot_offer`).

**If the patient accepts:**
- Rasa forwards acceptance to **Notification Service**, which triggers **Appointment Service** to reschedule the appointment.
- **Appointment Service** updates the DB and **Zoho Booking**.
- The old slot becomes free and re-enters the cancellation flow.

**If the patient declines or doesn't respond:**
- The system proceeds to the next candidate.
- If no one accepts, the slot remains open in Zoho.

---

## 6. 🧠 Smart Prioritization Logic

**Hybrid approach:**
- **Rules-based filters** (eligibility, VIP, opt-out, responsiveness).
- **AI scoring** (LightGBM) for acceptance prediction.

**Goal:** Offer the slot to the patient who is **both eligible and most likely to accept**, minimizing churn and maximizing fill rate.

---

## 7. 🏗️ Technical Architecture

| Component | Role |
|-----------|------|
| **Appointment Service** | Source of truth for appointments; syncs with Zoho; publishes events. |
| **Notification Service** | Core orchestrator for slot reallocation and messaging. |
| **Rules Engine** | Configurable logic for eligibility and sequential notification flow. |
| **AiBasedPatientPriorityEngine** | Calls LightGBM (via Feign) to obtain priority scores. |
| **Rasa** | Conversational AI for parsing replies and triggering actions. |
| **Twilio Integration** | Sends/receives WhatsApp and SMS messages. |
| **Zoho Booking Integration** | Syncs appointment state with external booking platform. |
| **Database** | Persists patients, appointments, waitlist entries, notifications, and metrics. |

---

## 8. 🧩 Key Configurations

Example config (`application.yml`):
```yaml
notification:
  scheduling:
    responseWindowMinutes: 1
    maxConsecutiveMisses: 3
    optOutDuration: 24h
```

- `responseWindowMinutes`: How long to wait for a response.
- `maxConsecutiveMisses`: Threshold for deprioritization.
- `optOutDuration`: Time period before patient can be re-notified.

---

## 9. 🧾 Design Goals

- Automate slot reassignments to **maximize utilization**.
- **Reduce manual intervention** for clinic staff.
- **Enable natural communication** via Rasa + Twilio.
- **Data-driven prioritization** through LightGBM.
- **Configurable** rules for clinic-specific workflows.

---

## 10. 🔮 Future Extensibility

- Predict response time and no-show probability.
- Use EHR data for medical prioritization.
- Retrain LightGBM with acceptance/rejection data.
- Expand integration to other booking platforms.

---

## 11. 🧭 End-to-End Flow Summary

| Stage | Trigger | Component | Outcome |
|-------|----------|------------|----------|
| 1 | Appointment Cancellation | Appointment Service | Event published |
| 2 | Event Received | Notification Service | Rules + AI triggered |
| 3 | Candidate Selection | Rules + AI | Ranked patient list |
| 4 | Notification Dispatch | Twilio + Rasa | Message sent |
| 5 | Response Handling | Rasa + Notification Service | Accept/decline processed |
| 6 | Slot Reassignment | Appointment Service | DB + Zoho updated |
| 7 | Feedback Loop | Notification Service | Metrics updated |

---

## ✅ Outcome

SmartScheduler is an **autonomous scheduling ecosystem** combining:

- **Rules-based governance**
- **LightGBM AI prioritization**
- **Rasa conversational AI**
- **Twilio messaging**
- **Zoho synchronization**

✅ Maximizes slot utilization  
✅ Reduces revenue loss  
✅ Improves patient satisfaction

---

## 👤 Author
**Project:** SmartScheduler  
**Author:** Stalin Jeyaraj  
**Year:** 2025