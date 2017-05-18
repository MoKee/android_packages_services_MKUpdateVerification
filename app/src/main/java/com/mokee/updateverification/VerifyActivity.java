/*
 * Copyright (C) 2017 The MoKee Open Source Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mokee.updateverification;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class VerifyActivity extends Activity {

    private ProgressDialog progressDialog;
    private Handler UiHandler;
    private Intent mIntent;

    private static int STATUS_SUCCESSFUL = 8000;
    private static int STATUS_DENIED = 8001;

    private static final String CHECK_LOG_FILE = "/cache/recovery/check_log";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!RootShell.requestRootAccess()) {
            setResult(STATUS_DENIED);
            finish();
        } else {
            mIntent = getIntent();
            Bundle mBundle = mIntent.getExtras();
            if (mBundle == null) finish();

            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
            progressDialog.setTitle(getText(R.string.verify_update_compatible_title));
            progressDialog.setMessage(getText(R.string.verify_update_compatible_message));
            progressDialog.show();

            UiHandler = new Handler();
            final String updatePackagePath = mBundle.getString("update_package_path");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String command = "mkchecker " + updatePackagePath + " " + CHECK_LOG_FILE;
                    RootShell.runCommand(command);
                    UiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            setResult(STATUS_SUCCESSFUL, mIntent);
                            finish();
                        }
                    });
                }
            }).start();
        }
    }

}
