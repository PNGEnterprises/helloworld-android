package com.png.helloworld;

/**
 * Created by Andy on 10/5/14.
 */
public class DisplayMessage{

    public Double lat;

    public Double lon;

    public String message;

    public Long timeStamp;

    public DisplayMessage (Double lat, Double lon, String message, Long timeStamp){
        this.lat = lat;
        this.lon = lon;
        this.message = message;
        this.timeStamp = timeStamp;
    }

}
