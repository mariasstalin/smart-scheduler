from datetime import datetime

from flask import Flask, request, jsonify

from service.predict import predict_priority_score

app = Flask("AcceptanceProbability")


def get_acceptance_probability():
    """

    :return:
    """
    payload = request.get_json()
    cancelled_slot_time = datetime.strptime(payload["cancelledSlot"], "%Y-%m-%d %H:%M")

    score = predict_priority_score(cancelled_slot_time, patient_data=payload["userDetails"])
    return jsonify(score)


app.add_url_rule(rule="/predict", endpoint="predict-batch", view_func=get_acceptance_probability, methods=["POST"])


if __name__ == "__main__":
    app.run(debug=True)