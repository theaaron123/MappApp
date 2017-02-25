package com.example.aaron.mapapp;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Build;
import org.tensorflow.contrib.android.Classifier;
import org.tensorflow.contrib.android.TensorFlowImageClassifier;

import java.util.List;

/**
 * Created by aaron on 16/02/17.
 */
public class TensorFlowHandler {

    private static final int NUM_CLASSES = 1008;
    private static final int INPUT_SIZE = 224;
    private static final int IMAGE_MEAN = 117;
    private static final float IMAGE_STD = 1;
    private static final String INPUT_NAME = "input:0";
    private static final String OUTPUT_NAME = "output:0";
    // TODO change model file
    private static final String MODEL_FILE = "file:///android_asset/tensorflow_inception_graph.pb";
    private static final String LABEL_FILE = "file:///android_asset/imagenet_comp_graph_label_strings.txt";

    private Classifier classifier;

    public void createClassifier(Activity activity) {
        try {
            classifier = TensorFlowImageClassifier.create(
                    activity.getAssets(),
                    MODEL_FILE,
                    LABEL_FILE,
                    NUM_CLASSES,
                    INPUT_SIZE,
                    IMAGE_MEAN,
                    IMAGE_STD,
                    INPUT_NAME,
                    OUTPUT_NAME);
        } catch (final Exception e) {
            throw new RuntimeException("Error initializing TensorFlow!", e);
        }
    }

    public List<Classifier.Recognition> classifyImage(Bitmap classifyBitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            classifyBitmap.setConfig(Bitmap.Config.ARGB_8888);
        }
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(classifyBitmap, INPUT_SIZE, INPUT_SIZE, false);
        return classifier.recognizeImage(scaledBitmap);
    }
}
