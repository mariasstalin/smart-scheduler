
import pandas as pd
import numpy as np

# Define the number of samples
num_samples = 1000

# Generate synthetic data
data = {
    "is_vip": np.random.choice([0, 1], size=num_samples, p=[0.9, 0.1]),
    "severity_level": np.random.randint(0, 10, size=num_samples),
    "acceptance_rate": np.random.uniform(0, 100, size=num_samples),
    "time_difference": np.random.randint(0, 5, size=num_samples),
    "time_consistency_std_dev": np.random.uniform(15, 120, size=num_samples)
}

df = pd.DataFrame(data)

# Engineer the priority score
# We'll create a score from 0 to 100
# 'is_vip' will have a significant impact
# 'severity_level' and 'acceptance_rate' are directly proportional
# 'time_difference' and 'time_consistency_std_dev' are inversely proportional

# Normalize the features to a 0-1 scale before calculating the score
df['severity_level_norm'] = df['severity_level'] / 10
df['acceptance_rate_norm'] = df['acceptance_rate'] / 100
df['time_difference_norm'] = (5 - df['time_difference']) / 5  # Inverted
df['time_consistency_std_dev_norm'] = (120 - df['time_consistency_std_dev']) / (120 - 15) # Inverted

# Define weights for each feature
weights = {
    "is_vip": 0.4,
    "severity_level": 0.2,
    "acceptance_rate": 0.2,
    "time_difference": 0.1,
    "time_consistency_std_dev": 0.1
}

# Calculate the priority score
df['priority_score'] = (
    df['is_vip'] * weights['is_vip'] +
    df['severity_level_norm'] * weights['severity_level'] +
    df['acceptance_rate_norm'] * weights['acceptance_rate'] +
    df['time_difference_norm'] * weights['time_difference'] +
    df['time_consistency_std_dev_norm'] * weights['time_consistency_std_dev']
) * 100

# Add some noise to make it more realistic
noise = np.random.normal(0, 5, size=num_samples)
df['priority_score'] = df['priority_score'] + noise
df['priority_score'] = np.clip(df['priority_score'], 0, 100)


# Drop the normalized columns
df = df.drop(columns=[
    'severity_level_norm',
    'acceptance_rate_norm',
    'time_difference_norm',
    'time_consistency_std_dev_norm'
])

# Save to CSV
df.to_csv('patient_data.csv', index=False)

print("Synthetic data generated and saved to patient_data.csv")
