package com.png.helloworld;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestHandle;
import com.loopj.android.http.ResponseHandlerInterface;

import org.apache.http.Header;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class MapsActivity extends ActionBarActivity {
    private LatLng currentLocation;
    private LatLng lastLoadedLocation;

    private AlertDialog.Builder builder;
    private EditText messageField;

    private HashMap<Marker, DisplayMessage> markerMessages;

    private Activity thisActivity;

    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
            System.out.println("Location updated!");
            centerMap();

            if (distance(currentLocation, lastLoadedLocation) >= 0.0004) {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        ActionBar actionBar = getActionBar();
        actionBar.hide();

        thisActivity = this;

        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Location currentLoc = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (currentLoc == null) {
            currentLocation = new LatLng(0, 0);
        } else {
            currentLocation = new LatLng(currentLoc.getLatitude(), currentLoc.getLongitude());
        }
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
        mMap.getUiSettings().setScrollGesturesEnabled(false); //stops map movment/scrolling
        mMap.getUiSettings().setTiltGesturesEnabled(false);  //stops tilting in the map
        mMap.getUiSettings().setZoomGesturesEnabled(false);  //stops zooming with touch
        mMap.getUiSettings().setZoomControlsEnabled(false);  //removes zoom buttons
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                AlertDialog.Builder builder = new AlertDialog.Builder(thisActivity);
                builder.setMessage(markerMessages.get(marker).message);
                builder.create().show();

                return true;
            }
        });
        //mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
    }

    public void writeMessage(View view) {
        createBuilder();
        System.out.println("Compose note");
        builder.create().show();
    }

    private void sendMessage(String message) {
        System.out.println(message);
        sendMessage(currentLocation.latitude, currentLocation.longitude, message);
    }

    private void createBuilder() {
        builder = new AlertDialog.Builder(this);
        messageField = new EditText(this);
        builder.setMessage(R.string.leave_note_title)
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
                    Context sentSuccessContext = getApplicationContext();
                    int duration = Toast.LENGTH_SHORT;

                    Toast sentSuccessToast = Toast.makeText(sentSuccessContext, R.string.message_sent_properly, duration);
                    sentSuccessToast.show();
                }
                else if (response.equals("database")) {
                    System.out.println("database fucked up try again in a min");
                    Context sentFailContext = getApplicationContext();
                    int duration = Toast.LENGTH_SHORT;

                    Toast sentFailToast = Toast.makeText(sentFailContext, R.string.message_sent_fail, duration);
                    sentFailToast.show();
                }
                else {
                    System.out.println("SOMETHNG FUCKED UP IDK");
                    Context sentGeneralFailContext = getApplicationContext();
                    int duration = Toast.LENGTH_SHORT;

                    Toast sentGeneralFailToast = Toast.makeText(sentGeneralFailContext, R.string.message_sent_general_fail, duration);
                    sentGeneralFailToast.show();
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
            System.out.println("Error making request!");
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
                if(wasSuccessful.equals("success")) {
                    //KEY = messages. list of JSON objects which are message objects (what I sent + timestamp)
                    JSONArray messages;
                    try {
                        messages = jsonResponse.getJSONArray("messages");
                    }

                    catch (JSONException e) {
                        System.out.println("No messages in response!");
                        return;
                    }
                    ArrayList<DisplayMessage> messageObjects = new ArrayList<DisplayMessage>();
                    //Make array of java objects to
                    for (int i = 0; i < messages.length(); i++) {
                        Double lon;
                        Double lat;
                        String message;
                        String timeStamp; //receive in UTC, convert to local time

                        String format = "yyyy/MM/dd HH:mm:ss";
                        //SimpleDateFormat sdf = new SimpleDateFormat(format);
                        Date sdf = new Date();
                        try {
                            lon = messages.getJSONObject(i).getDouble("lonLocation");
                            lat = messages.getJSONObject(i).getDouble("latLocation");
                            message = messages.getJSONObject(i).getString("message");
                            //timeStamp = messages.getJSONObject(i).get("timestamp").toString(); // XXX
                            try {
                                //sdf = new SimpleDateFormat(format).parse(timeStamp);
                            }
                            catch (Exception e) {

                            }

                        }
                        catch (JSONException e) {
                            System.out.println("Error processing message!!");
                            return;
                        }

                        DisplayMessage messageReceived = new DisplayMessage(lat, lon, message, sdf);
                        messageObjects.add(messageReceived);
                    }

                    placeMarkers(messageObjects);
                }

                else {
                    System.out.println("Error occurred!");
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
            System.out.println("Unsupported location error");
        }
    }

    private double distance(LatLng loc1, LatLng loc2) {
        return Math.sqrt(
                Math.pow(loc1.latitude - loc2.latitude, 2) +
                Math.pow(loc1.longitude - loc2.longitude, 2));
    }

    private void placeMarkers(List<DisplayMessage> messages) {
        markerMessages = new HashMap<Marker, DisplayMessage>();
        mMap.clear();

        for (int i = 0; i < messages.size(); ++i) {
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(messages.get(i).lat, messages.get(i).lon)));
            markerMessages.put(marker, messages.get(i));
        }
    }
}
