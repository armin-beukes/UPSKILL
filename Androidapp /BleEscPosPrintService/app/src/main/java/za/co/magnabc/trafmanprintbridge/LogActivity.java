package za.co.magnabc.trafmanprintbridge;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class LogActivity extends Activity {
    private TextView logsText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);

        logsText = findViewById(R.id.logsText);
        Button clearLogs = findViewById(R.id.btnClearLogs);
        clearLogs.setOnClickListener(v -> {
            MainActivity.LogStore.clear(this);
            loadLogs();
        });
        loadLogs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLogs();
    }

    private void loadLogs() {
        String logs = MainActivity.LogStore.read(this);
        logsText.setText(logs.trim().isEmpty() ? "No logs yet." : logs);
    }
}
