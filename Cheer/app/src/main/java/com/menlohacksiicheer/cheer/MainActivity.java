package com.menlohacksiicheer.cheer;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

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

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ArrayList<Message> messages;
    private String allUserMessages;

    // Views
    private RecyclerView recyclerView;
    private ChatRecyclerViewAdapter chatViewAdapter;
    private EditText userMessage;
    private ImageButton sendButton;

    private ConversationService convoService;
    private ToneAnalyzer toneAnalyzer;

    private MessageResponse prevResponse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        messages = new ArrayList<Message>();

        // IBM Connection toneAnalyzer
        convoService = new ConversationService(ConversationService.VERSION_DATE_2016_07_11);
        convoService.setUsernameAndPassword("873617a3-3cf9-4bba-aa12-6d982aa822a0", "UaciS4eba3bp");

        toneAnalyzer = new ToneAnalyzer(ToneAnalyzer.VERSION_DATE_2016_05_19);
        toneAnalyzer.setUsernameAndPassword("4be8d9d7-817e-47c0-8a1f-7f358696903f", "BMssqOcnDrd5");

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

    // gets the Watson message response
    private class MessageResponseTask extends AsyncTask<MessageRequest, Void, MessageResponse> {
        @Override
        protected MessageResponse doInBackground(MessageRequest... message) {
            MessageResponse response = convoService.message("b5f6d815-ea32-4f02-9a1a-dc537a69538e", message[0]).execute();
            prevResponse = response;
            System.out.println(response);
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
                    Log.i("MainActivity.class", "fucking work");
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
}

