package com.stem10a16.autostreak;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {

    public static final String PREFS_NAME    = "AutoStreakPrefs";
    public static final String KEY_TARGETS   = "all_targets";
    public static final String KEY_SELECTED  = "selected_targets";
    public static final String KEY_MSG       = "streak_message";
    public static final String KEY_HOUR      = "schedule_hour";
    public static final String KEY_MINUTE    = "schedule_minute";

    private static final String DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";

    private WebView webView;
    private LinearLayout checkboxContainer;
    private TextView logConsole;
    private EditText msgInput;
    private EditText hourInput;
    private EditText minInput;
    private SharedPreferences prefs;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isWebViewVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        ScrollView rootScroll = new ScrollView(this);
        rootScroll.setBackgroundColor(Color.parseColor("#121212"));
        rootScroll.setFillViewport(true);

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(32, 40, 32, 40);
        rootScroll.addView(mainLayout);

        // Tiêu đề App
        TextView title = new TextView(this);
        title.setText("🔥 AUTO-STREAK TIKTOK (10A16)");
        title.setTextColor(Color.parseColor("#FE2C55"));
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        mainLayout.addView(title);

        TextView subTitle = new TextView(this);
        subTitle.setText("Hệ thống tự động hóa giữ chuỗi tin nhắn định kỳ mỗi ngày 1 lần");
        subTitle.setTextColor(Color.parseColor("#AAAAAA"));
        subTitle.setTextSize(12);
        subTitle.setGravity(Gravity.CENTER);
        subTitle.setPadding(0, 8, 0, 24);
        mainLayout.addView(subTitle);

        // KHU VỰC 1: CẤP QUYỀN CHẠY NỀN
        mainLayout.addView(createSectionHeader("1. Cấp quyền hệ thống & Chạy nền"));
        Button btnPerm = createStyledButton("⚡ Cấp quyền Chạy Ngầm & Bỏ tối ưu Pin", "#25F4EE", "#000000");
        btnPerm.setOnClickListener(v -> requestSpecialPermissions());
        mainLayout.addView(btnPerm);

        // KHU VỰC 2: WEBVIEW ĐĂNG NHẬP TIKTOK
        mainLayout.addView(createSectionHeader("2. Tài khoản TikTok (Gmail / FB / SĐT / QR)"));

        LinearLayout webBtnRow = new LinearLayout(this);
        webBtnRow.setOrientation(LinearLayout.HORIZONTAL);

        Button btnToggleWeb = createStyledButton("🌐 Mở/Ẩn Khung Đăng Nhập", "#333333", "#FFFFFF");
        Button btnOpenMsg   = createStyledButton("📩 Vào Hộp Thư TikTok",       "#FE2C55", "#FFFFFF");

        LinearLayout.LayoutParams halfParam = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        halfParam.setMargins(4, 4, 4, 4);
        btnToggleWeb.setLayoutParams(halfParam);
        btnOpenMsg.setLayoutParams(halfParam);

        webBtnRow.addView(btnToggleWeb);
        webBtnRow.addView(btnOpenMsg);
        mainLayout.addView(webBtnRow);

        setupWebView();

        LinearLayout.LayoutParams webParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 950);
        webParams.setMargins(0, 12, 0, 12);
        webView.setLayoutParams(webParams);
        webView.setVisibility(View.GONE);
        mainLayout.addView(webView);

        btnToggleWeb.setOnClickListener(v -> {
            isWebViewVisible = !isWebViewVisible;
            webView.setVisibility(isWebViewVisible ? View.VISIBLE : View.GONE);
            if (isWebViewVisible && webView.getUrl() == null) {
                webView.loadUrl("https://www.tiktok.com/login");
                appendLog("Đã mở cổng đăng nhập TikTok.");
            }
        });

        btnOpenMsg.setOnClickListener(v -> {
            webView.setVisibility(View.VISIBLE);
            isWebViewVisible = true;
            webView.loadUrl("https://www.tiktok.com/messages");
            appendLog("Đang tải danh sách Hộp thư TikTok...");
        });

        // KHU VỰC 3: DANH SÁCH TÍCH CHỌN
        mainLayout.addView(createSectionHeader("3. Danh sách giữ chuỗi (Tích chọn)"));

        LinearLayout targetBtnRow = new LinearLayout(this);
        targetBtnRow.setOrientation(LinearLayout.HORIZONTAL);
        Button btnScan       = createStyledButton("🔍 Quét từ Hộp thư",        "#2A2A2A", "#25F4EE");
        Button btnAddManual  = createStyledButton("➕ Thêm tên thủ công",       "#2A2A2A", "#FFFFFF");
        btnScan.setLayoutParams(halfParam);
        btnAddManual.setLayoutParams(halfParam);
        targetBtnRow.addView(btnScan);
        targetBtnRow.addView(btnAddManual);
        mainLayout.addView(targetBtnRow);

        btnScan.setOnClickListener(v -> scanStreakContactsFromWebView());
        btnAddManual.setOnClickListener(v -> showAddContactDialog());

        checkboxContainer = new LinearLayout(this);
        checkboxContainer.setOrientation(LinearLayout.VERTICAL);
        checkboxContainer.setBackgroundColor(Color.parseColor("#1E1E1E"));
        checkboxContainer.setPadding(20, 20, 20, 20);
        mainLayout.addView(checkboxContainer);
        refreshTargetCheckboxes();

        // KHU VỰC 4: CẤU HÌNH NỘI DUNG & GIỜ GỬI
        mainLayout.addView(createSectionHeader("4. Cấu hình tự động gửi (1 lần/ngày)"));

        msgInput = new EditText(this);
        msgInput.setText(prefs.getString(KEY_MSG, "chuỗi"));
        msgInput.setHint("Nhập tin nhắn giữ chuỗi (VD: chuỗi)");
        msgInput.setTextColor(Color.WHITE);
        msgInput.setHintTextColor(Color.GRAY);
        msgInput.setBackgroundColor(Color.parseColor("#1E1E1E"));
        msgInput.setPadding(20, 20, 20, 20);
        mainLayout.addView(msgInput);

        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        timeRow.setGravity(Gravity.CENTER_VERTICAL);
        timeRow.setPadding(0, 12, 0, 12);

        TextView timeLabel = new TextView(this);
        timeLabel.setText("Giờ tự chạy mỗi ngày (24h): ");
        timeLabel.setTextColor(Color.WHITE);
        timeRow.addView(timeLabel);

        hourInput = new EditText(this);
        hourInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        hourInput.setText(String.valueOf(prefs.getInt(KEY_HOUR, 7)));
        hourInput.setTextColor(Color.WHITE);
        hourInput.setWidth(120);
        timeRow.addView(hourInput);

        TextView colon = new TextView(this);
        colon.setText(" : ");
        colon.setTextColor(Color.WHITE);
        timeRow.addView(colon);

        minInput = new EditText(this);
        minInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        minInput.setText(String.valueOf(prefs.getInt(KEY_MINUTE, 30)));
        minInput.setTextColor(Color.WHITE);
        minInput.setWidth(120);
        timeRow.addView(minInput);

        mainLayout.addView(timeRow);

        Button btnSaveStart = createStyledButton(
                "🚀 Lưu cấu hình & Bật Tự Động Chạy Ngầm", "#FE2C55", "#FFFFFF");
        btnSaveStart.setOnClickListener(v -> saveAndActivateService());
        mainLayout.addView(btnSaveStart);

        Button btnRunNow = createStyledButton(
                "⚡ Chạy gửi Chuỗi ngay lập tức (Test)", "#1F883D", "#FFFFFF");
        btnRunNow.setOnClickListener(v -> executeDailyStreakBatch(false));
        mainLayout.addView(btnRunNow);

        Button btnResetToday = createStyledButton(
                "🔄 Reset trạng thái ngày hôm nay (Để gửi lại)", "#444444", "#FFFFFF");
        btnResetToday.setOnClickListener(v -> resetTodayLogs());
        mainLayout.addView(btnResetToday);

        // KHU VỰC 5: NHẬT KÝ
        mainLayout.addView(createSectionHeader("5. Nhật ký thực thi (System Log)"));

        logConsole = new TextView(this);
        logConsole.setBackgroundColor(Color.parseColor("#0A0A0A"));
        logConsole.setTextColor(Color.parseColor("#00FF66"));
        logConsole.setTypeface(Typeface.MONOSPACE);
        logConsole.setTextSize(11);
        logConsole.setPadding(20, 20, 20, 20);
        logConsole.setText("[SYSTEM] Khởi tạo Auto-Streak 10A16 hoàn tất.\n");
        mainLayout.addView(logConsole);

        setContentView(rootScroll);

        if (getIntent() != null && getIntent().getBooleanExtra("AUTO_TRIGGER", false)) {
            appendLog("[AUTO] Kích hoạt phiên gửi chuỗi định kỳ...");
            mainHandler.postDelayed(() -> executeDailyStreakBatch(false), 3000);
        }
    }

    private void setupWebView() {
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setUserAgentString(DESKTOP_UA);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                CookieManager.getInstance().flush();
                appendLog("Đã tải trang: " + url);
            }
        });

        webView.addJavascriptInterface(new WebAppInterface(), "AndroidBridge");
    }

    private void requestSpecialPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } else {
            Toast.makeText(this, "Đã cấp quyền chạy nền tối đa!", Toast.LENGTH_SHORT).show();
        }
        appendLog("Đã kiểm tra và kích hoạt quyền chạy nền.");
    }

    private void scanStreakContactsFromWebView() {
        appendLog("Đang quét các cuộc trò chuyện trong hộp thư...");
        String jsScan =
                "(function() {" +
                "var names = [];" +
                "var items = document.querySelectorAll('[data-e2e=\"chat-list-item\"], p, span');" +
                "for (var i = 0; i < items.length; i++) {" +
                "  var txt = (items[i].innerText || '').trim();" +
                "  if (txt.length > 2 && txt.length < 35 && txt.indexOf('\\n') === -1 && " +
                "      !txt.match(/^(Tin nhắn|Messages|Tất cả|chuỗi|Hôm nay|Yesterday)$/i)) {" +
                "    if (names.indexOf(txt) === -1 && names.length < 25) names.push(txt);" +
                "  }" +
                "}" +
                "AndroidBridge.onContactsScanned(JSON.stringify(names));" +
                "})();";
        webView.evaluateJavascript(jsScan, null);
    }

    private void showAddContactDialog() {
        EditText input = new EditText(this);
        input.setHint("Nhập đúng tên hiển thị hoặc @username trên TikTok");
        new AlertDialog.Builder(this)
                .setTitle("Thêm người nhận chuỗi")
                .setView(input)
                .setPositiveButton("Thêm", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        Set<String> all = new HashSet<>(prefs.getStringSet(KEY_TARGETS, new HashSet<>()));
                        Set<String> selected = new HashSet<>(prefs.getStringSet(KEY_SELECTED, new HashSet<>()));
                        all.add(name);
                        selected.add(name);
                        prefs.edit()
                                .putStringSet(KEY_TARGETS, all)
                                .putStringSet(KEY_SELECTED, selected)
                                .apply();
                        refreshTargetCheckboxes();
                        appendLog("Đã thêm & tích chọn: " + name);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void refreshTargetCheckboxes() {
        checkboxContainer.removeAllViews();
        Set<String> allTargets = new HashSet<>(prefs.getStringSet(KEY_TARGETS, new HashSet<>()));
        Set<String> selectedTargets = new HashSet<>(prefs.getStringSet(KEY_SELECTED, new HashSet<>()));

        if (allTargets.isEmpty()) {
            allTargets.add("Bạn giữ chuỗi 1 (Bấm Thêm để nhập tên thật)");
            prefs.edit().putStringSet(KEY_TARGETS, allTargets).apply();
        }

        String today = getTodayDateString();
        for (String target : allTargets) {
            boolean alreadySentToday = prefs.getBoolean("sent_" + today + "_" + target, false);

            CheckBox cb = new CheckBox(this);
            String status = alreadySentToday ? " [✅ Đã gửi hôm nay]" : " [⏳ Chưa gửi]";
            cb.setText("🔥 " + target + status);
            cb.setTextColor(alreadySentToday ? Color.parseColor("#00FF66") : Color.WHITE);
            cb.setChecked(selectedTargets.contains(target));

            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Set<String> currentSelected =
                        new HashSet<>(prefs.getStringSet(KEY_SELECTED, new HashSet<>()));
                if (isChecked) currentSelected.add(target);
                else currentSelected.remove(target);
                prefs.edit().putStringSet(KEY_SELECTED, currentSelected).apply();
            });

            checkboxContainer.addView(cb);
        }
    }

    private void saveAndActivateService() {
        String msg = msgInput.getText().toString().trim();
        if (msg.isEmpty()) msg = "chuỗi";

        int hour = 7, min = 30;
        try {
            hour = Integer.parseInt(hourInput.getText().toString().trim());
            min  = Integer.parseInt(minInput.getText().toString().trim());
        } catch (NumberFormatException ignored) {}

        prefs.edit()
                .putString(KEY_MSG, msg)
                .putInt(KEY_HOUR, Math.max(0, Math.min(23, hour)))
                .putInt(KEY_MINUTE, Math.max(0, Math.min(59, min)))
                .apply();

        Intent serviceIntent = new Intent(this, StreakForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        DailyAlarmReceiver.scheduleDailyAlarm(this, hour, min);
        appendLog("Đã bật Dịch vụ chạy ngầm. Hẹn giờ gửi mỗi ngày lúc "
                + hour + ":" + String.format(Locale.US, "%02d", min));
        Toast.makeText(this, "Đã lưu & Kích hoạt chạy ngầm!", Toast.LENGTH_SHORT).show();
    }

    private void executeDailyStreakBatch(boolean forceIgnoreDate) {
        Set<String> selected = prefs.getStringSet(KEY_SELECTED, new HashSet<>());
        if (selected.isEmpty()) {
            appendLog("CẢNH BÁO: Bạn chưa tích chọn người nào trong danh sách!");
            return;
        }

        String today = getTodayDateString();
        String msg = prefs.getString(KEY_MSG, "chuỗi");
        List<String> pendingList = new ArrayList<>();

        for (String target : selected) {
            boolean sent = prefs.getBoolean("sent_" + today + "_" + target, false);
            if (!sent || forceIgnoreDate) pendingList.add(target);
            else appendLog("Bỏ qua [" + target + "] vì hôm nay (" + today + ") đã gửi rồi.");
        }

        if (pendingList.isEmpty()) {
            appendLog("Hoàn tất kiểm tra: Tất cả người được tích chọn đều đã nhận chuỗi hôm nay!");
            return;
        }

        appendLog("Bắt đầu phiên gửi tự động cho " + pendingList.size() + " người...");
        webView.loadUrl("https://www.tiktok.com/messages");

        for (int i = 0; i < pendingList.size(); i++) {
            final String targetName = pendingList.get(i);
            final int index = i;
            mainHandler.postDelayed(
                    () -> sendStreakToTargetViaJS(targetName, msg, today),
                    7000L + (index * 6000L));
        }
    }

    private void sendStreakToTargetViaJS(String targetName, String message, String today) {
        appendLog("Đang tìm cuộc trò chuyện: " + targetName + "...");

        String safeTarget = targetName.replace("'", "\\'");
        String safeMsg    = message.replace("'", "\\'");

        String jsAutoSend =
                "(function() {" +
                "var elements = document.querySelectorAll('p, span, div');" +
                "var foundChat = false;" +
                "for (var i = 0; i < elements.length; i++) {" +
                "  if (elements[i].innerText && elements[i].innerText.trim() === '" + safeTarget + "') {" +
                "    elements[i].click();" +
                "    foundChat = true;" +
                "    break;" +
                "  }" +
                "}" +
                "setTimeout(function() {" +
                "  var editor = document.querySelector('[contenteditable=\"true\"], textarea, input[type=\"text\"]');" +
                "  if (editor) {" +
                "    editor.focus();" +
                "    document.execCommand('insertText', false, '" + safeMsg + "');" +
                "    editor.dispatchEvent(new Event('input', {bubbles: true}));" +
                "    setTimeout(function() {" +
                "      var sendBtn = document.querySelector('[data-e2e=\"message-send\"], button[type=\"submit\"]');" +
                "      if (sendBtn) { sendBtn.click(); }" +
                "      var enterEvt = new KeyboardEvent('keydown', {key: 'Enter', code: 'Enter', keyCode: 13, which: 13, bubbles: true});" +
                "      editor.dispatchEvent(enterEvt);" +
                "      AndroidBridge.onMessageSent('" + safeTarget + "', true);" +
                "    }, 1000);" +
                "  } else {" +
                "    AndroidBridge.onMessageSent('" + safeTarget + "', foundChat);" +
                "  }" +
                "}, 2000);" +
                "})();";

        webView.evaluateJavascript(jsAutoSend, null);
    }

    private void resetTodayLogs() {
        String today = getTodayDateString();
        Set<String> all = prefs.getStringSet(KEY_TARGETS, new HashSet<>());
        SharedPreferences.Editor editor = prefs.edit();
        for (String target : all) editor.remove("sent_" + today + "_" + target);
        editor.apply();
        refreshTargetCheckboxes();
        appendLog("Đã xóa lịch sử gửi ngày hôm nay (" + today + "). Có thể gửi lại.");
    }

    public static String getTodayDateString() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private void appendLog(String text) {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
        mainHandler.post(() -> logConsole.append("[" + time + "] " + text + "\n"));
    }

    private TextView createSectionHeader(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#25F4EE"));
        tv.setTextSize(15);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(0, 28, 0, 10);
        return tv;
    }

    private Button createStyledButton(String text, String bgHex, String textHex) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setBackgroundColor(Color.parseColor(bgHex));
        btn.setTextColor(Color.parseColor(textHex));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 8, 0, 8);
        btn.setLayoutParams(params);
        return btn;
    }

    private class WebAppInterface {
        @JavascriptInterface
        public void onContactsScanned(String jsonNames) {
            mainHandler.post(() -> {
                try {
                    JSONArray arr = new JSONArray(jsonNames);
                    Set<String> all = new HashSet<>(prefs.getStringSet(KEY_TARGETS, new HashSet<>()));
                    int added = 0;
                    for (int i = 0; i < arr.length(); i++) {
                        String name = arr.getString(i).trim();
                        if (!name.isEmpty() && all.add(name)) added++;
                    }
                    prefs.edit().putStringSet(KEY_TARGETS, all).apply();
                    refreshTargetCheckboxes();
                    appendLog("Quét hoàn tất: Phát hiện thêm " + added + " liên hệ từ hộp thư.");
                } catch (Exception e) {
                    appendLog("Lỗi phân tích danh sách: " + e.getMessage());
                }
            });
        }

        @JavascriptInterface
        public void onMessageSent(String targetName, boolean success) {
            mainHandler.post(() -> {
                String today = getTodayDateString();
                prefs.edit().putBoolean("sent_" + today + "_" + targetName, true).apply();
                refreshTargetCheckboxes();
                appendLog("✅ Đã gửi chữ \"" + prefs.getString(KEY_MSG, "chuỗi")
                        + "\" tới [" + targetName + "] (1/1 lần hôm nay).");
            });
        }
    }
}
