package com.android.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.microsoft.projectoxford.emotion.EmotionServiceClient;
import com.microsoft.projectoxford.emotion.EmotionServiceRestClient;
import com.microsoft.projectoxford.emotion.contract.FaceRectangle;
import com.microsoft.projectoxford.emotion.contract.RecognizeResult;
import com.microsoft.projectoxford.emotion.rest.EmotionServiceException;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.FaceAttribute;
import com.microsoft.projectoxford.face.rest.ClientException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    Button btnCamera;
    Button btnGallery;
    ImageView imgPicked;
    EditText etResult;
    private static final int CAMERA_REQUEST = 1;
    private EmotionServiceClient client;
    private Bitmap bitmap;
    private List<FaceAttribute> faceAttributes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        btnCamera = (Button) findViewById(R.id.btn_camera);
        btnGallery = (Button) findViewById(R.id.btn_gallery);
        imgPicked = (ImageView) findViewById(R.id.iv_selected_Image);
        etResult = (EditText) findViewById(R.id.et_results);
        faceAttributes = new ArrayList<>();
        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }
        });

        btnGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(view.getContext(), "Work In Progress", Toast.LENGTH_SHORT).show();
            }
        });

        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            etResult.setText("");
            bitmap = ImageHelper.loadSizeLimitedBitmapFromUri(data.getData(), getContentResolver());
            bitmap = (Bitmap) data.getExtras().get("data");
            imgPicked.setImageBitmap(bitmap);
            getTheEmotionsFromTheImage();
        }
    }

    private void getTheEmotionsFromTheImage() {
        if (client == null) {
            client = new EmotionServiceRestClient(getString(R.string.emotion_subscription_key));
            new sendEmotionRequest().execute();
        } else {
            new sendEmotionRequest().execute();
        }
    }

    private class sendEmotionRequest extends AsyncTask<String, String, List<RecognizeResult>> {

        @Override
        protected List<RecognizeResult> doInBackground(String... strings) {

            try {
                return processImage();
            } catch (EmotionServiceException e) {
                e.printStackTrace();
            } catch (ClientException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<RecognizeResult> recognizeResults) {
            super.onPostExecute(recognizeResults);
            if (recognizeResults.size() == 0) {
                Toast.makeText(imgPicked.getContext(), "NO Emotions found", Toast.LENGTH_SHORT).show();
            } else {
                for (int i = 0; i < recognizeResults.size(); i++) {
                    etResult.append(String.format("\nFace #%1$d \n", i + 1));
                    etResult.append(faceAttributes.get(i).gender + "\n");
                    etResult.append(String.format("\tAge :%1$.5f\n ", faceAttributes.get(i).age));
                    etResult.append(String.format("\t anger: %1$.5f\n", recognizeResults.get(i).scores.anger));
                    etResult.append(String.format("\t contempt: %1$.5f\n", recognizeResults.get(i).scores.contempt));
                    etResult.append(String.format("\t disgust: %1$.5f\n", recognizeResults.get(i).scores.disgust));
                    etResult.append(String.format("\t fear: %1$.5f\n", recognizeResults.get(i).scores.fear));
                    etResult.append(String.format("\t happiness: %1$.5f\n", recognizeResults.get(i).scores.happiness));
                    etResult.append(String.format("\t neutral: %1$.5f\n", recognizeResults.get(i).scores.neutral));
                    etResult.append(String.format("\t sadness: %1$.5f\n", recognizeResults.get(i).scores.sadness));
                    etResult.append(String.format("\t surprise: %1$.5f\n", recognizeResults.get(i).scores.surprise));
                }
            }
        }
    }

    private List<RecognizeResult> processImage() throws EmotionServiceException, com.microsoft.projectoxford.face.rest.ClientException, IOException {

        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());
        FaceRectangle[] faceRectangles = null;
        FaceAttribute[] faceAttribute = null;
        String faceSubscriptionKey = getString(R.string.face_Subscription_key);
        FaceServiceClient faceClient = new FaceServiceRestClient("https://westcentralus.api.cognitive.microsoft.com/face/v1.0", faceSubscriptionKey);
        Face faces[] = faceClient.detect(inputStream, false, false, new FaceServiceClient.FaceAttributeType[]{FaceServiceClient.FaceAttributeType.Gender, FaceServiceClient.FaceAttributeType.Age});
        if (faces != null) {
            faceRectangles = new FaceRectangle[faces.length];
            faceAttribute = new FaceAttribute[faces.length];

            for (int i = 0; i < faceRectangles.length; i++) {
                // Face API and Emotion API have different FaceRectangle definition. Do the conversion.
                com.microsoft.projectoxford.face.contract.FaceRectangle rect = faces[i].faceRectangle;
                faceRectangles[i] = new com.microsoft.projectoxford.emotion.contract.FaceRectangle(rect.left, rect.top, rect.width, rect.height);
            }

            for (int i = 0; i < faceAttribute.length; i++) {
                // Face API and Emotion API have different FaceRectangle definition. Do the conversion.
                faceAttributes.add(faces[i].faceAttributes);

            }
        }

        List<RecognizeResult> result = null;
        if (faceRectangles != null) {
            inputStream.reset();
            result = this.client.recognizeImage(inputStream);
        }
        return result;
    }
}
