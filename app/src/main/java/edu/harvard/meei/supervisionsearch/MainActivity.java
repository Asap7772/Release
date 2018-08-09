package edu.harvard.meei.supervisionsearch;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.os.ConfigurationCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import de.cketti.mailto.EmailIntentBuilder;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener, PopupMenu.OnMenuItemClickListener {
    // =============================================================================================
    // Constants
    // =============================================================================================
    private static final int ACT_CHECK_TTS_DATA = 1000;
    private static final int SPEECH_REQUEST_CODE = 0;
    public static final int PERMISSION_REQUESTS = 1;
    public static final String TAG = "OCR Activity";
    public static final int vibrateLength = 100;
    private static final int MEDIA_TYPE_IMAGE = 14;
    private static final int MEDIA_TYPE_VIDEO = 15;
    private static final float VIDEO_STROKE = 5;
    private static final float PICTURE_STROKE = 40;
    private String sharedPrefFile = "com.example.anikaitsingh.realtimecamerasource";
    private float mDist = 0;


    // =============================================================================================
    // Camera Variables
    // =============================================================================================
    private CameraSourcePreview preview;
    public CameraSource cameraSource;

    // =============================================================================================
    // OCR Variables
    // =============================================================================================
    private OcrProcessor processor;
    private FrameProcessor frameProcessor;
    private ClassificationScheme scheme = ClassificationScheme.NONE;
    private ArrayList<Rect> arr;
    private Bitmap bmp;
    private Bitmap bmpaltr;

    // =============================================================================================
    // View Variables
    // =============================================================================================
    private RotatableZoomableImageView ziv;
    private ImageButton zoom;
    private ImageView next, previous;
    private ImageView settings, info;
    private GraphicOverlay graphicOverlay;
    private ImageView download;
    private TextToSpeech mTTS;
    EditText search;
    SeekBar zoomBar;
    DecimalFormat format = new DecimalFormat("0.0");

    private int progress = 0;
    SeekBar.OnSeekBarChangeListener a = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            MainActivity.this.progress = progress;
            if (!compProg) {
                updateZoom();
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };
    private boolean flashlightOn = false;
    private boolean compProg = false;
    private boolean rot = false;
    private Bitmap bmphil;
    private float angle;
    private ImageView scan;
    private ImageButton restart;
    private int oldWidth;
    private SharedPreferences mPreferences;
    private ImageButton flashlight;
    private Thread capture;
    private boolean zoomIn = true;
    private int cameraPictureRotation = 0;

    private void updateZoom() {
        cameraSource.zoom(progress / 100.0);
    }

    // =============================================================================================
    // Flags and other primitive values
    // =============================================================================================

    boolean secondTime = false;
    boolean processingDone = false;
    public boolean success = false;
    private int num = 0;
    private boolean multi = true;
    private boolean nextB = true;
    private boolean wordFound;


    // =============================================================================================
    // Listener Variables and Threads
    // =============================================================================================

    private Thread t;

    private boolean cameraCapture = false;
    private TextView.OnEditorActionListener listener = new TextView.OnEditorActionListener() {
        @Override
        /**
         * Method is called when there is a change in the status of the edittext used for input
         * If there is a change when the activity is in the second state (i.e. when the zoomable
         * imageview is present), then the update method is called to find a different word on the
         * screen.
         */

        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event != null &&
                            event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if (event == null || !event.isShiftPressed()) {
                    saySomething("Searching for " + search.getText().toString());
                    String query = v.getText().toString();
                    if (success && secondTime) {
                        update(query);

                        if (arr.size() != 1) {
                            saySomething(query + " found in " + arr.size() + " locations");
                        } else {
                            saySomething(query + " found in 1 location");
                        }
                    } else if (cameraCapture) {
                        update(query);

                        if (arr.size() != 1) {
                            saySomething(query + " found in " + arr.size() + " locations");
                        } else {
                            saySomething(query + " found in 1 location");
                        }
                    }
                    hideKeyboard(MainActivity.this);
                    return true; // consume.
                }
            }
            hideKeyboard(MainActivity.this);
            return false; // pass on to other listeners.
        }
    };

    private int previewZoomLevel = 0;
    private boolean keyboardVisible;

    // =============================================================================================
    // Methods
    // =============================================================================================

    /**
     * Method called when application is created. This is used to initialize initial values such as
     * visibility and the views. Detection thread is started here too.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (!allPermissionsGranted()) {
            getRuntimePermissions();
        }

        //superclass method called
        super.onCreate(savedInstanceState);

        //ensures that there is not title present on screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        //initialized views from the xml file activity_main.xml
        search = (EditText) findViewById(R.id.search);
        oldWidth = search.getWidth();

        search.setOnEditorActionListener(listener);
        preview = (CameraSourcePreview) findViewById(R.id.preview);
        graphicOverlay = (GraphicOverlay) findViewById(R.id.overlay);
        ziv = (RotatableZoomableImageView) findViewById(R.id.ivMainImageMainActivity);
        zoom = (ImageButton) findViewById(R.id.zoomButton);
        next = (ImageView) findViewById(R.id.next);
        previous = (ImageView) findViewById(R.id.previous);
        settings = (ImageView) findViewById(R.id.settings);
        info = (ImageView) findViewById(R.id.info);
        zoomBar = (SeekBar) findViewById(R.id.seekBar);
        download = (ImageView) findViewById(R.id.download);
        scan = (ImageView) findViewById(R.id.scan);
        restart = (ImageButton) findViewById(R.id.restart);
        flashlight = (ImageButton) findViewById(R.id.flashlight);

        setImage(zoom, R.raw.zoomin);

        scan.setOnTouchListener(new View.OnTouchListener() {
            /**
             * When touch event occurs, handles lift and a simple touch & hold
             * @param v
             * @param event
             * @return
             */
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    //sets boolean touch to true
                    cameraSource.touch = true;
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    //sets boolean lift to true if the touch variable was true
                    if (cameraSource.touch) {
                        cameraSource.lift = true;
                    } else cameraSource.lift = false;
                    cameraSource.touch = false;
                }
                return true;
            }
        });

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        KeyboardUtils.addKeyboardToggleListener(this, new KeyboardUtils.SoftKeyboardToggleListener() {
            @Override
            public void onToggleSoftKeyboard(boolean isVisible) {
                Log.d("keyboard", "keyboard visible: " + isVisible);
                keyboardVisible = isVisible;
            }
        });


        //initialized camera source
        if (allPermissionsGranted()) {
            initCameraSource();
            startCameraSource();
        } else {
            getRuntimePermissions();
        }

        // Check to see if we have Text To Speech (TTS) voice data
        Intent ttsIntent = new Intent();
        ttsIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(ttsIntent, ACT_CHECK_TTS_DATA);

        //reset variable values
        wordFound = false;
        if (processor != null) {
            processor.setFoundOnce(false);
        }

        zoomBar.setOnSeekBarChangeListener(a);

        //set initial visibility of views
        ziv.setVisibility(View.GONE);
        zoom.setVisibility(View.GONE);
        next.setVisibility(View.GONE);
        previous.setVisibility(View.GONE);
        download.setVisibility(View.GONE);
        preview.setVisibility(View.VISIBLE);
        settings.setVisibility(View.VISIBLE);
        findViewById(R.id.flashlight).setVisibility(View.VISIBLE);
        zoomBar.setVisibility(View.VISIBLE);
        scan.setVisibility(View.VISIBLE);
        findViewById(R.id.cameraPreview).setVisibility(View.VISIBLE);
        findViewById(R.id.blackLinear).setVisibility(View.GONE);
        restart.setVisibility(View.GONE);
        search.setText(Holder.searchWord);
        cameraCapture = false;

        setImage(flashlight, R.raw.flashlight_invert);
        ziv.clearAnimation();
        //starts detection thread
        startFoundOnceThread();

        if (savedInstanceState != null) {
            mPreferences = getSharedPreferences(sharedPrefFile, MODE_PRIVATE);
            compProg = true;
            int zoomVal = mPreferences.getInt("zoom", 0);
            zoomBar.setProgress(zoomVal);
            updateZoom();
            compProg = false;
            scheme = ClassificationScheme.valueOf(mPreferences.getString("region", ClassificationScheme.NONE.name()));
        } else {
            mPreferences = getSharedPreferences(sharedPrefFile, MODE_PRIVATE);
        }
    }

    /**
     * Method that creates the overarching thread that waits until the word is found and highlights
     * the word when found.
     */
    private void startFoundOnceThread() {

        bmp = null;
        if (t != null) {
            t.interrupt();
            t = null;
            System.gc();
        }
        t = new Thread() {
            @Override
            public void run() {

                String found = getText();
                while (!(processor.isFoundOnce() && cameraSource.lift)) {
                    if (cameraSource.lift) {
                        //in case of accidental lift
                        cameraSource.lift = false;
                    }

                    //updates search keyword
                    if (!keyboardVisible) {
                        found = getText();
                    }

                    if (found.equals(Holder.searchWord)) {
                        cameraSource.searching = false;
                    } else {
                        cameraSource.searching = true;
                    }
                    //Log.d(TAG, "Searching");
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        findViewById(R.id.capture).setVisibility(View.GONE);
                        findViewById(R.id.scan).setVisibility(View.GONE);
                    }
                });

                success = true;

                Log.d(TAG, "Found");


                getBitmap(camera);

                while (!pictureDone) {
                    Log.d(TAG, "taking picture");
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.playSound();
                    }
                });

                //rotates image 90 degrees
                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);

                secondTime = true;
                boolean prevFrame = false;
                float angle = 0;
                if (!multi) {
                    //processes latest image for accuracy of the rectangle
                    Rect c = processBitmap(found);
                    if (c == null) {
                        //fallback to previous frame if not found
                        c = processor.getBoundingRect();
                        if (c == null) {
                            //so that app does not crash
                            bmpaltr = bmp;
                            return;
                        }
                        c = new Rect(translateX(c.left), translateY(c.top), translateX(c.right), translateY(c.bottom));
                        bmpaltr = null;
                        System.gc();
                        bmpaltr = highlightWordFound(bmp, c);
                    }
                    //created highlighted bitmap
                    bmpaltr = highlightWordFound(bmp, c);
                } else {
                    arr = processBitmapMulti(found);
                    if (arr == null || arr.size() < 1) {
                        Rect c = processor.getBoundingRect();
                        if (c == null) {
                            //so that app does not crash
                            bmpaltr = bmp;
                            saySomething(found + " is not present");
                        } else {
                            //user feedback
                            if (camera) {
                                c = new Rect(translateX(c.left), translateY(c.top), translateX(c.right), translateY(c.bottom));
                            }
                            bmpaltr = null;
                            System.gc();
                            bmpaltr = highlightWordFound(bmp, c);
                            if (camera) {
                                saySomething(found + " is not present");
                            } else {
                                saySomething(found + "found in one location");
                                prevFrame = true;
                            }
                        }
                    } else {
                        //user feedback
                        bmpaltr = null;
                        System.gc();
                        frameProcessor.sort();
                        angle = showPoints();
                    }
                }

                final boolean foundRect = arr != null;
                if (arr != null) {
                    rot = angle != 0;
                    bmpaltr = highlightWordFound(bmp, arr);
                    final float angleSet = angle;
                    if (rot) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ziv.startAnimation(-(int) angleSet);
                            }
                        });
                    }

                    if (arr.size() != 1) {
                        saySomething(found + " found in " + arr.size() + " locations");
                    } else {
                        saySomething(found + " found in 1 location");
                    }
                } else {
                    if (!prevFrame) bmpaltr = bmp;
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //sets visibility of components in second part of the app
                        ziv.setImageBitmap(bmpaltr);
                        ziv.setVisibility(View.VISIBLE);
                        zoom.setVisibility(View.VISIBLE);
                        restart.setVisibility(View.VISIBLE);
                        download.setVisibility(View.VISIBLE);
                        settings.setVisibility(View.GONE);
                        findViewById(R.id.flashlight).setVisibility(View.GONE);
                        preview.setVisibility(View.GONE);
                        zoomBar.setVisibility(View.GONE);
                        scan.setVisibility(View.GONE);
                        findViewById(R.id.cameraPreview).setVisibility(View.GONE);
                        findViewById(R.id.blackLinear).setVisibility(View.VISIBLE);
                        cameraSource.torch(false);
                    }
                });
                //resets values of button visibility
                showZoom();

                if (!foundRect && !prevFrame) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            zoom.setVisibility(View.GONE);
                        }
                    });
                }

            }
        };

        // starts thread
        t.start();
    }

    private float showPoints() {
        ArrayList<Point[]> pointsS = frameProcessor.getPoints();
        if (pointsS != null) {
            ArrayList<Float> slope = new ArrayList<Float>();
            for (Point[] points : pointsS) {
                if (points.length >= 4) {
                    slope.add(slope(points[0], points[1]));
                    slope.add(slope(points[2], points[3]));
                }
            }
            return meanAngle(slope);
        }

        return 0;

    }

    private float meanAngle(ArrayList<Float> slope) {
        float t = 0;
        for (float d : slope) {
            t += d;
        }
        t /= slope.size();
        return (float) Math.toDegrees(Math.atan(t));
    }

    private float medianAngle(ArrayList<Float> slope) {
        float[] arr = new float[slope.size()];
        for (int i = 0; i < slope.size(); i++) {
            arr[i] = slope.get(i);
        }

        Arrays.sort(arr);

        if (arr.length % 2 == 0) {
            return (arr[arr.length / 2] + arr[arr.length / 2 + 1]) / 2;
        } else {
            return arr[arr.length / 2];
        }
    }

    private float slope(Point p1, Point p2) {
        return ((float) (p1.y - p2.y)) / (p1.x - p2.x);
    }

    /**
     * Method to look for a word in the bitmap present in the zoomable image view after initial word
     * is found in the location (for context)
     *
     * @param query
     */
    private void update(String query) {
        zoomIn = true;
        setImage(zoom, R.raw.zoomin);
        if (!multi) {
            //called update method
            frameProcessor.updateRecog(query);
            Rect c = frameProcessor.getR();
            if (c == null) {
                //fallback to previous frame if not found
                c = processor.getBoundingRect();
                if (c == null) {
                    //if word not found in current frame or previous frame
                    vibratePhone();
                    Toast.makeText(this, "Word not Found", Toast.LENGTH_LONG).show();
                    saySomething("Word not Found");
                }
            }
            bmpaltr = null;
            System.gc();
            bmpaltr = highlightWordFound(bmp, c);
        } else {
            //called update method
            frameProcessor.updateRecogMulti(query);
            arr = frameProcessor.getRects();
            if (arr == null || arr.size() < 1) {
                //if word not found in current frame
                vibratePhone();
                Toast.makeText(this, "Word not Found", Toast.LENGTH_LONG).show();
                saySomething("Word not Found");
            } else {
                bmpaltr = null;
                System.gc();
                bmpaltr = highlightWordFound(bmp, arr);
                frameProcessor.sort();
            }
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //updating highlighting
                ziv.setImageBitmap(bmpaltr);
                ziv.setVisibility(View.VISIBLE);
                findViewById(R.id.blackLinear).setVisibility(View.VISIBLE);
            }
        });
        //updating visibility of buttons
        showZoom();
    }

    /**
     * Method to update visibilty of button
     */
    private void showZoom() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                zoom.setVisibility(View.VISIBLE);
                // if multiple occurrences found
                if (arr != null && arr.size() > 1) {
                    next.setVisibility(View.VISIBLE);
                    previous.setVisibility(View.VISIBLE);
                    zoom.setVisibility(View.GONE);
                }
                // if 1 occurrences found
                // if none found (shows previous highlight to prevent app from crashing)
                if (arr == null || arr.size() <= 1) {
                    zoom.setVisibility(View.VISIBLE);
                    next.setVisibility(View.GONE);
                    previous.setVisibility(View.GONE);
                }
                findViewById(R.id.capture).setVisibility(View.GONE);
            }
        });
    }

    /**
     * process last frame (only finds one occurrence)
     *
     * @param found word to look for
     * @return a single rectangle with the location of the found word
     */
    private Rect processBitmap(String found) {
        frameProcessor = new FrameProcessor(this, false);
        frameProcessor.runTextRecognition(bmp, found);
        while (!frameProcessor.isFinished()) {
            Log.w(TAG, "Processing new Frame");
        }
        return frameProcessor.getR();
    }

    /**
     * process last frame (finds one or multiple occurrences)
     *
     * @param found word to look for
     * @return multiple (or just one) rectangles with the location of the found word
     */
    private ArrayList<Rect> processBitmapMulti(String found) {
        frameProcessor = new FrameProcessor(this, true);
        frameProcessor.runTextRecognition(bmp, found);
        while (!frameProcessor.isFinished()) {
            Log.w(TAG, "Processing new Frame");
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return frameProcessor.getRects();
    }

    /**
     * Method to get bitmap from last frame
     *
     * @return bitmap from camera source
     */
    private void getBitmap() {
        byte[] arr = cameraSource.getData();
        YuvImage yuvImage = new YuvImage(arr, ImageFormat.NV21, cameraSource.getWidth(), cameraSource.getHeight(), null);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, cameraSource.getWidth(), cameraSource.getHeight()), 100, os);
        byte[] jpegByteArray = os.toByteArray();
        bmp = BitmapFactory.decodeByteArray(jpegByteArray, 0, jpegByteArray.length);
    }

    private void getBitmap(boolean camera) {
        if (!camera) {
            pictureDone = true;
            getBitmap();
        } else {
            cameraSource.camera.takePicture(null, null, new Camera.PictureCallback() {

                @Override
                public void onPictureTaken(byte[] data, Camera camera) {

                    File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
                    if (pictureFile == null) {
                        Log.d(TAG, "Error creating media file, check storage permissions: ");
                        return;
                    }

                    try {
                        FileOutputStream fos = new FileOutputStream(pictureFile);
                        fos.write(data);
                        fos.close();
                        String filepath = pictureFile.getAbsolutePath();
                        bmp = BitmapFactory.decodeFile(filepath);
                        pictureDone = true;
                        ExifInterface exif = new ExifInterface(pictureFile.getAbsolutePath());
                        if (exif != null) {
                            Log.d("EXIF value", exif.getAttribute(ExifInterface.TAG_ORIENTATION));
                            if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("6")) {
                                cameraPictureRotation = 90;
                            } else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("8")) {
                                cameraPictureRotation = 270;
                            } else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("3")) {
                                cameraPictureRotation = 180;
                            }
                        }
                    } catch (FileNotFoundException e) {
                        Log.d(TAG, "File not found: " + e.getMessage());
                    } catch (IOException e) {
                        Log.d(TAG, "Error accessing file: " + e.getMessage());
                    }
                }
            });
        }
    }

    /**
     * when app resumes
     */
    @Override
    public void onResume() {
        super.onResume();
        Logging.log(TAG, "onResume");
        startCameraSource();

        mPreferences = getSharedPreferences(sharedPrefFile, MODE_PRIVATE);
        compProg = true;
        int zoomVal = mPreferences.getInt("zoom", 0);
        zoomBar.setProgress(zoomVal);
        performInitialZoom();
        compProg = false;
        scheme = ClassificationScheme.valueOf(mPreferences.getString("region", ClassificationScheme.NONE.name()));

    }

    private void performInitialZoom() {

        if (cameraSource.camera == null)
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    performInitialZoom();
                }
            }, 50);
        else
            updateZoom();
    }

    /**
     * when app is paused
     */
    @Override
    protected void onPause() {
        Logging.log(TAG, "onPause");
        super.onPause();
        preview.stop();

        SharedPreferences.Editor preferencesEditor = mPreferences.edit();
        preferencesEditor.clear();
        preferencesEditor.putInt("zoom", zoomBar.getProgress());
        preferencesEditor.putString("region", scheme.name());
        preferencesEditor.apply();
    }

    /**
     * when app is destroyed
     */
    @Override
    public void onDestroy() {
        Logging.log(TAG, "destroyed");
        super.onDestroy();
        if (cameraSource != null) {
            cameraSource.release();
        }

        if (capture != null) {
            capture.interrupt();
            capture = null;
        }

        if (t != null) {
            t.interrupt();
            t = null;
        }

        if (mTTS != null) {
            mTTS.stop();
            mTTS.shutdown();
        }

        finish();
    }

    /**
     * Checks whether all permisions are granted
     *
     * @return boolean whether the permisions are present
     */
    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if one permission is present
     *
     * @param context    the activity from which method is called
     * @param permission the permission to check
     * @return boolean whether the permisions are present
     */
    private static boolean isPermissionGranted(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED) {
            Logging.log(TAG, "Permission granted: " + permission);
            return true;
        }
        Logging.log(TAG, "Permission NOT granted: " + permission);
        return false;
    }

    /**
     * Method to determine which permssions to ask for
     *
     * @return returns permissions that are necessary
     */
    private String[] getRequiredPermissions() {
        try {
            PackageInfo info =
                    this.getPackageManager()
                            .getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps = info.requestedPermissions;
            if (ps != null && ps.length > 0) {
                return ps;
            } else {
                return new String[0];
            }
        } catch (Exception e) {
            return new String[0];
        }
    }

    /**
     * Method to get all required permssions for application
     */
    private void getRuntimePermissions() {
        List<String> allNeededPermissions = new ArrayList<>();
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                allNeededPermissions.add(permission);
            }
        }

        if (!allNeededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this, allNeededPermissions.toArray(new String[0]), PERMISSION_REQUESTS);
        }
    }

    /**
     * After permissions are requested, this method initializes the camera source only if all of the
     * permissions for the application are granted
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        Logging.log(TAG, "Permission granted!");
        if (allPermissionsGranted()) {
            initCameraSource();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * Initializes the camera and the OCR Processor
     * Sets the OCR processor to analyze each frame of the camera
     */
    private void initCameraSource() {
        cameraSource = new CameraSource(this, graphicOverlay);
        processor = new OcrProcessor(this);
        cameraSource.setMachineLearningFrameProcessor(processor);
    }

    /**
     * Initializes the preview of the camera
     */
    private void startCameraSource() {
        if (cameraSource != null) {
            try {
                if (preview == null) {
                    Logging.log(TAG, "resume: Preview is null");
                }
                if (graphicOverlay == null) {
                    Logging.log(TAG, "resume: graphOverlay is null");
                }

                preview.setBackgroundColor(Color.BLACK);

                preview.start(cameraSource, graphicOverlay);

                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

                ViewGroup.LayoutParams videoLayoutParams = preview.getLayoutParams();
                videoLayoutParams.width = displayMetrics.widthPixels;
                videoLayoutParams.height = displayMetrics.heightPixels;
                preview.setLayoutParams(videoLayoutParams);

                ViewGroup.LayoutParams videoParams = preview.surfaceView.getLayoutParams();
                videoParams.width = displayMetrics.widthPixels;
                videoParams.height = displayMetrics.heightPixels;
                preview.surfaceView.setLayoutParams(videoLayoutParams);

            } catch (IOException e) {
                Logging.logError(TAG, "Unable to start camera source.", e);
                cameraSource.release();
                cameraSource = null;
            }
        }
    }


    /**
     * Method to vibrate phone default amount
     */
    public void vibratePhone() {
        vibratePhone(MainActivity.vibrateLength);
    }

    /**
     * Method to vibrate phone a specified amount
     *
     * @param millis amount milliseconds vibrated
     */
    public void vibratePhone(long millis) {
        Vibrator v = this.getSystemService(Vibrator.class);
        if (v.hasVibrator())
            v.vibrate(millis);
    }


    //Processing get/set methods
    public void setWordFound(boolean b) {
        wordFound = b;
    }

    public boolean getWordFound() {
        return wordFound;
    }

    public ClassificationScheme getScheme() {
        return scheme;
    }

    public String getText() {
        return search.getText().toString();
    }

    /**
     * When restart button is pressed, this method is called
     *
     * @param view
     */
    public void restart(View view) {
        ziv.clearAnimation();
        restart();
    }

    /**
     * finishes current activity and creates new instance of the activity
     */

    private void forceRestart() {
        finish();
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
    }

    private void restart() {
        if (getText().equals(Holder.searchWord) && !secondTime) {
            return;
        }
        if (t != null) {
            t.interrupt();
            t = null;
        }

        //reset variable values
        wordFound = false;
        if (processor != null) {
            processor.setFoundOnce(false);
        }
        secondTime = false;
        processingDone = false;
        cameraCapture = false;
        success = false;
        num = 0;
        multi = true;
        nextB = true;
        bmp = null;
        bmphil = null;
        bmpaltr = null;
        zoomIn = true;

        if (frameProcessor != null) {
            frameProcessor.setFinished();
        }
        pictureDone = false;


        //set initial visibility of views
        ziv.setVisibility(View.GONE);
        zoom.setVisibility(View.GONE);
        next.setVisibility(View.GONE);
        download.setVisibility(View.GONE);
        previous.setVisibility(View.GONE);
        preview.setVisibility(View.VISIBLE);
        settings.setVisibility(View.VISIBLE);
        findViewById(R.id.flashlight).setVisibility(View.VISIBLE);
        zoomBar.setVisibility(View.VISIBLE);
        scan.setVisibility(View.VISIBLE);
        findViewById(R.id.capture).setVisibility(View.VISIBLE);
        findViewById(R.id.cameraPreview).setVisibility(View.VISIBLE);
        findViewById(R.id.blackLinear).setVisibility(View.GONE);
        restart.setVisibility(View.GONE);

        //initialized camera source
        if (allPermissionsGranted()) {
            initCameraSource();
            startCameraSource();
        } else {
            getRuntimePermissions();
        }

        updateZoom();

        cameraSource.torch(flashlightOn);
        if (flashlightOn) {
            setImage(flashlight, R.raw.flashlight);
        } else {
            setImage(flashlight, R.raw.flashlight_invert);
        }

        startFoundOnceThread();
    }

    /**
     * Method called to underline the word
     *
     * @param mutableBitmap bitmap to edit
     * @param rect          the rectangle of the word to underline
     * @return changed bitmap
     */
    Bitmap highlightWordFound(Bitmap mutableBitmap, Rect rect) {
        //set color and style of the paint
        Paint mPaint = new Paint();
        mPaint.setColor(Color.RED);
        mPaint.setStyle(Paint.Style.STROKE);
        if (camera) {
            mPaint.setStrokeWidth(PICTURE_STROKE);
        } else {
            mPaint.setStrokeWidth(VIDEO_STROKE);
        }

        //copy bitmap and access the canvas of the bitmap
        mutableBitmap = mutableBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        canvas.save();

        //coordinates for line of the bitmap
        int leftX = rect.centerX() - rect.width() / 2;
        int Y = rect.centerY() + rect.height() / 2;
        int rightX = rect.centerX() + rect.width() / 2;

        //underline for the word placed on canvas
        canvas.drawLine(leftX, Y, rightX, Y, mPaint);
        return mutableBitmap;
    }

    /**
     * Underlining multiple words
     *
     * @param mutableBitmap bitmap to edit
     * @param r             the list of rectangles of the word to underline
     * @return changed bitmap
     */
    Bitmap highlightWordFound(Bitmap mutableBitmap, ArrayList<Rect> r) {
        //set color and style of the paint
        Paint mPaint = new Paint();
        mPaint.setColor(Color.RED);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(5);

        //copy bitmap and access the canvas of the bitmap
        mutableBitmap = mutableBitmap.copy(Bitmap.Config.ARGB_8888, true);

        Canvas canvas = new Canvas(mutableBitmap);
        canvas.save();

        //For each rectangle in the list
        if (rot && frameProcessor.getWordPoints() != null && frameProcessor.getWordPoints().size() >= 1) {
            for (Point[] points : frameProcessor.getWordPoints()) {
                canvas.drawLine(points[2].x, points[2].y, points[3].x, points[3].y, mPaint);
            }
        } else {
            for (Rect rect : r) {
                //coordinates for line of the bitmap
                int leftX = rect.centerX() - rect.width() / 2;
                int Y = rect.centerY() + rect.height() / 2;
                int rightX = rect.centerX() + rect.width() / 2;
                //underline for the word placed on canvas
                canvas.drawLine(leftX, Y, rightX, Y, mPaint);
            }
        }

        return mutableBitmap;
    }

    /**
     * Adjusts zoom to certain width
     *
     * @param width width of the underlined word's rectangle
     */
    private void adjustZoomLevel(int width) {

        // If the next iteration in zoom level exceeds the with of the holder then return
        if (camera) {
            if ((ziv.getMaxScale() * (2 * width)) >= ziv.getWidth()) {
                ziv.setMaxScale(ziv.getMaxScale() - 0.1f);
                adjustZoomLevel(width);
            }
        } else {
            if ((ziv.getMaxScale() * (width)) >= ziv.getWidth()) {
                ziv.setMaxScale(ziv.getMaxScale() - 0.1f);
                adjustZoomLevel(width);
            }
        }

        if (ziv.getMaxScale() == 0)
            ziv.setMaxScale(1);
    }

    /**
     * Method to zoom into a certain rectangle on the screen
     *
     * @param r
     */
    void zoomWord(Rect r) {
        if (ziv.isNotZoomed()) {
            adjustZoomLevel(r.width());
            int x_tap = r.centerX();
            int y_tap = r.centerY();

            ziv.externalDoubleTapGestureGenerator(x_tap, y_tap);
            Log.w("Location: ", "( " + r.right + " , " + r.bottom + ")");
        } else {
            ziv.setMaxScale(ziv.getCurrentScale());
            ziv.externalDoubleTapGestureGenerator(ziv.getWidth() / 2, ziv.getHeight() / 2);

            ziv.resetMaxScale();
            adjustZoomLevel(r.width());
            int x_tap = r.centerX();
            int y_tap = r.centerY();

            performSecondTap(x_tap, y_tap);
        }
    }

    private void performSecondTap(final float x, final float y) {

        if (ziv.isAnimating())
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    performSecondTap(x, y);
                }
            }, 500);
        else
            ziv.externalDoubleTapGestureGenerator(x, y);
    }

    /**
     * When the zoom button is clicked, this method is called
     *
     * @param view
     */
    public void zoom(View view) {
        zoom();
    }

    /**
     * Checks whether there are multiple words to zoom into and zooms in accordingly.
     */
    public void zoom() {
        if (processor.isFoundOnce()) {
            if (!multi) {
                //if only one word
                zoomWord(processor.getBoundingRectTrans(), true);
            } else {
                if (arr == null || arr.size() < 1) {
                    zoomWord(processor.getBoundingRectTrans(), true);
                } else {
                    //cyclic remainder
                    num = (num % arr.size() + arr.size()) % arr.size();
                    Rect r = arr.get(num);
                    //need to translate the position of the rectangle according to the bitmap displayed
                    Rect transR = new Rect(translateX(r.left), translateY(r.top), translateX(r.right), translateY(r.bottom));
                    //zooms to specific word
                    if (arr.size() == 1) {
                        zoomWord(transR, true);
                    } else {
                        zoomWord(transR, false);
                    }
                }
            }
        } else if (cameraCapture) {
            if (arr == null || arr.size() < 1) {
                saySomething("Word is not found");
            } else {
                //cyclic remainder
                num = (num % arr.size() + arr.size()) % arr.size();
                Rect r = arr.get(num);
                //need to translate the position of the rectangle according to the bitmap displayed
                Rect transR = new Rect(translateX(r.left), translateY(r.top), translateX(r.right), translateY(r.bottom));
                //zooms to specific word
                if (arr.size() == 1) {
                    zoomWord(transR, true);
                } else {
                    zoomWord(transR, false);
                }
            }
        }
    }

    private void toggleZoomIcon() {
        if (zoomIn) {
            setImage(zoom, R.raw.zoomout);
            zoomIn = false;
        } else {
            setImage(zoom, R.raw.zoomin);
            zoomIn = true;
        }
    }

    private void zoomWord(Rect r, boolean b) {
        if (b) {
            toggleZoomIcon();
            if (ziv.isNotZoomed()) {
                adjustZoomLevel(r.width());
                int x_tap = r.centerX();
                int y_tap = r.centerY();

                ziv.externalDoubleTapGestureGenerator(x_tap, y_tap);
                Log.w("Location: ", "( " + r.right + " , " + r.bottom + ")");
            } else {
                ziv.setMaxScale(ziv.getCurrentScale());
                ziv.externalDoubleTapGestureGenerator(ziv.getWidth() / 2, ziv.getHeight() / 2);
                ziv.resetMaxScale();
            }
        } else {
            zoomWord(r);
        }
    }

    public int translateX(int x) {
        float newWidth = ziv.getWidth();
        float oldWidth = bmp.getWidth();
        return (int) (x / oldWidth * newWidth);
    }

    public int translateY(int x) {
        float newHeight = ziv.getHeight();
        float oldHeight = bmp.getHeight();
        return (int) (x / oldHeight * newHeight);
    }

    /**
     * When the next button is clicked, this method is called
     *
     * @param view
     */
    public void next(View view) {
        zoomAndReset(1);
    }

    /**
     * When the previous button is clicked, this method is called
     *
     * @param view
     */
    public void previous(View view) {
        zoomAndReset(-1);
    }

    /**
     * zoom logic for next and previous buttons
     *
     * @param i increment by which to skip buy
     */
    public void zoomAndReset(int i) {
        zoom();
        num += i;
    }

    /**
     * Checks if there is voice input or voice output needed
     * If there is a change when the activity is in the second state (i.e. when the zoomable
     * imageview is present), then the update method is called to find a different word on the
     * screen.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //voice input
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            //spoken text from google voice
            String spokenText = results.get(0);
            //set edit text's value to the spoken text
            search.setText(spokenText);
            //user output
            saySomething("Searching for " + search.getText().toString());
            if (success && secondTime) {
                // updates search query if on zoomable image view screen
                update(spokenText);

                if (arr.size() != 1) {
                    saySomething(spokenText + " found in " + arr.size() + " locations");
                } else {
                    saySomething(spokenText + " found in 1 location");
                }
            } else if (cameraCapture) {

                update(spokenText);
                //TODO check if word is found
                if (arr != null && arr.size() != 1) {
                    saySomething(spokenText + " found in " + arr.size() + " locations");
                } else {
                    saySomething(spokenText + " found in 1 location");
                }
            }

            compProg = true;
            zoomBar.setProgress(0);
            flashlightOn = false;
            compProg = false;
        }
        //voice output
        if (requestCode == ACT_CHECK_TTS_DATA) {
            if (resultCode ==
                    TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                // Data exists, so we instantiate the TTS engine
                mTTS = new TextToSpeech(this, this);
            } else {
                // Data is missing, so we start the TTS installation
                // process
                Intent installIntent = new Intent();
                installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installIntent);
            }
        }

        //called superclass method
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * When the voice input button is clicked, this method is called
     *
     * @param view
     */
    public void speech(View view) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    /**
     * When the select classification scheme button is clicked, this method is called
     * Creates Alert dialog and forces you to click one of the four schemes of classification
     */
    public void setSchemeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Please Select Scanning Region of Interest");
        builder.setItems(new CharSequence[]
                        {"Whole Screen", "Center", "Top", "Bottom"},
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position of the selected item
                        switch (which) {
                            case 0:
                                scheme = ClassificationScheme.NONE;
                                saySomething("Finding in the Whole Screen");
                                break;
                            case 1:
                                scheme = ClassificationScheme.FIND_AT_CENTER;
                                saySomething("Finding at the Center");
                                break;
                            case 2:
                                scheme = ClassificationScheme.FIND_AT_TOP;
                                saySomething("Finding at the Top");
                                break;
                            case 3:
                                scheme = ClassificationScheme.FIND_AT_BOTTOM;
                                saySomething("Finding at the Bottom");
                                break;
                        }
                    }
                });
        builder.create().show();
    }

    public void setInfoDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        WebView wv = new WebView(this);
        wv.loadUrl("file:///android_res/raw/infopage.html");

        alert.setView(wv);
        alert.setNegativeButton("Contact Us", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                EmailIntentBuilder.from(MainActivity.this).to("luoreslab@gmail.com")
                        .cc("singh.anikait@gmail.com").subject("Issue from app")
                        .body("The issue I am facing is").start();
            }
        });
        alert.setPositiveButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        alert.show();
    }

    /**
     * Hides soft keyboard
     *
     * @param activity static method to be called from any activity
     */
    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     * Initializes text to speech module
     *
     * @param status
     */
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            if (mTTS != null) {
                Log.e("Language Selection", ConfigurationCompat.getLocales(Resources.getSystem().getConfiguration()).get(0).toString());
                int result = mTTS.setLanguage(ConfigurationCompat.getLocales(Resources.getSystem().getConfiguration()).get(0));
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "TTS language is not supported", Toast.LENGTH_LONG).show();
                }
            }
        } else {
            Toast.makeText(this, "TTS initialization failed",
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Method to speak something (not default)
     *
     * @param text  what to speak
     * @param qmode can either be an addition to the queue or a flush
     */
    private void saySomething(String text, int qmode) {
        if (qmode == 1)
            mTTS.speak(text, TextToSpeech.QUEUE_ADD, null);
        else
            mTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    /**
     * Method to speak something (default)
     *
     * @param text what to speak
     */
    private void saySomething(String text) {
        saySomething(text, 0);
    }


    // =============================================================================================
    // Photo Capture Variables and Methods
    // =============================================================================================
    private boolean pictureDone = false;
    private boolean camera = false;

    /**
     * Create a File for saving an image or video
     */
    private File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(this.getApplicationContext().getExternalFilesDir(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    /**
     * Create a File for saving an image or video
     */
    private File getOutputMediaFile() {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), "RealOCR");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator + "OCR_" + timeStamp + ".txt");

        return mediaFile;
    }

    public void playSound() {
        MediaPlayer.create(this, R.raw.beep).start();
    }

    public void settings(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        // This activity implements OnMenuItemClickListener
        popup.setOnMenuItemClickListener(this);
        popup.inflate(R.menu.actions);
        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.setScheme:
                setSchemeDialog();
                return true;
            case R.id.infoPopup:
                setInfoDialog();
            default:
                return false;
        }
    }


    public void download(Bitmap bmp, String title, String description) {
        MediaStore.Images.Media.insertImage(getContentResolver(), bmp, title, description);
    }

    public void download(View view) {
        String str = frameProcessor.getText();
        if (str != null) {
            download(bmpaltr, "OCR PICTURE", str);

            File docFile = getOutputMediaFile();
            if (docFile == null) {
                Log.d(TAG, "Error creating media file, check storage permissions: ");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(docFile);
                fos.write(str.getBytes());
                fos.close();
                String filepath = docFile.getAbsolutePath();
                MediaScannerConnection.scanFile(MainActivity.this, new String[]{filepath}, null, null);
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
            Toast.makeText(getApplicationContext(), "Download Complete", Toast.LENGTH_LONG).show();

            //to open file and view it
            Intent i = new Intent();
            i.setAction(android.content.Intent.ACTION_VIEW);
            i.setDataAndType(FileProvider.getUriForFile(this, this.getApplicationContext().getPackageName() + ".my.package.name.provider", docFile), "text/plain");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(i);
        }
    }

    public void capture(View view) {
        findViewById(R.id.capture).setVisibility(View.GONE);
        findViewById(R.id.scan).setVisibility(View.GONE);

        if (capture != null) {
            capture.interrupt();
            capture = null;
        }
        capture = new Thread() {
            @Override
            public void run() {
                getBitmap(true);

                while (!pictureDone) {
                    Log.d(TAG, "taking picture");
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                final String[] found = new String[1];
                found[0] = "";
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.playSound();
                        found[0] = search.getText().toString();
                    }
                });

                //rotates image 90 degrees
                if (bmp.getHeight() < bmp.getWidth()) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(90);
                    bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
                }

                boolean captureFailed = false;
                secondTime = true;
                float angle = 0;
                arr = processBitmapMulti(found[0]);
                if (arr == null || arr.size() < 1) {
                    bmpaltr = bmp;
                    if (found[0].equals(Holder.searchWord)) {
                        saySomething("Please enter query");
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                update(found[0]);
                                if(arr == null) {
                                    saySomething(found[0] + " is not found");
                                }else {
                                    if (arr.size() != 1) {
                                        saySomething(found[0] + " found in " + arr.size() + " locations");
                                    } else {
                                        saySomething(found[0] + " found in 1 location");
                                    }
                                }
                            }
                        });
                    }
                } else {
                    //user feedback
                    bmpaltr = null;
                    System.gc();
                    frameProcessor.sort();
                    angle = showPoints();
                }
                if (angle != 0) {
                    rot = true;
                    bmpaltr = highlightWordFound(bmp, arr);
                    final float angleSet = angle;
                    if (rot) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ziv.startAnimation(-(int) angleSet);
                            }
                        });
                    }

                    if (arr.size() != 1) {
                        saySomething(found[0] + " found in " + arr.size() + " locations");
                    } else {
                        saySomething(found[0] + " found in 1 location");
                    }
                }

                cameraCapture = true;

                final boolean finalCaptureFailed = captureFailed;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //sets visibility of components in second part of the app
                        ziv.setImageBitmap(bmpaltr);
                        ziv.setVisibility(View.VISIBLE);
                        zoom.setVisibility(View.VISIBLE);
                        download.setVisibility(View.VISIBLE);
                        settings.setVisibility(View.GONE);
                        findViewById(R.id.flashlight).setVisibility(View.GONE);
                        preview.setVisibility(View.GONE);
                        zoomBar.setVisibility(View.GONE);
                        scan.setVisibility(View.GONE);
                        restart.setVisibility(View.VISIBLE);
                        findViewById(R.id.capture).setVisibility(View.GONE);
                        findViewById(R.id.cameraPreview).setVisibility(View.GONE);
                        findViewById(R.id.blackLinear).setVisibility(View.VISIBLE);
                        cameraSource.torch(false);

                        zoom.setVisibility(View.VISIBLE);
                        // if multiple occurrences found
                        if (arr != null && arr.size() > 1) {
                            next.setVisibility(View.VISIBLE);
                            previous.setVisibility(View.VISIBLE);
                            zoom.setVisibility(View.GONE);
                        } else if (arr != null && arr.size() == 1) {
                            next.setVisibility(View.GONE);
                            previous.setVisibility(View.GONE);
                            zoom.setVisibility(View.VISIBLE);
                        } else {
                            next.setVisibility(View.GONE);
                            previous.setVisibility(View.GONE);
                            zoom.setVisibility(View.GONE);
                        }

                        if (finalCaptureFailed) {
                            if (arr == null || arr.size() <= 1) {
                                zoom.setVisibility(View.GONE);
                                next.setVisibility(View.GONE);
                                previous.setVisibility(View.GONE);
                            }
                        }
                    }
                });
            }
        };
        capture.start();
    }

    public void toggleFlashlight(View view) {
        if (flashlightOn) {
            cameraSource.torch(false);
            flashlightOn = false;
            setImage(flashlight, R.raw.flashlight_invert);
        } else {
            cameraSource.torch(true);
            flashlightOn = true;
            setImage(flashlight, R.raw.flashlight);
        }
    }

    void setImage(ImageButton view, int imgId) {
        view.setImageResource(0);
        Drawable draw = getResources().getDrawable(imgId);
        view.setImageDrawable(draw);
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        view.setAdjustViewBounds(true);
    }

    public void updateZoomBar(int zoom) {
        compProg = true;
        zoomBar.setProgress(zoom);
        compProg = false;
    }
}
