
import pandas as pd
import lightgbm as lgb
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_squared_error

def train_model():
    # Load the dataset
    df = pd.read_csv("train/patient_data.csv")

    # Define features and target
    features = ["is_vip", "severity_level", "acceptance_rate", "time_difference", "time_consistency_std_dev"]
    target = "priority_score"

    X = df[features]
    y = df[target]

    # Split data into training and testing sets
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

    # Initialize and train the LightGBM model
    params = {
        "objective": "regression",
        "metric": "rmse",
        "n_estimators": 1000,
        "learning_rate": 0.05,
        "feature_fraction": 0.9,
        "bagging_fraction": 0.8,
        "bagging_freq": 5,
        "verbose": -1,
        "n_jobs": -1,
        "seed": 42
    }

    model = lgb.LGBMRegressor(**params)

    model.fit(X_train, y_train,
              eval_set=[(X_test, y_test)],
              eval_metric="rmse",
              callbacks=[lgb.early_stopping(100)])

    # Evaluate the model
    y_pred = model.predict(X_test)
    mse = mean_squared_error(y_test, y_pred)
    rmse = np.sqrt(mse)
    print(f"RMSE on test set: {rmse}")

    # Save the model
    model.booster_.save_model("service/lgbm_model.txt")
    print("Model saved to lgbm_model.txt")
