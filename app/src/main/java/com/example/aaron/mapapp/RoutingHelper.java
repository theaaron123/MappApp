package com.example.aaron.mapapp;

import android.graphics.Bitmap;

import com.graphhopper.PathWrapper;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.cache.TileCache;
import org.tensorflow.demo.Classifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class RoutingHelper {
    public static double classifyComplexity(MapsforgeFragment mapsforgeFragment, Set<Bitmap> routeBitmaps) {
        TensorFlowHandler tensorFlowHandler = new TensorFlowHandler();
        tensorFlowHandler.createClassifier(mapsforgeFragment.getActivity());
        double nonComplexValue = 0;
        double complexValue = 0;
        for (Bitmap bitmap : routeBitmaps) {
            List<Classifier.Recognition> recognitions = tensorFlowHandler.classifyImage(bitmap);
            for (Classifier.Recognition recognition : recognitions) {
                if (recognition.getTitle().contentEquals("NonComplex")) {
                    nonComplexValue += recognition.getConfidence();
                }
                if (recognition.getTitle().contentEquals("Complex")) {
                    complexValue += recognition.getConfidence();
                }
            }
        }
        return complexValue - nonComplexValue;
    }

    public static double classifyComplexityMultiThreaded(MapsforgeFragment mapsforgeFragment, Set<Bitmap> routeBitmaps) {
        double nonComplexValue = 0;
        double complexValue = 0;
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        List<Future<List<Classifier.Recognition>>> list = new ArrayList<>();

        for (Bitmap bitmap : routeBitmaps) {
            Callable<List<Classifier.Recognition>> callable = new TensorFlowHandler(mapsforgeFragment.getActivity(), bitmap);
            Future<List<Classifier.Recognition>> future = executorService.submit(callable);
            list.add(future);
        }
        for (Future<List<Classifier.Recognition>> recognitionFuture : list) {
            try {
                for (Classifier.Recognition recognition : recognitionFuture.get()) {
                    recognitionFuture.get();
                    if (recognition.getTitle().equalsIgnoreCase("nonComplex")) {
                        nonComplexValue += recognition.getConfidence();
                    }
                    if (recognition.getTitle().equalsIgnoreCase("complex")) {
                        complexValue += recognition.getConfidence();
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        executorService.shutdown();
        return complexValue - nonComplexValue;
    }

    public static Set<Bitmap> gatherRouteImages(MapView mapView, TileCache tileCache, PathWrapper pathWrapper) {
        ImageGatherer imageGatherer = new ImageGatherer(mapView);
        Set<Bitmap> bitmapArrayList = new HashSet<>();

        LatLong[] points = new LatLong[pathWrapper.getPoints().size()];
        for (int i = 0; i < pathWrapper.getPoints().size(); i++) {
            points[i] = new LatLong(pathWrapper.getPoints().getLatitude(i), pathWrapper.getPoints().getLongitude(i));
        }
        bitmapArrayList = imageGatherer.captureMapTiles(points, tileCache);
        return bitmapArrayList;
    }
}
