package com.example.capstone_kioskview;

import com.gluonhq.maps.MapLayer;
import com.gluonhq.maps.MapPoint;
import com.gluonhq.maps.MapView;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class KioskViewController {
   private final MapPoint cebuCity = new MapPoint(10.3157, 123.8854);


   @FXML
   private StackPane stackpaneLiveMap;

   public void initialize(){
       MapView mapView = createMapView();
       mapView.setCenter(cebuCity);
       mapView.setZoom(12); // // (0 = Whole World, 10 = Province, 14-15 = City Streets, 19 = Building level)
       stackpaneLiveMap.getChildren().add(mapView);
   }
   private MapView createMapView(){
       MapView mapView = new MapView();
       mapView.setPrefSize(500, 400);
       mapView.setZoom(5);
       mapView.flyTo(0, cebuCity, 0.1);
       return mapView;
   }
}
