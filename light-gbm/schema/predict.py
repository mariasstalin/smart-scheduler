from datetime import datetime
from pydantic import BaseModel, Field


class UserDetailsModel(BaseModel):
    id: str
    is_vip: int
    severity_level: int
    total_notifications_responded: int
    total_notifications_sent: int
    booking_history: list[datetime]


class PredictModel(BaseModel):
    cancelled_slot: str = Field()
    user_details: list[UserDetailsModel]
