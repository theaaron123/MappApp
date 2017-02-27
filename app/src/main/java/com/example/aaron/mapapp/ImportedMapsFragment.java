package com.example.aaron.mapapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ImportedMapsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ImportedMapsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ImportedMapsFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private RecyclerView mImportedMapsRecyclerView;
    private ImportedMapsAdapter mAdapter;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public ImportedMapsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ImportedMapsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ImportedMapsFragment newInstance(String param1, String param2) {
        ImportedMapsFragment fragment = new ImportedMapsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_imported_maps, container, false);
        mImportedMapsRecyclerView = (RecyclerView) view.findViewById(R.id.imported_maps_recycler_view);
        mImportedMapsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        updateUI();
        return view;
    }

    private void updateUI() {
        ArrayList<String> list  = new ArrayList<>();
        SharedPreferences sharedPreferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        Map<String, ?> sharedPreferencesAll = sharedPreferences.getAll();
        for (String val : sharedPreferencesAll.keySet()) {
            list.add(val);
        }

        mAdapter = new ImportedMapsAdapter(list);
        mImportedMapsRecyclerView.setAdapter(mAdapter);
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
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
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

    private class ImportedMapsHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView mTitleTextView;
        private String mImportedMap;

        public ImportedMapsHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            mTitleTextView = (TextView) itemView;
        }

        public void bindImportedMap(String importedMap) {
            mImportedMap = importedMap;
            mTitleTextView.setText(mImportedMap);
        }

        @Override
        public void onClick(View view) {
            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
            Fragment fragment = fragmentManager.findFragmentById(R.id.fragment_container);
            Bundle bundle = new Bundle();
            bundle.putString("PATH", mImportedMap);
            fragment = new MapsforgeFragment();
            fragment.setArguments(bundle);
            fragmentManager.beginTransaction()
                    .add(R.id.fragment_container, fragment)
                    .commit();
        }
    }

    private class ImportedMapsAdapter extends RecyclerView.Adapter<ImportedMapsHolder> {
        private ArrayList<String> mImportedMaps = new ArrayList<>();

        public ImportedMapsAdapter(ArrayList<String> importedMaps) {
            mImportedMaps = importedMaps;
        }

        @Override
        public ImportedMapsHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            View view = layoutInflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ImportedMapsHolder(view);
        }

        @Override
        public void onBindViewHolder(ImportedMapsHolder holder, int position) {
            String mapName = mImportedMaps.get(position);
            holder.bindImportedMap(mapName);
        }

        @Override
        public int getItemCount() {
            return mImportedMaps.size();
        }
    }
}
