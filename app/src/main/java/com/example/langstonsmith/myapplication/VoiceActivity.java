package com.example.langstonsmith.myapplication;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;

import java.util.ArrayList;
import java.util.Locale;

import ai.api.android.AIConfiguration;
import ai.api.android.AIDataService;
import ai.api.model.AIRequest;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class VoiceActivity extends AppCompatActivity implements OnMapReadyCallback {

  private static final int SPEECH_INPUT_CODE = 100;
  private MapView mapView;
  private MapboxMap mapboxMap;
  private AIDataService aiDataService;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Mapbox access token is configured here. This needs to be called either in your application
    // object or in the same activity which contains the mapview.
    Mapbox.getInstance(this, getString(R.string.mapbox_access_token));

    // This contains the MapView in XML and needs to be called after the access token is configured.
    setContentView(R.layout.activity_voice);

    ButterKnife.bind(this);

    mapView = findViewById(R.id.voice_mapView);
    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this);
  }

  @Override
  public void onMapReady(@NonNull final MapboxMap mapboxMap) {
    mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        // Map is set up and the style has loaded. Now you can add data or make other map adjustments
        VoiceActivity.this.mapboxMap = mapboxMap;
        setupApiAiConfiguration();
      }
    });
  }

  @OnClick(R.id.microphone_fab)
  public void microphoneButtonClick(View view) {
    beginListening();
  }

  private void setupApiAiConfiguration() {
    final AIConfiguration aiConfig = new AIConfiguration(getString(R.string.api_ai_access_token),
      AIConfiguration.SupportedLanguages.English,
      AIConfiguration.RecognitionEngine.System);
    aiDataService = new AIDataService(this, aiConfig);
  }

  private void beginListening() {
    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
      RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
    intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.ask_me));
    try {
      startActivityForResult(intent, SPEECH_INPUT_CODE);
    } catch (ActivityNotFoundException a) {
      Toast.makeText(getApplicationContext(),
        R.string.speech_not_supported,
        Toast.LENGTH_LONG).show();
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    switch (requestCode) {
      case SPEECH_INPUT_CODE: {
        if (resultCode == RESULT_OK && null != data) {
          ArrayList<String> result = data
            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
          AIRequest request = new AIRequest();
          request.setQuery(result.get(0));

          // Send API.AI the text captured by the device's microphone
          new ChatRequest(this, aiDataService, mapboxMap).execute(request);

        }
        break;
      }
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    mapView.onResume();
  }

  @Override
  protected void onStart() {
    super.onStart();
    mapView.onStart();
  }

  @Override
  protected void onStop() {
    super.onStop();
    mapView.onStop();
  }

  @Override
  public void onPause() {
    super.onPause();
    mapView.onPause();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mapView.onDestroy();
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    mapView.onLowMemory();
  }
}