package com.png.helloworld;

/**
 * Created by Andy on 10/5/14.
 */
public class DisplayMessage{

    private Double lat;

    private Double lon;

    private String message;

    private Long timeStamp;

    public DisplayMessage (Double lat, Double lon, String message, Long timeStamp){
        this.lat = lat;
        this.lon = lon;
        this.message = message;
        this.timeStamp = timeStamp;
    }

}
