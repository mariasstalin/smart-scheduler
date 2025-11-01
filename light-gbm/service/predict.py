from datetime import datetime

import pandas as pd
import lightgbm as lgb
from sklearn.metrics import accuracy_score

# Load the model
bst = lgb.Booster(model_file="service/lgbm_model.txt")


def predict_priority_score(cancelled_slot_time, patient_data):
    """
    Predicts the priority score for patients.

    Args:
        cancelled_slot_time (datetime): Cancelled slots datetime
        patient_data (list): A dictionary with the patient"s data.

    Returns:
        return user details with score
    """
    cancelled_slot_minutes = cancelled_slot_time.hour * 60 + cancelled_slot_time.minute
    feature_list = []
    for patient_info in patient_data:
        acceptance_rate = 0
        if patient_info.get("total_notifications_sent"):
            acceptance_rate = (patient_info["total_notifications_responded"] / patient_info["total_notifications_sent"]) * 100
        features = {"id": patient_info["id"], "is_vip": patient_info.get("is_vip"), "acceptance_rate": acceptance_rate,
                    "severity_level": patient_info.get("severity_level", 0)}
        history = patient_info["booking_history"]
        if not history:
            features["time_difference"] = 180
            features["time_consistency_std_dev"] = 90
        else:
            timestamps = pd.to_datetime(history, format="%Y-%m-%d %H:%M")
            minutes_from_midnight = (timestamps.hour * 60 + timestamps.minute).values
            avg_minutes = minutes_from_midnight.mean()

            features["time_difference"] = abs(cancelled_slot_minutes - avg_minutes)
            features["time_consistency_std_dev"] = minutes_from_midnight.std(ddof=0)
        feature_list.append(features)
    # Create a DataFrame from the input data
    df = pd.DataFrame(feature_list)

    # Ensure the order of columns is the same as in the training data
    features = ["is_vip", "severity_level", "acceptance_rate", "time_difference", "time_consistency_std_dev"]
    data = df[features]

    # Predict the score
    df["score"] = bst.predict(data)
    result = df.sort_values(by="score", ascending=False)
    return result.to_dict(orient="records")
