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

package com.mokee.verifier;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class RootShell {

    private static SU su;

    public static boolean requestRootAccess() {
        SU su = getSU();
        su.runCommand("echo /testRoot/");
        return !su.denied;
    }

    public static void runCommand(String command) {
        getSU().runCommand(command);
    }

    private static SU getSU() {
        if (su == null) su = new SU();
        else if (su.closed || su.denied) su = new SU();
        return su;
    }

    private static class SU {

        private Process process;
        private BufferedWriter bufferedWriter;
        private boolean closed;
        private boolean denied;
        private boolean firstTry;

        SU() {
            try {
                firstTry = true;
                process = Runtime.getRuntime().exec("su");
                bufferedWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            } catch (IOException e) {
                denied = true;
                closed = true;
            }
        }

        synchronized void runCommand(final String command) {
            try {
                bufferedWriter.write(command + "\n");
                bufferedWriter.flush();
                firstTry = false;
            } catch (IOException e) {
                closed = true;
                e.printStackTrace();
                if (firstTry) denied = true;
            } catch (ArrayIndexOutOfBoundsException e) {
                denied = true;
            } catch (Exception e) {
                e.printStackTrace();
                denied = true;
            }
        }
    }

}
