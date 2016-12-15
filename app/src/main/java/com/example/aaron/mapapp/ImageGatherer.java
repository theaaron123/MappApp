package com.example.aaron.mapapp;

import android.graphics.Canvas;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.view.MapView;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by aaron on 07/11/16.
 */
public class ImageGatherer implements Runnable{

    MapView mapView;

    public ImageGatherer(MapView mapView) {
        this.mapView = mapView;
    }

    public void run() {
        gatherImages();
    }

    // move to class
    public void captureImage(int index)
    {
        android.graphics.Bitmap b = android.graphics.Bitmap.createBitmap(mapView.getWidth(), mapView.getHeight(),   android.graphics.Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        mapView.draw(c);

        try {
            b.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, new FileOutputStream("/storage/emulated/0/AcquiredMapData/image"+ index + ".jpg"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void gatherImages()
    {
        LatLong origPos = this.mapView.getModel().mapViewPosition.getCenter();

        for (int i = 0; i < 15; i++) {
            origPos = new LatLong(origPos.latitude + 0.02, origPos.longitude + 0.01);
            this.mapView.setCenter(origPos);
            captureImage(i);
        }
    }
}
