# actions.py
from typing import Any, Dict, List, Text
from datetime import datetime, timedelta
from dateutil import parser
from rasa_sdk import Action, Tracker, FormValidationAction
from rasa_sdk.executor import CollectingDispatcher
from rasa_sdk.events import SlotSet, ActiveLoop, FollowupAction, EventType
import json 

# --- CONSTANTS & MOCK DATA ---
USER_APPOINTMENTS = {
    "+919876543210": [
        {"appointment_id": "A123", "service": "Dental Cleaning", "doctor": "Dr. Smith", "current_time": "2025-10-25 15:00"},
        {"appointment_id": "B234", "service": "Consultation", "doctor": "Dr. John", "current_time": "2025-10-26 14:00"},
    ]
}
BUSINESS_HOURS_START = 10
BUSINESS_HOURS_END = 18

# --- HELPER FUNCTIONS (Zoho/Backend API Simulation) ---

def _lookup_appointment_details(appointment_id: str) -> Dict[str, Any] | None:
    """Mocks looking up an appointment by its ID."""
    for appts in USER_APPOINTMENTS.values():
        for appt in appts:
            if appt["appointment_id"] == appointment_id:
                return appt
    return None

def is_valid_time(dt: datetime) -> bool:
    dt_naive = dt.replace(tzinfo=None)
    now_naive = datetime.now()
    is_future = dt_naive > now_naive
    is_within_hours = BUSINESS_HOURS_START <= dt_naive.hour <= BUSINESS_HOURS_END
    return is_future and is_within_hours

def next_available_slots(num_slots=3) -> List[str]:
    slots = []
    now = datetime.now()
    day = now
    while len(slots) < num_slots:
        for hour in range(BUSINESS_HOURS_START, BUSINESS_HOURS_END + 1):
            candidate = day.replace(hour=hour, minute=0, second=0, microsecond=0)
            if candidate > datetime.now():
                slots.append(candidate.strftime("%Y-%m-%d %H:%M"))
            if len(slots) >= num_slots:
                break
        day += timedelta(days=1)
    return slots

def call_zoho_reschedule_api(old_id, new_datetime):
    print(f"[API CALL] Rescheduling ID {old_id} to {new_datetime}...")
    if old_id == "FAIL123":
        return False, "API_ERROR"
    if new_datetime == "2025-10-20 10:00":
        return False, "SLOT_TAKEN"
    return True, "SUCCESS"

def call_deny_api(old_id):
    print(f"[API CALL] Denying slot for ID {old_id}. Making slot available...")
    return True

def call_cancel_api(appointment_id):
    print(f"[API CALL] Cancelling appointment ID {appointment_id}...")
    if appointment_id == "A999":
        return False
    return True

# ----------------------------------------------------------------------
# --- 1. UNIFIED APPOINTMENT LIST ACTION (RESCHEDULE & CANCEL) ---
# ----------------------------------------------------------------------

class ActionListAppointmentsWithButtons(Action):
    def name(self) -> Text:
        return "action_list_appointments_with_buttons"

    def run(self, dispatcher: CollectingDispatcher, tracker: Tracker, domain: Dict) -> List[Dict]:
        whatsapp_number = "+919876543210"
        appointments = USER_APPOINTMENTS.get(whatsapp_number, [])
        
        latest_intent = tracker.latest_message.get("intent", {}).get("name")
        
        if latest_intent == "reschedule_appointment":
            context = "reschedule"
            title_text = "Please select the appointment you want to **reschedule**:"
        elif latest_intent == "cancel_appointment":
            context = "cancel"
            title_text = "Please select the appointment you want to **cancel**:"
        else:
            dispatcher.utter_message(text="I'm not sure why you need a list of appointments. Please specify if you want to reschedule or cancel.")
            return [SlotSet("appointments_list", None), SlotSet("appointment_action_type", None)]

        if not appointments:
            dispatcher.utter_message(text=f"No upcoming appointments found to {context}.")
            return [SlotSet("appointments_list", None), SlotSet("appointment_action_type", None)]

        buttons = []
        msg = f"{title_text}\n"
        
        for idx, appt in enumerate(appointments, start=1):
            msg += f"**{idx}️⃣** {appt['service']} — {appt['doctor']} — {appt['current_time']}\n"
            
            buttons.append({
                "title": f"{appt['service']} on {appt['current_time']}",
                "payload": f'/provide_selection{{"appointment_selection": "{idx}"}}'
            })
            
        dispatcher.utter_message(text=msg, buttons=buttons)
        
        return [
            SlotSet("appointments_list", appointments),
            SlotSet("appointment_action_type", context)
        ]

# ----------------------------------------------------------------------
# --- 2. RESCHEDULE FORM & VALIDATION (Flow 1) ---
# ----------------------------------------------------------------------

class ValidateRescheduleForm(FormValidationAction):
    def name(self) -> Text:
        return "validate_reschedule_form"
        
    async def utter_ask_datetime(self, dispatcher: CollectingDispatcher, tracker: Tracker, domain: Dict) -> None:
        """Sends the utter_ask_datetime response when the form requests the 'datetime' slot."""
        dispatcher.utter_message(response="utter_ask_datetime")

    def validate_appointment_selection(self, slot_value: Any, dispatcher: CollectingDispatcher, tracker: Tracker, domain: Dict) -> Dict[Text, Any]:
        
        # --- Robustly extract selection from payload or slot_value ---
        text = tracker.latest_message.get("text")
        selection = None

        if text and text.startswith("/provide_selection"):
            try:
                # Slot value is ignored by the mapping, so we must extract selection from the text payload
                json_part = text.split("/provide_selection", 1)[1]
                payload_data = json.loads(json_part) 
                selection = payload_data.get("appointment_selection")
            except (json.JSONDecodeError, IndexError, TypeError):
                selection = slot_value
        
        if selection is None:
            selection = slot_value
        # --- END Robust extraction ---
        
        appointments = tracker.get_slot("appointments_list")
        
        if not appointments:
            dispatcher.utter_message(text="No appointments found. Please start the reschedule process again.")
            return {"appointment_selection": None, "appointment_id": None, "service": None, "requested_slot": None, "appointments_list": None}

        try:
            idx = int(str(selection)) - 1 
            if 0 <= idx < len(appointments):
                selected_appt = appointments[idx]
            else:
                raise ValueError
        except (ValueError, TypeError): 
            dispatcher.utter_message(text="Invalid selection. Please use the buttons or reply with the number of the appointment you want to reschedule.")
            return {"appointment_selection": None}
            
        # Success: Prepare slot updates. The Slot Mapping in domain.yml forces the form to continue.
        return {
            "appointment_selection": str(selection),
            "appointment_id": selected_appt["appointment_id"],
            "service": selected_appt["service"],
            "requested_slot": "datetime", # Signals the form what to ask next
        }


    def validate_datetime(self, slot_value: Any, dispatcher: CollectingDispatcher, tracker: Tracker, domain: Dict) -> Dict[Text, Any]:
        suggested_slots = tracker.get_slot("suggested_slots")

        if suggested_slots and str(slot_value).isdigit():
            idx = int(slot_value) - 1
            if 0 <= idx < len(suggested_slots):
                selected_time = suggested_slots[idx]
                return {"datetime": selected_time, "suggested_slots": None}
            else:
                dispatcher.utter_message(text="Invalid selection. Please pick one of the suggested slots (number).")
                return {"datetime": None}

        time_entity = next(tracker.get_latest_entity_values("time"), None)
        
        latest_intent = tracker.latest_message.get("intent", {}).get("name")
        if latest_intent in ["deny", "goodbye"]:
             dispatcher.utter_message(text="Understood. We'll stop the rescheduling process for now.")
             return {"datetime": None, "suggested_slots": None, "requested_slot": None}

        if not time_entity:
            return {"datetime": None}

        try:
            dt = parser.parse(time_entity)
        except Exception:
            dispatcher.utter_message(text="Invalid date/time format. Please try again.")
            return {"datetime": None}

        if not is_valid_time(dt):
            slots = next_available_slots()
            list_msg = "\n".join([f"{idx}️⃣ {s}" for idx, s in enumerate(slots, start=1)])
            msg = f"Sorry, that time is unavailable or in the past. Here are the next available slots:\n{list_msg}\nPlease reply with one of the suggested times (number) or try a new time."
            dispatcher.utter_message(text=msg)
            
            return {"datetime": None, "suggested_slots": slots}

        return {"datetime": dt.strftime("%Y-%m-%d %H:%M"), "suggested_slots": None}

class ActionSubmitRescheduleForm(Action):
    def name(self) -> Text:
        return "action_submit_reschedule_form"

    def run(self, dispatcher: CollectingDispatcher, tracker: Tracker, domain: Dict) -> List[Dict]:
        appointment_id = tracker.get_slot("appointment_id")
        service_name = tracker.get_slot("service")
        datetime_slot = tracker.get_slot("datetime")
        
        events = []
        
        if datetime_slot is not None:
            reschedule_success, reason = call_zoho_reschedule_api(appointment_id, datetime_slot)

            if not reschedule_success:
                if reason == "SLOT_TAKEN":
                    dispatcher.utter_message(text="I'm sorry, that specific time has just been taken. Please try to select a new time.")
                else:
                    dispatcher.utter_message(text="Something went wrong with the rescheduling process. Please contact support.")
                return events 

            dispatcher.utter_message(
                text=f"✅ Your {service_name} appointment has been rescheduled to **{datetime_slot}**."
            )

        # Clear all slots related to the flow
        events.extend([
            SlotSet("appointments_list", None),
            SlotSet("appointment_selection", None),
            SlotSet("appointment_id", None),
            SlotSet("service", None),
            SlotSet("datetime", None),
            SlotSet("suggested_slots", None),
            SlotSet("requested_slot", None),
            SlotSet("appointment_action_type", None),
        ])
        return events
        
# ----------------------------------------------------------------------
# --- 3. SLOT OFFER CONFIRMATION ACTIONS (Flow 2) ---
# ----------------------------------------------------------------------

class ActionSetSlotOfferDetails(Action): 
    def name(self) -> Text:
        return "action_set_slot_offer_details"

    async def run(self, dispatcher: CollectingDispatcher, tracker: Tracker, domain: Dict[Text, Any]) -> List[Dict[Text, Any]]:
        new_slot = tracker.get_slot("new_slot_datetime")
        old_id = tracker.get_slot("old_appointment_id")
        unique_offer_id = tracker.get_slot("slot_offer_id")

        if not (new_slot and old_id and unique_offer_id):
            dispatcher.utter_message(text="Error: Missing external offer details. Cannot process.")
            return [SlotSet("new_slot_datetime", None), SlotSet("old_appointment_id", None), SlotSet("slot_offer_id", None)]

        appt_details = _lookup_appointment_details(old_id)
        old_time = appt_details.get("current_time") if appt_details else "an unknown time"
        
        events = [
            SlotSet("old_appointment_id", old_id),
            SlotSet("slot_offer_id", unique_offer_id),
            SlotSet("old_appointment_datetime", old_time),
            ActiveLoop("slot_offer_confirmation_form"),
            FollowupAction("utter_ask_slot_confirmation")
        ]
        return events
        
class ActionConfirmSlotOffer(Action): 
    def name(self) -> Text:
        return "action_confirm_slot_offer"

    async def run(self, dispatcher: CollectingDispatcher, tracker: Tracker, domain: Dict[Text, Any]) -> List[Dict[Text, Any]]:
        old_id = tracker.get_slot("old_appointment_id")
        new_datetime = tracker.get_slot("new_slot_datetime") 
        old_time = tracker.get_slot("old_appointment_datetime")
        
        events: List[EventType] = [ActiveLoop(None)] 

        if not old_id or not new_datetime:
            dispatcher.utter_message(text="I can't complete the confirmation as the required information is missing. Please contact staff.")
            events.append(FollowupAction("utter_goodbye"))
            return events

        reschedule_success, reason = call_zoho_reschedule_api(old_id, new_datetime)
        
        if reschedule_success:
            confirmation_message = (
                f"Great! Your appointment (originally {old_time}) has been successfully "
                f"moved to **{new_datetime}**. The system has been updated."
            )
            dispatcher.utter_message(text=confirmation_message)
        else:
            if reason == "SLOT_TAKEN":
                dispatcher.utter_message(text="I'm sorry, that specific time has just been taken. We will search for the next best time for you.")
            else: 
                dispatcher.utter_message(
                    text="I apologize, there was an issue updating your slot due to a system error. A staff member will contact you shortly."
                )
        
        events.extend([
            SlotSet("new_slot_datetime", None), SlotSet("old_appointment_id", None),
            SlotSet("slot_offer_id", None), SlotSet("old_appointment_datetime", None),
            SlotSet("requested_slot", None), FollowupAction("utter_goodbye")
        ])
        return events


class ActionDenySlotOffer(Action): 
    def name(self) -> Text:
        return "action_deny_slot_offer"

    async def run(self, dispatcher: CollectingDispatcher, tracker: Tracker, domain: Dict[Text, Any]) -> List[Dict[Text, Any]]:
        old_id = tracker.get_slot("old_appointment_id")
        call_deny_api(old_id)

        dispatcher.utter_message(
            text="Understood. We will make this slot available for another patient. "
                 "We'll notify you if another opening occurs that suits you."
        )

        events: List[EventType] = [
            ActiveLoop(None),
            SlotSet("new_slot_datetime", None), SlotSet("old_appointment_id", None),
            SlotSet("slot_offer_id", None), SlotSet("old_appointment_datetime", None),
            SlotSet("requested_slot", None), FollowupAction("utter_goodbye")
        ]
        return events

# ----------------------------------------------------------------------
# --- 4. CANCEL APPOINTMENT ACTIONS (Flow 3) ---
# ----------------------------------------------------------------------

class ActionCancelAppointment(Action):
    def name(self) -> Text:
        return "action_cancel_appointment"

    def run(self, dispatcher: CollectingDispatcher, tracker: Tracker, domain: Dict) -> List[Dict]:
        appointments = tracker.get_slot("appointments_list")
        context = tracker.get_slot("appointment_action_type")
        
        if context != "cancel":
            dispatcher.utter_message(text="I can't perform cancellation with the current context.")
            return [SlotSet("appointment_action_type", None), SlotSet("appointments_list", None)]

        # --- Get selection from the latest message payload ---
        latest_message_text = tracker.latest_message.get("text")
        selection = None
        
        if latest_message_text and latest_message_text.startswith("/provide_selection"):
            try:
                json_part = latest_message_text.split("/provide_selection", 1)[1]
                payload_data = json.loads(json_part) 
                selection = payload_data.get("appointment_selection")
            except (json.JSONDecodeError, IndexError, TypeError):
                 selection = None
        
        if not appointments or not selection:
             dispatcher.utter_message(text="Sorry, I lost the appointment context. Please try starting the cancellation again.")
             return [SlotSet("appointment_action_type", None), SlotSet("appointments_list", None), SlotSet("appointment_selection", None)]
        
        try:
            idx = int(selection) - 1
            if 0 <= idx < len(appointments):
                selected_appt = appointments[idx]
                appointment_id = selected_appt["appointment_id"]
                service_name = selected_appt["service"]
            else:
                raise ValueError("Selection index out of bounds")
        
        except (ValueError, IndexError):
            dispatcher.utter_message(text="Invalid selection. Please try again.")
            return []

        cancellation_success = call_cancel_api(appointment_id)

        if not cancellation_success:
            dispatcher.utter_message(
                text="I apologize, there was an issue attempting to cancel your appointment. "
                     "A staff member has been alerted and will call you to confirm cancellation."
            )
            return [] 

        dispatcher.utter_message(
            text=f"✅ Your {service_name} appointment (ID: {appointment_id}) has been successfully **cancelled**."
        )

        # Clear all related slots
        return [
            SlotSet("appointments_list", None), SlotSet("appointment_selection", None), 
            SlotSet("appointment_id", None), SlotSet("service", None), 
            SlotSet("requested_slot", None), SlotSet("appointment_action_type", None),
            FollowupAction("utter_goodbye")
        ]