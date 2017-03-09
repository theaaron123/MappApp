package com.example.aaron.mapapp;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Build;
import org.tensorflow.demo.Classifier;
import org.tensorflow.demo.TensorFlowImageClassifier;


import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by aaron on 16/02/17.
 */
public class TensorFlowHandler implements Callable {

    private static final int NUM_CLASSES = 2;
    private static final int INPUT_SIZE = 299;
    private static final int IMAGE_MEAN = 128;
    private static final float IMAGE_STD = 128;
    private static final String INPUT_NAME = "Mul:0";
    private static final String OUTPUT_NAME = "final_result:0";
    private static final String MODEL_FILE = "file:///android_asset/graph_optimised.pb";
    private static final String LABEL_FILE = "file:///android_asset/output_labels.txt";

    private Classifier classifier;
    private Bitmap classifyBitmap;

    public TensorFlowHandler(){}

    public TensorFlowHandler(Activity activity, Bitmap classifyBitmap) {
        this.classifyBitmap = classifyBitmap;
        createClassifier(activity);
    }

    public void createClassifier(Activity activity) {
        try {
            classifier = TensorFlowImageClassifier.create(
                    activity.getAssets(),
                    MODEL_FILE,
                    LABEL_FILE,
                    INPUT_SIZE,
                    IMAGE_MEAN,
                    IMAGE_STD,
                    INPUT_NAME,
                    OUTPUT_NAME);
        } catch (final Exception e) {
            throw new RuntimeException("Error initializing TensorFlow!", e);
        }
    }

    public List<Classifier.Recognition> classifyImage() {
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(this.classifyBitmap, INPUT_SIZE, INPUT_SIZE, false);
        return classifier.recognizeImage(scaledBitmap);
    }
    public List<Classifier.Recognition> classifyImage(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //  classifyBitmap.setConfig(Bitmap.Config.ARGB_8888);
        }
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);
        return classifier.recognizeImage(scaledBitmap);
    }

    @Override
    public List<Classifier.Recognition> call() throws Exception {
        return classifyImage();
    }
}
