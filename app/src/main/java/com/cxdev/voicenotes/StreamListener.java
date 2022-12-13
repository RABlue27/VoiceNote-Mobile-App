package com.cxdev.voicenotes;

public interface StreamListener {

    void onVoiceStreaming(short[] data, int size);
}
