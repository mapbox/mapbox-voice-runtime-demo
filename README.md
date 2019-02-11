# mapbox-voice-runtime-demo

This demo shows the flexibility and power of runtime styling functionality within the Mapbox Maps SDK for Android. Runtime styling can be combined with essentially any type of API that sends back a response. In this example, the Dialogflow artificial intelligence platform receives voice commands from the device, figures out what the command's intent is (tilt map, move camera to new location, change color of the water layer, etc.), and sends back the appropriate action for the map to take.

![](https://github.com/mapbox/mapbox-voice-runtime-demo/blob/master/demo-in-action.gif)



**More information about this project can be found in [this Mapbox blog post](https://blog.mapbox.com/style-a-map-on-android-with-your-voice-9cb611bbc901)**


## Instructions to open this project on your device

1. Clone this repo and open the project in [Android Studio](https://developer.android.com/studio/preview/index.html). 

2. Retrieve your Mapbox account's _default public token_, which can be found at [https://www.mapbox.com/studio/account/tokens/](https://www.mapbox.com/studio/account/tokens/).

3. Find [this project's `strings.xml` file](https://github.com/mapbox/mapbox-voice-runtime-demo/blob/master/app/src/main/res/values/strings.xml) in Android Studio and paste the Mapbox default public token in [the string resource that's already in the `strings.xml` file](https://github.com/mapbox/mapbox-voice-runtime-demo/blob/master/app/src/main/res/values/strings.xml#L4):
```<string name="mapbox_access_token">PASTE_YOUR_MAPBOX_TOKEN_HERE</string>```

4. Create an API.AI account at [Dialogflow](https://dialogflow.com/) and then retrieve your _client access token_. [Instructions on how to do that can be found here](https://dialogflow.com/docs/reference/agent/#obtaining_access_tokens).

5. Find [this project's `strings.xml` file](https://github.com/mapbox/mapbox-voice-runtime-demo/blob/master/app/src/main/res/values/strings.xml) in Android Studio and paste the API.AI client access token in [the string resource that's already in the `strings.xml` file](https://github.com/mapbox/mapbox-voice-runtime-demo/blob/master/app/src/main/res/values/strings.xml#L5):
```<string name="api_ai_access_token">PASTE_YOUR_DIALOGFLOW_CLIENT_ACCESS_TOKEN_HERE</string>```

6. You're all set, so run the app from Android Studio!
