package com.example.organizeit_tal_aviv;

public class Image {
    private String url;
    private String name;
    private String location;

    public Image(String url, String name, String location) {
        this.url = url;
        this.name = name;
        this.location = location;
    }

    public Image() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

}
