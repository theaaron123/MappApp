package com.example.aaron.mapapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Path;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
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


import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import static android.app.Activity.RESULT_OK;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MapsforgeFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class MapsforgeFragment extends Fragment {
    private MapView mapView;
    Uri selectedFile;
    private GraphHopper hopper;
    private OnFragmentInteractionListener mListener;
    private LatLong start;
    private  LatLong end;
    private File mapsDir;
    private volatile boolean shortestPathRunning = false;

    public MapsforgeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidGraphicFactory.createInstance(getActivity().getApplication());
        mapsDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "/graphhopper/maps/");
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

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 123 && resultCode == RESULT_OK) {
            selectedFile = data.getData(); //The uri with the location of the file
        }
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (selectedFile != null) {
            renderMap(selectedFile.getPath());
        }
    }

    /**
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    public void renderMap(String path) {
        // create a tile cache of suitable size
        TileCache tileCache = AndroidUtil.createTileCache(this.getActivity(), "mapcache",
                mapView.getModel().displayModel.getTileSize(), 1f,
                this.mapView.getModel().frameBufferModel.getOverdrawFactor());

        // tile renderer layer using specified render theme
        MapDataStore mapDataStore = new MapFile(new File(path));
        TileRendererLayer tileRendererLayer = new TileRendererLayer(tileCache, mapDataStore,
                this.mapView.getModel().mapViewPosition, AndroidGraphicFactory.INSTANCE) {
            @Override
            public boolean onTap(LatLong tapLatLong, org.mapsforge.core.model.Point layerXY, org.mapsforge.core.model.Point tapXY) {
                // single tap removes markers
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
    }

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

    public void addMarker(Layers layers, LatLong position) {
        MapsforgeFragment.TappableMarker tappableMarker = new MapsforgeFragment.TappableMarker(R.drawable.ic_menu_mylocation, position);
        mapView.getLayerManager().getLayers().add(tappableMarker);
    }

    private void removeLayersOnMap() {
        for (int i = 1; i < mapView.getLayerManager().getLayers().size(); i++) {
            mapView.getLayerManager().getLayers().remove(i);
        }
    }

    private class TappableMarker extends Marker {
        public TappableMarker(int icon, LatLong localLatLong) {
            // TODO override onTap to show location of marker.
            super(localLatLong, AndroidGraphicFactory.convertToBitmap(MapsforgeFragment.this.getResources().getDrawable(icon)),
                    AndroidGraphicFactory.convertToBitmap(MapsforgeFragment.this.getResources().getDrawable(icon)).getWidth() / 2,
                    -1 * (AndroidGraphicFactory.convertToBitmap(MapsforgeFragment.this.getResources().getDrawable(icon)).getHeight()) / 2);
        }
    }

    public void calcPath(final double fromLat, final double fromLon,
                         final double toLat, final double toLon) {

        log("calculating path ...");
        new AsyncTask<Void, Void, PathWrapper>() {
            float time;

            protected PathWrapper doInBackground(Void... v) {
                StopWatch sw = new StopWatch().start();
                GHRequest req = new GHRequest(fromLat, fromLon, toLat, toLon).
                        setAlgorithm(Algorithms.DIJKSTRA_BI);
                req.getHints().
                        put(Routing.INSTRUCTIONS, "false");
                GHResponse resp = hopper.route(req);
                time = sw.stop().getSeconds();
                return resp.getBest();
            }

            protected void onPostExecute(PathWrapper resp) {
                if (!resp.hasErrors()) {
                    log("from:" + fromLat + "," + fromLon + " to:" + toLat + ","
                            + toLon + " found path with distance:" + resp.getDistance()
                            / 1000f + ", nodes:" + resp.getPoints().getSize() + ", time:"
                            + time + " " + resp.getDebugInfo());
                    logDisplayToUser("the route is " + (int) (resp.getDistance() / 160) / 10f +" miles long, time:" + resp.getTime() / 60000f + "min, debug:" + time);
                    LatLong[] points = new LatLong[resp.getPoints().size()];

                    for (int i = 0; i < resp.getPoints().size(); i++) {

                        points[i] = new LatLong(resp.getPoints().getLatitude(i), resp.getPoints().getLongitude(i));
                    }
                    drawPolyline(mapView.getLayerManager().getLayers(), points);
                } else {
                    logDisplayToUser("Error:" + resp.getErrors());
                }
                shortestPathRunning = false;
            }
        }.execute();
    }
    private void loadGraphStorage() {
        logDisplayToUser("loading graph (" + Constants.VERSION + ") ... ");
        new GHAsyncTask<Void, Void, Path>() {
            protected Path saveDoInBackground(Void... v) throws Exception {
                GraphHopper tmpHopp = new GraphHopper().forMobile();
                // TODO remove hardcoded path
                tmpHopp.load(new File("/storage/emulated/0/Download/graphhopper/maps/sweden").getAbsolutePath() + "-gh");
                log("found graph " + tmpHopp.getGraphHopperStorage().toString() + ", nodes:" + tmpHopp.getGraphHopperStorage().getNodes());
                hopper = tmpHopp;
                return null;
            }

            protected void onPostExecute(Path o) {
                if (hasError()) {
                    logDisplayToUser("An error happened while creating graph:"
                            + getErrorMessage());
                } else {
                    logDisplayToUser("Finished loading graph. Press long to define where to start and end the route.");
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
