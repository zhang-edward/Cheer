package com.menlohacksiicheer.cheer;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.ibm.watson.developer_cloud.android.library.camera.CameraHelper;
import com.ibm.watson.developer_cloud.conversation.v1.ConversationService;
import com.ibm.watson.developer_cloud.conversation.v1.model.Intent;
import com.ibm.watson.developer_cloud.conversation.v1.model.MessageRequest;
import com.ibm.watson.developer_cloud.conversation.v1.model.MessageResponse;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.ToneAnalyzer;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ElementTone;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.SentenceTone;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneAnalysis;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneCategory;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneScore;
import com.ibm.watson.developer_cloud.visual_recognition.v3.VisualRecognition;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifyImagesOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualClassification;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualClassifier;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static com.ibm.watson.developer_cloud.android.library.camera.CameraHelper.REQUEST_IMAGE_CAPTURE;

public class MainActivity extends AppCompatActivity {

    private ArrayList<Message> messages;
    private String allUserMessages;

    static final int REQUEST_TAKE_PHOTO = 1;

    // Views
    private RecyclerView recyclerView;
    private ChatRecyclerViewAdapter chatViewAdapter;
    private EditText userMessage;
    private ImageButton sendButton;

    private ConversationService convoService;
    private ToneAnalyzer toneAnalyzer;
    private VisualRecognition visualRecognition;

    private MessageResponse prevResponse;

    String mCurrentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        messages = new ArrayList<Message>();

        // IBM services
        convoService = new ConversationService(ConversationService.VERSION_DATE_2016_07_11);
        convoService.setUsernameAndPassword("873617a3-3cf9-4bba-aa12-6d982aa822a0", "UaciS4eba3bp");

        toneAnalyzer = new ToneAnalyzer(ToneAnalyzer.VERSION_DATE_2016_05_19);
        toneAnalyzer.setUsernameAndPassword("4be8d9d7-817e-47c0-8a1f-7f358696903f", "BMssqOcnDrd5");

        visualRecognition = new VisualRecognition(VisualRecognition.VERSION_DATE_2016_05_20);
        visualRecognition.setApiKey("68d02db2652ec6a41786e9824b4f07fa185b47b6");

        // Views
        userMessage = (EditText)findViewById(R.id.user_message);
        sendButton = (ImageButton)findViewById(R.id.send_button);
        recyclerView = (RecyclerView)findViewById(R.id.chat_window);

        // RecyclerView
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        llm.setStackFromEnd(true);
        recyclerView = (RecyclerView)findViewById(R.id.chat_window);
        recyclerView.setLayoutManager(llm);

        chatViewAdapter = new ChatRecyclerViewAdapter<>(messages, this);
        recyclerView.setAdapter(chatViewAdapter);

        // Set on click listener
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = userMessage.getText().toString();

                MessageRequest newMessage;
                // send new MessageRequest with context of previous response, if applicable
                if (prevResponse != null)
                    newMessage = new MessageRequest.Builder()
                        .inputText(message)
                        .context(prevResponse.getContext())
                        .build();
                else
                    newMessage = new MessageRequest.Builder()
                            .inputText(message)
                            .build();

                //System.out.println(newMessage);

                new MessageResponseTask().execute(newMessage);
                addMessage(false, message);     // update RecyclerView

                // add to total user messages list
                allUserMessages += " " + message;
                userMessage.setText("");

            }
        });
    }

    private void addMessage(boolean incoming, String message) {

        messages.add(new Message(incoming, message));
        chatViewAdapter.notifyDataSetChanged();
        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                // Call smooth scroll
                recyclerView.smoothScrollToPosition(chatViewAdapter.getItemCount());
            }
        });
    }

    private void dispatchTakePictureIntent() {
        android.content.Intent takePictureIntent = new android.content.Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.e("MainActivity.java", "Oh darn, the camera done broke!");
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.menlohacksiicheer.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    // Menu icons are inflated just as they were with actionbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.action_camera:
                dispatchTakePictureIntent();
                Toast.makeText(this, "Camera selected", Toast.LENGTH_SHORT)
                        .show();
                break;
            // action with ID action_settings was selected
            case R.id.action_settings:
                Toast.makeText(this, "Settings selected", Toast.LENGTH_SHORT)
                        .show();
                break;
            default:
                break;
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_TAKE_PHOTO) {
            File f = new File(mCurrentPhotoPath);
            new VisualAnalysisTask().execute(f);
        }
    }

    // gets the Watson message response
    private class MessageResponseTask extends AsyncTask<MessageRequest, Void, MessageResponse> {
        @Override
        protected MessageResponse doInBackground(MessageRequest... message) {
            MessageResponse response = convoService.message("b5f6d815-ea32-4f02-9a1a-dc537a69538e", message[0]).execute();
            prevResponse = response;
            // System.out.println(response);
            return response;
        }
        // prints the message to the user
        protected void onPostExecute(MessageResponse response) {
            if (response.getText().size() > 0)
                addMessage(true, response.getText().get(0));
            for (Intent intent : response.getIntents()) {
                Log.i("", intent.getIntent());
                if (intent.getIntent().equals("goodbye") || intent.getIntent().equals("No")) {
                    new ToneAnalysisTask().execute(allUserMessages);
                }
            }
        }
    }

    private class ToneAnalysisTask extends AsyncTask<String, Void, ToneAnalysis> {
        @Override
        protected ToneAnalysis doInBackground(String... text) {
            ToneAnalysis tone = toneAnalyzer.getTone(text[0], null).execute();
            return tone;
        }
        protected void onPostExecute(ToneAnalysis result) {
            ToneCategory toneCategory = result.getDocumentTone().getTones().get(0);
            String highestTone = "";
            double highScore = 0.0;
            for (ToneScore score : toneCategory.getTones()) {
                if (score.getScore() > highScore) {
                    highestTone = score.getName();
                    highScore = score.getScore();
                }
            }
            highScore *= 100;
            addMessage(true, "During this conversation, I detected a lot of " + highestTone + ". " +
                    "Your propensity for " + highestTone + " is " + highScore + "%");
        }
    }

    private class VisualAnalysisTask extends AsyncTask<File, Void, VisualClassification> {
        @Override
        protected VisualClassification doInBackground(File... images) {
            ClassifyImagesOptions options = new ClassifyImagesOptions.Builder()
                    .images(images[0])
                    .classifierIds("happyorsad_823488379")
                    .build();
            VisualClassification result = visualRecognition.classify(options).execute();
            return result;
        }
        protected void onPostExecute(VisualClassification result) {
            System.out.println("RESULT: " + result);
            VisualClassifier.VisualClass visualClass = result
                    .getImages().get(0)
                    .getClassifiers().get(0)
                    .getClasses().get(0);
            String visualClassName = visualClass.getName();
            String reaction;
            if (visualClassName.equals("happy"))
                reaction = "Things are looking up!";
            else
                reaction = "Feel better!";
            addMessage(true, "According to our models, you seem to be " + visualClass.getName() + ". " +
                    reaction);
        }
    }
}

