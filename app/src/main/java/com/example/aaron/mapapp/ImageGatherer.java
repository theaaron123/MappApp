package com.example.aaron.mapapp;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import org.mapsforge.core.graphics.GraphicContext;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.GraphicUtils;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.android.graphics.AndroidBitmap;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.queue.Job;
import org.mapsforge.map.util.LayerUtil;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    public Bitmap captureMapViewImage() {
        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(mapView.getWidth(), mapView.getHeight(), android.graphics.Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        mapView.draw(canvas);
        return  bitmap;
    }


    public Bitmap captureMapViewImage(LatLong latLong) {
        mapView.setCenter(latLong);
        mapView.setZoomLevel((byte) 18);
        mapView.repaint();

        AndroidBitmap androidBitmap = (AndroidBitmap) mapView.getFrameBuffer().getDrawingBitmap();
        Bitmap bitmap = AndroidGraphicFactory.getBitmap(androidBitmap);

        return  bitmap;
    }

    public void captureImageToDisk(int index)
    {
        Bitmap b = captureMapViewImage();
        saveImageToDisk(b, index);
    }

    private void saveImageToDisk(Bitmap bitmap, int fileIndex) {
        try {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, new FileOutputStream("/storage/emulated/0/AcquiredMapData/image"+ fileIndex + ".jpg"));
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
            captureImageToDisk(i);
        }
    }

    public List<Bitmap> captureMapTiles(TileCache tileCache) {
        List<Bitmap> bitmapArrayList = new ArrayList<>();
        Set<Tile> tiles = LayerUtil.getTiles(mapView.getBoundingBox(), (byte) 16, 768);

        for (Tile tile: tiles) {
            TileBitmap tileCacheImmediately = tileCache.get(new Job((tile.getParent()), false));
            bitmapArrayList.add(AndroidGraphicFactory.getBitmap(tileCacheImmediately));
        }
        return bitmapArrayList;
    }
    public Set<Bitmap> captureMapTiles(LatLong[] points, TileCache tileCache) {

        Set<Bitmap> bitmapArrayList = new HashSet<>();

        for(int i = 0; i < points.length; i++) {
            BoundingBox boundingBox = new BoundingBox(points[i].getLatitude(),
                    points[i].getLongitude(),
                    points[i].getLatitude(),
                    points[i].getLongitude());

            Set<Tile> tiles = LayerUtil.getTiles(boundingBox,  mapView.getModel().mapViewPosition.getZoomLevel(), 200);

            for (Tile tile: tiles) {
                TileBitmap tileCacheImmediatelyParent = tileCache.get(new Job((tile.getParent()), false));
                if (tileCacheImmediatelyParent != null) {
                    bitmapArrayList.add(AndroidGraphicFactory.getBitmap(tileCacheImmediatelyParent));
                }
                TileBitmap tileCacheImmediately = tileCache.get(new Job((tile), false));
                if (tileCacheImmediately != null) {
                    bitmapArrayList.add(AndroidGraphicFactory.getBitmap(tileCacheImmediately));
                }
            }
        }
        return bitmapArrayList;
    }
}
