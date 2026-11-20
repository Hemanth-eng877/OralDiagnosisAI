package com.oraldiagnosis.ai.tflite;

import android.content.Context;
import android.graphics.Bitmap;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class TFLiteInferenceManager {
    private Interpreter interpreter;
    private int inputImageWidth;
    private int inputImageHeight;
    private DataType inputDataType;
    private DataType outputDataType;
    private int[] outputShape;

    // Configurable class mapping in a single location
    private static final Map<Integer, String> CLASS_LABELS = new HashMap<Integer, String>() {{
        put(0, "Normal");
        put(1, "OSCC");
    }};

    public TFLiteInferenceManager(Context context, String modelPath) throws IOException {
        MappedByteBuffer modelFile = FileUtil.loadMappedFile(context, modelPath);
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(4); // Use multiple threads for faster inference
        interpreter = new Interpreter(modelFile, options);

        // Dynamically detect input tensor shape and type
        Tensor inputTensor = interpreter.getInputTensor(0);
        int[] inputShape = inputTensor.shape();
        inputImageHeight = inputShape[1];
        inputImageWidth = inputShape[2];
        inputDataType = inputTensor.dataType();

        // Dynamically detect output tensor shape and type
        Tensor outputTensor = interpreter.getOutputTensor(0);
        outputShape = outputTensor.shape();
        outputDataType = outputTensor.dataType();
    }

    public PredictionResult predict(Bitmap bitmap) throws Exception {
        if (interpreter == null) {
            throw new IllegalStateException("Interpreter is not initialized.");
        }

        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputImageWidth, inputImageHeight, true);

        int bytesPerChannel = inputDataType == DataType.FLOAT32 ? 4 : 1;
        java.nio.ByteBuffer inputBuffer = java.nio.ByteBuffer.allocateDirect(1 * inputImageWidth * inputImageHeight * 3 * bytesPerChannel);
        inputBuffer.order(java.nio.ByteOrder.nativeOrder());

        int[] intValues = new int[inputImageWidth * inputImageHeight];
        resizedBitmap.getPixels(intValues, 0, inputImageWidth, 0, 0, inputImageWidth, inputImageHeight);

        for (int val : intValues) {
            int r = (val >> 16) & 0xFF;
            int g = (val >> 8) & 0xFF;
            int b = val & 0xFF;

            if (inputDataType == DataType.FLOAT32) {
                inputBuffer.putFloat(r / 255.0f);
                inputBuffer.putFloat(g / 255.0f);
                inputBuffer.putFloat(b / 255.0f);
            } else {
                inputBuffer.put((byte) r);
                inputBuffer.put((byte) g);
                inputBuffer.put((byte) b);
            }
        }

        TensorBuffer outputBuffer = TensorBuffer.createFixedSize(outputShape, outputDataType);

        interpreter.run(inputBuffer, outputBuffer.getBuffer().rewind());

        float[] results = outputBuffer.getFloatArray();
        if (results == null || results.length == 0) {
            throw new Exception("Inference returned empty results.");
        }

        int maxIndex = -1;
        float maxConfidence = -1.0f;

        for (int i = 0; i < results.length; i++) {
            if (results[i] > maxConfidence) {
                maxConfidence = results[i];
                maxIndex = i;
            }
        }

        String label = CLASS_LABELS.containsKey(maxIndex) ? CLASS_LABELS.get(maxIndex) : "Unknown";
        // Ensure confidence is out of 100 as per app logic
        if (maxConfidence <= 1.0f) {
            maxConfidence *= 100.0f;
        }

        return new PredictionResult(label, maxConfidence);
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
    }

    public static class PredictionResult {
        public String label;
        public float confidence;

        public PredictionResult(String label, float confidence) {
            this.label = label;
            this.confidence = confidence;
        }
    }
}
