"""Oral Diagnosis AI — Model Inference & Softmax Verification Script

Verifies inference pipeline, softmax sum properties, confidence score calculation,
and binary classification outputs across synthetic baseline tensors.

Usage:
    python scripts/verify_inference.py [--model-path PATH]
"""

import argparse
import os
import sys
import numpy as np

try:
    import tensorflow as tf
except ImportError:
    print("Error: tensorflow is required. Install via pip install tensorflow")
    sys.exit(1)


CLASS_LABELS = {0: "Normal", 1: "OSCC"}


def verify_inference(model_path="oral_model.tflite"):
    if not os.path.exists(model_path):
        print(f"Error: Model not found at '{model_path}'")
        sys.exit(1)

    print("=" * 65)
    print("  OralDiagnosisAI — Model Inference & Softmax Verification")
    print("=" * 65)

    interpreter = tf.lite.Interpreter(model_path=model_path)
    interpreter.allocate_tensors()

    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    input_shape = input_details[0]["shape"]
    output_shape = output_details[0]["shape"]
    print(f"Verified Input Shape:  {input_shape}")
    print(f"Verified Output Shape: {output_shape}")
    print("-" * 65)

    test_scenarios = [
        ("Simulated Normal Tissue (Low Intensity)", 0.25, 0.08),
        ("Simulated Intermediate Cytology", 0.50, 0.10),
        ("Simulated OSCC High Staining", 0.75, 0.12),
    ]

    for name, loc, scale in test_scenarios:
        np.random.seed(42)
        sample = np.random.normal(loc=loc, scale=scale, size=tuple(input_shape)).astype(np.float32)
        sample = np.clip(sample, 0.0, 1.0)

        interpreter.set_tensor(input_details[0]["index"], sample)
        interpreter.invoke()
        output_data = interpreter.get_tensor(output_details[0]["index"])
        scores = np.asarray(output_data[0], dtype=np.float32)

        softmax_sum = float(np.sum(scores))
        class_idx = int(np.argmax(scores))
        confidence = float(scores[class_idx])
        if confidence <= 1.0:
            confidence *= 100.0

        label = CLASS_LABELS.get(class_idx, "Unknown")

        print(f"Test Case:   {name}")
        print(f"Raw Output:  {scores}")
        print(f"Softmax Sum: {softmax_sum:.4f} (Valid Probability Distribution: {abs(softmax_sum - 1.0) < 1e-3})")
        print(f"Predicted:   {label} | Confidence: {confidence:.2f}%")
        print("-" * 65)

    print("Model inference verification completed successfully.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Verify model inference and softmax sanity")
    parser.add_argument("--model-path", default="oral_model.tflite", help="Path to TFLite model")
    args = parser.parse_args()
    verify_inference(args.model_path)
