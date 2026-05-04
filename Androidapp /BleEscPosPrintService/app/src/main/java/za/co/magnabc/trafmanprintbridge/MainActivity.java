package za.co.magnabc.trafmanprintbridge;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {
    public static final String ACTION_PRINT = "za.co.magnabc.trafmanprintbridge.PRINT";
    public static final String LEGACY_ACTION_PRINT = "com.example.bleescposprintservice.PRINT";

    private static final int REQUEST_BLUETOOTH_CONNECT = 21;
    private static final String PREFS = "trafman_print_bridge";
    private static final String PREF_BT_ADDRESS = "bluetooth_address";
    private static final String MODE_BLUETOOTH = "BLUETOOTH";
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final Charset PRINTER_CHARSET = Charset.forName("CP437");

    private final List<BluetoothDevice> pairedDevices = new ArrayList<>();

    private ArrayAdapter<String> bluetoothAdapter;
    private Spinner bluetoothSpinner;
    private TextView statusText;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        bluetoothSpinner = findViewById(R.id.bluetoothSpinner);
        statusText = findViewById(R.id.txtStatus);

        bluetoothAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        bluetoothAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bluetoothSpinner.setAdapter(bluetoothAdapter);

        bind(R.id.btnRefreshBluetooth, v -> refreshPairedPrinters());
        bind(R.id.btnSaveSettings, v -> saveSettings());
        bind(R.id.btnPlainText, v -> printBytes(buildPlainTextTest(), selectedTargetFromUi(), "plain text test"));
        bind(R.id.btnReceipt, v -> printBytes(buildReceipt(false), selectedTargetFromUi(), "ESC/POS receipt test"));
        bind(R.id.btnQr, v -> printBytes(buildQrTest(), selectedTargetFromUi(), "QR code test"));
        bind(R.id.btnFeedCut, v -> printBytes(buildReceipt(true), selectedTargetFromUi(), "feed/cut test"));
        bind(R.id.btnSampleUri, v -> showSampleUri());
        bind(R.id.btnLogs, v -> startActivity(new Intent(this, LogActivity.class)));

        requestBluetoothPermissionIfNeeded();
        refreshPairedPrinters();
        handleIncomingIntent(getIntent());
        announce("Ready. Select the paired Bluetooth printer, save settings, then print a test receipt.");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    private void bind(int viewId, View.OnClickListener listener) {
        Button button = findViewById(viewId);
        button.setOnClickListener(listener);
    }

    private void saveSettings() {
        BluetoothDevice selected = selectedBluetoothDevice();
        if (selected == null) {
            announce("No Bluetooth printer selected.");
            return;
        }

        prefs.edit().putString(PREF_BT_ADDRESS, selected.getAddress()).apply();
        announce("Printer settings saved.");
    }

    private PrintTarget selectedTargetFromUi() {
        saveSettings();
        return selectedBluetoothTarget();
    }

    private void requestBluetoothPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_CONNECT);
        }
    }

    private boolean hasBluetoothPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    private void refreshPairedPrinters() {
        pairedDevices.clear();
        bluetoothAdapter.clear();

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            bluetoothAdapter.add("Bluetooth not available on this device");
            announce("Bluetooth not available on this Android device.");
            return;
        }

        if (!adapter.isEnabled()) {
            bluetoothAdapter.add("Bluetooth is disabled");
            announce("Bluetooth is disabled. Enable it in Android settings.");
            return;
        }

        if (!hasBluetoothPermission()) {
            bluetoothAdapter.add("Bluetooth permission missing");
            requestBluetoothPermissionIfNeeded();
            announce("Bluetooth permission missing.");
            return;
        }

        Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();
        if (bondedDevices == null || bondedDevices.isEmpty()) {
            bluetoothAdapter.add("No paired Bluetooth printers");
            announce("No paired Bluetooth devices. Pair the printer in Android Bluetooth settings first.");
            return;
        }

        String savedAddress = prefs.getString(PREF_BT_ADDRESS, null);
        int selectedIndex = 0;
        for (BluetoothDevice device : bondedDevices) {
            pairedDevices.add(device);
            String name = safeDeviceName(device);
            bluetoothAdapter.add(name + " (" + device.getAddress() + ")");
            if (device.getAddress().equals(savedAddress)) {
                selectedIndex = pairedDevices.size() - 1;
            }
        }
        bluetoothSpinner.setSelection(selectedIndex);
        log("Found " + pairedDevices.size() + " paired Bluetooth device(s).");
    }

    private BluetoothDevice selectedBluetoothDevice() {
        int index = bluetoothSpinner.getSelectedItemPosition();
        if (index < 0 || index >= pairedDevices.size()) {
            return null;
        }
        return pairedDevices.get(index);
    }

    private PrintTarget selectedBluetoothTarget() {
        return PrintTarget.bluetooth(prefs.getString(PREF_BT_ADDRESS, null));
    }

    private String safeDeviceName(BluetoothDevice device) {
        if (!hasBluetoothPermission()) {
            return "Paired device";
        }
        String name = device.getName();
        return name == null || name.trim().isEmpty() ? "Paired device" : name;
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        try {
            String action = intent.getAction();
            if (Intent.ACTION_SEND.equals(action)) {
                CharSequence shared = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
                if (shared != null) {
                    PrintJob job = PrintJob.text(shared.toString());
                    printJob(job);
                }
                return;
            }

            if (Intent.ACTION_VIEW.equals(action) && intent.getData() != null) {
                PrintJob job = printJobFromUri(intent.getData());
                if (job != null) {
                    printJob(job);
                }
                return;
            }

            if (ACTION_PRINT.equals(action) || LEGACY_ACTION_PRINT.equals(action)) {
                PrintJob job = printJobFromExtras(intent);
                if (job != null) {
                    printJob(job);
                }
            }
        } catch (Exception e) {
            announce("Failed to read incoming print job: " + e.getMessage());
        }
    }

    private PrintJob printJobFromUri(Uri uri) throws Exception {
        if (!"trafmanprint".equalsIgnoreCase(uri.getScheme())) {
            return null;
        }

        String json = uri.getQueryParameter("job");
        if (json == null) {
            json = uri.getQueryParameter("json");
        }
        if (json != null && json.trim().startsWith("{")) {
            return PrintJob.fromJson(new JSONObject(json));
        }

        String payload = uri.getQueryParameter("payload");
        String payloadType = uri.getQueryParameter("payloadType");
        String printerMode = uri.getQueryParameter("printerMode");
        String documentType = uri.getQueryParameter("documentType");
        String documentNumber = uri.getQueryParameter("documentNumber");

        if (payload != null) {
            return PrintJob.fromPayload(payloadType, payload, printerMode, documentType, documentNumber);
        }

        String text = uri.getQueryParameter("text");
        if (text != null) {
            PrintJob job = PrintJob.text(text);
            job.printerMode = printerMode;
            job.documentType = documentType;
            job.documentNumber = documentNumber;
            return job;
        }
        return null;
    }

    private PrintJob printJobFromExtras(Intent intent) throws Exception {
        String jobJson = intent.getStringExtra("jobJson");
        if (jobJson == null) {
            jobJson = intent.getStringExtra("job");
        }
        if (jobJson != null) {
            return PrintJob.fromJson(new JSONObject(jobJson));
        }

        String payload = intent.getStringExtra("payload");
        String payloadType = intent.getStringExtra("payloadType");
        String printerMode = intent.getStringExtra("printerMode");
        String text = intent.getStringExtra("text");

        if (payload != null) {
            return PrintJob.fromPayload(payloadType, payload, printerMode,
                    intent.getStringExtra("documentType"),
                    intent.getStringExtra("documentNumber"));
        }
        if (text != null) {
            PrintJob job = PrintJob.text(text);
            job.printerMode = printerMode;
            return job;
        }
        return null;
    }

    private void printJob(PrintJob job) {
        byte[] bytes;
        if ("ESC_POS_BASE64".equalsIgnoreCase(job.payloadType)) {
            bytes = decodeBase64Payload(job.payload);
        } else if ("TEXT".equalsIgnoreCase(job.payloadType)) {
            bytes = buildText(safe(job.payload, ""));
        } else {
            bytes = buildStructuredJobReceipt(job);
        }

        if (job.printerMode != null && !job.printerMode.trim().isEmpty()
                && !MODE_BLUETOOTH.equalsIgnoreCase(job.printerMode)) {
            log("Ignoring unsupported printerMode=" + job.printerMode + ". This bridge is Bluetooth only.");
        }
        int copies = Math.max(1, job.copies);
        printBytes(bytes, selectedBluetoothTarget(), copies,
                "Trafman " + safe(job.documentType, "print job") + " " + safe(job.documentNumber, ""));
    }

    private byte[] decodeBase64Payload(String payload) {
        try {
            return Base64.decode(payload, Base64.DEFAULT);
        } catch (IllegalArgumentException ignored) {
            return Base64.decode(payload, Base64.URL_SAFE);
        }
    }

    private void printBytes(byte[] bytes, PrintTarget target, String description) {
        printBytes(bytes, target, 1, description);
    }

    private void printBytes(byte[] bytes, PrintTarget target, int copies, String description) {
        if (target == null) {
            announce("Printer target is not configured.");
            return;
        }

        announce("Printing " + description + " via Bluetooth...");
        new Thread(() -> {
            try {
                for (int i = 0; i < copies; i++) {
                    sendBluetooth(bytes, target.bluetoothAddress);
                }
                runOnUiThread(() -> announce("Print successful: " + description + "."));
            } catch (Exception e) {
                runOnUiThread(() -> announce("Print failed: " + e.getMessage()));
            }
        }).start();
    }

    private void sendBluetooth(byte[] bytes, String address) throws IOException {
        if (address == null || address.trim().isEmpty()) {
            throw new IOException("Bluetooth printer not selected.");
        }
        if (!hasBluetoothPermission()) {
            throw new IOException("Bluetooth permission missing.");
        }

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            throw new IOException("Bluetooth not available.");
        }
        if (!adapter.isEnabled()) {
            throw new IOException("Bluetooth is disabled.");
        }

        BluetoothDevice device = adapter.getRemoteDevice(address);

        try (BluetoothSocket socket = device.createRfcommSocketToServiceRecord(SPP_UUID)) {
            socket.connect();
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(bytes);
            outputStream.flush();
        }
    }

    private byte[] buildPlainTextTest() {
        return buildText("Trafman Print Bridge\nPlain text ESC/POS test\n"
                + "Printer: Xprinter XP-P323B\n"
                + "Date: " + now() + "\n");
    }

    private byte[] buildText(String text) {
        EscPosBuilder b = new EscPosBuilder();
        b.init();
        b.text(text);
        b.text("\n");
        b.feed(3);
        return b.toByteArray();
    }

    private byte[] buildReceipt(boolean feedAndCut) {
        EscPosBuilder b = new EscPosBuilder();
        b.init();
        b.align(1);
        b.bold(true);
        b.size(1);
        b.text("TRAFMAN PRINT BRIDGE\n");
        b.size(0);
        b.bold(false);
        b.text("Free ESC/POS Android bridge\n");
        b.text("Xprinter XP-P323B / 203 DPI\n");
        b.text("--------------------------------\n");
        b.align(0);
        b.text("Document    TEST-RECEIPT\n");
        b.text("Mode        BLUETOOTH SPP\n");
        b.text("Date        " + now() + "\n");
        b.text("--------------------------------\n");
        b.text("Plain text               OK\n");
        b.text("ESC/POS commands         OK\n");
        b.text("Trafman bridge input     OK\n");
        b.text("--------------------------------\n");
        b.align(1);
        b.text("No RawBT required\n");
        b.feed(3);
        if (feedAndCut) {
            b.cut();
        }
        return b.toByteArray();
    }

    private byte[] buildQrTest() {
        EscPosBuilder b = new EscPosBuilder();
        b.init();
        b.align(1);
        b.bold(true);
        b.text("TRAFMAN QR TEST\n");
        b.bold(false);
        b.feed(1);
        b.qr("TRAFMAN PRINT BRIDGE OK " + now());
        b.feed(1);
        b.text("QR support depends on printer\n");
        b.text("ESC/POS mode/firmware.\n");
        b.feed(3);
        return b.toByteArray();
    }

    private byte[] buildStructuredJobReceipt(PrintJob job) {
        EscPosBuilder b = new EscPosBuilder();
        b.init();
        b.align(1);
        b.bold(true);
        b.text("TRAFMAN PRINT JOB\n");
        b.bold(false);
        b.text("--------------------------------\n");
        b.align(0);
        b.text("Source      " + safe(job.sourceSystem, "TRAFMAN") + "\n");
        b.text("Type        " + safe(job.documentType, "UNKNOWN") + "\n");
        b.text("Number      " + safe(job.documentNumber, "-") + "\n");
        b.text("Date        " + now() + "\n");
        b.text("--------------------------------\n");
        if (job.lines.isEmpty()) {
            b.text("Structured JSON accepted.\n");
            b.text("Add payloadType TEXT or\n");
            b.text("ESC_POS_BASE64 for full output.\n");
        } else {
            for (String line : job.lines) {
                b.text(line + "\n");
            }
        }
        b.text("--------------------------------\n");
        b.feed(3);
        return b.toByteArray();
    }

    private void showSampleUri() {
        String payload = Base64.encodeToString(buildText("Trafman Chrome bridge sample\n" + now()),
                Base64.NO_WRAP | Base64.URL_SAFE);
        String sample = "intent://print?payloadType=ESC_POS_BASE64&printerMode=BLUETOOTH"
                + "&documentType=NOTICE_RECEIPT&documentNumber=ABC123456&payload=" + payload
                + "#Intent;scheme=trafmanprint;package=za.co.magnabc.trafmanprintbridge;end";
        announce(sample);
    }

    private String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private void announce(String message) {
        statusText.setText(message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        log(message);
    }

    private void log(String message) {
        LogStore.append(this, message);
    }

    private static class PrintTarget {
        final String bluetoothAddress;

        private PrintTarget(String bluetoothAddress) {
            this.bluetoothAddress = bluetoothAddress;
        }

        static PrintTarget bluetooth(String address) {
            return new PrintTarget(address);
        }
    }

    private static class PrintJob {
        String sourceSystem = "TRAFMAN";
        String documentType;
        String documentNumber;
        String printerMode;
        int copies = 1;
        String payloadType = "STRUCTURED_JSON";
        String payload;
        final List<String> lines = new ArrayList<>();

        static PrintJob text(String text) {
            PrintJob job = new PrintJob();
            job.payloadType = "TEXT";
            job.payload = text;
            return job;
        }

        static PrintJob fromPayload(String payloadType, String payload, String printerMode,
                                    String documentType, String documentNumber) {
            PrintJob job = new PrintJob();
            job.payloadType = payloadType == null ? "ESC_POS_BASE64" : payloadType;
            job.payload = payload;
            job.printerMode = printerMode;
            job.documentType = documentType;
            job.documentNumber = documentNumber;
            return job;
        }

        static PrintJob fromJson(JSONObject json) throws Exception {
            PrintJob job = new PrintJob();
            job.sourceSystem = json.optString("sourceSystem", "TRAFMAN");
            job.documentType = json.optString("documentType", null);
            job.documentNumber = json.optString("documentNumber", null);
            job.printerMode = json.optString("printerMode", null);
            job.copies = json.optInt("copies", 1);
            job.payloadType = json.optString("payloadType", "STRUCTURED_JSON");
            job.payload = json.optString("payload", null);
            JSONArray lines = json.optJSONArray("lines");
            if (lines != null) {
                for (int i = 0; i < lines.length(); i++) {
                    job.lines.add(lines.getString(i));
                }
            }
            return job;
        }
    }

    private static class EscPosBuilder {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        void init() {
            bytes(0x1B, 0x40);
        }

        void align(int align) {
            bytes(0x1B, 0x61, align);
        }

        void bold(boolean enabled) {
            bytes(0x1B, 0x45, enabled ? 1 : 0);
        }

        void size(int size) {
            bytes(0x1D, 0x21, size == 0 ? 0x00 : 0x11);
        }

        void text(String value) {
            byte[] data = value.getBytes(PRINTER_CHARSET);
            out.write(data, 0, data.length);
        }

        void feed(int lines) {
            bytes(0x1B, 0x64, lines);
        }

        void cut() {
            bytes(0x1D, 0x56, 0x00);
        }

        void qr(String value) {
            byte[] data = value.getBytes(PRINTER_CHARSET);
            int storeLength = data.length + 3;
            int pL = storeLength % 256;
            int pH = storeLength / 256;

            bytes(0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00);
            bytes(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, 0x06);
            bytes(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x31);
            bytes(0x1D, 0x28, 0x6B, pL, pH, 0x31, 0x50, 0x30);
            out.write(data, 0, data.length);
            bytes(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30);
        }

        byte[] toByteArray() {
            return out.toByteArray();
        }

        private void bytes(int... values) {
            for (int value : values) {
                out.write(value);
            }
        }
    }

    public static class LogStore {
        private static final String LOG_KEY = "logs";
        private static final int MAX_LOG_CHARS = 30000;

        static void append(Context context, String message) {
            SharedPreferences preferences = context.getSharedPreferences(PREFS, MODE_PRIVATE);
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
            String existing = preferences.getString(LOG_KEY, "");
            String updated = existing + timestamp + "  " + message + "\n";
            if (updated.length() > MAX_LOG_CHARS) {
                updated = updated.substring(updated.length() - MAX_LOG_CHARS);
            }
            preferences.edit().putString(LOG_KEY, updated).apply();
        }

        static String read(Context context) {
            return context.getSharedPreferences(PREFS, MODE_PRIVATE).getString(LOG_KEY, "");
        }

        static void clear(Context context) {
            context.getSharedPreferences(PREFS, MODE_PRIVATE).edit().remove(LOG_KEY).apply();
        }
    }
}
