# 🚀 SmartScheduler Deployment Guide

Follow these steps to deploy the **SmartScheduler** application using **Docker Compose**.

---

## 🧩 Prerequisites
- **Docker** and **Docker Compose** must be installed.  

---

## ⚙️ Step 1: Clone the Repository
```bash
git clone https://github.com/mariasstalin/smart-scheduler.git
cd smart-scheduler
```

---

## ⚙️ Step 2: Configure Environment Variables
In the root directory, open the `.env` file.  
It already contains all the keys — just update the values as needed:

```env
ZOHO_TOKEN_BASE_URL=<Zoho Accounts Base URL>
ZOHO_CLIENT_ID=<Your Zoho Client ID>
ZOHO_CLIENT_SECRET=<Your Zoho Client Secret>
ZOHO_REFRESH_TOKEN=<Your Zoho Refresh Token>
ZOHO_API_BASE_URL=<Zoho API Base URL>

NGROK_AUTHTOKEN=<Your Ngrok Auth Token>
NGROK_DOMAIN=<Leave empty initially>

NOTIFICATION_MESSAGING_PROVIDER=demo
NOTIFICATION_SYSTEM_PHONE=919999999999

NOTIFICATION_PATIENT_PRIORITY_ENGINE=ai-based
NOTIFICATION_SCHEDULING_RESPONSE_WINDOW_MINUTES=1
NOTIFICATION_SCHEDULING_MAX_CONSECUTIVE_MISSES=3
NOTIFICATION_SCHEDULING_OPT_OUT_DURATION=PT15M

APPLICATION_BASE_URL=http://gateway:8080
```

---

## 🐳 Step 3: Start All Services

Run the following command to bring up all SmartScheduler components in production mode:
```bash
docker compose --profile prod up -d --build
```
Note:
If any of the SmartScheduler services fail to start after executing "docker compose --profile prod up -d --build". Please run the below command a few more times until all services are up and healthy.
```
docker compose --profile prod up -d
```
This will start:
- `discovery service` (Eureka)
- `gateway service`
- `config service`
- `appointment service`
- `notification service`
- `demo service`
- `rasa bot`
- `rasa actions`
- `light GBM (ai priority engine)`
- `duckling`
- `rabbitmq`
- `mysql (db:smart_scheduler; username:root; password:root)`
- `redis`
- `ngrok` (to expose localhost for Zoho)
---

## 🌐 Step 4: Verify Running Containers
Check that all services are up:
```bash
docker ps
```

You should see containers for `discovery`, `gateway`, `config`, `notification`, `appointment`, `demo`, `duckling`, `rasa`, `rasa-actions`, `rabbitmq`, `mysql`, `redis`, `light-gbm`, and `ngrok`.

---

## 🔗 Step 5: Access the UI and Dashboards

| Service | Description | URL                                                                                     |
|----------|--------------|-----------------------------------------------------------------------------------------|
| **Discovery Server (Eureka)** | Service registry for all microservices | [http://localhost:8761](http://localhost:8761)                                          |
| **RabbitMQ Management UI** | Queue monitoring | [http://localhost:15672](http://localhost:15672)<br>*(user: `root` / password: `root`)* |
| **Notification Demo UI** | Displays outgoing messages and simulated patient replies | [http://localhost:8080/demo/chat/patients/919999999911](http://localhost:8082/demo)                                |
| **Ngrok Web UI** | Shows generated public domain | [http://localhost:4040](http://localhost:4040)                                          |

---

## 🌍 Step 6: Retrieve Ngrok Domain
After the containers are running, open:

👉 [http://localhost:4040](http://localhost:4040)

Copy the **public HTTPS domain** (e.g. `https://geodic-annetta-resistlessly.ngrok-free.dev`).  
This is your externally accessible domain.

Update your `.env` file with:
```env
NGROK_DOMAIN=https://geodic-annetta-resistlessly.ngrok-free.dev
```

Then restart ngrok (if needed):
```bash
docker compose restart ngrok
```

---

## 🔁 Step 7: Update Zoho Flow Webhooks
There are **three Zoho Flows** already created:
1. **Book Appointment Flow**
2. **Cancel Appointment Flow**
3. **Reschedule Appointment Flow**

Reviewers only need to:
- Open each flow in **Zoho Flow**
- Update the webhook URL with the new `NGROK_DOMAIN` value in my account:
  ```
  https://<your-ngrok-domain>/appointment/webhook/zoho/book
  https://<your-ngrok-domain>/appointment/webhook/zoho/cancel
  https://<your-ngrok-domain>/appointment/webhook/zoho/reschedule
  ```

That’s it — SmartScheduler will start receiving live webhook events from Zoho.

---

## ✅ Step 8: Verify the Flow
1. Book, cancel, or reschedule an appointment on the Zoho Patient Portal: https://smartscheduler.zohobookings.in/
2. Check logs in:
   ```bash
   docker compose logs -f notification
   ```
3. Open **Demo UI** to see notification messages and simulated patient responses.

---

🎉 **SmartScheduler is now fully deployed and ready for review!**