package com.example.aaron.mapapp;

import android.content.Intent;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidGraphicFactory.createInstance(this.getApplication());
        setContentView(R.layout.activity_main);

        this.mapView = (MapView) findViewById(R.id.mapView);
        this.mapView.setClickable(true);
        this.mapView.getMapScaleBar().setVisible(true);
        this.mapView.setBuiltInZoomControls(true);
        this.mapView.setZoomLevelMin((byte) 10);
        this.mapView.setZoomLevelMax((byte) 20);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_import) {
            // Handle the import action
            Intent intent = new Intent()
                    .setType("*/*")
                    .setAction(Intent.ACTION_GET_CONTENT);

            startActivityForResult(Intent.createChooser(intent, "Select a file"), 123);

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 123 && resultCode == RESULT_OK) {
            Uri selectedFile = data.getData(); //The uri with the location of the file
            renderMap(selectedFile.getPath());
            //drawPolyline(mapView.getLayerManager().getLayers());
            //  ImageGatherer imageGatherer = new ImageGatherer(this.mapView);
            // imageGatherer.run();
        }
    }

    // move to own mapsforge to own activity class
    public void renderMap(String path) {
        // create a tile cache of suitable size
        TileCache tileCache = AndroidUtil.createTileCache(this, "mapcache",
                mapView.getModel().displayModel.getTileSize(), 1f,
                this.mapView.getModel().frameBufferModel.getOverdrawFactor());

        // tile renderer layer using internal render theme
        MapDataStore mapDataStore = new MapFile(new File(path));
        TileRendererLayer tileRendererLayer = new TileRendererLayer(tileCache, mapDataStore,
                this.mapView.getModel().mapViewPosition, AndroidGraphicFactory.INSTANCE) {
            @Override
            public boolean onTap(LatLong tapLatLong, org.mapsforge.core.model.Point layerXY, org.mapsforge.core.model.Point tapXY) {
                if (mapView.getLayerManager().getLayers().size() > 2) {
                    for (int i = 1; i < mapView.getLayerManager().getLayers().size(); i++) {
                        mapView.getLayerManager().getLayers().remove(i);
                    }
                    addMarker(mapView.getLayerManager().getLayers(), tapLatLong);

                    LatLong[] points = new LatLong[2];
                    points[0] = mapView.getLayerManager().getLayers().get(1).getPosition();
                    points[1] = mapView.getLayerManager().getLayers().get(2).getPosition();
                    drawPolyline(mapView.getLayerManager().getLayers(), points);
                } else {
                    addMarker(mapView.getLayerManager().getLayers(), tapLatLong);
                }
                return super.onTap(tapLatLong, layerXY, tapXY);
            }
        };
        tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER);

        // only once a layer is associated with a mapView the rendering starts
        this.mapView.getLayerManager().getLayers().add(tileRendererLayer);
        this.mapView.setCenter(mapDataStore.startPosition());
        this.mapView.setZoomLevel((byte) 15);

    }

    // move to mapview class
    public void drawPolyline(Layers layer, LatLong[] points) {
        Paint paint = AndroidGraphicFactory.INSTANCE.createPaint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(20);
        paint.setStyle(Style.STROKE);

        Polyline polyline = new Polyline(paint, AndroidGraphicFactory.INSTANCE);
        List<LatLong> latLongs = polyline.getLatLongs();
        for (LatLong latLong : points) {
            if (latLong != null) {
                latLongs.add(latLong);
            }
        }
        layer.add(polyline);
    }

    // move to class
    public void captureImage(int index) {
        android.graphics.Bitmap b = android.graphics.Bitmap.createBitmap(mapView.getWidth(), mapView.getHeight(), android.graphics.Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        mapView.draw(c);

        try {
            b.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, new FileOutputStream("/storage/emulated/0/AcquiredMapData/image" + index + ".jpg"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void gatherImages() {
        LatLong origPos = this.mapView.getModel().mapViewPosition.getCenter();

        for (int i = 0; i < 1500; i++) {
            origPos = new LatLong(origPos.latitude + 0.1, origPos.longitude + 0.1);
            this.mapView.setCenter(origPos);
            captureImage(i);
        }
    }

    public void addMarker(Layers layers, LatLong position) {
        TappableMarker positionmarker = new TappableMarker(R.drawable.ic_menu_mylocation, position);
        mapView.getLayerManager().getLayers().add(positionmarker);
    }

    private class TappableMarker extends Marker {
        public TappableMarker(int icon, LatLong localLatLong) {
            super(localLatLong, AndroidGraphicFactory.convertToBitmap(MainActivity.this.getApplicationContext().getResources().getDrawable(icon)),
                    (AndroidGraphicFactory.convertToBitmap(MainActivity.this.getApplicationContext().getResources().getDrawable(icon)).getWidth()) / 2,
                    -1 * (AndroidGraphicFactory.convertToBitmap(MainActivity.this.getApplicationContext().getResources().getDrawable(icon)).getHeight()) / 2);
        }
    }

}
