package com.cxdev.voicenotes;

import android.content.Context;

import com.cxdev.voicenotes.R;
import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

import java.io.IOException;
import java.io.InputStream;

public class SpeechCredentialsProvider implements CredentialsProvider {
    private Context context;

    public SpeechCredentialsProvider(Context context) {
        this.context = context;
    }

    @Override
    public Credentials getCredentials() throws IOException {
        InputStream fileStream = context.getResources().openRawResource(R.raw.credential);
        return ServiceAccountCredentials.fromStream(fileStream);
    }
}
