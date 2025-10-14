from typing import Any, Text, Dict, List
from rasa_sdk import Action, Tracker, FormValidationAction
from rasa_sdk.executor import CollectingDispatcher
from rasa_sdk.types import DomainDict
from rasa_sdk.events import SlotSet

# --- Scenario 1: Proactive Notification Handler ---

class ActionHandleSlotConfirmation(Action):
    def name(self) -> Text:
        return "action_handle_slot_confirmation"

    def run(
        self,
        dispatcher: CollectingDispatcher,
        tracker: Tracker,
        domain: Dict[Text, Any],
    ) -> List[Dict[Text, Any]]:

        # Get the new slot time from the previously set slot
        new_slot_time = tracker.get_slot("temp_new_slot_datetime")

        # Check user's response (affirm or deny)
        if tracker.get_intent_of_latest_message() == "affirm":
            # LOGIC: Call Spring Boot Notification Service to confirm the slot
            response = f"Great! Your new slot on **{new_slot_time}** has been confirmed (DUMMY RESPONSE). You will receive an SMS shortly."
        else: # deny or anything else
            # LOGIC: Call Spring Boot Notification Service to reject the slot
            response = f"Understood. The slot on {new_slot_time} will not be confirmed (DUMMY RESPONSE). Please reach out if you need further help."

        dispatcher.utter_message(text=response)

        # Clear the temporary slot to reset for next proactive event
        return [SlotSet("temp_new_slot_datetime", None)]

# --- Scenario 2: Cancel Appointment Form Validation and Action ---

class ValidateCancelForm(FormValidationAction):
    def name(self) -> Text:
        return "validate_cancel_form"

    async def validate_cancel_datetime(
        self,
        slot_value: Any,
        dispatcher: CollectingDispatcher,
        tracker: Tracker,
        domain: DomainDict,
    ) -> Dict[Text, Any]:

        # This method relies on the standard Rasa slot filling via the 'time' entity.
        if slot_value:
            # Add complex business logic here (e.g., check if an appointment exists at this time)
            return {"cancel_datetime": slot_value}

        # If no time was extracted, request it from the user (standard fallback)
        dispatcher.utter_message(response="utter_ask_cancel_datetime")
        return {"cancel_datetime": None}


class ActionCancelAppointment(Action):
    def name(self) -> Text:
        return "action_cancel_appointment"

    def run(
        self,
        dispatcher: CollectingDispatcher,
        tracker: Tracker,
        domain: Dict[Text, Any],
    ) -> List[Dict[Text, Any]]:

        cancel_time = tracker.get_slot("cancel_datetime")

        # LOGIC: Call Spring Boot Appointment Service API to cancel the booking

        # Simple formatting for response visibility
        try:
             # Attempt to parse ISO format (e.g., 2025-10-20T10:00:00.000+05:30)
             time_parts = cancel_time.split('T')
             date_part = time_parts[0]
             time_part = time_parts[1].split(':')[0] + ':' + time_parts[1].split(':')[1].split('.')[0]
             time_display = f"{date_part} at {time_part}"
        except:
             time_display = cancel_time # Fallback to the raw text (e.g., "next Monday")

        response = f"Your appointment scheduled for **{time_display}** has been **cancelled successfully (DUMMY RESPONSE)**."
        dispatcher.utter_message(text=response)

        # Clear the slot after successful action
        return [SlotSet("cancel_datetime", None)]


# --- Scenario 3: Reschedule Appointment Form Validation and Action ---

class ValidateRescheduleForm(FormValidationAction):
    def name(self) -> Text:
        return "validate_reschedule_form"

    def validate(
        self,
        slot_value: Any,
        dispatcher: CollectingDispatcher,
        tracker: Tracker,
        domain: DomainDict,
    ) -> Dict[Text, Any]:

        # Rule for interruption: If the user suddenly intents to cancel
        if tracker.get_intent_of_latest_message() == "cancel_appointment":
            return {"requested_slot": None} # Stop the form and let RulePolicy handle cancellation

        return super().validate(slot_value, dispatcher, tracker, domain)

    # --- Validation for the current (original) appointment time ---
    async def validate_current_datetime(
        self,
        slot_value: Any,
        dispatcher: CollectingDispatcher,
        tracker: Tracker,
        domain: DomainDict,
    ) -> Dict[Text, Any]:

        # FIX: Force the slot to be filled with the full user text when the bot is asking.
        # This overrides potential partial entity extraction by NLU.
        text_of_latest_message = tracker.latest_message.get("text")

        if text_of_latest_message:
            # Set the full text as the slot value
            return {"current_datetime": text_of_latest_message}

        # If no text (e.g., user sent an attachment), prompt the user
        dispatcher.utter_message(response="utter_ask_current_datetime")
        return {"current_datetime": None}


    # --- Validation for the new appointment time ---
    async def validate_new_datetime(
        self,
        slot_value: Any,
        dispatcher: CollectingDispatcher,
        tracker: Tracker,
        domain: DomainDict,
    ) -> Dict[Text, Any]:

        # FIX: Force the slot to be filled with the full user text when the bot is asking.
        # This overrides potential partial entity extraction by NLU.
        text_of_latest_message = tracker.latest_message.get("text")

        if text_of_latest_message:
            # Set the full text as the slot value
            return {"new_datetime": text_of_latest_message}

        # If no text, request it again
        dispatcher.utter_message(response="utter_ask_new_datetime")
        return {"new_datetime": None}


class ActionRescheduleAppointment(Action):
    def name(self) -> Text:
        return "action_reschedule_appointment"

    def run(
        self,
        dispatcher: CollectingDispatcher,
        tracker: Tracker,
        domain: Dict[Text, Any],
    ) -> List[Dict[Text, Any]]:

        current_time = tracker.get_slot("current_datetime")
        new_time = tracker.get_slot("new_datetime")

        # LOGIC: Call Spring Boot Appointment Service API to reschedule the booking

        # Since validation forces the full text, this line should now correctly print
        # the entire date/time phrase as entered by the user.
        response = f"Your appointment originally scheduled for **{current_time}** has been **rescheduled to {new_time} (DUMMY RESPONSE)**."
        dispatcher.utter_message(text=response)

        # Clear slots after successful action
        return [
            SlotSet("current_datetime", None),
            SlotSet("new_datetime", None)
        ]