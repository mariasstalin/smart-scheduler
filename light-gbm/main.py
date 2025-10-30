import logging
from datetime import datetime

from flask import Flask, request, jsonify

from schema.predict import PredictModel
from service.predict import predict_priority_score

app = Flask("AcceptanceProbability")


def get_acceptance_probability():
    """

    :return:
    """
    payload = request.get_json()
    try:
        PredictModel.model_validate(payload)
    except ValueError as e:
        logging.exception(e)
        return jsonify({"status": "Failed", "error": "Payload validation failed"}), 400
    cancelled_slot_time = datetime.strptime(payload.get("cancelled_slot"), "%Y-%m-%d %H:%M")

    score = predict_priority_score(cancelled_slot_time, patient_data=payload["user_details"])
    return jsonify(score)


def health_check():
    return jsonify({"status": "ok"})


app.add_url_rule(rule="/predict", endpoint="predict", view_func=get_acceptance_probability, methods=["POST"])
app.add_url_rule(rule="/health", endpoint="health", view_func=health_check, methods=["GET"])


if __name__ == "__main__":
    app.run(debug=True)