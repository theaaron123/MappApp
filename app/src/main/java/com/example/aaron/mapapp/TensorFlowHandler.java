package com.example.aaron.mapapp;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.SystemClock;
import org.tensorflow.contrib.android.Classifier;
import org.tensorflow.contrib.android.TensorFlowImageClassifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

    private static final String MODEL_FILE = "file:///android_asset/tensorflow_inception_graph.pb";
    private static final String LABEL_FILE =
            "file:///android_asset/imagenet_comp_graph_label_strings.txt";

    private Classifier classifier;

    private int previewWidth = 224;
    private int previewHeight = 224;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap;
    private boolean computing = false;
    private long lastProcessingTimeMs;


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

    public void classifyImage() {
        // TODO don't classify hardcoded cat image
        File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download/download.jpg");
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        try {
            croppedBitmap = BitmapFactory.decodeStream(new FileInputStream(f), null, options);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, 224, 224, false);

        //Runnable runnable = new Runnable() {
        //   @Override
        //public void run() {
        final long startTime = SystemClock.uptimeMillis();
        final List<Classifier.Recognition> results = classifier.recognizeImage(scaledBitmap);
        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
        computing = false;
        //  }
        // };
        // runnable.run();
    }
}
