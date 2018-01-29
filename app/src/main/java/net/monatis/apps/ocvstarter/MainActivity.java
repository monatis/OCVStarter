package net.monatis.apps.ocvstarter;

import android.content.Context;
import android.content.res.AssetManager;
import android.support.v7.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import net.monatis.apps.ocvstarter.R;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "OCVSample::Activity";
    String datapath = "";
    String mainCascadeName = "haarcascade_frontalface_alt2.xml";
    String nestedCascadeName = "haarcascade_eye_tree_eyeglasses.xml";
    ListView listView;

    private CameraBridgeViewBase _cameraBridgeViewBase;

    private BaseLoaderCallback _baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                           Log.i(TAG, "OpenCV loaded successfully");
                    // Load ndk built module, as specified in moduleName in build.gradle
                    // after opencv initialization
                    System.loadLibrary("native-lib");
                    String mainPath = datapath + "/cascades/" + mainCascadeName;
                    String nestedPath = datapath + "/cascades/" + nestedCascadeName;
                    setCascadePaths(mainPath, nestedPath);
                    TextView mainText = (TextView) findViewById(R.id.text_main);
                    mainText.setText("Shop on mobile with augmented reality");
                    _cameraBridgeViewBase.enableFpsMeter();
                    _cameraBridgeViewBase.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        datapath = getApplicationContext().getExternalFilesDir(null).getAbsolutePath();
        checkFiles(new File(datapath + "/cascades/"));

        // Permissions for Android 6+
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.CAMERA},
                1);

        _cameraBridgeViewBase = (CameraBridgeViewBase) findViewById(R.id.main_surface);
        _cameraBridgeViewBase.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
        _cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        _cameraBridgeViewBase.setCvCameraViewListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        disableCamera();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_3_0, this, _baseLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            _baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(MainActivity.this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public void onDestroy() {
        super.onDestroy();
        disableCamera();
    }

    public void disableCamera() {
        if (_cameraBridgeViewBase != null)
            _cameraBridgeViewBase.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        Toast.makeText(getApplicationContext(),"Camera view started",Toast.LENGTH_SHORT).show();
    }

    public void onCameraViewStopped() {
        Toast.makeText(getApplicationContext(),"Camera view stopped",Toast.LENGTH_SHORT).show();
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat matRgba = inputFrame.rgba();
        //Mat matGray = inputFrame.gray();
        //salt(matGray.getNativeObjAddr(), 2000);
        //return matGray;
        detectEyes(matRgba.getNativeObjAddr());
        return matRgba;
    }

    private void checkFiles(File dir) {
        if (!dir.exists()&& dir.mkdirs()){
            copyFile(mainCascadeName);
            copyFile(nestedCascadeName);
        }

        if(dir.exists()) {
            File datafile;
            String mainCascadeFilepath = datapath + "/cascades/" + mainCascadeName;
            datafile = new File(mainCascadeFilepath);

            if (!datafile.exists()) {
                copyFile(mainCascadeName);
                Toast.makeText(getApplicationContext(),"Copying main cascade",Toast.LENGTH_SHORT).show();
            }

            String nestedCascadeFilepath = datapath + "/cascades/" + nestedCascadeName;
            datafile = new File(nestedCascadeFilepath);

            if (!datafile.exists()) {
                copyFile(nestedCascadeName);
                Toast.makeText(getApplicationContext(), "Copying nested cascade", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void copyFile(String filename) {
        try {
            String filepath = datapath + "/cascades/" + filename;
            AssetManager assetManager = getAssets();

            InputStream instream = assetManager.open("cascades/" + filename);
            OutputStream outstream = new FileOutputStream(filepath);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }


            outstream.flush();
            outstream.close();
            instream.close();

            File file = new File(filepath);
            if (!file.exists()) {
                throw new FileNotFoundException();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public native void salt(long matAddrGray, int nbrElem);
    public native void setCascadePaths(String jMainCascadePath, String jNestedCascadePath);
    public native void detectEyes(long frame);


}

