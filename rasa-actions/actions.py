from typing import Any, Text, Dict, List
from rasa_sdk import Action, Tracker
from rasa_sdk.executor import CollectingDispatcher
from rasa_sdk.forms import FormValidationAction
from rasa_sdk.types import DomainDict
from rasa_sdk.events import SlotSet
from datetime import datetime
import re

# Mock doctor and service lists
DOCTORS = ["Dr. Smith", "Dr. John", "Dr. Brown", "Dr. Lee", "Dr. Taylor", "Dr. Adams"]
SERVICES = ["cardiology", "dentistry", "orthopedics", "neurology", "general checkup", "pediatrics"]

class ValidateAppointmentForm(FormValidationAction):
    def name(self) -> Text:
        return "validate_appointment_form"

    def validate_patient_name(
        self,
        slot_value: Any,
        dispatcher: CollectingDispatcher,
        tracker: Tracker,
        domain: DomainDict,
    ) -> Dict[Text, Any]:
        if slot_value:
            return {"patient_name": slot_value}
        dispatcher.utter_message(text="Please provide a valid patient name.")
        return {"patient_name": None}

    def validate_doctor_name(
        self,
        slot_value: Any,
        dispatcher: CollectingDispatcher,
        tracker: Tracker,
        domain: DomainDict,
    ) -> Dict[Text, Any]:
        if slot_value in DOCTORS:
            return {"doctor_name": slot_value}
        dispatcher.utter_message(text=f"Sorry, we don't have {slot_value}. Choose from {', '.join(DOCTORS)}.")
        return {"doctor_name": None}

    def validate_service(
        self,
        slot_value: Any,
        dispatcher: CollectingDispatcher,
        tracker: Tracker,
        domain: DomainDict,
    ) -> Dict[Text, Any]:
        if slot_value.lower() in SERVICES:
            return {"service": slot_value.lower()}
        dispatcher.utter_message(text=f"We offer {', '.join(SERVICES)}. Please select one.")
        return {"service": None}

    def validate_appointment_date(
        self,
        slot_value: Any,
        dispatcher: CollectingDispatcher,
        tracker: Tracker,
        domain: DomainDict,
    ) -> Dict[Text, Any]:
        try:
            dt = datetime.strptime(slot_value, "%Y-%m-%d")
            if dt.date() >= datetime.today().date():
                return {"appointment_date": slot_value}
            dispatcher.utter_message(text="Please select a future date.")
            return {"appointment_date": None}
        except:
            dispatcher.utter_message(text="Please provide the date in YYYY-MM-DD format.")
            return {"appointment_date": None}

    def validate_appointment_time(
        self,
        slot_value: Any,
        dispatcher: CollectingDispatcher,
        tracker: Tracker,
        domain: DomainDict,
    ) -> Dict[Text, Any]:
        try:
            datetime.strptime(slot_value, "%H:%M")
            return {"appointment_time": slot_value}
        except:
            dispatcher.utter_message(text="Please provide time in HH:MM format (24-hour).")
            return {"appointment_time": None}

    def validate_phone_number(
        self,
        slot_value: Any,
        dispatcher: CollectingDispatcher,
        tracker: Tracker,
        domain: DomainDict,
    ) -> Dict[Text, Any]:
        if re.fullmatch(r"\d{10}", slot_value):
            return {"phone_number": slot_value}
        dispatcher.utter_message(text="Please provide a valid 10-digit phone number.")
        return {"phone_number": None}

    def validate_email(
        self,
        slot_value: Any,
        dispatcher: CollectingDispatcher,
        tracker: Tracker,
        domain: DomainDict,
    ) -> Dict[Text, Any]:
        if re.fullmatch(r"[^@]+@[^@]+\.[^@]+", slot_value):
            return {"email": slot_value}
        dispatcher.utter_message(text="Please provide a valid email address.")
        return {"email": None}


class ActionSubmitAppointment(Action):
    def name(self) -> Text:
        return "action_submit_appointment"

    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker,
            domain: Dict[Text, Any]) -> List[Dict[Text, Any]]:

        patient_name = tracker.get_slot("patient_name")
        doctor_name = tracker.get_slot("doctor_name")
        service = tracker.get_slot("service")
        appointment_date = tracker.get_slot("appointment_date")
        appointment_time = tracker.get_slot("appointment_time")
        phone_number = tracker.get_slot("phone_number")
        email = tracker.get_slot("email")

        # Here you would call your backend API to store appointment
        dispatcher.utter_message(text=f"Appointment booked successfully for {patient_name} with {doctor_name} ({service}) on {appointment_date} at {appointment_time}. Contact: {phone_number}, {email}")

        # Clear slots after submission
        return [SlotSet("patient_name", None),
                SlotSet("doctor_name", None),
                SlotSet("service", None),
                SlotSet("appointment_date", None),
                SlotSet("appointment_time", None),
                SlotSet("phone_number", None),
                SlotSet("email", None)]
