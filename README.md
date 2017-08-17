# mapbox-voice-runtime-demo

- Runtime styling allows developers to push the limits of map interaction
- Mapbox supports greater accessibility in your Android project
- Mapbox can somehow be combined with almost any API that sends back some sort of response.


![](https://github.com/mapbox/mapbox-voice-runtime-demo/blob/master/demo-in-action.gif)



[Full video of demo in action](https://drive.google.com/a/mapbox.com/file/d/0B66w40cI4PGHS2FKVTRGZnE2c0E/view?usp=sharing)


## Instructions to open this project on your device

1. Clone this repo and open the project in Android Studio. 

2. Retrieve your Mapbox account's _default public token_, which can be found at [https://www.mapbox.com/studio/account/tokens/](https://www.mapbox.com/studio/account/tokens/).

3. Find this project's strings.xml file in Android Studio and enter the Mapbox default public token like below:
```<string name="mapbox_access_token">PASTE_YOUR_MAPBOX_TOKEN_HERE</string>```

4. Create an API.AI account at [api.ai](api.ai) and then retrieve your _client access token_. [Instructions on how to do that can be found here](https://api.ai/docs/reference/agent/#obtaining_access_tokens).

5. Find this project's strings.xml file in Android Studio and enter the API.AI client access token like below:
```<string name="api_ai_access_token">PASTE_YOUR_API_AI_CLIENT_ACCESS_TOKEN_HERE</string>```

6. You're all set, so run the app from Android Studio!
