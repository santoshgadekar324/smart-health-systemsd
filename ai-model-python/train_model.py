"""
train_model.py
AI-Powered Smart Health Diagnosis System
Trains a Random Forest classifier on symptom-disease data
"""

import pandas as pd
import numpy as np
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, accuracy_score
from sklearn.preprocessing import LabelEncoder
import pickle
import json
import os

# ─────────────────────────────────────────────────────────────
# SYMPTOM-DISEASE TRAINING DATA
# Each row: disease, then 1/0 for each symptom presence
# ─────────────────────────────────────────────────────────────
SYMPTOMS = [
    'fever', 'cough', 'headache', 'fatigue', 'shortness_of_breath',
    'chest_pain', 'nausea', 'vomiting', 'diarrhea', 'stomach_pain',
    'sore_throat', 'runny_nose', 'body_aches', 'chills', 'rash',
    'joint_pain', 'dizziness', 'swollen_lymph_nodes', 'loss_of_appetite',
    'night_sweats', 'blurred_vision', 'frequent_urination', 'excessive_thirst',
    'weight_loss', 'itching', 'swelling', 'back_pain', 'confusion',
    'palpitations', 'sneezing'
]

# Disease -> Specialization mapping
DISEASE_SPECIALTY = {
    'Common Cold':              'General Physician',
    'Influenza':                'General Physician',
    'COVID-19':                 'Pulmonologist',
    'Pneumonia':                'Pulmonologist',
    'Bronchitis':               'Pulmonologist',
    'Asthma':                   'Pulmonologist',
    'Dengue Fever':             'Infectious Disease Specialist',
    'Malaria':                  'Infectious Disease Specialist',
    'Typhoid':                  'Gastroenterologist',
    'Gastroenteritis':          'Gastroenterologist',
    'Migraine':                 'Neurologist',
    'Diabetes Type 2':          'Endocrinologist',
    'Hypertension':             'Cardiologist',
    'Heart Disease':            'Cardiologist',
    'Anemia':                   'Hematologist',
    'Urinary Tract Infection':  'Urologist',
    'Tuberculosis':             'Pulmonologist',
    'Chickenpox':               'Dermatologist',
    'Hepatitis':                'Gastroenterologist',
    'Allergy':                  'Allergist',
    'Arthritis':                'Rheumatologist',
    'Depression':               'Psychiatrist',
}

def build_dataset():
    """Create a labelled training dataset with symptom vectors."""

    # fmt: off  (symptom order matches SYMPTOMS list above)
    records = [
        # disease,              fever cough head  fat  sob   cp  naus vom  dia  stom sore run  body chill rash jnt  dizz lymp appt nswt bvis furi eth  wlss itch swel bp   conf palp snez
        ('Common Cold',            0,  1,   1,   1,   0,   0,   0,  0,   0,   0,   1,  1,   1,   1,   0,  0,   0,  0,   1,   0,   0,  0,  0,   0,   0,  0,  0,  0,  0,  1),
        ('Common Cold',            1,  1,   1,   1,   0,   0,   1,  0,   0,   0,   1,  1,   1,   1,   0,  0,   0,  0,   1,   0,   0,  0,  0,   0,   0,  0,  0,  0,  0,  1),
        ('Common Cold',            0,  1,   0,   1,   0,   0,   0,  0,   0,   0,   1,  1,   0,   0,   0,  0,   0,  0,   0,   0,   0,  0,  0,   0,   0,  0,  0,  0,  0,  1),
        ('Influenza',              1,  1,   1,   1,   0,   0,   1,  0,   0,   0,   1,  1,   1,   1,   0,  0,   1,  0,   1,   0,   0,  0,  0,   0,   0,  0,  0,  0,  0,  0),
        ('Influenza',              1,  1,   1,   1,   1,   0,   1,  1,   0,   0,   1,  1,   1,   1,   0,  0,   0,  0,   1,   0,   0,  0,  0,   0,   0,  0,  0,  0,  0,  0),
        ('COVID-19',               1,  1,   1,   1,   1,   1,   1,  0,   0,   0,   1,  0,   1,   1,   0,  0,   0,  0,   1,   0,   0,  0,  0,   0,   0,  0,  0,  0,  0,  0),
        ('COVID-19',               1,  1,   1,   1,   1,   0,   0,  0,   0,   0,   0,  0,   1,   1,   0,  0,   0,  0,   1,   0,   0,  0,  0,   0,   0,  0,  0,  0,  0,  0),
        ('COVID-19',               0,  1,   0,   1,   1,   0,   0,  0,   0,   0,   0,  0,   0,   0,   0,  0,   0,  0,   0,   0,   0,  0,  0,   0,   0,  0,  0,  0,  0,  0),
        ('Pneumonia',              1,  1,   1,   1,   1,   1,   0,  0,   0,   0,   0,  0,   1,   1,   0,  0,   0,  0,   1,   1,   0,  0,  0,   0,   0,  0,  0,  1,  0,  0),
        ('Pneumonia',              1,  1,   0,   1,   1,   1,   0,  0,   0,   0,   0,  0,   1,   1,   0,  0,   1,  0,   1,   0,   0,  0,  0,   0,   0,  0,  0,  1,  0,  0),
        ('Bronchitis',             1,  1,   0,   1,   1,   0,   0,  0,   0,   0,   1,  1,   1,   0,   0,  0,   0,  0,   0,   0,   0,  0,  0,   0,   0,  0,  0,  0,  0,  0),
        ('Asthma',                 0,  1,   0,   1,   1,   1,   0,  0,   0,   0,   0,  0,   0,   0,   0,  0,   0,  0,   0,   0,   0,  0,  0,   0,   0,  0,  0,  0,  1,  0),
        ('Dengue Fever',           1,  0,   1,   1,   0,   0,   1,  1,   0,   0,   0,  0,   1,   1,   1,  1,   0,  0,   1,   0,   0,  0,  0,   0,   0,  0,  0,  0,  0,  0),
        ('Dengue Fever',           1,  0,   1,   1,   0,   0,   1,  1,   0,   1,   0,  0,   1,   1,   1,  1,   1,  0,   1,   0,   0,  0,  0,   0,   0,  0,  0,  0,  0,  0),
        ('Malaria',                1,  0,   1,   1,   0,   0,   1,  1,   0,   0,   0,  0,   1,   1,   0,  1,   1,  0,   1,   1,   0,  0,  0,   0,   0,  0,  0,  1,  0,  0),
        ('Typhoid',                1,  0,   1,   1,   0,   0,   1,  0,   1,   1,   0,  0,   1,   0,   1,  0,   0,  0,   1,   1,   0,  0,  0,   0,   0,  0,  0,  1,  0,  0),
        ('Gastroenteritis',        1,  0,   1,   1,   0,   0,   1,  1,   1,   1,   0,  0,   1,   0,   0,  0,   0,  0,   1,   0,   0,  0,  0,   0,   0,  0,  0,  0,  0,  0),
        ('Gastroenteritis',        0,  0,   0,   1,   0,   0,   1,  1,   1,   1,   0,  0,   0,   0,   0,  0,   0,  0,   1,   0,   0,  0,  0,   0,   0,  0,  0,  0,  0,  0),
        ('Migraine',               0,  0,   1,   1,   0,   0,   1,  1,   0,   0,   0,  0,   0,   0,   0,  0,   1,  0,   0,   0,   1,  0,  0,   0,   0,  0,  0,  1,  0,  0),
        ('Migraine',               0,  0,   1,   1,   0,   0,   1,  0,   0,   0,   0,  0,   0,   0,   0,  0,   1,  0,   0,   0,   1,  0,  0,   0,   0,  0,  0,  0,  0,  0),
        ('Diabetes Type 2',        0,  0,   0,   1,   0,   0,   0,  0,   0,   0,   0,  0,   0,   0,   0,  0,   1,  0,   0,   0,   1,  1,  1,   1,   1,  0,  0,  1,  0,  0),
        ('Diabetes Type 2',        0,  0,   1,   1,   0,   0,   0,  0,   0,   0,   0,  0,   0,   0,   0,  0,   1,  0,   0,   0,   1,  1,  1,   1,   0,  1,  0,  0,  0,  0),
        ('Hypertension',           0,  0,   1,   1,   1,   1,   0,  0,   0,   0,   0,  0,   0,   0,   0,  0,   1,  0,   0,   0,   1,  0,  0,   0,   0,  0,  0,  0,  1,  0),
        ('Hypertension',           0,  0,   1,   0,   1,   1,   0,  0,   0,   0,   0,  0,   0,   0,   0,  0,   1,  0,   0,   0,   0,  0,  0,   0,   0,  0,  0,  1,  1,  0),
        ('Heart Disease',          0,  0,   0,   1,   1,   1,   1,  0,   0,   0,   0,  0,   0,   0,   0,  0,   1,  0,   0,   1,   0,  0,  0,   0,   0,  1,  0,  0,  1,  0),
        ('Heart Disease',          0,  0,   0,   1,   1,   1,   0,  0,   0,   0,   0,  0,   0,   0,   0,  0,   0,  0,   0,   1,   0,  0,  0,   0,   0,  0,  0,  0,  1,  0),
        ('Anemia',                 0,  0,   1,   1,   1,   0,   0,  0,   0,   0,   0,  0,   0,   0,   1,  0,   1,  0,   1,   0,   0,  0,  0,   0,   0,  0,  0,  1,  1,  0),
        ('Urinary Tract Infection',1,  0,   0,   1,   0,   0,   0,  0,   0,   1,   0,  0,   0,   0,   0,  0,   0,  0,   0,   0,   0,  1,  0,   0,   0,  0,  1,  0,  0,  0),
        ('Tuberculosis',           1,  1,   0,   1,   1,   1,   0,  0,   0,   0,   0,  0,   1,   1,   0,  0,   0,  1,   1,   1,   0,  0,  0,   1,   0,  0,  0,  0,  0,  0),
        ('Chickenpox',             1,  0,   1,   1,   0,   0,   0,  0,   0,   0,   0,  0,   1,   0,   1,  0,   0,  1,   1,   0,   0,  0,  0,   0,   1,  0,  0,  0,  0,  0),
        ('Hepatitis',              1,  0,   1,   1,   0,   0,   1,  1,   0,   1,   0,  0,   1,   0,   1,  1,   0,  0,   1,   0,   0,  0,  0,   1,   1,  0,  0,  0,  0,  0),
        ('Allergy',                0,  1,   1,   1,   0,   0,   0,  0,   0,   0,   0,  1,   0,   0,   1,  0,   0,  0,   0,   0,   1,  0,  0,   0,   1,  1,  0,  0,  0,  1),
        ('Arthritis',              0,  0,   1,   1,   0,   0,   0,  0,   0,   0,   0,  0,   0,   0,   0,  1,   0,  0,   0,   0,   0,  0,  0,   0,   0,  1,  1,  0,  0,  0),
        ('Depression',             0,  0,   1,   1,   0,   0,   0,  0,   0,   0,   0,  0,   0,   0,   0,  0,   1,  0,   1,   1,   0,  0,  0,   1,   0,  0,  0,  1,  0,  0),
    ]
    # fmt: on

    # Augment data with random noise to increase dataset size
    augmented = []
    rng = np.random.default_rng(42)
    for row in records:
        disease = row[0]
        vec = list(row[1:])
        augmented.append(row)
        for _ in range(8):                       # 8 variants per base record
            noisy = [min(1, max(0, v + rng.integers(-1, 2))) for v in vec]
            augmented.append(tuple([disease] + noisy))

    df = pd.DataFrame(augmented, columns=['disease'] + SYMPTOMS)
    return df


def train():
    print("🏥 Training Smart Health AI Model...")
    df = build_dataset()
    print(f"   Dataset size: {len(df)} records, {df['disease'].nunique()} diseases")

    X = df[SYMPTOMS].values
    y = df['disease'].values

    le = LabelEncoder()
    y_enc = le.fit_transform(y)

    X_train, X_test, y_train, y_test = train_test_split(
        X, y_enc, test_size=0.2, random_state=42, stratify=y_enc
    )

    model = RandomForestClassifier(
        n_estimators=200,
        max_depth=None,
        min_samples_split=2,
        random_state=42,
        n_jobs=-1,
    )
    model.fit(X_train, y_train)

    y_pred = model.predict(X_test)
    acc = accuracy_score(y_test, y_pred)
    print(f"\n✅ Test Accuracy: {acc:.4f} ({acc*100:.2f}%)")
    print("\nClassification Report:")
    print(classification_report(y_test, y_pred, target_names=le.classes_))

    # Save artefacts
    os.makedirs('model', exist_ok=True)
    with open('model/disease_model.pkl', 'wb') as f:
        pickle.dump(model, f)
    with open('model/label_encoder.pkl', 'wb') as f:
        pickle.dump(le, f)
    with open('model/symptoms_list.json', 'w') as f:
        json.dump(SYMPTOMS, f, indent=2)
    with open('model/disease_specialty.json', 'w') as f:
        json.dump(DISEASE_SPECIALTY, f, indent=2)

    print("\n💾 Model saved to model/ directory")
    print("   - model/disease_model.pkl")
    print("   - model/label_encoder.pkl")
    print("   - model/symptoms_list.json")
    print("   - model/disease_specialty.json")


if __name__ == '__main__':
    train()
