from typing import Any, Text, Dict, List
from rasa_sdk import Action, Tracker
from rasa_sdk.executor import CollectingDispatcher
from rasa_sdk.events import SlotSet, EventType
from datetime import datetime
import os
import requests
import logging

logger = logging.getLogger(__name__)

# Service endpoints
APPOINTMENT_URL = os.getenv("APPOINTMENT_URL", "http://appointment:8080")
NOTIFICATION_URL = os.getenv("NOTIFICATION_URL", "http://notification:8083/notify")


def send_notification_to_service(payload: dict) -> bool:
    try:
        resp = requests.post(NOTIFICATION_URL, json=payload, timeout=5)
        return 200 <= resp.status_code < 300
    except Exception as e:
        logger.error(f"Failed to send notification: {e}")
        return False


def fetch_appointments(patient_name: str) -> List[Dict]:
    if not patient_name:
        return []
    try:
        resp = requests.get(f"{APPOINTMENT_URL}/appointments", params={"patient": patient_name}, timeout=5)
        if resp.status_code == 200:
            return resp.json()
    except Exception as e:
        logger.error(f"Error fetching appointments: {e}")
    return []


def fetch_available_slots(doctor_name: str, date: str) -> List[str]:
    if not doctor_name or not date:
        return []
    try:
        resp = requests.get(f"{APPOINTMENT_URL}/slots", params={"doctor": doctor_name, "date": date}, timeout=5)
        if resp.status_code == 200:
            return resp.json()
    except Exception as e:
        logger.error(f"Error fetching slots: {e}")
    return []


def update_appointment(patient: str, doctor: str, new_date: str, new_time: str) -> bool:
    if not patient or not doctor or not new_date or not new_time:
        return False
    try:
        resp = requests.put(f"{APPOINTMENT_URL}/appointments/reschedule", json={
            "patient_name": patient,
            "doctor_name": doctor,
            "appointment_date": new_date,
            "appointment_time": new_time,
        }, timeout=5)
        return resp.status_code == 200
    except Exception as e:
        logger.error(f"Error updating appointment: {e}")
        return False


def cancel_appointment(patient: str, doctor: str) -> bool:
    if not patient or not doctor:
        return False
    try:
        resp = requests.delete(f"{APPOINTMENT_URL}/appointments/cancel", params={"patient": patient, "doctor": doctor}, timeout=5)
        return resp.status_code == 200
    except Exception as e:
        logger.error(f"Error cancelling appointment: {e}")
        return False


def parse_duckling_time(tracker: Tracker, slot_name: str) -> str:
    entities = tracker.latest_message.get("entities", [])
    for ent in entities:
        if ent.get("entity") == "time":
            value = ent.get("value", {}).get("value")
            if value:
                try:
                    dt = datetime.fromisoformat(value.replace("Z", "+00:00"))
                    if "date" in slot_name:
                        return dt.date().isoformat()
                    else:
                        return dt.time().strftime("%H:%M")
                except Exception as e:
                    logger.warning(f"Failed to parse Duckling time: {value} -> {e}")
    return tracker.get_slot(slot_name)  # fallback


class ActionFetchAvailableSlots(Action):
    def name(self) -> Text:
        return "action_fetch_available_slots"

    def run(self, dispatcher: CollectingDispatcher, tracker: Tracker, domain: Dict[Text, Any]) -> List[EventType]:
        doctor = tracker.get_slot("doctor_name")
        date = parse_duckling_time(tracker, "appointment_date")
        if not doctor:
            dispatcher.utter_message(text="Please provide the doctor's name.")
            return []
        if not date:
            dispatcher.utter_message(text="Please provide the date for checking available slots.")
            return []
        slots = fetch_available_slots(doctor, date)
        if slots:
            dispatcher.utter_message(text=f"Available slots for {doctor} on {date}: {', '.join(slots)}")
        else:
            dispatcher.utter_message(text=f"No slots available for {doctor} on {date}.")
        return []


class ActionBookAppointment(Action):
    def name(self) -> Text:
        return "action_book_appointment"

    def run(self, dispatcher: CollectingDispatcher, tracker: Tracker, domain: Dict[Text, Any]) -> List[EventType]:
        patient = tracker.get_slot("patient_name")
        doctor = tracker.get_slot("doctor_name")
        date = parse_duckling_time(tracker, "appointment_date")
        time = parse_duckling_time(tracker, "appointment_time")

        if not all([patient, doctor, date, time]):
            dispatcher.utter_message(text="Please provide patient, doctor, date, and time to reschedule.")
            return []

        success = update_appointment(patient, doctor, date, time)
        if success:
            dispatcher.utter_message(text=f"Appointment booked for {patient} with {doctor} on {date} at {time}.")
            #send_notification_to_service({"event": "appointment.rescheduled", "patient_name": patient, "doctor_name": doctor, "date": date, "time": time})
        else:
            dispatcher.utter_message(text="Unable to book. Please try again later.")
        return []

class ActionRescheduleAppointment(Action):
    def name(self) -> Text:
        return "action_reschedule_appointment"

    def run(self, dispatcher: CollectingDispatcher, tracker: Tracker, domain: Dict[Text, Any]) -> List[EventType]:
        patient = tracker.get_slot("patient_name")
        doctor = tracker.get_slot("doctor_name")
        new_date = parse_duckling_time(tracker, "appointment_date")
        new_time = parse_duckling_time(tracker, "appointment_time")

        if not all([patient, doctor, new_date, new_time]):
            dispatcher.utter_message(text="Please provide patient, doctor, date, and time to reschedule.")
            return []

        success = update_appointment(patient, doctor, new_date, new_time)
        if success:
            dispatcher.utter_message(text=f"Appointment rescheduled for {patient} with {doctor} on {new_date} at {new_time}.")
            send_notification_to_service({"event": "appointment.rescheduled", "patient_name": patient, "doctor_name": doctor, "new_date": new_date, "new_time": new_time})
        else:
            dispatcher.utter_message(text="Unable to reschedule. Please try again later.")
        return []


class ActionCancelAppointment(Action):
    def name(self) -> Text:
        return "action_cancel_appointment"

    def run(self, dispatcher: CollectingDispatcher, tracker: Tracker, domain: Dict[Text, Any]) -> List[EventType]:
        patient = tracker.get_slot("patient_name")
        doctor = tracker.get_slot("doctor_name")
        if not all([patient, doctor]):
            dispatcher.utter_message(text="Please provide both patient and doctor details to cancel the appointment.")
            return []

        success = cancel_appointment(patient, doctor)
        if success:
            dispatcher.utter_message(text=f"Appointment for {patient} with {doctor} has been cancelled.")
            send_notification_to_service({"event": "appointment.cancelled", "patient_name": patient, "doctor_name": doctor})
        else:
            dispatcher.utter_message(text="Unable to cancel. Please try again later.")
        return []


class ActionListAppointments(Action):
    def name(self) -> Text:
        return "action_list_appointments"

    def run(self, dispatcher: CollectingDispatcher, tracker: Tracker, domain: Dict[Text, Any]) -> List[EventType]:
        patient = tracker.get_slot("patient_name")
        if not patient:
            dispatcher.utter_message(text="Please provide the patient name to view appointments.")
            return []

        appointments = fetch_appointments(patient)
        if appointments:
            message = "Your upcoming appointments:\n" + "\n".join(
                f"- {appt.get('service')} with {appt.get('doctor_name')} on {appt.get('appointment_date')} at {appt.get('appointment_time')}"
                for appt in appointments
            )
            dispatcher.utter_message(text=message)
            send_notification_to_service({"event": "appointments.queried", "patient_name": patient, "count": len(appointments)})
        else:
            dispatcher.utter_message(text="No upcoming appointments found.")
        return []


class ActionSendNotification(Action):
    def name(self) -> Text:
        return "action_send_notification"

    def run(self, dispatcher: CollectingDispatcher, tracker: Tracker, domain: Dict[Text, Any]) -> List[EventType]:
        patient = tracker.get_slot("patient_name")
        if not patient:
            dispatcher.utter_message(text="Cannot send notification: patient name is missing.")
            return []
        send_notification_to_service({"event": "ad_hoc_notification", "patient_name": patient})
        dispatcher.utter_message(text=f"Notification request enqueued for {patient}.")
        return []
