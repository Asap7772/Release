# Release Application
Release version of Supervision Search + Scan<br/>
Signed APK and output json found in Release/app/release

# Setup for Google MLKIT (Current API)
1. Access the Firebase console and create a new project.
    1. Link to console: https://console.firebase.google.com
    2. Other Firebase dependencies such as analytics can also be accessed here
2. Click on the setting icon and click project settings and add firebase to an android, iOS, or web application (Android Steps Below)
    1. Add the name of the package from your project
    2. Optionally add a nickname to the Application and Debug signing certificate SHA-1
    3. After you register the app, download the config json file and navigate to your android studio project. There go to project view (not android view), and place the file under the app folder. The file is called google-services.json
    4. Navigate back to Android View and add the following dependencies to your app level build.gradle file (Module: app)
        1. `implementation 'com.google.firebase:firebase-core:16.0.0'`
        2. `implementation 'com.google.firebase:firebase-ml-vision:16.0.0'`
        3. `implementation 'com.google.firebase:firebase-ml-vision-image-label-model:15.0.0'`
        4. `implementation 'com.google.firebase:firebase-ml-model-interpreter:16.0.0'`
    5. Add the following line to the end of the build.gradle file
        1. `apply plugin: 'com.google.gms.google-services'`
    6. The compile sdk for this application was set at 28 with the minimum level being 23
    7. In the Manifest File, add the needed permissions and features
3. The CameraSource.java, CameraSourcePreview.java, and OCRProcessor.java are three files within this repository that were written to handle processing an image in a scan based mode. Multithreading was used to keep this functionality off the main UI Thread.
4. StringComp.java houses the Modified Levenshtein Distance Algorithm used in the application for comparing the OCR Text to a search word
5. The functionality that MLKIT currently offers includes:
    1. Text Recognition
    2. Barcode Scanning
    3. Face Detection
    4. Image Labeling
    5. Custom TensorFlow Model 

# Setup for Amazon Rekognition (Alternate API Possibility)
1. Access the AWS Console and create a project make sure you save the access key id and secret key for your credentials
    1. Current Keys:
        1. `private static final String ACCESS_KEY_ID = "AKIAJBXGT2WC6GRKIYDA";`
        2. `private static final String SECRET_KEY = "33+1F7blzVMKXTJncK9h9Iy0GkptUa6mqs3H0CKB";`
    2. Create a new AWSCredentials object in your Activity that returns the access key id and the secret key above
    3. Then create a AmazonRekognition Object the following way: <br/> AmazonRekognition recognition = new AmazonRekognitionClient(credentials); 
2. Use the following dependencies in your app level build.gradle file (Module: app)
    1. `implementation 'com.amazonaws:aws-android-sdk-core:2.6.+'`
    2. `implementation 'com.amazonaws:aws-android-sdk-s3:2.6.+'`
    3. `implementation 'com.amazonaws:aws-android-sdk-ddb:2.6.+'`
    4. `implementation 'com.amazonaws:aws-android-sdk-rekognition:2.6.+'`
    5. `implementation ('com.amazonaws:aws-android-sdk-cognitoauth:2.6.+@aar') { transitive = true }`
3. Ensure that StrictMode is turned off in the OnCreate method in your activity. This allows for you to access the api with a network connection. Note: you should check whether a network connection is possible 
    1. `StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();`
    2. `StrictMode.setThreadPolicy(policy);` 
4. In the Manifest File, add the needed permissions and features such as an internet connection
