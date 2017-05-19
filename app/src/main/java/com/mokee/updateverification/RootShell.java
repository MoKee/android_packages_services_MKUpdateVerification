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

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import eu.chainfire.libsuperuser.Shell;

@SuppressWarnings("WeakerAccess")
public class RootShell {
    private static final RootShell _instance = new RootShell();
    private Boolean _hasRoot = null;
    private Shell.Interactive _rootSession;

    private RootShell() {
    }

    @AnyThread
    public static RootShell getInstance() {
        return _instance;
    }

    /**
     * @return true iff you currently have root privilege and can perform root operations using this class
     */
    @AnyThread
    public boolean hasRoot() {
        return _hasRoot != null && _hasRoot && _rootSession != null && _rootSession.isRunning();
    }

    /**
     * tries to gain root privilege.
     *
     * @return true iff got root
     */
    @WorkerThread
    public synchronized boolean getRoot() {
        if (_hasRoot != null && _hasRoot && _rootSession.isRunning())
            return true;
        final Handler handler = new Handler(Looper.getMainLooper());
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final AtomicBoolean gotRoot = new AtomicBoolean();
        handler.post(new Runnable() {
            @Override
            public void run() {
                getRoot(new IGotRootListener() {
                    @Override
                    public void onGotRootResult(final boolean hasRoot) {
                        gotRoot.set(hasRoot);
                        countDownLatch.countDown();
                    }
                });
            }
        });
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return gotRoot.get();
    }

    /**
     * tries to gain root privilege. Will call the listener when it's done
     */
    @UiThread
    public void getRoot(@NonNull final IGotRootListener listener) {
        if (hasRoot()) {
            listener.onGotRootResult(true);
            return;
        }
        final AtomicReference<Shell.Interactive> rootSessionRef = new AtomicReference<>();
        rootSessionRef.set(new Shell.Builder().useSU().setWantSTDERR(true).setWatchdogTimeout(5).setMinimalLogging(true).open(//
                new Shell.OnCommandResultListener() {
                    @Override
                    public void onCommandResult(final int commandCode, final int exitCode, final List<String> output) {
                        final boolean success = exitCode == Shell.OnCommandResultListener.SHELL_RUNNING;
                        if (success)
                            _rootSession = rootSessionRef.get();
                        _hasRoot = success;
                        listener.onGotRootResult(success);
                    }
                }));
    }

    /**
     * perform root operations.
     *
     * @return null if error or root not gained. Otherwise, a list of the strings that are the output of the commands
     */
    @WorkerThread
    @Nullable
    public synchronized List<String> runCommands(@NonNull final List<String> commands) {
        if (commands == null)
            return null;
        return runCommands(commands.toArray(new String[commands.size()]));
    }

    /**
     * perform root operations.
     *
     * @return null if error or root not gained. Otherwise, a list of the strings that are the output of the commands
     */
    @WorkerThread
    @Nullable
    public synchronized List<String> runCommands(@NonNull final String... commands) {
        if (commands == null || commands.length == 0 || !hasRoot())
            return null;
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final AtomicReference<List<String>> resultRef = new AtomicReference<>();
        _rootSession.addCommand(commands, 0, new Shell.OnCommandResultListener() {
            @Override
            public void onCommandResult(final int commandCode, final int exitCode, final List<String> output) {
                resultRef.set(output);
                countDownLatch.countDown();
                _rootSession.close();
            }
        });
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        @SuppressWarnings("unchecked")
        final List<String> result = resultRef.get();
        return result;
    }

    public interface IGotRootListener {
        /**
         * called when we know if you got root or not
         *
         * @param hasRoot true iff you got root.
         */
        void onGotRootResult(boolean hasRoot);
    }
}
