package com.dev.fa_alfygeorge_c0836170_android;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.dev.fa_alfygeorge_c0836170_android.database.PlaceDAO;
import com.dev.fa_alfygeorge_c0836170_android.database.RoomDB;
import com.dev.fa_alfygeorge_c0836170_android.databinding.ActivityMainBinding;
import com.dev.fa_alfygeorge_c0836170_android.model.Place;
import com.dev.fa_alfygeorge_c0836170_android.model.Result;
import com.dev.fa_alfygeorge_c0836170_android.network.ApiInterface;
import com.dev.fa_alfygeorge_c0836170_android.utils.DirectionsJSONParser;
import com.dev.fa_alfygeorge_c0836170_android.utils.SharedPrefHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;
import org.w3c.dom.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    GoogleMap map;
    ActivityMainBinding binding;
    Place place;
    PlaceDAO placeDAO;
    RoomDB roomDB;
    List<Place> placeList = new ArrayList<>();
    String favAddress;
    SimpleDateFormat formatter;
    Date date;
    boolean isNewFavLocation = false;
    double latitude,longitude;
    double userLat,userLong;
    String savedLocation;
    LatLng origin;
    LatLng dest;
    double dist;

    private static final int REQUEST_CODE = 1;
    public static final int UPDATE_INTERVAl = 5000;
    public static final int FASTEST_INTERVAl = 3000;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private List<String> permissionToRequest;
    private List<String> permissions = new ArrayList<>();
    private List<String> permissionsRejected = new ArrayList<>();

    private Marker favMarker,userMarker;
    private List<Marker> markerList = new ArrayList<>();
    LatLng latLng = null;
    String serverKey = "AIzaSyA0KSQvEjOO7EqwXW8dY0ntLxFrgn1_Zpo";
    Document document;

    private ApiInterface apiInterface;
    private Retrofit retrofit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //initialize roomDB
        roomDB = RoomDB.getInstance(this);
        retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .baseUrl("https://maps.googleapis.com/")
                .build();
        apiInterface = retrofit.create(ApiInterface.class);


        //toolbar
        binding.toolbarMain.setNavigationIcon(R.drawable.ic_back);
        binding.toolbarMain.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                    Intent intent = new Intent(getApplicationContext(),HomeActivity.class);
                    startActivity(intent);
            }
        });
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //instantiation
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        //get the specific place through intent
        try {
            place = new Place();
            place = (Place) getIntent().getSerializableExtra("fav_place");
            binding.txtPlaceName.setText(place.getPlaceName());
            savedLocation = place.getPlaceName();

            if (place.isVisited){
                binding.btnVisited.setEnabled(false);
                binding.btnVisited.setText("VISITED");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        binding.btnVisited.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                roomDB.placeDAO().update(place.id,savedLocation,place.getCreatedDate(),place.getLatitude(),place.getLongitude(),true);
                Intent intent = new Intent(getApplicationContext(),HomeActivity.class);
                startActivity(intent);
                binding.btnVisited.setEnabled(false);

            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

        if(SharedPrefHelper.getInstance(getApplicationContext()).getBolIsUpdate()){
           editSavedLocationMarker();
           binding.txtInstruction.setText("*Search a new location to update your favourite place");
            binding.txtInstruction.setVisibility(View.VISIBLE);
            binding.searchButton.setText("UPDATE");
        }else {

            if (!isGranted()) {
                requestLocationPermission();
            } else {
                startUpdateLocation();
            }
            if (binding.txtPlaceName.getText().toString().matches("")) {
                map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                    @Override
                    public void onMapLongClick(@NonNull LatLng latLng) {
                        if (isNewFavLocation) {
                            setMarker(latLng);
                        }
                    }
                });
            } else {
                setSavedLocationMarker();
                binding.btnVisited.setVisibility(View.VISIBLE);
                binding.txtInstruction.setVisibility(View.GONE);
                binding.llSearchlayout.setVisibility(View.GONE);

//                if (place.isVisited) {
//                    binding.btnVisited.setEnabled(false);
//                }
            }
        }

    }

    //adding favourite place on long click
    private void setMarker(LatLng latLng) {
        MarkerOptions options = new MarkerOptions()
                .position(latLng)
                .title("Favourite")
                .draggable(true)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                .snippet("Nice place to visit");
        favMarker = map.addMarker(options);
        //drawLine();
        latitude = latLng.latitude;
        longitude = latLng.longitude;
        findFavAddress(latLng.latitude,latLng.longitude);
        setDateTime();
        insertFavPlaces();
    }

    private void setDateTime() {
        formatter = new SimpleDateFormat("EEE, d MMM yyyy HH:mm a");
        date = new Date();
        favMarker.setSnippet(formatter.format(date));
    }

    private void insertFavPlaces(){
        place = new Place();
        place.setPlaceName(favAddress);
        place.setCreatedDate(formatter.format(date));
        place.setVisited(false);
        place.setLatitude(latitude);
        place.setLongitude(longitude);
        roomDB.placeDAO().insert(place);
        Toast.makeText(this, "New Favourite Place Added", Toast.LENGTH_SHORT).show();
    }


    //Request for Location
    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this
                ,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_CODE);


    }
    private boolean isGranted() {
        return ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) ==
                (PackageManager.PERMISSION_GRANTED);

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (REQUEST_CODE == requestCode){


            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)){
                new AlertDialog.Builder(this).setMessage("Accessing the location is mandatory")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(MainActivity.this
                                        ,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_CODE);

                            }
                        }).setNegativeButton("Cancel",null)
                        .create()
                        .show();


            }else {
               startUpdateLocation();
            }
        }
    }

    //setting user location on map
    private void startUpdateLocation() {
        //location request
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAl);
        locationRequest.setFastestInterval(FASTEST_INTERVAl);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                // mMap.clear();
                    Location location = locationResult.getLastLocation();
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    userLat = location.getLatitude();
                    userLong = location.getLongitude();
                    origin = new LatLng(userLat, userLong);
                    if (!isNewFavLocation) {
                        userMarker = map.addMarker(new MarkerOptions().position(latLng).title("User Location"));
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                        isNewFavLocation = true;

                    }

                    if (!SharedPrefHelper.getInstance(getApplicationContext()).getBolIsUpdate()) {
                        drawPolylines();
                    }

                try {
                    getDistance(userLat+","+userLong,dest.latitude+","+dest.longitude);

                }catch (Exception e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onLocationAvailability(@NonNull LocationAvailability locationAvailability) {
                super.onLocationAvailability(locationAvailability);
            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            return;
        }


        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

    }

    //get the favorite address
    private  void  findFavAddress(double latitude,double longitude){
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(this, Locale.getDefault());

        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
            favAddress = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
            favMarker.setTitle(favAddress);

        } catch (IOException e) {
            Log.d(TAG, "findFavAddress: "+e.getMessage());
            e.printStackTrace();
        }


    }

    //adding different map types
    public void normal(View view) {
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
    }

    public void satellite(View view) {
        map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
    }

    public void terrain(View view) {
        map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
    }

    public void hybrid(View view) {
        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);

    }

    public void onMapSearch(View view) {
        if (SharedPrefHelper.getInstance(getApplicationContext()).getBolIsUpdate()){
            String location = binding.editText.getText().toString();
            List<Address> addressList = null;

            Geocoder geocoder = new Geocoder(this);
            try {
                map.clear();
                addressList = geocoder.getFromLocationName(location, 1);
                assert addressList != null;
                Address address = addressList.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                favAddress = addressList.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()

                favMarker = map.addMarker(new MarkerOptions().position(latLng).title(favAddress));
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                setDateTime();
                // drawLine();
                latitude = address.getLatitude();
                longitude = address.getLongitude();
                favMarker.setSnippet(formatter.format(date));
                roomDB.placeDAO().update(place.getId(),favAddress,formatter.format(date),latitude,longitude,false);
                placeList.clear();
                placeList.addAll( roomDB.placeDAO().getAllPlaces());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else {
            String location = binding.editText.getText().toString();
            List<Address> addressList = null;

            Geocoder geocoder = new Geocoder(this);
            try {
                addressList = geocoder.getFromLocationName(location, 1);
                assert addressList != null;
                Address address = addressList.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                favAddress = addressList.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()

                favMarker = map.addMarker(new MarkerOptions().position(latLng).title(favAddress));
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                setDateTime();
                // drawLine();
                latitude = address.getLatitude();
                longitude = address.getLongitude();
                favMarker.setSnippet(formatter.format(date));
                insertFavPlaces();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setSavedLocationMarker(){
        List<Address> addressList = null;

        Geocoder geocoder = new Geocoder(this);
        try {
            addressList = geocoder.getFromLocationName(savedLocation, 1);
            assert addressList != null;
            Address address = addressList.get(0);
            LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
            favMarker = map.addMarker(new MarkerOptions().position(latLng).title(savedLocation).draggable(true));
            map.animateCamera(CameraUpdateFactory.zoomIn());
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            dest = new LatLng(address.getLatitude(), address.getLongitude());

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public void editSavedLocationMarker(){
        List<Address> addressList = null;

        Geocoder geocoder = new Geocoder(this);
        try {
            addressList = geocoder.getFromLocationName(savedLocation, 1);
            assert addressList != null;
            Address address = addressList.get(0);
            LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
            favMarker = map.addMarker(new MarkerOptions().position(latLng).title(savedLocation).draggable(true));
            map.animateCamera(CameraUpdateFactory.zoomIn());
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            dest = new LatLng(address.getLatitude(), address.getLongitude());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //draw polyline
    private void drawPolylines() {

        // Checks, whether start and end locations are captured
        // Getting URL to the Google Directions API
        try{
            String url = getDirectionsUrl(origin, dest);
            Log.d("url", url + "");
            DownloadTask downloadTask = new DownloadTask();
            // Start downloading json data from Google Directions API
            downloadTask.execute(url);

           // getDistance(userLat+","+userLong,dest.latitude+","+dest.longitude);

        }catch (Exception e){
            e.printStackTrace();
        }


    }

    private void getDistance(String origin, String destination){
        Call<Result> call = apiInterface.getDistance(origin,destination,serverKey);
        call.enqueue(new Callback<Result>() {
            @Override
            public void onResponse(Call<Result> call, Response<Result> response) {


                binding.txtDistance.setVisibility(View.VISIBLE);
                binding.txtDuration.setVisibility(View.VISIBLE);
               binding.txtDistance.setText("Total Distance: "+response.body().getRows().get(0).getElements().get(0).getDistance().getText());
               binding.txtDuration.setText("Total Duration: "+response.body().getRows().get(0).getElements().get(0).getDuration().getText());
            //  Toast.makeText(MainActivity.this, response.body().getRows().get(0).getElements().get(0).getDistance().getText(), Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onFailure(Call<Result> call, Throwable t) {

            }
        });

    }




    /*implemention direction API*/
    private class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            String data = "";

            try {
                data = downloadUrl(url[0]);

            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();


            parserTask.execute(result);

        }
    }
    /**
     * A class to parse the Google Places in JSON format
     */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;
            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();


                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = new ArrayList<LatLng>();;
            PolylineOptions lineOptions = new PolylineOptions();;

            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            lineOptions.width(8);
            lineOptions.color(Color.RED);
            MarkerOptions markerOptions = new MarkerOptions();
            // Traversing through all the routes
            for(int i=0;i<result.size();i++){
                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);
                // Fetching all the points in i-th route
                for(int j=0;j<path.size();j++){
                    HashMap<String,String> point = path.get(j);
                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);
                    points.add(position);
                    builder.include(position);
                }
                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);

            }
            // Drawing polyline in the Google Map for the i-th route
            if(points.size()!=0)map.addPolyline(lineOptions);//to avoid crash


            int width = getResources().getDisplayMetrics().widthPixels;
            int padding = (int) (width * 0.05);
            /*create the bounds from latlngBuilder to set into map camera*/
            LatLngBounds bounds = builder.build();
            /*create the camera with bounds and padding to set into map*/
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
            map.animateCamera(cu);
        }
    }
    private String getDirectionsUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        // Sensor enabled
        String sensor = "sensor=false";
        String mode = "mode=driving";
        String key ="key=" +serverKey;
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + key;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;
        // https://maps.googleapis.com/maps/api/directions/json?origin=Time+Square&destination=Chelsea+Market&key=YOUR_API_KEY

        return url;
    }
    /**
     * A method to download json data from url
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.connect();

            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();
            Log.d("data", data);

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }


}