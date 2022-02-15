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
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.widget.Toolbar;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.RequestResult;
import com.akexorcist.googledirection.constant.TransportMode;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Info;
import com.akexorcist.googledirection.model.Leg;
import com.akexorcist.googledirection.model.Route;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.dev.fa_alfygeorge_c0836170_android.adapter.PlacesAdapter;
import com.dev.fa_alfygeorge_c0836170_android.database.PlaceDAO;
import com.dev.fa_alfygeorge_c0836170_android.database.RoomDB;
import com.dev.fa_alfygeorge_c0836170_android.databinding.ActivityMainBinding;
import com.dev.fa_alfygeorge_c0836170_android.model.Place;
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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
    String serverKey = "AIzaSyCL4gLFBHhmr1fvxVxTYSZEru6t3CxyrxE";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //initialize roomDB
        roomDB = RoomDB.getInstance(this);

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
            savedLocation = place.placeName;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

        if (!isGranted()){
            requestLocationPermission();
        }else {
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
        }else {
            setSavedLocationMarker();
            binding.txtInstruction.setVisibility(View.GONE);
            binding.llSearchlayout.setVisibility(View.GONE);
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

    private void drawLine() {
        PolylineOptions polylineOptions = new PolylineOptions()
                .color(Color.BLACK)
                .width(10)
                .add(userMarker.getPosition(),favMarker.getPosition());

        map.addPolyline(polylineOptions);
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
                if (!isNewFavLocation) {
                    Location location = locationResult.getLastLocation();

                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    userLat = location.getLatitude();
                    userLong = location.getLongitude();
                    userMarker = map.addMarker(new MarkerOptions().position(latLng).title("User Location"));
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

                    isNewFavLocation = true;
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
        String location = binding.editText.getText().toString();
        List<Address> addressList = null;

        Geocoder geocoder = new Geocoder(this);
        try {
            addressList = geocoder.getFromLocationName(location, 1);

        } catch (IOException e) {
            e.printStackTrace();
        }
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
    }

    public void setSavedLocationMarker(){
        List<Address> addressList = null;

        Geocoder geocoder = new Geocoder(this);
        try {
            addressList = geocoder.getFromLocationName(savedLocation, 1);
            assert addressList != null;
            Address address = addressList.get(0);
            LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
            map.addMarker(new MarkerOptions().position(latLng).title(savedLocation));
            map.animateCamera(CameraUpdateFactory.zoomIn());
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            getDestinationInfo(latLng);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Draw polyline on map, get distance and duration of the route
     *
     * @param latLngDestination LatLng of the destination
     */
    private void getDestinationInfo(LatLng latLngDestination) {
 // Api Key For Google Direction API \\
        final LatLng origin = new LatLng(43.9999, 79.654654);
        final LatLng destination = latLngDestination;
        //-------------Using AK Exorcist Google Direction Library---------------\\
        GoogleDirection.withServerKey(serverKey)
                .from(origin)
                .to(destination)
                .transportMode(TransportMode.DRIVING)
                .execute(new DirectionCallback() {
                    @Override
                    public void onDirectionSuccess(Direction direction, String rawBody) {

                        String status = direction.getStatus();
                        if (status.equals(RequestResult.OK)) {
                            Route route = direction.getRouteList().get(0);
                            Leg leg = route.getLegList().get(0);
                            Info distanceInfo = leg.getDistance();
                            Info durationInfo = leg.getDuration();
                            String distance = distanceInfo.getText();
                            String duration = durationInfo.getText();

                            //------------Displaying Distance and Time-----------------\\
                           // showingDistanceTime(distance, duration); // Showing distance and time to the user in the UI \\
                            String message = "Total Distance is " + distance + " and Estimated Time is " + duration;
                            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();

                            //--------------Drawing Path-----------------\\
                            ArrayList<LatLng> directionPositionList = leg.getDirectionPoint();
                            PolylineOptions polylineOptions = DirectionConverter.createPolyline(getApplicationContext(),
                                    directionPositionList, 5, getResources().getColor(R.color.teal_200));
                            map.addPolyline(polylineOptions);
                            //--------------------------------------------\\

                            //-----------Zooming the map according to marker bounds-------------\\
                            LatLngBounds.Builder builder = new LatLngBounds.Builder();
                            builder.include(origin);
                            builder.include(destination);
                            LatLngBounds bounds = builder.build();

                            int width = getResources().getDisplayMetrics().widthPixels;
                            int height = getResources().getDisplayMetrics().heightPixels;
                            int padding = (int) (width * 0.20); // offset from edges of the map 10% of screen

                            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);
                            map.animateCamera(cu);
                            //------------------------------------------------------------------\\

                        } else if (status.equals(RequestResult.NOT_FOUND)) {
                            Toast.makeText(getApplicationContext(), "No routes exist", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onDirectionFailure(Throwable t) {
                        // Do something here
                    }
                });
        //-------------------------------------------------------------------------------\\

    }
}