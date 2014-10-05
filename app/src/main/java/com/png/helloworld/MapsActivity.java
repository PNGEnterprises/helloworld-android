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
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class MapsActivity extends ActionBarActivity {
    private Location currentLocation;

    private AlertDialog.Builder builder;
    private EditText messageField;

    private Activity thisActivity;

    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            currentLocation = location;
            System.out.println("Location updated!");
            centerMap();
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
        thisActivity = this;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        currentLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
                0, mLocationListener); // Too many updates?

        setUpMapIfNeeded();

        centerMap();

        sendMessage ("hello", "pasta", "yolo");
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
        //mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
    }

    private void writeMessage() {
        createBuilder();
        System.out.println("Compose message");
        builder.create().show();
    }

    private void sendMessage(String message) {
        System.out.println(message);
        sendMessage(currentLocation.getLatitude(), currentLocation.getLongitude(), message);
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

        mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(
                currentLocation.getLatitude(),
                currentLocation.getLongitude())));
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
                    AlertDialog.Builder workedAlert = new AlertDialog.Builder(thisActivity);
                    workedAlert.setMessage("@string/message_sent_properly");

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
}
