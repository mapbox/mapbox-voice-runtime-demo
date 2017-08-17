# mapbox-voice-runtime-demo

This demo shows the flexibility and power of runtime styling functionality within the Mapbox Maps SDK for Android. Runtime styling can be combined with essentially any type of API that sends back a response. In this example, the API.AI artificial intelligence platform receives voice commands from the device, figures out what the command's intent is (tilt map, move camera to new location, change color of the water layer, etc.), and sends back the appropriate action for the map to take.

![](https://github.com/mapbox/mapbox-voice-runtime-demo/blob/master/demo-in-action.gif)



**[Full video of demo in action](https://drive.google.com/a/mapbox.com/file/d/0B66w40cI4PGHS2FKVTRGZnE2c0E/view?usp=sharing)**


## Instructions to open this project on your device

1. Clone this repo and open the project in [Android Studio **3.0**](https://developer.android.com/studio/preview/index.html). 

2. Retrieve your Mapbox account's _default public token_, which can be found at [https://www.mapbox.com/studio/account/tokens/](https://www.mapbox.com/studio/account/tokens/).

3. Find this project's `strings.xml` file in Android Studio and paste the Mapbox default public token in the string resource that's already in the `strings.xml` file:
```<string name="mapbox_access_token">PASTE_YOUR_MAPBOX_TOKEN_HERE</string>```

4. Create an API.AI account at [api.ai](api.ai) and then retrieve your _client access token_. [Instructions on how to do that can be found here](https://api.ai/docs/reference/agent/#obtaining_access_tokens).

5. Find this project's `strings.xml` file in Android Studio and paste the API.AI client access token in the string resource that's already in the `strings.xml` file:
```<string name="api_ai_access_token">PASTE_YOUR_API_AI_CLIENT_ACCESS_TOKEN_HERE</string>```

6. You're all set, so run the app from Android Studio!
