package dev.dev7.example;

import static dev.dev7.lib.v2ray.utils.V2rayConstants.SERVICE_CONNECTION_STATE_BROADCAST_EXTRA;
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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;

import dev.dev7.lib.v2ray.V2rayController;
import dev.dev7.lib.v2ray.utils.V2rayConstants;

public class MainActivity extends AppCompatActivity {

    private Button btnConnect;
    private EditText etServer;
    private EditText etSni;
    private SharedPreferences sharedPreferences;
    private BroadcastReceiver v2rayBroadCastReceiver;

    @SuppressLint({"SetTextI18n", "UnspecifiedRegisterReceiverFlag"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // تهيئة محرك V2ray
        if (savedInstanceState == null) {
            V2rayController.init(this, R.drawable.ic_launcher, "My Custom VPN");
        }

        // ربط العناصر بالواجهة
        btnConnect = findViewById(R.id.btn_connect);
        etServer = findViewById(R.id.et_server);
        etSni = findViewById(R.id.et_sni);

        // تهيئة الذاكرة لحفظ السيرفر والـ SNI
        sharedPreferences = getSharedPreferences("vpn_conf", MODE_PRIVATE);
        etServer.setText(sharedPreferences.getString("saved_server", ""));
        etSni.setText(sharedPreferences.getString("saved_sni", ""));

        // برمجة زر الاتصال
        btnConnect.setOnClickListener(view -> {
            
            // إذا كان متصلاً بالفعل، قم بقطع الاتصال
            if (V2rayController.getConnectionState() != V2rayConstants.CONNECTION_STATES.DISCONNECTED) {
                V2rayController.stopV2ray(this);
                return;
            }

            String serverInput = etServer.getText().toString().trim();
            String sniInput = etSni.getText().toString().trim();

            if (serverInput.isEmpty() || sniInput.isEmpty()) {
                Toast.makeText(this, "يرجى ملء جميع الخانات", Toast.LENGTH_SHORT).show();
                return;
            }

            // حفظ البيانات لعدم كتابتها مرة أخرى
            sharedPreferences.edit()
                    .putString("saved_server", serverInput)
                    .putString("saved_sni", sniInput)
                    .apply();

            try {
                // تفكيك صيغتك: Ip:port@name:password
                String[] parts = serverInput.split("@");
                String[] ipPort = parts[0].split(":");
                String[] namePass = parts[1].split(":");

                String ip = ipPort[0];
                String port = ipPort[1];
                String name = namePass[0];
                String password = namePass[1]; // هذا هو الـ UUID

                // دمج البيانات لصناعة رابط Vless
                String vlessUri = "vless://" + password + "@" + ip + ":" + port + 
                                  "?encryption=none&security=tls&sni=" + sniInput + 
                                  "&type=ws#" + name;

                // إرسال الرابط لمحرك الـ VPN لبدء الاتصال
                V2rayController.startV2ray(this, "My Custom VPN", vlessUri, null);

            } catch (Exception e) {
                Toast.makeText(this, "خطأ في الصيغة! استخدم: Ip:port@name:password", Toast.LENGTH_LONG).show();
            }
        });

        // التحقق من حالة الاتصال عند فتح التطبيق
        updateButtonState(V2rayController.getConnectionState());

        // استقبال تحديثات حالة الاتصال من المحرك في الخلفية
        v2rayBroadCastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                runOnUiThread(() -> {
                    V2rayConstants.CONNECTION_STATES state = (V2rayConstants.CONNECTION_STATES) Objects.requireNonNull(intent.getExtras().getSerializable(SERVICE_CONNECTION_STATE_BROADCAST_EXTRA));
                    updateButtonState(state);
                });
            }
        };

        // تسجيل المستقبل (Receiver) بناءً على إصدار الأندرويد
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(v2rayBroadCastReceiver, new IntentFilter(V2RAY_SERVICE_STATICS_BROADCAST_INTENT), RECEIVER_EXPORTED);
        } else {
            registerReceiver(v2rayBroadCastReceiver, new IntentFilter(V2RAY_SERVICE_STATICS_BROADCAST_INTENT));
        }
    }

    // دالة لتحديث شكل الزر بناءً على حالة الاتصال
    @SuppressLint("SetTextI18n")
    private void updateButtonState(V2rayConstants.CONNECTION_STATES state) {
        switch (state) {
            case CONNECTED:
                btnConnect.setText("متصل (اضغط لقطع الاتصال)");
                break;
            case DISCONNECTED:
                btnConnect.setText("اتصال");
                break;
            case CONNECTING:
                btnConnect.setText("جاري الاتصال...");
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
