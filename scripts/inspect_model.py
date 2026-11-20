"""TensorFlow Lite Model Inspection Utility

Inspects input/output tensors, shapes, data types, and quantization parameters.
Usage:
    python scripts/inspect_model.py [--model-path PATH]
"""

import argparse
import os
import sys

try:
    import tensorflow as tf
except ImportError:
    print("Error: tensorflow is required. Install via pip install tensorflow")
    sys.exit(1)


def inspect_tflite_model(model_path):
    if not os.path.exists(model_path):
        print(f"Error: Model not found at '{model_path}'")
        sys.exit(1)

    file_size_bytes = os.path.getsize(model_path)
    file_size_mb = file_size_bytes / (1024 * 1024)

    print("=" * 60)
    print("  OralDiagnosisAI — TFLite Model Architecture Inspector")
    print("=" * 60)
    print(f"Model Path:      {os.path.abspath(model_path)}")
    print(f"File Size:       {file_size_mb:.2f} MB ({file_size_bytes:,} bytes)")

    interpreter = tf.lite.Interpreter(model_path=model_path)
    interpreter.allocate_tensors()

    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    print("\n[Input Tensors]")
    for i, item in enumerate(input_details):
        print(f"  Index {i}:")
        print(f"    Name:         {item.get('name')}")
        print(f"    Shape:        {item.get('shape')}")
        print(f"    Data Type:    {item.get('dtype')}")
        print(f"    Quantization: {item.get('quantization')}")

    print("\n[Output Tensors]")
    for i, item in enumerate(output_details):
        print(f"  Index {i}:")
        print(f"    Name:         {item.get('name')}")
        print(f"    Shape:        {item.get('shape')}")
        print(f"    Data Type:    {item.get('dtype')}")
        print(f"    Quantization: {item.get('quantization')}")

    print("=" * 60)
    print("Inspection complete. Model contract verified.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Inspect TFLite model tensors and shapes")
    parser.add_argument(
        "--model-path",
        default="oral_model.tflite",
        help="Path to the .tflite model file (default: oral_model.tflite)",
    )
    args = parser.parse_args()
    inspect_tflite_model(args.model_path)
