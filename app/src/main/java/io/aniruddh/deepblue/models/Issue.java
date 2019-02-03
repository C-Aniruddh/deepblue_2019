package io.aniruddh.deepblue.models;

import java.io.Serializable;

public class Issue implements Serializable {

    String issue_id;
    String category;
    String points;
    String locality;
    String image_one_full;
    String image_two_full;
    String description;
    String uploaded_by;
    String time;
    String detection;
    String label;
    String title;

    float lat;
    float lon;

    public String getIssue_id(){
        return issue_id;
    }

    public String getCategory(){
        return category;
    }

    public String getPoints(){
        return points;
    }

    public String getLocality(){
        return locality;
    }

    public String getImage_one_full(){
        return image_one_full;
    }

    public String getImage_two_full(){
        return image_two_full;
    }

    public String getDescription(){
        return description;
    }

    public String getUploaded_by(){
        return uploaded_by;
    }

    public String getTime(){
        return time;
    }

    public String getDetection(){
        return detection;
    }

    public String getLabel(){
        return label;
    }

    public String getTitle(){
        return title;
    }

    public float getLat(){
        return lat;
    }

    public float getLon(){
        return lon;
    }
}
