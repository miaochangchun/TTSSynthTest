package com.example.sinovoice.ttssynth;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.sinovoice.hcicloudsdk.api.tts.HciCloudTts;
import com.sinovoice.hcicloudsdk.common.HciErrorCode;
import com.sinovoice.hcicloudsdk.common.Session;
import com.sinovoice.hcicloudsdk.common.tts.ITtsSynthCallback;
import com.sinovoice.hcicloudsdk.common.tts.TtsConfig;
import com.sinovoice.hcicloudsdk.common.tts.TtsInitParam;
import com.sinovoice.hcicloudsdk.common.tts.TtsSynthResult;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by miaochangchun on 2016/10/27.
 */
public class HciCloudTtsHelper {
    private static final String TAG = HciCloudTtsHelper.class.getSimpleName();

    private static HciCloudTtsHelper mHciCloudTtsHelper = null;
    private FileOutputStream mFos = null;
    String filePath;

    private HciCloudTtsHelper(){

    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }

    public static HciCloudTtsHelper getInstance() {
        if (mHciCloudTtsHelper == null) {
            return new HciCloudTtsHelper();
        }
        return mHciCloudTtsHelper;
    }

    /**
     * TTS的初始化功能
     * @param context   上下文参数
     * @param capkey    capkey功能，需要根据所需能力选择
     * @return  返回0为成功，其他为失败
     */
    public int initTts(Context context, String capkey) {
        TtsInitParam ttsInitParam = new TtsInitParam();
        //本地的合成capkey为tts.local.synth；云端的合成capkey需要根据发音人选择，比如tts.cloud.wangjing
        ttsInitParam.addParam(TtsInitParam.PARAM_KEY_INIT_CAP_KEYS, capkey);
        //本地的合成能力需要设置dataPath，可以设置到so所在的目录下，并修改音库的名称。云端则不需要此配置。
        String dataPath = context.getFilesDir().getAbsolutePath().replace("files", "lib");
        ttsInitParam.addParam(TtsInitParam.PARAM_KEY_DATA_PATH, dataPath);
        ttsInitParam.addParam(TtsInitParam.PARAM_KEY_FILE_FLAG, "android_so");
        int errorCode = HciCloudTts.hciTtsInit(ttsInitParam.getStringConfig());
        return errorCode;
    }

    /**
     * 语音合成函数
     * @param text  需要合成的文本
     * @param capkey    capkey功能，需要根据所需能力选择
     * @return  返回0为成功，其他为失败
     */
    public int synthTts(String text, String capkey) {
        TtsConfig sessionConfig = new TtsConfig();
        sessionConfig.addParam(TtsConfig.SessionConfig.PARAM_KEY_CAP_KEY, capkey);
        Session session = new Session();
        int errorCode = HciCloudTts.hciTtsSessionStart(sessionConfig.getStringConfig(), session);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "hciTtsSessionStart error and return " + errorCode);
            return errorCode;
        }
        //TTS合成函数，第三个参数如果设置为空，则使用sessionConfig配置
        errorCode = HciCloudTts.hciTtsSynth(session, text, "", new TtsSynthCallback());
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "hciTtsSynth error and return " + errorCode);
            return  errorCode;
        }
        errorCode = HciCloudTts.hciTtsSessionStop(session);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "hciTtsSessionStop error and return " + errorCode);
            return errorCode;
        }
        return HciErrorCode.HCI_ERR_NONE;
    }

    /**
     * 反初始化TTS能力
     * @return  返回0为成功，其他为失败
     */
    public int releaseTts(){
        return HciCloudTts.hciTtsRelease();
    }

    /**
     * TTS的合成回调函数
     */
    private class TtsSynthCallback implements ITtsSynthCallback{

        @Override
        public boolean onSynthFinish(int i, TtsSynthResult ttsSynthResult) {
            // errorCode 为当前合成操作返回的错误码,如果返回值为HciErrorCode.HCI_ERR_NONE则表示合成成功
            if (i != HciErrorCode.HCI_ERR_NONE) {
                Log.e(TAG, "synth error, code = " + i);
                return false;
            }

            if (mFos == null){
                initFileOutputStream();
            }

            if(ttsSynthResult != null && ttsSynthResult.getVoiceData() != null){
                //将合成的音频数据保存到文件
                int length = ttsSynthResult.getVoiceData().length;
                if (length > 0) {
                    try {
                        mFos.write(ttsSynthResult.getVoiceData(), 0, length);
                        mFos.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (!ttsSynthResult.isHasMoreData()) {
                flushOutputStream();
            }

            // 返回true表示处理结果成功,通知引擎可以继续合成并返回下一次的合成结果; 如果不希望引擎继续合成, 则返回false
            // 该方法在引擎中是同步的,即引擎会持续阻塞一直到该方法执行结束
            return true;
        }
    }

    private void flushOutputStream() {
        try {
            mFos.close();
            mFos = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initFileOutputStream() {
        try {
            filePath = Environment.getExternalStorageDirectory() + File.separator
                    + "pcm" + File.separator
                    + "synth" + System.currentTimeMillis() + ".pcm";
            File file = new File(filePath);
            File parentFile = file.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }
            if (file.exists()) {
                file.delete();
            } else {
                file.createNewFile();
            }
            mFos = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
