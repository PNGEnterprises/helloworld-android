package com.png.helloworld;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MapsActivity extends ActionBarActivity {
    private LatLng currentLocation;
    private LatLng lastLoadedLocation;

    private AlertDialog.Builder builder;
    private EditText messageField;

    private HashMap<Marker, DisplayMessage> markerMessages;

    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
            System.out.println("Location updated!");
            centerMap();

            if (distance(currentLocation, lastLoadedLocation) > 0.0003) {
                getMessages(currentLocation);
            }
        }

        @Override
        public void onStatusChanged(String str, int i, Bundle b) {
            System.out.println(str);
        }

        @Override
        public void onProviderEnabled(String p) {
            System.out.println(p);
        }

        @Override
        public void onProviderDisabled(String p) {
            System.out.println(p);
        }
    };

    private LocationManager mLocationManager;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private Activity thisActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        thisActivity = this;

        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Location currentLoc = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        currentLocation = new LatLng(currentLoc.getLatitude(), currentLoc.getLongitude());
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
                0, mLocationListener); // Too many updates?

        getMessages(currentLocation);

        setUpMapIfNeeded();

        centerMap();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the map_menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.map_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_compose:
                writeMessage();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.moveCamera(CameraUpdateFactory.zoomTo(16));
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                centerMap();
            }
        });
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                AlertDialog.Builder builder = new AlertDialog.Builder(thisActivity);
                builder.setMessage(markerMessages.get(marker).message)
                        .setTitle("The Messsage");
                builder.create().show();

                return true;
            }
        });
        //mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
    }

    private void writeMessage() {
        createBuilder();
        System.out.println("Compose message");
        builder.create().show();
    }

    private void sendMessage(String message) {
        System.out.println(message);
        sendMessage(currentLocation.latitude, currentLocation.longitude, message);
    }

    private void createBuilder() {
        builder = new AlertDialog.Builder(this);
        messageField = new EditText(this);
        builder.setMessage("Type a message to leave here")
                .setTitle("Leave a message")
                .setView(messageField)
                .setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        sendMessage(messageField.getText().toString());
                    }
                });
    }

    private void centerMap() {
        // Center the map on user's current location
        if (currentLocation == null) {
            System.out.println("Null location!!");
            return;
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLng(
                new LatLng(currentLocation.latitude, currentLocation.longitude)));
    }
    
    private void sendMessage(final double latitude, final double longitude, final String message) {

        JSONObject jmessage = new JSONObject();
        try {
            jmessage.put("latLocation", latitude);
            jmessage.put("lonLocation", longitude);
            jmessage.put("message", message);
        } catch (JSONException e) {

        }
        //Create a client to make networking happen
        AsyncHttpClient client = new AsyncHttpClient();

        JsonHttpResponseHandler jsonHttpResponseHandler = new JsonHttpResponseHandler() {
            //"helloworldbackend.herokuapp.com/api/v1/post-message
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject jsonResponse) {
                System.out.println("yay");
                String response = "";
                try {
                    response = jsonResponse.get("error").toString();
                }
                catch (JSONException e) {

                }
                if(response.equals("success")) {
                    System.out.println("IT WORKED");

                }
                else if (response.equals("database")) {
                    System.out.println("database fucked up try again in a min");
                }
                else {
                    System.out.println("SOMETHNG FUCKED UP IDK");
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject error) {
                System.out.println(":(");
            }
        };
        StringEntity stringEntity;
        try{
            stringEntity = new StringEntity(jmessage.toString());
            client.post(getApplicationContext(),
                    "http://helloworldbackend.herokuapp.com/api/v1/post-message",
                    stringEntity,
                    "application/json",
                    jsonHttpResponseHandler);
        }
        catch (UnsupportedEncodingException e) {

        }
    }

    private void getMessages(LatLng loc) {

        lastLoadedLocation = new LatLng(loc.latitude, loc.longitude);

        JSONObject jmessage = new JSONObject();
        try {
            jmessage.put("latLocation", loc.latitude);
            jmessage.put("lonLocation", loc.longitude);
        } catch (JSONException e) {

        }
        //Create a client to make networking happen
        AsyncHttpClient client = new AsyncHttpClient();

        JsonHttpResponseHandler jsonHttpResponseHandler = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject jsonResponse) {
                //make list of JSON objects into list of Java objects to send to Justin

                //IN CASE OF ERROR = SUCCESS PARSE SOME SHIT
                String wasSuccessful = null;
                try {
                    wasSuccessful = jsonResponse.getString("error");
                } catch (JSONException e) {
                    e.printStackTrace();
                    return;
                }
                if(wasSuccessful.equals("success")){
                    //KEY = messages. list of JSON objects which are message objects (what I sent + timestamp)
                    JSONArray messages;
                    try {
                        messages = jsonResponse.getJSONArray("messages");
                    }

                    catch (JSONException e) {
                        return;
                    }
                    ArrayList<DisplayMessage> messageObjects = new ArrayList<DisplayMessage>();
                    //Make array of java objects to
                    for (int i = 0; i < messages.length(); i++) {
                        Double lon;
                        Double lat;
                        String message;
                        Long timeStamp;

                        try {
                            lon = messages.getJSONObject(i).getDouble("lon");
                            lat = messages.getJSONObject(i).getDouble("lon");
                            message = messages.getJSONObject(i).getString("message");
                            timeStamp = new Long(0); // XXX
                        }
                        catch (JSONException e) {
                            return;
                        }

                        DisplayMessage messageReceived = new DisplayMessage(lon, lat, message, timeStamp);
                        messageObjects.add(messageReceived);
                    }

                    placeMarkers(messageObjects);
                }

                else {

                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject error) {
                System.out.println(":(");
            }
        };


        StringEntity stringEntity;
        try{
            stringEntity = new StringEntity(jmessage.toString());
            client.post(getApplicationContext(),
                    "http://helloworldbackend.herokuapp.com/api/v1/get-messages",
                    stringEntity,
                    "application/json",
                    jsonHttpResponseHandler);
        }
        catch (UnsupportedEncodingException e) {

        }
    }

    private double distance(LatLng loc1, LatLng loc2) {
        return Math.sqrt(
                Math.pow(loc1.latitude - loc2.latitude, 2) +
                Math.pow(loc1.longitude - loc2.longitude, 2));
    }

    private void placeMarkers(List<DisplayMessage> messages) {
        markerMessages = new HashMap<Marker, DisplayMessage>();

        for (int i = 0; i < messages.size(); ++i) {
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(messages.get(i).lat, messages.get(i).lon)));
            markerMessages.put(marker, messages.get(i));
        }
    }
}
