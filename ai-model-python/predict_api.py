"""
predict_api.py
Flask REST API for disease prediction
Run:  python predict_api.py
Port: 5000
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
import pickle
import json
import numpy as np
import os
import logging

# ─────────────────────────────────────────────────────────────
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)
CORS(app)   # Allow calls from Spring Boot and frontend

# ─────────────────────────────────────────────────────────────
# Load ML artefacts at startup
# ─────────────────────────────────────────────────────────────
MODEL_DIR = os.path.join(os.path.dirname(__file__), 'model')

def load_artefacts():
    with open(os.path.join(MODEL_DIR, 'disease_model.pkl'), 'rb') as f:
        model = pickle.load(f)
    with open(os.path.join(MODEL_DIR, 'label_encoder.pkl'), 'rb') as f:
        le = pickle.load(f)
    with open(os.path.join(MODEL_DIR, 'symptoms_list.json')) as f:
        symptoms = json.load(f)
    with open(os.path.join(MODEL_DIR, 'disease_specialty.json')) as f:
        specialty_map = json.load(f)
    return model, le, symptoms, specialty_map

try:
    MODEL, LABEL_ENC, SYMPTOMS_LIST, SPECIALTY_MAP = load_artefacts()
    logger.info(f"✅ Model loaded. Knows {len(LABEL_ENC.classes_)} diseases.")
    logger.info(f"   Symptoms: {len(SYMPTOMS_LIST)}")
except Exception as e:
    logger.error(f"❌ Failed to load model: {e}")
    logger.error("   Run train_model.py first to generate model artefacts.")
    MODEL = None


# ─────────────────────────────────────────────────────────────
# HELPERS
# ─────────────────────────────────────────────────────────────
def symptoms_to_vector(symptom_names: list[str]) -> np.ndarray:
    """Convert list of symptom names to binary feature vector."""
    normalised = [s.lower().replace(' ', '_') for s in symptom_names]
    vec = [1 if sym in normalised else 0 for sym in SYMPTOMS_LIST]
    return np.array([vec])


# ─────────────────────────────────────────────────────────────
# ROUTES
# ─────────────────────────────────────────────────────────────
@app.route('/health', methods=['GET'])
def health():
    return jsonify({
        'status': 'ok',
        'model_loaded': MODEL is not None,
        'version': '1.0'
    })


@app.route('/symptoms', methods=['GET'])
def list_symptoms():
    """Return master list of supported symptoms."""
    return jsonify({
        'symptoms': [s.replace('_', ' ').title() for s in SYMPTOMS_LIST],
        'count': len(SYMPTOMS_LIST)
    })


@app.route('/predict-disease', methods=['POST'])
def predict_disease():
    """
    POST body:
    {
        "symptoms": ["Fever", "Cough", "Headache", "Fatigue"]
    }

    Response:
    {
        "predicted_disease": "Influenza",
        "confidence": 0.87,
        "recommended_specialty": "General Physician",
        "alternatives": [
            {"disease": "Common Cold", "probability": 0.08},
            {"disease": "COVID-19",    "probability": 0.05}
        ],
        "all_probabilities": { ... },
        "model_version": "1.0"
    }
    """
    if MODEL is None:
        return jsonify({'error': 'Model not loaded. Run train_model.py first.'}), 503

    data = request.get_json(silent=True)
    if not data or 'symptoms' not in data:
        return jsonify({'error': 'Request body must include "symptoms" array.'}), 400

    symptoms_input = data['symptoms']
    if not isinstance(symptoms_input, list) or len(symptoms_input) == 0:
        return jsonify({'error': '"symptoms" must be a non-empty array.'}), 400

    # Build feature vector
    vec = symptoms_to_vector(symptoms_input)
    matched = int(vec.sum())
    if matched == 0:
        return jsonify({
            'error': 'None of the provided symptoms are recognised.',
            'supported_symptoms': [s.replace('_', ' ').title() for s in SYMPTOMS_LIST]
        }), 400

    # Predict
    proba = MODEL.predict_proba(vec)[0]                      # shape: (n_classes,)
    classes = LABEL_ENC.classes_

    top_idx = int(np.argmax(proba))
    predicted = classes[top_idx]
    confidence = float(proba[top_idx])

    # Top-5 alternatives (excluding best)
    sorted_idx = np.argsort(proba)[::-1]
    alternatives = [
        {'disease': classes[i], 'probability': round(float(proba[i]), 4)}
        for i in sorted_idx[1:6]
        if float(proba[i]) > 0.01
    ]

    specialty = SPECIALTY_MAP.get(predicted, 'General Physician')

    logger.info(
        f"Prediction: {predicted} ({confidence:.2%}) | "
        f"Symptoms: {symptoms_input} | Specialty: {specialty}"
    )

    return jsonify({
        'predicted_disease':      predicted,
        'confidence':             round(confidence, 4),
        'recommended_specialty':  specialty,
        'alternatives':           alternatives,
        'symptoms_matched':       matched,
        'total_symptoms_input':   len(symptoms_input),
        'model_version':          '1.0'
    })


@app.route('/batch-predict', methods=['POST'])
def batch_predict():
    """Predict for multiple patients in one call."""
    if MODEL is None:
        return jsonify({'error': 'Model not loaded.'}), 503

    data = request.get_json(silent=True)
    if not data or 'patients' not in data:
        return jsonify({'error': 'Request body must include "patients" array.'}), 400

    results = []
    for patient in data['patients']:
        pid = patient.get('patient_id')
        symptoms_input = patient.get('symptoms', [])
        if not symptoms_input:
            results.append({'patient_id': pid, 'error': 'No symptoms provided.'})
            continue

        vec = symptoms_to_vector(symptoms_input)
        proba = MODEL.predict_proba(vec)[0]
        classes = LABEL_ENC.classes_
        top_idx = int(np.argmax(proba))
        predicted = classes[top_idx]

        results.append({
            'patient_id':            pid,
            'predicted_disease':     predicted,
            'confidence':            round(float(proba[top_idx]), 4),
            'recommended_specialty': SPECIALTY_MAP.get(predicted, 'General Physician')
        })

    return jsonify({'results': results})


# ─────────────────────────────────────────────────────────────
if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=False)
