package com.contoso.facetutorial;

import java.io.*;
import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;
import android.view.*;
import android.graphics.*;
import android.widget.*;
import android.provider.*;

import com.microsoft.projectoxford.face.*;
import com.microsoft.projectoxford.face.contract.*;

import android.util.Log;

public class MainActivity extends Activity {
    // Replace `<API endpoint>` with the Azure region associated with
    // your subscription key. For example,
    // apiEndpoint = "https://westcentralus.api.cognitive.microsoft.com/face/v1.0"
    private final String apiEndpoint = "https://westcentralus.api.cognitive.microsoft.com/face/v1.0";

    // Replace `<Subscription Key>` with your subscription key.
    // For example, subscriptionKey = "0123456789abcdef0123456789ABCDEF"
    private final String subscriptionKey = "88045bc6bb7f4679a8de7f546a045807";

    private final FaceServiceClient faceServiceClient =
            new FaceServiceRestClient(apiEndpoint, subscriptionKey);

    private final int PICK_IMAGE = 1;
    private ProgressDialog detectionProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button1 = findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(
                        intent, "Select Picture"), PICK_IMAGE);
            }
        });

        detectionProgressDialog = new ProgressDialog(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK &&
                data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                        getContentResolver(), uri);
                ImageView imageView = findViewById(R.id.imageView1);
                imageView.setImageBitmap(bitmap);

                // Comment out for tutorial
                detectAndFrame(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Detect faces by uploading a face image.
    // Frame faces after detection.
    private void detectAndFrame(final Bitmap imageBitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(outputStream.toByteArray());

        AsyncTask<InputStream, String, Face[]> detectTask =
                new AsyncTask<InputStream, String, Face[]>() {
                    String exceptionMessage = "";

                    @Override
                    protected Face[] doInBackground(InputStream... params) {
                        try {
                            publishProgress("Detecting...");
                            Face[] result = faceServiceClient.detect(
                                    params[0],
                                    true,         // returnFaceId
                                    false,        // returnFaceLandmarks
                                    //null          // returnFaceAttributes:
                                     new FaceServiceClient.FaceAttributeType[] {
                                        FaceServiceClient.FaceAttributeType.Emotion
                                        //FaceServiceClient.FaceAttributeType.Gender
                                    }

                            );
                            if (result == null){
                                publishProgress(
                                        "Detection Finished. Nothing detected");
                                return null;
                            }
                            publishProgress(String.format(
                                    "Detection Finished. %d face(s) detected",
                                    result.length));
                            Log.i("myTag", " returning value is "+result);
                            return result;
                        } catch (Exception e) {
                            exceptionMessage = String.format(
                                    "Detection failed: %s", e.getMessage());
                            return null;
                        }
                    }

                    @Override
                    protected void onPreExecute() {
                        //TODO: show progress dialog
                        detectionProgressDialog.show();
                    }
                    @Override
                    protected void onProgressUpdate(String... progress) {
                        //TODO: update progress
                        detectionProgressDialog.setMessage(progress[0]);
                    }
                    @Override
                    protected void onPostExecute(Face[] result) {
                        //TODO: update face frames
                        detectionProgressDialog.dismiss();

                        if(!exceptionMessage.equals("")){
                            showError(exceptionMessage);
                        }
                        if (result == null) return;

                        ImageView imageView = findViewById(R.id.imageView1);
                        imageView.setImageBitmap(
                                drawFaceRectanglesOnBitmap(imageBitmap, result));
                        imageBitmap.recycle();
                    }
                };

        detectTask.execute(inputStream);
    }

    private void showError(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }})
                .create().show();
    }

    private static Bitmap drawFaceRectanglesOnBitmap(
            Bitmap originalBitmap, Face[] faces) {


        Bitmap bitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);
        paint.setStrokeWidth(10);
        if (faces != null) {
            for (Face face : faces) {
                //Log.i("myTag", " face value is "+face.faceAttributes.emotion.anger);
                paint.setTextSize(100);
                //canvas.drawText("hello",900,1500,paint);

                double max = 0;
                double min = 0;
                double scores[] = new double[] {face.faceAttributes.emotion.anger,
                        face.faceAttributes.emotion.happiness,
                        face.faceAttributes.emotion.contempt,
                        face.faceAttributes.emotion.disgust,
                        face.faceAttributes.emotion.fear,
                        face.faceAttributes.emotion.neutral,
                        face.faceAttributes.emotion.sadness,
                        face.faceAttributes.emotion.surprise};
                String emotion = "";
                for (int j = 0; j < scores.length; j++) {
                    Log.i("myTag", " value is "+scores[j]);
                    if (scores[j] > max) {
                        max = scores[j];
                    }
                    if (scores[j] < min) {
                        min = scores[j];
                    }
                }
                double range = max-min;
                double perEmo =0;
                double perc[] = new double[] {(face.faceAttributes.emotion.anger-min)/range,
                        (face.faceAttributes.emotion.happiness-min)/range,
                        (face.faceAttributes.emotion.contempt-min)/range,
                        (face.faceAttributes.emotion.disgust-min)/range,
                        (face.faceAttributes.emotion.fear-min)/range,
                        (face.faceAttributes.emotion.neutral-min)/range,
                        (face.faceAttributes.emotion.sadness-min)/range,
                        (face.faceAttributes.emotion.surprise-min)/range};

                if(max==scores[0]){
                    emotion="anger";
                    perEmo=scores[0];
                }
                if(max==scores[1]){
                    emotion="happiness";
                    perEmo=scores[1];
                }
                if(max==scores[2]){
                    emotion="contempt";
                    perEmo=scores[2];
                }
                if(max==scores[3]) {
                    emotion = "disgust";
                    perEmo = scores[3];
                }
                if(max==scores[4]) {
                    emotion = "fear";
                    perEmo = scores[4];
                }
                if(max==scores[5]) {
                    emotion = "neutral";
                    perEmo = scores[5];
                }
                if(max==scores[6]) {
                    emotion = "sadness";
                    perEmo = scores[6];
                }
                if(max==scores[7]) {
                    emotion = "surprise";
                    perEmo = scores[7];
                }



                perEmo = perEmo*100;
                FaceRectangle faceRectangle = face.faceRectangle;
                Log.i("myTag", " face rectangle is "+faceRectangle.left);
                canvas.drawText(emotion + " " +perEmo + " %",faceRectangle.left +10,faceRectangle.top + faceRectangle.height+100,paint);

                canvas.drawRect(
                        faceRectangle.left,
                        faceRectangle.top,
                        //canvas.drawText("hello",faceRectangle.left,paint);
                        faceRectangle.left + faceRectangle.width,
                        faceRectangle.top + faceRectangle.height,
                        paint);
            }
        }
        return bitmap;
    }
}
