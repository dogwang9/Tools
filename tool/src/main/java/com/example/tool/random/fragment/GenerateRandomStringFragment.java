package com.example.tool.random.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.example.lib.utils.AndroidUtils;
import com.example.tool.R;
import com.google.android.material.slider.RangeSlider;
import com.google.android.material.slider.Slider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GenerateRandomStringFragment extends Fragment {

    private SwitchCompat mNumberSwitch;
    private RangeSlider mNumberLengthSlider;
    private SwitchCompat mUppercaseSwitch;
    private SwitchCompat mLowercaseSwitch;
    private SwitchCompat mSpecialCharsSwitch;
    private Slider mStringLengthSlider;
    private Button mGenerateButton;
    private TextView mResultTextView;
    private ImageView mCopyImageView;

    private static final String NUMBERS = "0123456789";
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;':,.<>?";

    private final Random mRandom = new Random();

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_generate_random_string, container, false);

        initView(view);
        initDefaultState();
        initListeners();

        return view;
    }

    private void initView(View view) {
        mNumberSwitch = view.findViewById(R.id.switch_number);
        mNumberLengthSlider = view.findViewById(R.id.slider_range_number);
        mUppercaseSwitch = view.findViewById(R.id.switch_upper_case);
        mLowercaseSwitch = view.findViewById(R.id.switch_lower_case);
        mSpecialCharsSwitch = view.findViewById(R.id.switch_special_character);
        mStringLengthSlider = view.findViewById(R.id.slider_length);
        mGenerateButton = view.findViewById(R.id.btn_generate);
        mResultTextView = view.findViewById(R.id.tv_result);
        mCopyImageView = view.findViewById(R.id.iv_copy);
    }

    private void initDefaultState() {
        mNumberSwitch.setChecked(true);
        mUppercaseSwitch.setChecked(true);
        mLowercaseSwitch.setChecked(true);
        mSpecialCharsSwitch.setChecked(true);
    }

    private void initListeners() {
        mGenerateButton.setOnClickListener(v -> generateRandomString());

        mCopyImageView.setOnClickListener(v -> {
            String text = mResultTextView.getText().toString();
            if (!TextUtils.isEmpty(text)) {
                AndroidUtils.INSTANCE.copyToClipboard(requireContext(), text, null);
                Toast.makeText(getContext(), R.string.toast_copied, Toast.LENGTH_SHORT).show();
            }
        });

        // 数字开关联动 RangeSlider
        mNumberSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                mNumberLengthSlider.setEnabled(isChecked));
    }

    private void generateRandomString() {

        boolean includeNumbers = mNumberSwitch.isChecked();
        boolean includeUppercase = mUppercaseSwitch.isChecked();
        boolean includeLowercase = mLowercaseSwitch.isChecked();
        boolean includeSpecial = mSpecialCharsSwitch.isChecked();

        if (!includeNumbers && !includeUppercase && !includeLowercase && !includeSpecial) {
            Toast.makeText(
                    getContext(),
                    R.string.toast_at_least_one_character_type,
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        int totalLength = (int) mStringLengthSlider.getValue();
        List<Character> result = new ArrayList<>();

        // 1. 数字处理（严格使用 RangeSlider 区间）
        if (includeNumbers) {
            List<Float> values = mNumberLengthSlider.getValues();
            int min = values.get(0).intValue();
            int max = values.get(1).intValue();

            int numberCount = min + mRandom.nextInt(max - min + 1);
            numberCount = Math.min(numberCount, totalLength);

            addRandomChars(result, NUMBERS, numberCount);
            totalLength -= numberCount;
        }

        // 2. 其他字符池
        StringBuilder pool = new StringBuilder();
        if (includeUppercase) pool.append(UPPERCASE);
        if (includeLowercase) pool.append(LOWERCASE);
        if (includeSpecial) pool.append(SPECIAL_CHARS);

        if (pool.length() == 0 && totalLength > 0) {
            Toast.makeText(getContext(), R.string.toast_invalid_length, Toast.LENGTH_SHORT).show();
            return;
        }

        addRandomChars(result, pool.toString(), totalLength);

        // 3. 打乱顺序
        Collections.shuffle(result);

        // 4. 输出
        StringBuilder sb = new StringBuilder();
        for (char c : result) {
            sb.append(c);
        }
        mResultTextView.setText(sb.toString());
    }

    private void addRandomChars(List<Character> target, String source, int count) {
        for (int i = 0; i < count; i++) {
            target.add(source.charAt(mRandom.nextInt(source.length())));
        }
    }
}