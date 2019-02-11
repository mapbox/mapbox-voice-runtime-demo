package com.example.langstonsmith.myapplication;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;

import java.util.List;

import ai.api.AIDataService;
import ai.api.AIServiceException;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Result;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.mapbox.mapboxsdk.style.layers.Property.NONE;
import static com.mapbox.mapboxsdk.style.layers.Property.VISIBLE;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;

/**
 * Async task to get JSON response from API.AI
 */
public class ChatRequest extends AsyncTask<AIRequest, Void, AIResponse> {

  private static final String ORANGE_COLOR = "#FFA500";
  private static final String INDIGO_COLOR = "#4B0082";
  private static final String VIOLET_COLOR = "#8D38C9";
  private static final String PURPLE_COLOR = "#800080";
  private static final String MAGENTA_COLOR = "#FF00FF";
  private static final String PINK_COLOR = "#FAAFBE";
  private static final String BROWN_COLOR = "#A52A2A";

  private AIDataService service;
  private MapboxMap map;
  private Context context;
  private String searchedLocation;
  private String TAG = "ChatRequest";
  private int cameraUpdateSpeedInMillSec = 1500;
  private int colorIntToUse;
  private boolean searchedForSpecificAddress;

  public ChatRequest(Context context, AIDataService service, MapboxMap mapboxMap) {
    this.service = service;
    this.service = service;
    this.map = mapboxMap;
    this.context = context;
  }

  @Override
  protected AIResponse doInBackground(AIRequest... requests) {
    final AIRequest request = requests[0];
    try {
      return service.request(request);
    } catch (AIServiceException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  protected void onPostExecute(AIResponse aiResponse) {
    if (aiResponse != null) {

      // Get result from API.AI
      final Result result = aiResponse.getResult();

      // Parse through API.AI JSON response to get intent text
      String intentName = result.getMetadata().getIntentName();

      switch (intentName) {

        case "Adjust layer properties":
          String colorString = "";
          String layerToAdjust = "";
          String propFactorMethodToUse = "";
          float numberForAdjustment = 0;
          if (result.getParameters().get("propFactory_method") != null) {
            propFactorMethodToUse = result.getParameters().get("propFactory_method").getAsString();
          } else {
            Toast.makeText(context, R.string.please_repeat, Toast.LENGTH_LONG).show();
          }
          if (result.getParameters().get("number") != null) {
            numberForAdjustment = result.getParameters().get("number").getAsFloat();
          }
          if (result.getParameters().get("layer") != null) {
            try {
              layerToAdjust = result.getParameters().get("layer").getAsJsonArray().get(0).getAsString();
            } catch (Exception exception) {
              Log.e(TAG, "onPostExecute: " + exception);
            }
          } else {
            Toast.makeText(context, R.string.please_repeat_layer, Toast.LENGTH_LONG).show();
          }
          if (result.getParameters().get("color").getAsJsonArray().size() > 0) {
            colorString = result.getParameters().get("color").getAsString();
            adjustMap(propFactorMethodToUse, numberForAdjustment, layerToAdjust, colorString);
          } else {
            adjustMap(propFactorMethodToUse, numberForAdjustment, layerToAdjust, null);
          }
          break;

        case "Change camera properties":
          // Parse through JSON to figure out what camera property is being changed
          String cameraPropertyToChange = result.getParameters().get("camera_property").getAsString();
          // Parse through JSON to figure out how much to zoom, tilt, rotate the map by
          double numberToUse = result.getParameters().get("number").getAsFloat();
          switch (cameraPropertyToChange) {
            case "mapbox_cameraTargetLng":
              updateToNewLat(numberToUse);
              break;
            case "mapbox_cameraTargetLat":
              updateToNewLong(numberToUse);
              break;
            case "mapbox_cameraZoom":
              // If required, converting said negative number to positive for adjustment to work
              if (numberToUse < 0) {
                numberToUse = numberToUse * -1;
              }
              updateToNewZoomLevel(numberToUse);
              break;
            case "mapbox_cameraBearing":
              // If required, converting said negative number to positive for adjustment to work
              if (numberToUse < 0) {
                numberToUse = numberToUse * -1;
              }
              updateToNewBearing(numberToUse);
              break;
            case "mapbox_cameraTilt":
              // If required, converting said negative number to positive for adjustment to work
              if (numberToUse < 0) {
                numberToUse = numberToUse * -1;
              }
              updateToNewTilt(numberToUse);
              break;
          }
          break;

        case "Adjust layer visibility":
          if (result.getParameters().get("visibility").getAsString().contentEquals("hide")) {
            String layerToHide = result.getParameters().get("layer").getAsString();
            Layer layerToShow = map.getStyle().getLayer(layerToHide);
            layerToShow.setProperties(visibility(NONE));
          }
          if (result.getParameters().get("visibility").getAsString().contentEquals("show")) {
            String layerName = result.getParameters().get("layer").getAsString();
            Layer layerToShow = map.getStyle().getLayer(layerName);
            layerToShow.setProperties(visibility(VISIBLE));
          }
          break;

        case "Move map to location":
          if (result.getParameters().get("address") != null) {
            searchedLocation = result.getParameters().get("address").getAsString();
            searchedForSpecificAddress = true;
            searchForRequestedLocationWithMapboxGeocoder(searchedLocation);
          } else if (result.getParameters().get("geo-city") != null) {
            searchedLocation = result.getParameters().get("geo-city").getAsString();
            searchForRequestedLocationWithMapboxGeocoder(searchedLocation);
          } else if (result.getParameters().get("geo-country") != null) {
            searchedLocation = result.getParameters().get("geo-country").getAsString();
            searchForRequestedLocationWithMapboxGeocoder(searchedLocation);
          }
          break;
      }
    }
  }

  private void searchForRequestedLocationWithMapboxGeocoder(String searchedLocation) {
    MapboxGeocoding mapboxGeocoding = MapboxGeocoding.builder()
      .accessToken(context.getString(R.string.mapbox_access_token))
      .query(searchedLocation)
      .build();
    mapboxGeocoding.enqueueCall(new Callback<GeocodingResponse>() {
      @Override
      public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {
        List<CarmenFeature> results = response.body().features();
        if (results.size() > 0) {
          // Log the first results position.
          Point firstResultPoint = results.get(0).center();

          // Create a new LatLng object with the requested position
          LatLng requestedCoordinate = new LatLng(firstResultPoint.latitude(),
            firstResultPoint.longitude());

          // Update map with the new LatLng object
          updateMapToNewLocation(requestedCoordinate);

        } else {
          // No result for the geocoder request was found
          Log.e(TAG, "onResponse: No geocoder result found");
          Toast.makeText(context, R.string.could_not_find_location, Toast.LENGTH_LONG).show();
        }
      }

      @Override
      public void onFailure(Call<GeocodingResponse> call, Throwable throwable) {
        throwable.printStackTrace();
      }
    });
  }

  private void updateMapToNewLocation(LatLng requestedCoordinate) {
    // Set zoom based on whether search was for a specific address vs. a entire city/country
    double finalZoom = searchedForSpecificAddress ? 15 : 8;

    // Pass LatLng object into .target() for the camera update
    CameraPosition position = new CameraPosition.Builder()
      .target(requestedCoordinate)
      .zoom(finalZoom)
      .build();

    // Animate the camera to move it to the new location
    map.animateCamera(CameraUpdateFactory
      .newCameraPosition(position), cameraUpdateSpeedInMillSec);
  }


  private void updateToNewLat(double newLatValue) {
    LatLng newLatLng = new LatLng(newLatValue, map.getCameraPosition().target.getLongitude());

    CameraPosition position = new CameraPosition.Builder()
      .target(newLatLng)
      .build();
    map.animateCamera(CameraUpdateFactory
      .newCameraPosition(position), cameraUpdateSpeedInMillSec);
  }

  private void updateToNewLong(double newLongValue) {
    LatLng newLatLng = new LatLng(map.getCameraPosition().target.getLatitude(), newLongValue);

    CameraPosition position = new CameraPosition.Builder()
      .target(newLatLng)
      .build();
    map.easeCamera(CameraUpdateFactory
      .newCameraPosition(position), cameraUpdateSpeedInMillSec);
  }

  private void updateToNewZoomLevel(double newZoomLevel) {
    CameraPosition position = new CameraPosition.Builder()
      .zoom(newZoomLevel)
      .build();
    map.easeCamera(CameraUpdateFactory
      .newCameraPosition(position), cameraUpdateSpeedInMillSec);
  }

  private void updateToNewBearing(double newBearing) {
    CameraPosition position = new CameraPosition.Builder()
      .bearing(newBearing)
      .build();

    map.easeCamera(CameraUpdateFactory
      .newCameraPosition(position), cameraUpdateSpeedInMillSec);
  }

  private void updateToNewTilt(double newTilt) {
    CameraPosition position = new CameraPosition.Builder()
      .tilt(newTilt)
      .build();
    map.easeCamera(CameraUpdateFactory
      .newCameraPosition(position), cameraUpdateSpeedInMillSec);
  }

  private void adjustMap(String propFactorMethodToUse, float numberForAdjustment, String layerName, @Nullable String colorToChangeLayerTo) {
    // Get specified layer from map
    Layer layerToChange = map.getStyle().getLayerAs(layerName);
    if (layerToChange == null) {
      Toast.makeText(context, R.string.no_layer_in_map, Toast.LENGTH_LONG).show();
      return;
    }

    setColorToUse(colorToChangeLayerTo);

    //region Code for runtime styling the specific layer
    // TODO: Only some of the PropertyFactory runtime styling methods have been made available in this example
    if (propFactorMethodToUse != null) {
      switch (propFactorMethodToUse) {
        case "backgroundColor":
          try {
            layerToChange.setProperties(PropertyFactory.backgroundColor(colorIntToUse));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "backgroundOpacity":
          try {
            layerToChange.setProperties(PropertyFactory.backgroundOpacity(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "backgroundPattern":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.backgroundPattern());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "circleBlur":
          try {
            layerToChange.setProperties(PropertyFactory.circleBlur(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "circleColor":
          try {
            layerToChange.setProperties(PropertyFactory.circleColor(colorIntToUse));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "circleOpacity":
          try {
            layerToChange.setProperties(PropertyFactory.circleOpacity(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "circlePitchScale":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.circlePitchScale());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "circleRadius":
          try {
            layerToChange.setProperties(PropertyFactory.circleRadius(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "circleStrokeColor":
          try {
            layerToChange.setProperties(PropertyFactory.circleStrokeColor(colorIntToUse));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "circleStrokeOpacity":
          try {
            layerToChange.setProperties(PropertyFactory.circleStrokeOpacity(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "circleStrokeWidth":
          try {
            layerToChange.setProperties(PropertyFactory.circleStrokeWidth(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "circleTranslate":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.circleTranslate());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "circleTranslateAnchor":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.circleTranslateAnchor());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "fillAntialias":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.fillAntialias());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "fillColor":
          try {
            layerToChange.setProperties(PropertyFactory.fillColor(colorIntToUse));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "fillExtrusionBase":
          try {
            layerToChange.setProperties(PropertyFactory.fillExtrusionBase(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "fillExtrusionColor":
          try {
            layerToChange.setProperties(PropertyFactory.fillExtrusionColor(colorIntToUse));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "fillExtrusionHeight":
          try {
            layerToChange.setProperties(PropertyFactory.fillExtrusionHeight(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "fillExtrusionOpacity":
          try {
            layerToChange.setProperties(PropertyFactory.fillExtrusionOpacity(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "fillExtrusionPattern":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.fillExtrusionPattern());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "fillExtrusionTranslate":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.fillExtrusionTranslate());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "fillExtrusionTranslateAnchor":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.fillExtrusionTranslateAnchor());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "fillOpacity":
          try {
            layerToChange.setProperties(PropertyFactory.fillOpacity(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "fillOutlineColor":
          try {
            layerToChange.setProperties(PropertyFactory.fillOutlineColor(colorIntToUse));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "fillPattern":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.fillPattern());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "fillTranslate":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.fillTranslate());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "fillTranslateAnchor":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.fillTranslateAnchor());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "iconAllowOverlap":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.iconAllowOverlap());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "iconColor":
          try {
            layerToChange.setProperties(PropertyFactory.iconColor(colorIntToUse));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "iconHaloBlur":
          try {
            layerToChange.setProperties(PropertyFactory.iconHaloBlur(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "iconHaloColor":
          try {
            layerToChange.setProperties(PropertyFactory.iconHaloColor(colorIntToUse));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "iconHaloWidth":
          try {
            layerToChange.setProperties(PropertyFactory.iconHaloWidth(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "iconIgnorePlacement":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.iconIgnorePlacement());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "iconImage":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.iconImage());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "iconKeepUpright":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.iconKeepUpright());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "iconOffset":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.iconOffset());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "iconOpacity":
          try {
            layerToChange.setProperties(PropertyFactory.iconOpacity(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "iconOptional":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.iconOptional());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "iconPadding":
          try {
            layerToChange.setProperties(PropertyFactory.iconPadding(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "iconRotate":
          try {
            layerToChange.setProperties(PropertyFactory.iconRotate(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "iconRotationAlignment":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.iconRotationAlignment());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "iconSize":
          try {
            layerToChange.setProperties(PropertyFactory.iconSize(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "iconTextFit":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.iconTextFit());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "iconTextFitPadding":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.iconTextFitPadding());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "iconTranslate":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.iconTranslate());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "iconTranslateAnchor":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.iconTranslateAnchor());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "lineBlur":
          try {
            layerToChange.setProperties(PropertyFactory.lineBlur(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "lineCap":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.lineCap());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "lineColor":
          try {
            layerToChange.setProperties(PropertyFactory.lineColor(colorIntToUse));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "lineDasharray":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.lineDasharray());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "lineGapWidth":
          try {
            layerToChange.setProperties(PropertyFactory.lineGapWidth(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "lineJoin":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.lineJoin());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "lineMiterLimit":
          try {
            layerToChange.setProperties(PropertyFactory.lineMiterLimit(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "lineOffset":
          try {
            layerToChange.setProperties(PropertyFactory.lineOffset(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "lineOpacity":
          try {
            layerToChange.setProperties(PropertyFactory.lineOpacity(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "linePattern":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.linePattern());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "lineRoundLimit":
          try {
            layerToChange.setProperties(PropertyFactory.lineRoundLimit(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "lineTranslate":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.lineTranslate());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "lineTranslateAnchor":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.lineTranslateAnchor());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "lineWidth":
          try {
            layerToChange.setProperties(PropertyFactory.lineWidth(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "rasterBrightnessMax":
          try {
            layerToChange.setProperties(PropertyFactory.rasterBrightnessMax(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "rasterBrightnessMin":
          try {
            layerToChange.setProperties(PropertyFactory.rasterBrightnessMin(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "rasterContrast":
          try {
            layerToChange.setProperties(PropertyFactory.rasterContrast(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "rasterFadeDuration":
          try {
            layerToChange.setProperties(PropertyFactory.rasterFadeDuration(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "rasterHueRotate":
          try {
            layerToChange.setProperties(PropertyFactory.rasterHueRotate(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "rasterOpacity":
          try {
            layerToChange.setProperties(PropertyFactory.rasterOpacity(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "rasterSaturation":
          try {
            layerToChange.setProperties(PropertyFactory.rasterSaturation(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "symbolAvoidEdges":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.symbolAvoidEdges());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "symbolPlacement":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.symbolPlacement());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "symbolSpacing":
          try {
            layerToChange.setProperties(PropertyFactory.symbolSpacing(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "textAllowOverlap":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.textAllowOverlap());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "textAnchor":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.textAnchor());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "textColor":
          try {
            layerToChange.setProperties(PropertyFactory.textColor(colorIntToUse));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "textField":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.textField());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "textFont":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.textFont());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "textHaloBlur":
          try {
            layerToChange.setProperties(PropertyFactory.textHaloBlur(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "textHaloColor":
          try {
            layerToChange.setProperties(PropertyFactory.textHaloColor(colorIntToUse));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "textHaloWidth":
          try {
            layerToChange.setProperties(PropertyFactory.textHaloWidth(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "textIgnorePlacement":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.textIgnorePlacement());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "textJustify":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.textJustify());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "textKeepUpright":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.textKeepUpright());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "textLetterSpacing":
          try {
            layerToChange.setProperties(PropertyFactory.textLetterSpacing(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "textLineHeight":
          try {
            layerToChange.setProperties(PropertyFactory.textLineHeight(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "textMaxAngle":
          try {
            layerToChange.setProperties(PropertyFactory.textMaxAngle(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "textMaxWidth":
          try {
            layerToChange.setProperties(PropertyFactory.textMaxWidth(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "textOffset":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.textOffset());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "textOpacity":
          try {
            layerToChange.setProperties(PropertyFactory.textOpacity(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "textOptional":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.textOptional());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "textPadding":
          try {
            layerToChange.setProperties(PropertyFactory.textPadding(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "textPitchAlignment":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.textPitchAlignment());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "textRotate":
          try {
            layerToChange.setProperties(PropertyFactory.textRotate(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "textRotationAlignment":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.textRotationAlignment());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "textSize":
          try {
            layerToChange.setProperties(PropertyFactory.textSize(numberForAdjustment));
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "textTransform":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.textTransform());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "textTranslate":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.textTranslate());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;
        case "textTranslateAnchor":
          try {
            showNotAvailableYetToast();
            // Uncomment method below and pass through needed boolean, String, or other variable/parameter
            //          layerToChange.setProperties(PropertyFactory.textTranslateAnchor());
          } catch (Exception exception) {
            Toast.makeText(context, "" + exception, Toast.LENGTH_LONG).show();
          }
          break;

      }
    }
    //endregion
  }

  private void setColorToUse(String colorToChangeLayerTo) {

    //region Retrieving color
    if (colorToChangeLayerTo != null) {
      // Change layer's color via runtime styling. Add another case statement if you want a specific color name/value
      switch (colorToChangeLayerTo) {
        case "Red":
          colorIntToUse = Color.RED;
          break;
        case "Orange":
          colorIntToUse = Color.parseColor(ORANGE_COLOR);
          break;
        case "Yellow":
          colorIntToUse = Color.YELLOW;
          break;
        case "Green":
          colorIntToUse = Color.GREEN;
          break;
        case "Cyan":
          colorIntToUse = Color.CYAN;
          break;
        case "Blue":
          colorIntToUse = Color.BLUE;
          break;
        case "Indigo":
          colorIntToUse = Color.parseColor(INDIGO_COLOR);
          break;
        case "Violet":
          colorIntToUse = Color.parseColor(VIOLET_COLOR);
          break;
        case "Purple":
          colorIntToUse = Color.parseColor(PURPLE_COLOR);
          break;
        case "Magenta":
          colorIntToUse = Color.parseColor(MAGENTA_COLOR);
          break;
        case "Pink":
          colorIntToUse = Color.parseColor(PINK_COLOR);
          break;
        case "Brown":
          colorIntToUse = Color.parseColor(BROWN_COLOR);
          break;
        case "White":
          colorIntToUse = Color.WHITE;
          break;
        case "Gray":
          colorIntToUse = Color.GRAY;
          break;
        case "Grey":
          colorIntToUse = Color.GRAY;
          break;
        case "Black":
          colorIntToUse = Color.BLACK;
          break;
      }
    }
  }

  private void showNotAvailableYetToast() {
    Toast.makeText(context, R.string.prop_factory_method_not_set_up, Toast.LENGTH_LONG).show();
  }
}

