package com.sonic.digiledger;

import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                dispatchBackToWeb();
            }
        });
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        dispatchBackToWeb();
    }

    private void dispatchBackToWeb() {
        if (getBridge() != null && getBridge().getWebView() != null) {
            getBridge().getWebView().evaluateJavascript(
                    "(function(){window.__digiledgerBackResult='handled';"
                            + "window.dispatchEvent(new Event('digiledger-backbutton'));"
                            + "return window.__digiledgerBackResult;})()",
                    result -> {
                        if ("\"exit\"".equals(result)) {
                            finishAndRemoveTask();
                        }
                    });
        }
    }
}
