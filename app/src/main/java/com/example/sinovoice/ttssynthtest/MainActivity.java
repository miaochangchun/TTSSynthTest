package com.example.sinovoice.ttssynthtest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.sinovoice.ttssynth.HciCloudSysHelper;
import com.example.sinovoice.ttssynth.HciCloudTtsHelper;
import com.sinovoice.hcicloudsdk.common.HciErrorCode;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = MainActivity.class.getSimpleName();
    private EditText myText;
    private Button synthButton;
    private TextView textView;
    private HciCloudSysHelper mHciCloudSysHelper;
    private HciCloudTtsHelper mHciCloudTtsHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myText = (EditText) findViewById(R.id.editText);
        synthButton = (Button) findViewById(R.id.button);
        textView = (TextView) findViewById(R.id.textView);

        synthButton.setOnClickListener(this);

        mHciCloudSysHelper = HciCloudSysHelper.getInstance();
        mHciCloudTtsHelper = HciCloudTtsHelper.getInstance();

        int errorCode = mHciCloudSysHelper.init(this);
        Log.d(TAG, "mHciCloudSysHelper.init return " + errorCode);
        errorCode = mHciCloudTtsHelper.initTts(this, "tts.local.synth");
        Log.d(TAG, "mHciCloudTtsHelper.initTts return " + errorCode);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button:
                int nRet = mHciCloudTtsHelper.synthTts(myText.getText().toString(), "tts.local.synth");
                if (nRet == HciErrorCode.HCI_ERR_NONE) {
                    textView.setText("语音合成文件保存在：" + mHciCloudTtsHelper.getFilePath());
                }else{
                    textView.setText("语音合成失败了！");
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        mHciCloudTtsHelper.releaseTts();
        mHciCloudSysHelper.release();
        super.onDestroy();
    }
}
