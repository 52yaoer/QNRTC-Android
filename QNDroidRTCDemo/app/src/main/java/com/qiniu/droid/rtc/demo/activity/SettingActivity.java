package com.qiniu.droid.rtc.demo.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

import com.qiniu.droid.rtc.demo.BuildConfig;
import com.qiniu.droid.rtc.demo.R;
import com.qiniu.droid.rtc.demo.ui.SpinnerPopupWindow;
import com.qiniu.droid.rtc.demo.utils.Config;
import com.qiniu.droid.rtc.demo.utils.QNAppServer;
import com.qiniu.droid.rtc.demo.utils.ToastUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class SettingActivity extends AppCompatActivity {

    private EditText mUserNameEditText;
    private TextView mConfigTextView;
    private TextView mVersionCodeTextView;
    private RadioGroup mCodecModeRadioGroup;
    private RadioButton mHwCodecMode;
    private RadioButton mSwCodecMode;
    private RadioGroup mAudioSampleRateRadioGroup;
    private RadioButton mLowSampleRateBtn;
    private RadioButton mHighSampleRateBtn;
    private RadioGroup mMaintainResRadioGroup;
    private RadioButton mMaintainResolutionYes;
    private RadioButton mMaintainResolutionNo;
    private EditText mAppIdEditText;
    private Switch mAec3Switch;

    private int mSelectPos = 0;
    private String mUserName;
    private int mEncodeMode = 0;
    private int mSampleRatePos = 0;
    private boolean mMaintainResolution = false;
    private boolean mIsAec3Enabled = false;
    private List<String> mDefaultConfiguration = new ArrayList<>();
    private ArrayAdapter<String> mAdapter;
    private SpinnerPopupWindow mSpinnerPopupWindow;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        mUserNameEditText = (EditText) findViewById(R.id.user_name_edit_text);
        mConfigTextView = (TextView) findViewById(R.id.config_text_view);
        mVersionCodeTextView = (TextView) findViewById(R.id.version_code);
        mCodecModeRadioGroup = (RadioGroup) findViewById(R.id.codec_mode_button);
        mCodecModeRadioGroup.setOnCheckedChangeListener(mOnCheckedChangeListener);
        mHwCodecMode = (RadioButton) findViewById(R.id.hw_radio_button);
        mSwCodecMode = (RadioButton) findViewById(R.id.sw_radio_button);
        mAudioSampleRateRadioGroup = findViewById(R.id.sample_rate_button);
        mAudioSampleRateRadioGroup.setOnCheckedChangeListener(mOnCheckedChangeListener);
        mLowSampleRateBtn = findViewById(R.id.low_sample_rate_button);
        mHighSampleRateBtn = findViewById(R.id.high_sample_rate_button);

        mMaintainResRadioGroup = (RadioGroup) findViewById(R.id.maintain_resolution_button);
        mMaintainResRadioGroup.setOnCheckedChangeListener(mOnMaintainResCheckedChangeListener);
        mMaintainResolutionYes = (RadioButton) findViewById(R.id.maintain_res_button_yes);
        mMaintainResolutionNo = (RadioButton) findViewById(R.id.maintain_res_button_no);

        mAppIdEditText = (EditText) findViewById(R.id.app_id_edit_text);
        mAec3Switch = findViewById(R.id.webrtc_aec3_enable_btn);
        mAec3Switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mIsAec3Enabled = isChecked;
            }
        });

        mVersionCodeTextView.setText(String.format(getString(R.string.version_code), getVersionDescription(), getBuildTimeDescription()));

        SharedPreferences preferences = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        mUserName = preferences.getString(Config.USER_NAME, "");
        mUserNameEditText.setHint("用户名称：" + mUserName);
        String mAppId = preferences.getString(Config.APP_ID, QNAppServer.APP_ID);

        if (!mAppId.equals(QNAppServer.APP_ID)) {
            mAppIdEditText.setText(mAppId);
        }

        //test mode setting
        LinearLayout testModeLayout = (LinearLayout) findViewById(R.id.test_mode_layout);
        testModeLayout.setVisibility(isTestMode() ? View.VISIBLE : View.GONE);

        String[] configurations = getResources().getStringArray(R.array.conference_configuration);
        mDefaultConfiguration.addAll(Arrays.asList(configurations));

        mSelectPos = preferences.getInt(Config.CONFIG_POS, 1);
        mConfigTextView.setText(mDefaultConfiguration.get(mSelectPos));

        int codecMode = preferences.getInt(Config.CODEC_MODE, Config.HW);
        if (codecMode == Config.HW) {
            mHwCodecMode.setChecked(true);
        } else {
            mSwCodecMode.setChecked(true);
        }

        int sampleRatePos = preferences.getInt(Config.SAMPLE_RATE, Config.HIGH_SAMPLE_RATE);
        if (sampleRatePos == Config.LOW_SAMPLE_RATE) {
            mLowSampleRateBtn.setChecked(true);
        } else {
            mHighSampleRateBtn.setChecked(true);
        }

        mMaintainResolution = preferences.getBoolean(Config.MAINTAIN_RES, false);
        if (mMaintainResolution) {
            mMaintainResolutionYes.setChecked(true);
        } else {
            mMaintainResolutionNo.setChecked(true);
        }

        mIsAec3Enabled = preferences.getBoolean(Config.AEC3_ENABLE, false);
        mAec3Switch.setChecked(mIsAec3Enabled);

        mSpinnerPopupWindow = new SpinnerPopupWindow(this);
        mSpinnerPopupWindow.setOnSpinnerItemClickListener(mOnSpinnerItemClickListener);

        mAdapter = new ArrayAdapter<String>(this, R.layout.spinner_item, mDefaultConfiguration);
    }

    public void onClickBack(View v) {
        finish();
    }

    public void onClickConfigParams(View v) {
        showPopupWindow();
    }

    public void onClickSaveConfiguration(View v) {
        String userName = mUserNameEditText.getText().toString().trim();
        String appId = mAppIdEditText.getText().toString().trim();

        SharedPreferences.Editor editor = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE).edit();
        if (!userName.equals("")) {
            if (!MainActivity.isUserNameOk(userName)) {
                ToastUtils.s(this, getString(R.string.wrong_user_name_toast));
                return;
            }

            if (!mUserName.equals(userName)) {
                editor.putString(Config.USER_NAME, userName);
            }
        }

        editor.putString(Config.APP_ID, TextUtils.isEmpty(appId) ? QNAppServer.APP_ID : appId);

        editor.putInt(Config.CONFIG_POS, mSelectPos);
        editor.putInt(Config.CODEC_MODE, mEncodeMode);
        editor.putInt(Config.SAMPLE_RATE, mSampleRatePos);
        editor.putBoolean(Config.MAINTAIN_RES, mMaintainResolution);
        editor.putBoolean(Config.AEC3_ENABLE, mIsAec3Enabled);

        editor.putInt(Config.WIDTH, Config.DEFAULT_RESOLUTION[mSelectPos][0]);
        editor.putInt(Config.HEIGHT, Config.DEFAULT_RESOLUTION[mSelectPos][1]);
        editor.putInt(Config.FPS, Config.DEFAULT_FPS[mSelectPos]);
        editor.putInt(Config.BITRATE, Config.DEFAULT_BITRATE[mSelectPos]);

        if (isTestMode()) {
            saveTestMode(editor);
        }
        editor.apply();
        finish();
    }

    private void saveTestMode(SharedPreferences.Editor editor) {
        int testModeWidth = 0;
        int testModeHigh = 0;
        int testModeFPS = 0;
        int testModeBitrate = 0;

        EditText testModeWidthEditText = (EditText) findViewById(R.id.test_mode_width);
        EditText testModeHighEditText = (EditText) findViewById(R.id.test_mode_high);
        EditText testModeFPSEditText = (EditText) findViewById(R.id.test_mode_fps);
        EditText testModeBitrateEditText = (EditText) findViewById(R.id.test_mode_bitrate);

        try {
            testModeWidth = Integer.parseInt(testModeWidthEditText.getText().toString());
            testModeHigh = Integer.parseInt(testModeHighEditText.getText().toString());
            testModeFPS = Integer.parseInt(testModeFPSEditText.getText().toString());
            testModeBitrate = Integer.parseInt(testModeBitrateEditText.getText().toString());
        } catch (NumberFormatException e) {
        }

        if (testModeWidth > 0 && testModeHigh > 0 && testModeFPS > 0 && testModeBitrate > 0) {
            editor.putInt(Config.WIDTH, testModeWidth);
            editor.putInt(Config.HEIGHT, testModeHigh);
            editor.putInt(Config.FPS, testModeFPS);
            editor.putInt(Config.BITRATE, testModeBitrate);
        }
    }

    private void showPopupWindow() {
        mSpinnerPopupWindow.setAdapter(mAdapter);
        mSpinnerPopupWindow.setWidth(mConfigTextView.getWidth());
        mSpinnerPopupWindow.showAsDropDown(mConfigTextView);
    }

    private String getVersionDescription() {
        PackageManager packageManager = getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "unknown";
    }

    protected String getBuildTimeDescription() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(BuildConfig.BUILD_TIMESTAMP);
    }

    private boolean isTestMode() {
//        if (mAppIdEditText.getText().toString().compareTo(QNAppServer.TEST_MODE_APP_ID) == 0) {
//            return true;
//        }
        return false;
    }

    private SpinnerPopupWindow.OnSpinnerItemClickListener mOnSpinnerItemClickListener = new SpinnerPopupWindow.OnSpinnerItemClickListener() {
        @Override
        public void onItemClick(int pos) {
            mSelectPos = pos;
            mConfigTextView.setText(mDefaultConfiguration.get(mSelectPos));
            mSpinnerPopupWindow.dismiss();
        }
    };

    private RadioGroup.OnCheckedChangeListener mOnCheckedChangeListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            switch (group.getCheckedRadioButtonId()) {
                case R.id.hw_radio_button:
                    mEncodeMode = Config.HW;
                    mMaintainResRadioGroup.setVisibility(View.GONE);
                    break;
                case R.id.sw_radio_button:
                    mEncodeMode = Config.SW;
                    mMaintainResRadioGroup.setVisibility(View.VISIBLE);
                    break;
                case R.id.low_sample_rate_button:
                    mSampleRatePos = Config.LOW_SAMPLE_RATE;
                    break;
                case R.id.high_sample_rate_button:
                    mSampleRatePos = Config.HIGH_SAMPLE_RATE;
                    break;
            }
        }
    };

    private RadioGroup.OnCheckedChangeListener mOnMaintainResCheckedChangeListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            switch (group.getCheckedRadioButtonId()) {
                case R.id.maintain_res_button_yes:
                    mMaintainResolution = true;
                    break;
                case R.id.maintain_res_button_no:
                    mMaintainResolution = false;
                    break;
            }
        }
    };
}
