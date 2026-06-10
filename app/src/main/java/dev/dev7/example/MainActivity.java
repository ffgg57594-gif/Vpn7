package dev.dev7.example;

import static dev.dev7.lib.v2ray.utils.V2rayConstants.SERVICE_CONNECTION_STATE_BROADCAST_EXTRA;
import static dev.dev7.lib.v2ray.utils.V2rayConstants.SERVICE_DOWNLOAD_SPEED_BROADCAST_EXTRA;
import static dev.dev7.lib.v2ray.utils.V2rayConstants.SERVICE_UPLOAD_SPEED_BROADCAST_EXTRA;
import static dev.dev7.lib.v2ray.utils.V2rayConstants.V2RAY_SERVICE_STATICS_BROADCAST_INTENT;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;

import dev.dev7.lib.v2ray.V2rayController;
import dev.dev7.lib.v2ray.utils.V2rayConstants;

public class MainActivity extends AppCompatActivity {

    private Button btnConnect;
    private EditText etServer;
    private EditText etSni;
    private TextView tvLogs;
    private SharedPreferences sharedPreferences;
    private BroadcastReceiver v2rayBroadCastReceiver;

    @SuppressLint({"SetTextI18n", "UnspecifiedRegisterReceiverFlag"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        if (savedInstanceState == null) {
            V2rayController.init(this, R.drawable.ic_launcher, "My Custom VPN");
        }

        btnConnect = findViewById(R.id.btn_connect);
        etServer = findViewById(R.id.et_server);
        etSni = findViewById(R.id.et_sni);
        tvLogs = findViewById(R.id.tv_logs);

        sharedPreferences = getSharedPreferences("vpn_conf", MODE_PRIVATE);
        etServer.setText(sharedPreferences.getString("saved_server", ""));
        etSni.setText(sharedPreferences.getString("saved_sni", ""));

        btnConnect.setOnClickListener(view -> {
            if (V2rayController.getConnectionState() != V2rayConstants.CONNECTION_STATES.DISCONNECTED) {
                V2rayController.stopV2ray(this);
                tvLogs.append("\n[+] تم طلب قطع الاتصال.");
                return;
            }

            String serverInput = etServer.getText().toString().trim();
            String sniInput = etSni.getText().toString().trim();

            if (serverInput.isEmpty() || sniInput.isEmpty()) {
                Toast.makeText(this, "يرجى ملء الخانات", Toast.LENGTH_SHORT).show();
                return;
            }

            sharedPreferences.edit()
                    .putString("saved_server", serverInput)
                    .putString("saved_sni", sniInput)
                    .apply();

            try {
                String[] parts = serverInput.split("@");
                String[] ipPort = parts[0].split(":");
                String[] namePass = parts[1].split(":");

                String ip = ipPort[0];
                String port = ipPort[1];
                String name = namePass[0];
                String password = namePass[1];

                // تمت إضافة host و path لضمان الاتصال الصحيح وتدفق البيانات
                String vlessUri = "vless://" + password + "@" + ip + ":" + port + 
                                  "?encryption=none&security=tls&sni=" + sniInput + 
                                  "&type=ws&host=" + sniInput + "&path=%2F#" + name;

                tvLogs.setText("=== بدء عملية الاتصال ===\n");
                tvLogs.append("-> IP: " + ip + "\n");
                tvLogs.append("-> Port: " + port + "\n");
                tvLogs.append("-> SNI: " + sniInput + "\n");
                tvLogs.append("\n[+] جاري الاتصال بالمحرك...\n");

                V2rayController.startV2ray(this, "My Custom VPN", vlessUri, null);

            } catch (Exception e) {
                tvLogs.setText("❌ خطأ في تفكيك البيانات! تأكد من استخدام الصيغة:\nIp:port@name:password");
            }
        });

        updateButtonState(V2rayController.getConnectionState());

        v2rayBroadCastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                runOnUiThread(() -> {
                    V2rayConstants.CONNECTION_STATES state = (V2rayConstants.CONNECTION_STATES) Objects.requireNonNull(intent.getExtras().getSerializable(SERVICE_CONNECTION_STATE_BROADCAST_EXTRA));
                    updateButtonState(state);

                    // طباعة السرعة في السجل للتأكد من سحب البيانات
                    if (state == V2rayConstants.CONNECTION_STATES.CONNECTED) {
                        String up = intent.getExtras().getString(SERVICE_UPLOAD_SPEED_BROADCAST_EXTRA);
                        String down = intent.getExtras().getString(SERVICE_DOWNLOAD_SPEED_BROADCAST_EXTRA);
                        tvLogs.setText("الحالة: متصل ✅\n↑ رفع: " + up + " | ↓ تحميل: " + down);
                    }
                });
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(v2rayBroadCastReceiver, new IntentFilter(V2RAY_SERVICE_STATICS_BROADCAST_INTENT), Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(v2rayBroadCastReceiver, new IntentFilter(V2RAY_SERVICE_STATICS_BROADCAST_INTENT));
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateButtonState(V2rayConstants.CONNECTION_STATES state) {
        switch (state) {
            case CONNECTED:
                btnConnect.setText("قطع الاتصال");
                btnConnect.setBackgroundColor(0xFFFF0000); // لون أحمر عند الاتصال
                break;
            case DISCONNECTED:
                btnConnect.setText("اتصال");
                btnConnect.setBackgroundColor(0xFF007BFF); // أزرق
                break;
            case CONNECTING:
                btnConnect.setText("جاري الاتصال...");
                btnConnect.setBackgroundColor(0xFFFFA500); // برتقالي
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (v2rayBroadCastReceiver != null) {
            unregisterReceiver(v2rayBroadCastReceiver);
        }
    }
}
