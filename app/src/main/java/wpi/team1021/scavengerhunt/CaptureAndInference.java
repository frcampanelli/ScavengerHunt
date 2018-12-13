package wpi.team1021.scavengerhunt;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.graphics.Bitmap.createScaledBitmap;

public class CaptureAndInference extends AppCompatActivity {

    private String TAG = "wpi.team1021.scavengerhunt";
    private ImageView mCapturedImage;
    private PackageManager packageManager;
    private File dir;
    private File temp;
    private String mCurrentPhotoPath;
    private String mCurrentPhotoName;
    private Bitmap currentPhotoBitmap;
    private TextView mTargetImageLabel;
    private TextView mCheckInferenceLabel;
    private Button mCheckInferenceButton;
    private ToggleButton mOffDeviceToggle;
    private Button mCaptureImageButton;
    private Button mGetNewTargetButton;
    private String randId;
    private TextView mTimerLabel;
    private ByteBuffer imgData;
    private int DIM_BATCH_SIZE = 1;
    private int SIZE_X = 299;
    private int SIZE_Y = 299;
    private int DIM_PIXEL_SIZE = 3;
    private int NUM_BYTES_PER_CHANNEL = 4;
    private int IMAGE_MEAN = 128;
    private float IMAGE_STD = 128.0f;
    private ArrayList<String> mLabels;
    private String currentLabel = "";
    private Integer currentPoints = 0;
    private float[][] labelProbArray;
    private String hostUrl = "http://35.243.243.163:54321/inception";
    private Long startTime;
    private Long timeInterval;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_and_inference);
        packageManager = getPackageManager();
        dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            temp = File.createTempFile("temp", "jpeg", dir);
        } catch (java.io.IOException e) {
            return;
        }
        mCapturedImage = (ImageView) findViewById(R.id.captured_image);
        mTargetImageLabel = (TextView) findViewById(R.id.target_image_label);
        mTargetImageLabel.setText(R.string.target_image_label);
        mCheckInferenceLabel = (TextView) findViewById(R.id.check_inference_label);
        mCheckInferenceLabel.setText(R.string.check_correct);
        mCheckInferenceButton = (Button) findViewById(R.id.check_inference_button);
        mOffDeviceToggle = (ToggleButton) findViewById(R.id.off_device_toggle);
        mCaptureImageButton = (Button) findViewById(R.id.capture_image_button);
        mGetNewTargetButton = (Button) findViewById(R.id.get_new_target_button);
        mTimerLabel = (TextView) findViewById(R.id.timer_label);
        imgData = ByteBuffer.allocateDirect(
                DIM_BATCH_SIZE
                        * SIZE_X
                        * SIZE_Y
                        * DIM_PIXEL_SIZE
                        * NUM_BYTES_PER_CHANNEL);
        imgData.order(ByteOrder.nativeOrder());
        mLabels = new ArrayList<String>();
        readLabels();
        labelProbArray = new float[DIM_BATCH_SIZE][mLabels.size()];
        Intent intent = getIntent();
        randId = intent.getStringExtra("RANDOM_ID");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode != 400) || (resultCode != RESULT_OK)) {
            //Failed request
            return;
        }
        currentPhotoBitmap = BitmapFactory.decodeFile(mCurrentPhotoPath);
        ImageView mImageView = (ImageView) findViewById(R.id.captured_image);
        mImageView.setImageBitmap(currentPhotoBitmap);
        mCheckInferenceLabel.setText(R.string.check_correct);
    }

    CountDownTimer timer = new CountDownTimer(3600000, 1000) {

        public void onTick(long millisUntilFinished) {
            mTimerLabel.setText(String.format(Locale.US, "Time left: \n" +
                    "%02d:%02d", millisUntilFinished/60000, ((millisUntilFinished/1000) % 60)));
        }

        public void onFinish() {
            mTimerLabel.setText("Time's up!");
        }
    }.start();


    private void readLabels() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(getAssets().open("model/labels.txt")))) {
            String mLine;
            while ((mLine = reader.readLine()) != null) {
                mLabels.add(mLine);
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        //log the exception
    }

    public void onClickTakePictureButton(View v) {
        Uri photoURI = FileProvider.getUriForFile(this, "wpi.team1021.scavengerhunt.fileprovider", temp);
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        List<ResolveInfo> activities = packageManager.queryIntentActivities(takePictureIntent, 0);
        startActivityForResult(takePictureIntent, 400);
        mCurrentPhotoPath = temp.getAbsolutePath();
        mCurrentPhotoName = temp.getName();
    }

    public void onClickCheckInferenceButton(View v) {
        if(mOffDeviceToggle.isChecked()) {  //If button is checked then it is set to on-device inference
            onDeviceInference(v);
        } else {
            offDeviceInference(v);  //Otherwise do it off-device
        }
        //TODO
    }

    public void onDeviceInference(View v) {
        new OnDeviceInferenceAsync().execute(currentPhotoBitmap);
    }

    public void offDeviceInference(View v) {
        new offDeviceInferenceASync().execute(mCurrentPhotoName);
    }

    public void getNewTarget(View v) {
        Random rand = new Random();
        int random = rand.nextInt(1000);

        currentLabel = mLabels.get(random);
        mTargetImageLabel.setText("Your current target is " + currentLabel);
        mCheckInferenceLabel.setText("Take a picture containing the target and then check the image!");
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = getAssets().openFd(getModelPath());
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private String getModelPath() {
        return "model/inception_v3.tflite";
    }

    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        int[] intValues = new int[SIZE_X * SIZE_Y];

        if (imgData == null) {
            return;
        }
        imgData.rewind();
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        // Convert the image to floating point.
        int pixel = 0;
        for (int i = 0; i < SIZE_X; ++i) {
            for (int j = 0; j < SIZE_Y; ++j) {
                final int val = intValues[pixel++];
                addPixelValue(val);
            }
        }
    }

    protected void addPixelValue(int pixelValue) {
        imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
        imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
        imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
    }


    private class OnDeviceInferenceAsync extends AsyncTask<Bitmap, Void, Bitmap> {
        String time;

        protected void onPreExecute() {
            // Stuff to do before inference starts
            startTime = SystemClock.uptimeMillis();  //Start timing
            mTargetImageLabel.setText(R.string.running);
            mCheckInferenceLabel.setText(R.string.calculating);
        }

        protected Bitmap doInBackground(Bitmap... bitmaps) {

            Bitmap image_bitmap = bitmaps[0];

            // Do inference here!
            Interpreter tflite = null;
            try {
                MappedByteBuffer tfliteModel = loadModelFile();
                tflite = new Interpreter(tfliteModel);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }

            Bitmap scaledBitmap = createScaledBitmap(image_bitmap, SIZE_X, SIZE_Y, false);
            convertBitmapToByteBuffer(scaledBitmap);
            try {
                tflite.run(imgData, labelProbArray);
            } catch(NullPointerException e) {
                Log.e(TAG, e.getMessage());
            }
            return image_bitmap;
        }

        protected void onPostExecute(Bitmap image_bitmap) {
            // Stuff to do after inference ends
            float max = labelProbArray[0][0];
            int maxIndex = 0;
            for (int i = 1; i < labelProbArray[0].length; i++)
            {
                if (labelProbArray[0][i] > max)
                {
                    max = labelProbArray[0][i];
                    maxIndex = i;
                }
            }
            //Log.i(TAG, "Max is: " + max + " with index: " + maxIndex);

            timeInterval = SystemClock.uptimeMillis() - startTime;  //Stop timing and get interval
            //mTargetImageLabel.setText(String.format(Locale.US, "%s: %f%%", mLabels.get(maxIndex), max*100));
            if (mLabels.get(maxIndex).contains(currentLabel)) {
                mTargetImageLabel.setText("We found the target in your picture (" + mLabels.get(maxIndex) + max * 100 +")");
                currentPoints++;
            } else {
                mTargetImageLabel.setText("The picture doesn't seem to contain the target");
            }
            mCheckInferenceLabel.setText(String.format(Locale.US, "Your device took %dms to process", timeInterval));
        }
    }
    private class offDeviceInferenceASync extends AsyncTask<String, Float, String> {
        String result;
        String time;

        protected void onPreExecute() {
            // Stuff to do before inference starts
            startTime = SystemClock.uptimeMillis();  //Start timing
            mTargetImageLabel.setText(R.string.running);
            mCheckInferenceLabel.setText(R.string.calculating);
        }

        protected String doInBackground(String... img_files) {

            String img_path = img_files[0];

            // Do inference here!
            final MediaType MEDIA_TYPE_JPEG = MediaType.get("image/jpeg");
            OkHttpClient client = new OkHttpClient();
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", mCurrentPhotoName, RequestBody.create(MEDIA_TYPE_JPEG, temp))
                    .build();

            Request request = new Request.Builder()
                    .url(hostUrl)
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                result = response.body().string();

            } catch (IOException e) {
                Log.e("HTTPERROR", "Request error");
                e.printStackTrace();
            }
            return result;
        }

        protected void onPostExecute(String result) {
            Log.d("RESPONSE","Response body: " + result);
            timeInterval = SystemClock.uptimeMillis() - startTime;  //Stop timing and get interval
            if (result.contains(currentLabel)) {
                mTargetImageLabel.setText("We found the target in your picture (" + result +")");
                currentPoints++;
            } else {
                mTargetImageLabel.setText("The picture doesn't seem to contain the target");
            }

            mCheckInferenceLabel.setText(String.format(Locale.US, "Server took %dms to process", timeInterval));
        }
    }
}


