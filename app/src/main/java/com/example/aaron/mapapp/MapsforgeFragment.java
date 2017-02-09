package com.example.aaron.mapapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Path;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Spinner;
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
import org.mapsforge.map.layer.Layer;
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
import com.graphhopper.util.Helper;
import com.graphhopper.util.Parameters.Algorithms;
import com.graphhopper.util.Parameters.Routing;
import com.graphhopper.util.PointList;
import com.graphhopper.util.ProgressListener;
import com.graphhopper.util.StopWatch;


import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
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
    private Layer pathLayer;
    private LatLong start;
    private  LatLong end;

    public MapsforgeFragment() {
        // Required empty public constructor
    }
    protected boolean onLongPress(LatLong p) {
        if (shortestPathRunning) {
            logUser("Calculation still in progress");
            return false;
        }

        if (start != null && end == null) {
            end = p;
            shortestPathRunning = true;
            //itemizedLayer.addItem(createMarkerItem(p, R.drawable.marker_icon_red));
            //addMarker();
            //mapView.map().updateMap(true);

            calcPath(start.getLatitude(), start.getLongitude(), end.getLatitude(),
                    end.getLongitude());
        } else {
            start = p;
            end = null;
            // remove routing layers
           // mapView.map().layers().remove(pathLayer);
            //itemizedLayer.removeAllItems();

           // itemizedLayer.addItem(createMarkerItem(start, R.drawable.marker_icon_green));
           // mapView.map().updateMap(true);
        }
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidGraphicFactory.createInstance(getActivity().getApplication());
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

    // TODO: Rename method, update argument and hook method into UI event
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
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
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
            public boolean onLongPress(LatLong tapLatLong, Point layerXY, Point tapXY) {
                if (shortestPathRunning) {
                    logUser("Calculation still in progress");
                    return false;
                }

                if (start != null && end == null) {
                    end = tapLatLong;
                    shortestPathRunning = true;
                    //itemizedLayer.addItem(createMarkerItem(p, R.drawable.marker_icon_red));
                    //addMarker();
                    //mapView.map().updateMap(true);

                    calcPath(start.getLatitude(), start.getLongitude(), end.getLatitude(),
                            end.getLongitude());
                } else {
                    start = tapLatLong;
                    end = null;
                    // remove routing layers
                    // mapView.map().layers().remove(pathLayer);
                    //itemizedLayer.removeAllItems();

                    // itemizedLayer.addItem(createMarkerItem(start, R.drawable.marker_icon_green));
                    // mapView.map().updateMap(true);
                }
                return true;
            }
        };



        XmlRenderTheme renderTheme = null;

        try {
            renderTheme = new ExternalRenderTheme("Elevate.xml");
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

    public Polyline createPathPolyline(LatLong[] points) {
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
        return polyline;
    }

    public void addMarker(Layers layers, LatLong position) {
        MapsforgeFragment.TappableMarker tappableMarker = new MapsforgeFragment.TappableMarker(R.drawable.ic_menu_mylocation, position);
        mapView.getLayerManager().getLayers().add(tappableMarker);
    }

    private class TappableMarker extends Marker {
        public TappableMarker(int icon, LatLong localLatLong) {
            super(localLatLong, AndroidGraphicFactory.convertToBitmap(MapsforgeFragment.this.getResources().getDrawable(icon)),
                    AndroidGraphicFactory.convertToBitmap(MapsforgeFragment.this.getResources().getDrawable(icon)).getWidth() / 2,
                    -1 * (AndroidGraphicFactory.convertToBitmap(MapsforgeFragment.this.getResources().getDrawable(icon)).getHeight()) / 2);
        }
    }

    private volatile boolean shortestPathRunning = false;

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
                    logUser("the route is " + (int) (resp.getDistance() / 100) / 10f
                            + "km long, time:" + resp.getTime() / 60000f + "min, debug:" + time);
                    LatLong[] points = new LatLong[resp.getPoints().size()];

                    for (int i = 0; i < resp.getPoints().size(); i++) {

                        points[i] = new LatLong(resp.getPoints().getLatitude(i), resp.getPoints().getLongitude(i));
                    }

                    drawPolyline(mapView.getLayerManager().getLayers(), points);

                } else {
                    logUser("Error:" + resp.getErrors());
                }
                shortestPathRunning = false;
            }
        }.execute();
    }
    private void loadGraphStorage() {
        logUser("loading graph (" + Constants.VERSION + ") ... ");
        new GHAsyncTask<Void, Void, Path>() {
            protected Path saveDoInBackground(Void... v) throws Exception {
                GraphHopper tmpHopp = new GraphHopper().forMobile();
                tmpHopp.load(new File("/storage/emulated/0/Download/graphhopper/maps/sweden").getAbsolutePath() + "-gh");
                log("found graph " + tmpHopp.getGraphHopperStorage().toString() + ", nodes:" + tmpHopp.getGraphHopperStorage().getNodes());
                hopper = tmpHopp;
                return null;
            }

            protected void onPostExecute(Path o) {
                if (hasError()) {
                    logUser("An error happened while creating graph:"
                            + getErrorMessage());
                } else {
                    logUser("Finished loading graph. Press long to define where to start and end the route.");
                }
            }
        }.execute();
    }
    private void log(String str) {
        Log.i("GH", str);
    }
    private void logUser(String str) {
        log(str);
        Toast.makeText(this.getContext(), str, Toast.LENGTH_LONG).show();
    }

}
