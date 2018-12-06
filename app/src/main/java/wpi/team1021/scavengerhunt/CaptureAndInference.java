package wpi.team1021.scavengerhunt;

import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.SystemClock;
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

import static android.graphics.Bitmap.createScaledBitmap;

public class CaptureAndInference extends AppCompatActivity {

    private String TAG = "wpi.team1021.scavengerhunt";
    private ImageView mCapturedImage;
    private TextView mTargetImageLabel;
    private TextView mCheckInferenceLabel;
    private Button mCheckInferenceButton;
    private ToggleButton mOffDeviceToggle;
    private Button mCaptureImageButton;
    private Button mGetNewTargetButton;
    private ByteBuffer imgData;
    private int DIM_BATCH_SIZE = 1;
    private int SIZE_X = 299;
    private int SIZE_Y = 299;
    private int DIM_PIXEL_SIZE = 3;
    private int NUM_BYTES_PER_CHANNEL = 4;
    private int IMAGE_MEAN = 128;
    private float IMAGE_STD = 128.0f;
    private ArrayList<String> mLabels;
    private float[][] labelProbArray;
    private String hostUrl = "http://35.243.243.163:54321/inception";
    private Long startTime;
    private Long timeInterval;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_and_inference);
        mCapturedImage = (ImageView) findViewById(R.id.captured_image);
        mTargetImageLabel = (TextView) findViewById(R.id.target_image_label);
        mTargetImageLabel.setText(R.string.target_image_label);
        mCheckInferenceLabel = (TextView) findViewById(R.id.check_inference_label);
        mCheckInferenceLabel.setText(R.string.latency_start);
        mCheckInferenceButton = (Button) findViewById(R.id.check_inference_button);
        mOffDeviceToggle = (ToggleButton) findViewById(R.id.off_device_toggle);
        mCaptureImageButton = (Button) findViewById(R.id.capture_image_button);
        mGetNewTargetButton = (Button) findViewById(R.id.get_new_target_button);
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
        /*
        InputStream image_stream = null;
        try {
            image_stream = getAssets().open("0.jpg");
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCapturedImage.setImageBitmap(BitmapFactory.decodeStream(image_stream));
        */
    }

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
        //TODO add camera image capture here from Project2
    }

    public void onClickCheckInferenceButton(View v) {
        if(mOffDeviceToggle.isChecked()) {  //If button is checked then it is set to on-device inference
            onDeviceInference(v);
        } else {
            //offDeviceInference(v);  //Otherwise do it off-device
        }
    }
    public void onDeviceInference(View v) {
        //TODO change this to use the image that the user takes, possibly from a saved file or file descriptor
        startTime = SystemClock.uptimeMillis();  //Start timing
        InputStream image_stream = null;
        Random rand = new Random();
        try {
            image_stream = getAssets().open("imgs/" + rand.nextInt(4) + ".jpg");
        } catch (IOException e) {
            e.printStackTrace();
        }
        new OnDeviceInferenceAsync().execute(image_stream);
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


    private class OnDeviceInferenceAsync extends AsyncTask<InputStream, Float, InputStream> {
        String time;

        protected void onPreExecute() {
            // Stuff to do before inference starts
            startTime = SystemClock.uptimeMillis();  //Start timing
            mTargetImageLabel.setText(R.string.running);
            mCheckInferenceLabel.setText(R.string.calculating);
        }

        protected InputStream doInBackground(InputStream... streams) {

            InputStream image_stream = streams[0];

            // Do inference here!
            Interpreter tflite = null;
            try {
                MappedByteBuffer tfliteModel = loadModelFile();
                tflite = new Interpreter(tfliteModel);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }

            Bitmap scaledBitmap = createScaledBitmap(BitmapFactory.decodeStream(image_stream), SIZE_X, SIZE_Y, false);
            convertBitmapToByteBuffer(scaledBitmap);
            try {
                tflite.run(imgData, labelProbArray);
            } catch(NullPointerException e) {
                Log.e(TAG, e.getMessage());
            }
            return image_stream;
        }

        protected void onPostExecute(InputStream image_stream) {
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
            mTargetImageLabel.setText(String.format(Locale.US, "%s: %f%%", mLabels.get(maxIndex), max*100));
            mCapturedImage.setImageBitmap(BitmapFactory.decodeStream(image_stream));
            mCheckInferenceLabel.setText(String.format(Locale.US, "%dms", timeInterval));
        }
    }
    //TODO never got it to save a file, and OkHttp3.x didnt like the sample code's MultipartBody.Builder() logic
    /*private class RunInferenceAsync extends AsyncTask<String, Float, Long> {
        String results;
        String time;

        protected void onPreExecute() {
            // Stuff to do before inference starts
        }

        protected Long doInBackground(String... img_files) {

            String img_path = img_files[0];

            // Do inference here!
            final MediaType MEDIA_TYPE_JPEG = MediaType.get("image/jpeg");
            OkHttpClient client = new OkHttpClient();
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBuilder.FORM)
                    .addFormDataPart("file", filename, RequestBody.create(MEDIA_TYPE_JPEG, file))
                    .build();

            Request request = new Request.Builder()
                    .url(hostUrl)
                    .post(requestBody)
                    .build();

            Response response = client.newCall(request).execute();
            return null;
        }

        protected void onPostExecute(Long result) {
            // Stuff to do after inference ends
        }
    }
    */
}


