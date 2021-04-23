package syrol.lenos.com.ng;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.view.View;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;
import java.io.File;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    ProgressBar progressBar;
    WebView app;
    String appURL;

    private static final int FILECHOOSER_RESULTCODE = 1;
    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mUploadMessages;
    private Uri mCapturedImageURI = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        appURL = "https://lenos.com.ng/?req=app";
        app =findViewById(R.id.app);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        loadApp(savedInstanceState);
    }

    //    load
    public void loadApp(Bundle savedInstanceState) {
        WebSettings settings = app.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setDomStorageEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_NORMAL);
        settings.setDatabaseEnabled(true);
        settings.setSaveFormData(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setGeolocationEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(false);

        app.getSettings().setLoadWithOverviewMode(true);
        app.getSettings().setUseWideViewPort(true);
        app.setWebChromeClient(new WebChromeClient());
        app.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        app.clearCache(false);
        if (savedInstanceState != null) {
            app.restoreState(savedInstanceState);
        }else{
            app.loadUrl(appURL);
        }

        app.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);

                if (url.matches("(.*)api.whatsapp.com(.*)") || url.matches("(.*)wa.me(.*)") || url.matches("(.*)whatsapp://send/(.*)")) {
                    startActivity(
                            new Intent(Intent.ACTION_VIEW,
                                    Uri.parse(url)
                            )
                    );

                } else if (url.matches("(.*)tel:(.*)") || url.matches("(.*)callto:(.*)")) {
                    Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + url.replace("tel:", "").replace("callto:", "")));
                    startActivity(intent);
                } else if (url.matches("(.*)mailto:(.*)")) {
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("mailto:")); // only email apps should handle this
                    intent.putExtra(Intent.EXTRA_EMAIL, url.replace("mailto:", "support@herb.ng"));
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Hello Herbs NG");
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }
                }
            }


            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
                if (url.matches("(.*)api.whatsapp.com(.*)") || url.matches("(.*)wa.me(.*)") || url.matches("(.*)whatsapp://send/(.*)")) {
                    app.stopLoading();
                    startActivity(
                            new Intent(Intent.ACTION_VIEW,
                                    Uri.parse(url)
//                                Uri.parse(
//                                        String.format("https://api.whatsapp.com/send?phone=%s&text=%s", phoneNumberWithCountryCode, message)
//                                )
                            )
                    );

                } else if (url.matches("(.*)tel:(.*)") || url.matches("(.*)callto:(.*)")) {
                    app.stopLoading();
                    Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + url.replace("tel:", "").replace("callto:", "")));
                    startActivity(intent);
                } else if (url.matches("(.*)mailto:(.*)")) {
                    app.stopLoading();
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("mailto:")); // only email apps should handle this
                    intent.putExtra(Intent.EXTRA_EMAIL, url.replace("mailto:", ""));
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Hello Herbs NG");
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                appURL = url;
                progressBar.setVisibility(View.INVISIBLE);
                app.setVisibility(View.VISIBLE);
            }


            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (!isConnected()) {
                    startActivity(new Intent(getApplicationContext(), syrol.lenos.com.ng.NetworkError.class).putExtra("page","network_error"));
                    finish();
                } else {
                    app.loadUrl(url);
                }

                return true;

            }

        });

        app.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (!isConnected()) {
                    app.stopLoading();
                    app.setVisibility(View.INVISIBLE);
                }
                super.onProgressChanged(view, newProgress);
            }

            private String getTitleFromUrl(String url) {
                String title = url;
                try {
                    URL urlObj = new URL(url);
                    String host = urlObj.getHost();
                    if (host != null && !host.isEmpty()) {
                        return urlObj.getProtocol() + "://" + host;
                    }
                    if (url.startsWith("file:")) {
                        String fileName = urlObj.getFile();
                        if (fileName != null && !fileName.isEmpty()) {
                            return fileName;
                        }
                    }
                } catch (Exception e) {
                    // ignore
                }

                return title;
            }

            @Override
            public boolean onJsAlert(android.webkit.WebView view, String url, String message, final JsResult result) {
                String newTitle = getTitleFromUrl(url);
                new AlertDialog.Builder(MainActivity.this).setTitle(newTitle).setMessage(message).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        result.confirm();
                    }
                }).setCancelable(false).create().show();
                return true;
            }

            @Override
            public boolean onJsConfirm(android.webkit.WebView view, String url, String message, final JsResult result) {

                String newTitle = getTitleFromUrl(url);
                new AlertDialog.Builder(MainActivity.this).setTitle(newTitle).setMessage(message).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        result.confirm();
                    }
                }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        result.cancel();
                    }
                }).setCancelable(false).create().show();
                return true;
            }

            // openFileChooser for Android 3.0+
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
                mUploadMessage = uploadMsg;
                openImageChooser();
            }
            // For Lollipop 5.0+ Devices
            public boolean onShowFileChooser(WebView mWebView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                mUploadMessages = filePathCallback;
                openImageChooser();
                return true;
            }
            // openFileChooser for Android < 3.0

            public void openFileChooser(ValueCallback<Uri> uploadMsg) {
                openFileChooser(uploadMsg, "");
            }
            //openFileChooser for other Android versions

            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                openFileChooser(uploadMsg, acceptType);
            }

            private void openImageChooser() {
                try {
                    File imageStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Images");
                    if (!imageStorageDir.exists()) {
                        imageStorageDir.mkdirs();
                    }
                    File file = new File(imageStorageDir + File.separator + "IMG_" + String.valueOf(System.currentTimeMillis()) + ".jpg");
                    mCapturedImageURI = Uri.fromFile(file);

                    final Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);

                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("image/*");
                    i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    Intent chooserIntent = Intent.createChooser(i, "Image Chooser");
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Parcelable[]{captureIntent});

                    startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILECHOOSER_RESULTCODE) {

            if (null == mUploadMessage && null == mUploadMessages) {
                return;
            }
            if (null != mUploadMessage) {
                handleUploadMessage(requestCode, resultCode, data);
            } else if (mUploadMessages != null) {
                handleUploadMessages(requestCode, resultCode, data);
            }
        }
    }

    private void handleUploadMessage(final int requestCode, final int resultCode, final Intent data) {
        Uri result = null;
        try {
            if (resultCode != RESULT_OK) {
                result = null;
            } else {
                // retrieve from the private variable if the intent is null
                result = data == null ? mCapturedImageURI : data.getData();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mUploadMessage.onReceiveValue(result);
        mUploadMessage = null;

        // code for all versions except of Lollipop
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {

            result = null;

            try {
                if (resultCode != RESULT_OK) {
                    result = null;
                } else {
                    // retrieve from the private variable if the intent is null
                    result = data == null ? mCapturedImageURI : data.getData();
                }
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "activity :" + e, Toast.LENGTH_LONG).show();
            }

            mUploadMessage.onReceiveValue(result);
            mUploadMessage = null;
        }

    } // end of code for all versions except of Lollipop

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void handleUploadMessages(final int requestCode, final int resultCode, final Intent data) {
        Uri[] results = null;
        try {
            if (resultCode != RESULT_OK) {
                results = null;
            } else {
                if (data != null) {
                    String dataString = data.getDataString();
                    ClipData clipData = data.getClipData();
                    if (clipData != null) {
                        results = new Uri[clipData.getItemCount()];
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            ClipData.Item item = clipData.getItemAt(i);
                            results[i] = item.getUri();
                        }
                    }
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                } else {
                    results = new Uri[]{mCapturedImageURI};
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mUploadMessages.onReceiveValue(results);
        mUploadMessages = null;
    }

    @Override
    public void onBackPressed() {
        if (app.canGoBack()) {
            app.goBack();
        } else {
            super.onBackPressed();
        }
    }

    //Network State
    private boolean isConnected() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }
}