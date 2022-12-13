package com.cxdev.voicenotes;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class SpeechRecognizer {

    private AudioRecord voiceRecorder;
    private StreamListener voiceStreamListener;
    private final ExecutorService streamExecutorService = Executors.newFixedThreadPool(1);
    private boolean isStreaming = false;

    public void registerOnVoiceListener(StreamListener voiceStreamListener) {
        this.voiceStreamListener = voiceStreamListener;
    }

    private final Thread runnableAudioStream = new Thread() {
        public void run() {
            try {
                short[] buffer = new short[minBufferSize];
                if (voiceRecorder == null) {
                    voiceRecorder = new AudioRecord(
                            MediaRecorder.AudioSource.MIC,
                            sampleRate,
                            channelConfig,
                            audioFormat,
                            minBufferSize * 10
                    );
                }
                voiceRecorder.startRecording();
                while (isStreaming) {
                    minBufferSize = voiceRecorder.read(buffer, 0, buffer.length);
                    voiceStreamListener.onVoiceStreaming(buffer, minBufferSize);
                    Log.i("MinBufferSize : ", String.valueOf(buffer.length));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public int getSampleRate() {
        return (voiceRecorder != null) ? voiceRecorder.getSampleRate() : 0;
    }

    public void stopVoiceStreaming() {
        isStreaming = false;
        voiceRecorder.release();
        voiceRecorder = null;
        if (runnableAudioStream.isAlive())
            streamExecutorService.shutdown();
    }


    private void startVoiceStreaming() {
        isStreaming = true;
        streamExecutorService.submit(runnableAudioStream);
    }

    private static final int sampleRate = 44000;
    private static final int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private static final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private static int minBufferSize = 2200;
}