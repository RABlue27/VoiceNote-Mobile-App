package com.cxdev.voicenotes;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.cxdev.voicenotes.SpeechCredentialsProvider;
import com.google.api.gax.rpc.ClientStream;
import com.google.api.gax.rpc.ResponseObserver;
import com.google.api.gax.rpc.StreamController;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.StreamingRecognitionConfig;
import com.google.cloud.speech.v1.StreamingRecognitionResult;
import com.google.cloud.speech.v1.StreamingRecognizeRequest;
import com.google.cloud.speech.v1.StreamingRecognizeResponse;
import com.google.cloud.speech.v1.stub.GrpcSpeechStub;
import com.google.cloud.speech.v1.stub.SpeechStubSettings;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.util.ArrayList;

public class GoogleSpeech extends Service {

    private static final String TAG = "SpeechService";
    private ClientStream<StreamingRecognizeRequest> clientStream;
    private SpeechClient speechClient;
    private final SpeechBinder speechBinder = new SpeechBinder();
    private final List<SpeechListener> speechResultListeners = new ArrayList<>();


    // initiate speech client after service connected
    public void initSpeechClient() throws IOException {
        if(speechClient == null) {
            SpeechStubSettings.newBuilder()
                    .setCredentialsProvider(new SpeechCredentialsProvider(this))
                    .setEndpoint("$HOSTNAME:$PORT")
                    .build();
            GrpcSpeechStub grpcStub = GrpcSpeechStub.create(this);
            speechClient = SpeechClient.create(grpcStub);
        }
    }


    public void createRecognizingRequest(int sampleRate) throws IOException {
        if (speechClient == null) {
            initSpeechClient();
        }

        clientStream = speechClient.streamingRecognizeCallable().splitCall(responseObserver);

        StreamingRecognizeRequest streamRequest = StreamingRecognizeRequest.newBuilder()
                .setStreamingConfig(StreamingRecognitionConfig.newBuilder()
                        .setConfig(RecognitionConfig.newBuilder()
                                .setLanguageCode(SpeechLanguageCodes.ENGLISH)
                                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                                .setSampleRateHertz(sampleRate)
                                .build())
                        .setInterimResults(true)
                        .setSingleUtterance(false)
                        .build())
                .build();

        clientStream.send(streamRequest);
    }


    // method used to recognize audio
    public void recognize(byte[] data, int size, int sampleRate) {
        if(clientStream == null){
            createRecognizingRequest(sampleRate);
        }

        clientStream.send(
                StreamingRecognizeRequest.newBuilder()
                        .setAudioContent(ByteString.copyFrom(data, 0, size))
                        .build()
        );
    }

    public void addListener(SpeechListener listener) {
        speechResultListeners.add(listener);
    }

    public void removeListener(SpeechListener listener) {
        speechResultListeners.remove(listener);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return speechBinder;
    }

    public class SpeechBinder extends Binder {
        public SpeechService getService() {
            return SpeechService.this;
        }
    }

    public static SpeechService from(IBinder binder) {
        return ((SpeechBinder) binder).getService();
    }
    public static final String HOSTNAME = "speech.googleapis.com";
    public static final int PORT = 443;

    // responsible to observe speech results and send them to added listeners
    private ResponseObserver<StreamingRecognizeResponse> responseObserver = new ResponseObserver<StreamingRecognizeResponse>() {

        @Override
        public void onStart(StreamController controller) {}

        @Override
        public void onResponse(StreamingRecognizeResponse response) {
            if(response != null) {
                String text = null;
                boolean isFinal = false;
                if (response.getResultsCount() > 0) {
                    StreamingRecognitionResult result = response.getResults(0);
                    isFinal = result.getIsFinal();
                    if (result.getAlternativesCount() > 0) {
                        SpeechRecognitionAlternative alternative = result.getAlternatives(0);
                        text = alternative.getTranscript();
                    }
                }
                if (text != null) {
                    for (SpeechListener listener : speechResultListeners) {
                        listener.onSpeechRecognized(text, isFinal);
                    }
                }
            }
        }
    };
}