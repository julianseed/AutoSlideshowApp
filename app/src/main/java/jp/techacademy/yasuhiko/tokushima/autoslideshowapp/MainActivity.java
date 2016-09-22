package jp.techacademy.yasuhiko.tokushima.autoslideshowapp;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE = 100;

    boolean debug_f = false;     // Debug用ログの表示（true : する、false : しない）
    Long id = 0L;
    ImageView imageView;
    Uri imageUri;
    Cursor cursor;
    Timer timer;
    Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // バージョン判定
        // Android 6.0以降の場合
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // パーミッションの許可状態を確認する
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED) {
                // 許可されている
                getContentsCtrl();
            } else {
                // 許可されていないので、許可ダイアログを表示する
                requestPermissions(new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE
                },PERMISSIONS_REQUEST_CODE);
            }
        } else {
            // Android 5系以下の場合
            getContentsCtrl();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permission[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getContentsCtrl();
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Appを終了する時にcursorをcloseする
        cursor.close();
    }

    // 画像表示コントロール用の関数
    private void getContentsCtrl() {
        logD("getContentsCtrl start");

        // 画像の情報を取得する
        ContentResolver resolver = getContentResolver();
        cursor = resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,   // データの種類
                null,       // 項目（null = 全項目）
                null,       // フィルタ条件（null = フィルタなし）
                null,       // フィルタ用パラメータ
                null        // ソート（null = ソートなし）
        );

        // 最初の画像を表示する
        if (cursor.moveToFirst()) {
            id = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media._ID));
            imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

            imageView = (ImageView) findViewById(R.id.imageView);
            imageView.setImageURI(imageUri);
        } else {
            // 画像が１つもない場合のメッセージ表示
            showErrorDialog("画像がありません。Galleryに画像を追加して再度起動して下さい。");
            return;
        }

        final Button button_Forword = (Button) findViewById(R.id.button_Forward);
        button_Forword.setEnabled(true);
        button_Forword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imageNext();
            }
        });
        final Button button_Back = (Button) findViewById(R.id.button_Back);
        button_Back.setEnabled(true);
        button_Back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imagePrev();
            }
        });

        final Button button_Play = (Button) findViewById(R.id.button_Play);
        button_Play.setEnabled(true);
        button_Play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logD("button_Play onClick start");

                // timerを複数稼働させないための安全装置
                if (null != timer) {
                    timer.cancel();
                    timer = null;
                }

                if (button_Play.getText().equals("再生")) {
                    // ボタンが「再生」の時は、止まっている状態　→　スライドショー開始
                    // 進むボタンと戻るボタンを使用不可にする
                    button_Forword.setEnabled(false);
                    button_Back.setEnabled(false);
                    // 再生ボタンのTextを停止に変更する
                    button_Play.setText("停止");

                    // timerを使って、スライドショーを２秒毎にに自動実行
                    timer = new Timer();

                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            handler.post(new Runnable() {
                               public void run() {
                                   imageNext();
                               }
                            });
                        }
                    }, 0, 2000);
                } else {
                    // ボタンが「再生」以外（つまり「停止」）の時は、スライドショー状態　→　停止
                    // 進むボタンと戻るボタンを使用可にする
                    button_Forword.setEnabled(true);
                    button_Back.setEnabled(true);
                    // 再生ボタンのTextを再生に変更する
                    button_Play.setText("再生");

                    // 念のため入れておく
                    if (null != timer) {
                        timer.cancel();
                        timer = null;
                    }
                }
            }
        });

    }

    // 次の画像を表示する関数
    private void imageNext() {
        logD("imageNext start");

        if (cursor.isLast()) {
            cursor.moveToFirst();
        } else {
            cursor.moveToNext();
        }

        id = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media._ID));
        imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
        imageView.setImageURI(imageUri);
    }

    // 前の画像を表示する関数
    private void imagePrev() {
        logD("imagePrev start");

        if (cursor.isFirst()) {
            cursor.moveToLast();
        } else {
            cursor.moveToPrevious();
        }

        id = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media._ID));
        imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
        imageView.setImageURI(imageUri);
    }
    // 文字を渡せば、Log表示をよろしくやってくれる関数
    private void logD(String s) {
        if (debug_f) Log.d("Android", s);
    }

    // エラー時のメッセージを表示する
    private void showErrorDialog(String msg) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("エラー");
        alertDialogBuilder.setMessage(msg);
        alertDialogBuilder.setPositiveButton("OK", null);

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}
