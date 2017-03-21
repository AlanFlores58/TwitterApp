package com.example.alanflores.twitterapp;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class MainActivity extends AppCompatActivity {

    Button buttonConectar, buttonEnviar;
    EditText editTwitter;
    ImageView imageView;

    private SharedPreferences sharedPreferencesTwitter;
    private static Twitter twitter;
    private static RequestToken requestToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v("111","wentro");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonConectar = (Button) findViewById(R.id.button_conectar);
        buttonEnviar = (Button) findViewById(R.id.button_enviar_tweeter);
        editTwitter = (EditText) findViewById(R.id.edit_tweet);
        imageView = (ImageView) findViewById(R.id.imagen_usuario);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
        StrictMode.setThreadPolicy(policy);

        sharedPreferencesTwitter = getSharedPreferences(Constantes.ARCHIVOS_PREFERENCIAS, MODE_PRIVATE);

        Uri uri = getIntent().getData();
        if(uri != null && uri.toString().startsWith(Constantes.CALLBACK_URL)){
            String verificador = uri.getQueryParameter(Constantes.VERIFICADOR_OAUTH);
            try {
                AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, verificador);
                SharedPreferences.Editor editor = sharedPreferencesTwitter.edit();
                editor.putString(Constantes.PREFERENCIA_KEY_TPKEN, accessToken.getToken());
                editor.putString(Constantes.PREFERENCIA_KEY_SECRET, accessToken.getTokenSecret());
                editor.apply();

                User user = twitter.showUser(twitter.getId());
                URL urlImage = new URL(user.getOriginalProfileImageURL());
                imageView.setImageDrawable(Drawable.createFromStream((InputStream)urlImage.getContent(),""));

            } catch (TwitterException e) {
                e.printStackTrace();
            }catch (MalformedURLException e){
                e.printStackTrace();
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        buttonConectar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(estadoConexion()){
                    cerrarSesion();
                    buttonConectar.setText("Conectar");
                }else
                    obtenerAutorizacion();
            }
        });

        buttonEnviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enviarTweet();
            }
        });
    }

    private boolean estadoConexion(){
        return sharedPreferencesTwitter.getString(Constantes.PREFERENCIA_KEY_TPKEN, null) != null;
    }

    private void cerrarSesion(){
        SharedPreferences.Editor editor = sharedPreferencesTwitter.edit();
        editor.remove(Constantes.PREFERENCIA_KEY_TPKEN);
        editor.remove(Constantes.PREFERENCIA_KEY_SECRET);
        editor.apply();
        imageView.setImageDrawable(null);

    }

    private void obtenerAutorizacion(){
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();

        configurationBuilder.setOAuthConsumerKey(Constantes.CONSUMER_KEY);
        configurationBuilder.setOAuthConsumerSecret(Constantes.CONSUMER_SECRET);

        Configuration configuration = configurationBuilder.build();
        twitter = new TwitterFactory(configuration).getInstance();
        try {
            requestToken = twitter.getOAuthRequestToken(Constantes.CALLBACK_URL);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(requestToken.getAuthenticationURL()));
            startActivity(intent);
        } catch (TwitterException e) {
            e.printStackTrace();
        }
    }

    private void enviarTweet(){
        String oAuthAccessToken = sharedPreferencesTwitter.getString(Constantes.PREFERENCIA_KEY_TPKEN, "");
        String oAuthAccessTokenSecret = sharedPreferencesTwitter.getString(Constantes.PREFERENCIA_KEY_SECRET, "");

        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        Configuration configuration = configurationBuilder.setDebugEnabled(true)
                .setOAuthConsumerKey(Constantes.CONSUMER_KEY)
                .setOAuthConsumerSecret(Constantes.CONSUMER_SECRET)
                .setOAuthAccessToken(oAuthAccessToken)
                .setOAuthAccessTokenSecret(oAuthAccessTokenSecret)
                .build();

        Twitter twitter = new TwitterFactory(configuration).getInstance();
        try {
            twitter.updateStatus(editTwitter.getText().toString());
            Toast.makeText(getApplicationContext(), "Tweet publicado", Toast.LENGTH_SHORT).show();
        } catch (TwitterException e) {
            e.printStackTrace();
            DialogoTwitter dialogoTwitter = new DialogoTwitter();
            dialogoTwitter.setMessage("Error al publicar el twitte");
            dialogoTwitter.show(getSupportFragmentManager(), "tw");
        }

    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if(estadoConexion()){
            buttonConectar.setText("Desconectar");
        }
        else buttonConectar.setText("Conectar");
    }

    static class DialogoTwitter extends DialogFragment{

        private String message;

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreateDialog(savedInstanceState);
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
            alertDialog.setMessage(message);
            alertDialog.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dismiss();
                }
            });

            return alertDialog.create();
        }

        public void setMessage(String message){
            this.message = message;
        }
    }
}
