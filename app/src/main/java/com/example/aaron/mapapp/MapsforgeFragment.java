package com.example.aaron.mapapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.*;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import com.graphhopper.util.Parameters;
import org.mapsforge.core.graphics.*;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.ExternalRenderTheme;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.PathWrapper;
import com.graphhopper.util.Constants;
import com.graphhopper.util.Parameters.Algorithms;
import com.graphhopper.util.Parameters.Routing;
import com.graphhopper.util.StopWatch;
import org.tensorflow.demo.Classifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.*;

import static android.app.Activity.RESULT_OK;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MapsforgeFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class MapsforgeFragment extends Fragment implements View.OnClickListener {
    private MapView mapView;
    private TileCache tileCache;
    private Uri selectedFile;
    private GraphHopper hopper;
    private LatLong start;
    private LatLong end;
    private File mapsDir;
    private File mapArea;
    private volatile boolean shortestPathRunning = false;
    private float time = 0;

    // TODO move public and private methods into order and review access modifiers.
    // TODO document methods

    public MapsforgeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidGraphicFactory.createInstance(getActivity().getApplication());
        mapsDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "/graphhopper/maps/");
        if (getArguments().containsKey("PATH")) {
            selectedFile = Uri.parse(getArguments().getString("PATH"));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_mapsforge, container, false);

        this.mapView = (MapView) view.findViewById(R.id.mapView);
        this.mapView.setClickable(true);
        this.mapView.getMapScaleBar().setVisible(true);
        this.mapView.setBuiltInZoomControls(true);
        this.mapView.setZoomLevelMin((byte) 10);
        this.mapView.setZoomLevelMax((byte) 20);

        Button b = (Button) view.findViewById(R.id.buttonGPS);
        b.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonGPS:
                getGPSLocation();
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 123 && resultCode == RESULT_OK) {
            selectedFile = data.getData(); //The uri with the location of the file
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (selectedFile != null) {
            renderMap(selectedFile.getPath());
        }
    }

    @Override
    public void onDestroy() {
        this.mapView.destroyAll();
        AndroidGraphicFactory.clearResourceMemoryCache();
        super.onDestroy();
    }

    /**
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    public void renderMap(String path) {
        mapView.getModel().displayModel.setFixedTileSize(256);
        time = SystemClock.currentThreadTimeMillis();
        mapArea = new File(path);
        // create a tile cache of suitable size
        tileCache = AndroidUtil.createTileCache(this.getActivity(), "mapcache",
                256, 1f,
                1.3);

        // tile renderer layer using specified render theme
        MapDataStore mapDataStore = new MapFile(new File(path));
        final TileRendererLayer tileRendererLayer = new TileRendererLayer(tileCache, mapDataStore,
                this.mapView.getModel().mapViewPosition, AndroidGraphicFactory.INSTANCE) {
            @Override
            public boolean onTap(LatLong tapLatLong, org.mapsforge.core.model.Point layerXY, org.mapsforge.core.model.Point tapXY) {
                // single tap removes markers
                start = null;
                end = null;
                removeLayersOnMap();
                return super.onTap(tapLatLong, layerXY, tapXY);
            }

            public boolean onLongPress(LatLong tapLatLong, Point layerXY, Point tapXY) {
                if (shortestPathRunning) {
                    logDisplayToUser("Calculation still in progress");
                    return false;
                }
                if (start != null && end == null) {
                    end = tapLatLong;
                    shortestPathRunning = true;
                    addMarker(mapView.getLayerManager().getLayers(), tapLatLong);
                    calcPath(start.getLatitude(), start.getLongitude(), end.getLatitude(), end.getLongitude());
                } else {
                    start = tapLatLong;
                    end = null;
                    removeLayersOnMap();
                    addMarker(mapView.getLayerManager().getLayers(), tapLatLong);
                }
                return true;
            }
        };

        XmlRenderTheme renderTheme = null;

        try {
            renderTheme = new ExternalRenderTheme("Elevate.xml");
            // TODO try pull from server?
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (renderTheme == null) {
            tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER);
        } else {
            tileRendererLayer.setXmlRenderTheme(renderTheme);
        }

        // only once a layer is associated with a mapView the rendering starts
        this.mapView.getLayerManager().getLayers().add(tileRendererLayer);
        this.mapView.setCenter(mapDataStore.startPosition());
        this.mapView.setZoomLevel((byte) 15);
        // storage of routing information
        loadGraphStorage();
        writeSharedPreferene(path, path);
    }

    public void drawPolyline(Layers layer, LatLong[] points, Color color) {
        Paint paint = AndroidGraphicFactory.INSTANCE.createPaint();
        paint.setColor(color);
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

    public void addMarker(Layers layers, LatLong position) {
        MapsforgeFragment.TappableMarker tappableMarker = new MapsforgeFragment.TappableMarker(R.drawable.ic_menu_mylocation, position);
        mapView.getLayerManager().getLayers().add(tappableMarker);
    }

    private void removeLayersOnMap() {
        // TODO doesn't always remove all other layers.
        for (int i = 1; i < mapView.getLayerManager().getLayers().size(); i++) {
            mapView.getLayerManager().getLayers().remove(i);
        }
    }

    private void writeSharedPreferene(String key, String val) {
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, val);
        editor.commit();
    }

    private String readSharedPreferenceString(String key) {
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        String defaultVal = "";
        String val = sharedPref.getString(key, defaultVal);
        return val;
    }

    private Boolean readSharedPreferenceBool(String key) {
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        Boolean defaultVal = false;
        Boolean val = sharedPref.getBoolean(key, defaultVal);
        return val;
    }

    private class TappableMarker extends Marker {
        @Override
        public boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
            double centerX = layerXY.x + getHorizontalOffset();
            double centerY = layerXY.y + getVerticalOffset();

            double radiusX = (getBitmap().getWidth() / 2) * 1.1;
            double radiusY = (getBitmap().getHeight() / 2) * 1.1; // 10% margin on tap

            double distX = Math.abs(centerX - tapXY.x);
            double distY = Math.abs(centerY - tapXY.y);

            if (distX < radiusX && distY < radiusY) {
                Toast.makeText(mapView.getContext(), "Location: " + tapLatLong, Toast.LENGTH_LONG).show();
                return true;
            }
            return false;
        }

        public TappableMarker(int icon, LatLong localLatLong) {
            super(localLatLong, AndroidGraphicFactory.convertToBitmap(MapsforgeFragment.this.getResources().getDrawable(icon)),
                    AndroidGraphicFactory.convertToBitmap(MapsforgeFragment.this.getResources().getDrawable(icon)).getWidth() / 2,
                    -1 * (AndroidGraphicFactory.convertToBitmap(MapsforgeFragment.this.getResources().getDrawable(icon)).getHeight()) / 2);
        }
    }

    public void calcPath(final double fromLat, final double fromLon,
                         final double toLat, final double toLon) {

        log("calculating route ...");
        new AsyncTask<Void, Void, PathWrapper>() {
            float time;

            protected PathWrapper doInBackground(Void... v) {
                StopWatch sw = new StopWatch().start();
                GHRequest req = new GHRequest(fromLat, fromLon, toLat, toLon).setAlgorithm(Algorithms.ALT_ROUTE);
                req.getHints().put(Routing.INSTRUCTIONS, "false");
                req.getHints().put(Parameters.CH.DISABLE, "true");
                req.getHints().put(Parameters.CH.INIT_DISABLING_ALLOWED, "true");

                GHResponse resp = hopper.route(req);

                if (readSharedPreferenceBool("EnhancedRouting") == true) {
                    for (PathWrapper pathWrapper : resp.getAll()) {
                        pathWrapper.setRouteWeight(pathWrapper.getRouteWeight() - classifyComplexity(gatherRouteImages(pathWrapper)));
                    }
                }
                time = sw.stop().getSeconds();
                return resp.getBest();
            }

            protected void onPostExecute(PathWrapper resp) {
                if (!resp.hasErrors()) {
                    log("from:" + fromLat + "," + fromLon + " to:" + toLat + ","
                            + toLon + " found route with distance:" + resp.getDistance()
                            / 1000f + ", nodes:" + resp.getPoints().getSize() + ", time:"
                            + time + " " + resp.getDebugInfo());
                    logDisplayToUser("the route is " + (int) (resp.getDistance() / 160) / 10f
                            + " miles long, walking time:" + resp.getTime() / 60000f
                            + "min, calculation time:" + time + " seconds");
                    LatLong[] points = new LatLong[resp.getPoints().size()];

                    for (int i = 0; i < resp.getPoints().size(); i++) {
                        points[i] = new LatLong(resp.getPoints().getLatitude(i), resp.getPoints().getLongitude(i));
                    }

                    drawPolyline(mapView.getLayerManager().getLayers(), points, Color.RED);
                } else {
                    logDisplayToUser("Error:" + resp.getErrors());
                }
                shortestPathRunning = false;
            }
        }.execute();
    }

    public void getGPSLocation() {
        LocationManager locationManager = (LocationManager) super.getActivity().getSystemService(Context.LOCATION_SERVICE);
        LocationListener myLocationListener = new MyLocationListener();

        Location lastKnownLocation = locationManager.getLastKnownLocation("GPS");
        if (lastKnownLocation != null) {
            logDisplayToUser(lastKnownLocation.toString());
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, myLocationListener);
    }

    public class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            logDisplayToUser(location.toString());
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
        }

        @Override
        public void onProviderEnabled(String s) {
        }

        @Override
        public void onProviderDisabled(String s) {
        }
    }

    // TODO evaluate moving to different class.
    public Set<Bitmap> gatherRouteImages(PathWrapper pathWrapper) {
        ImageGatherer imageGatherer = new ImageGatherer(mapView);
        Set<Bitmap> bitmapArrayList = new HashSet<>();

        LatLong[] points = new LatLong[pathWrapper.getPoints().size()];
        for (int i = 0; i < pathWrapper.getPoints().size(); i++) {
            points[i] = new LatLong(pathWrapper.getPoints().getLatitude(i), pathWrapper.getPoints().getLongitude(i));
        }
        bitmapArrayList = imageGatherer.captureMapTiles(points, tileCache);
        return bitmapArrayList;
    }

    public double classifyComplexityMultiThreaded(Set<Bitmap> routeBitmaps) {
        double nonComplexValue = 0;
        double complexValue = 0;
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        List<Future<List<Classifier.Recognition>>> list = new ArrayList<>();

        for (Bitmap bitmap : routeBitmaps) {
            Callable<List<Classifier.Recognition>> callable = new TensorFlowHandler(this.getActivity(), bitmap);
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

    public double classifyComplexity(Set<Bitmap> routeBitmaps) {
        TensorFlowHandler tensorFlowHandler = new TensorFlowHandler();
        tensorFlowHandler.createClassifier(this.getActivity());
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

    private void loadGraphStorage() {
        logDisplayToUser("loading graph (" + Constants.VERSION + ") ... ");
        new GHAsyncTask<Void, Void, Path>() {
            protected Path saveDoInBackground(Void... v) throws Exception {
                GraphHopper tmpHopp = new GraphHopper().forMobile();
                tmpHopp.setCHEnabled(false);
                //tmpHopp.setEncodingManager(new EncodingManager("foot"));
                tmpHopp.load(mapArea.getAbsoluteFile().getParent());
                log("found graph " + tmpHopp.getGraphHopperStorage().toString() + ", nodes:" + tmpHopp.getGraphHopperStorage().getNodes());
                hopper = tmpHopp;
                return null;
            }

            protected void onPostExecute(Path o) {
                if (hasError()) {
                    logDisplayToUser("An error occurred while creating graph:"
                            + getErrorMessage());
                } else {
                    logDisplayToUser("Finished loading graph. Long press to define where to start and end the route. Time to load  = " + (SystemClock.currentThreadTimeMillis() - time) / 100 + " seconds");
                }
            }
        }.execute();
    }

    private void log(String logText) {
        Log.i("GH", logText);
    }

    private void logDisplayToUser(String logText) {
        log(logText);
        Toast.makeText(this.getContext(), logText, Toast.LENGTH_LONG).show();
    }
}
