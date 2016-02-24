/*
    Written by the Cordova team for cordova-plugin-camera,
    and modified by Elizabeth Rego.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
*/
package com.wodify.cordova.plugin.filepicker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

/**
 * This class launches the camera view, allows the user to take a picture, closes the camera view,
 * and returns the captured image.  When the camera view is closed, the screen displayed before
 * the camera view was shown is redisplayed.
 */
public class FilePicker extends CordovaPlugin {

    private static boolean returnFileWithDetails = false;

    public CallbackContext callbackContext;

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action            The action to execute.
     * @param args              JSONArry of arguments for the plugin.
     * @param callbackContext   The callback id used when calling back into JavaScript.
     * @return                  A PluginResult object with a status and message.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;

        if (action.equals("pickFile")) {
            if (args.length() > 1) {
                returnFileWithDetails = args.getBoolean(1);
            }

            this.pickFile();

            PluginResult r = new PluginResult(PluginResult.Status.NO_RESULT);
            r.setKeepCallback(true);
            callbackContext.sendPluginResult(r);

            return true;
        } else if (action.equals("deviceSupported")) {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, true));
        }
        return false;
    }

    //--------------------------------------------------------------------------
    // LOCAL METHODS
    //--------------------------------------------------------------------------

    /**
     * Pick file from device.
     */
    public void pickFile() {
        Intent intent = new Intent();

        intent.setType("*/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        if (this.cordova != null) {
            this.cordova.startActivityForResult((CordovaPlugin) this, Intent.createChooser(intent, "Pick File"), 0);
        }
    }

    /**
     * Applies all needed transformation to the image received from the gallery.
     *
     * @param destType          In which form should we return the image
     * @param intent            An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
     */
    private void processResult(Intent intent) {
        Uri uri = intent.getData();
        if (uri == null) {
            this.failFile("null data from photo library");
            return;
        }

        String fileLocation = FileHelper.getRealPath(uri, this.cordova);
        Log.v("chromium", "LOCATION: " + fileLocation);
        Log.v("chromium", "URI LOCATION: " + uri.getPath().toString());

        if (fileLocation == null || fileLocation.length() == 0) {
            fileLocation = FileHelper.getRealPathFromExternal(uri, this.cordova.getActivity());
            Log.v("chromium", "NEW LOCATION: " + fileLocation);
        }

        // If you ask for video or all media type you will automatically get back a file URI
        // and there will be no attempt to resize any returned data
        if (returnFileWithDetails) {
            JSONArray details = this.getFileDetails(fileLocation);

            if (details != null && details.length() == 3) {
                this.callbackContext.success(details);
            } else {
                this.failFile("Error parsing file from URI.");
            }
        }
        else {
           this.callbackContext.success(fileLocation);
        }
    }

    private JSONArray getFileDetails(String path) {
        File file;

        file = new File(path);
        Log.v("chromium", path);
        /*try {
            file = new File(uri.getPath());
        } catch (URISyntaxException e) {
            file = null;
        } catch (IllegalArgumentException e) {
            e.printStackTrace;
            file = null;
        }*/

        if (file != null) {
            Log.v("chromium", "File not null");
            byte[] bytesOfFile;

            try {
                bytesOfFile = loadFile(file);
            } catch (IOException e) {
                bytesOfFile = null;
            }

            if (bytesOfFile != null && bytesOfFile.length > 0) {
                Log.v("chromium", "Bytes not null");
                String[] details = new String[3];

                Log.v("chromium", "encoding once...");
                byte[] base64EncodedFile = Base64.encode(bytesOfFile, Base64.NO_WRAP);
                Log.v("chromium", "encoding twice...");
                details[0] = new String(base64EncodedFile);
                Log.v("chromium", "ENCODED LENGTH: " + details[0].length());

                String name = file.getName();

                int pos = name.lastIndexOf(".");
                if (pos > 0) {
                    details[1] = name.substring(0, pos);
                    details[2] = name.substring(pos + 1);
                } else {
                    details[1] = name;
                    details[2] = "";
                }

                try {
                    return new JSONArray(details);
                } catch (JSONException e) {}
            }
        }
        return null;
    }

    /**
     * Returns file in byte array.
     * See: https://gist.github.com/utkarsh2012/1276960
     * @param  file        File to convert to byte array.
     */
    private static byte[] loadFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);

        long length = file.length();
        
        if (length > Integer.MAX_VALUE) {
            return null;
        }

        byte[] bytes = new byte[(int)length];
        
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length
               && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
            offset += numRead;
        }

        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }

        is.close();
        return bytes;
    }

    /**
     * Called when the camera view exits.
     *
     * @param requestCode       The request code originally supplied to startActivityForResult(),
     *                          allowing you to identify who this result came from.
     * @param resultCode        The integer result code returned by the child activity through its setResult().
     * @param intent            An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
     */
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == Activity.RESULT_OK && intent != null) {
            final Intent i = intent;
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    processResult(i);
                }
            });
        }
        else if (resultCode == Activity.RESULT_CANCELED) {
            this.failFile("Selection cancelled.");
        }
        else {
            this.failFile("Selection did not complete!");
        }
    }

    /**
     * Send error message to JavaScript.
     *
     * @param err
     */
    public void failFile(String err) {
        this.callbackContext.error(err);
    }
}