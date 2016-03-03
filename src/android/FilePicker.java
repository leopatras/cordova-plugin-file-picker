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
import java.lang.NullPointerException;
import java.io.FileNotFoundException;
import org.apache.commons.io.IOUtils;

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
import android.provider.OpenableColumns;
import android.database.Cursor;

/**
 * This plugin allows a user to pick a file from their device.
 */
public class FilePicker extends CordovaPlugin {

    // Static flag to determine whether to return
    // verbose details of file instead of its Uri
    private static boolean returnFileWithDetails = false;

    // Reference to callbackContext so result can be
    // sent outside of its initial scope
    public CallbackContext callbackContext;

    /**
     * Executes the request sent from JavaScript.
     *
     * @param action
     *      The action to execute.
     * @param args
     *      The exec() arguments in JSON form.
     * @param command
     *      The callback context used when calling back into JavaScript.
     * @return
     *      Whether the action was valid.
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
            callbackContext.success("true");
        }
        return false;
    }

    /**
     * Pick file from device.
     */
    public void pickFile() {
        Intent intent = new Intent();

        // Any type of file may be picked
        intent.setType("*/*");

        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        if (this.cordova != null) {
            this.cordova.startActivityForResult((CordovaPlugin) this, Intent.createChooser(intent, "Pick File"), 0);
        }
    }

    /**
     * Gets and returns data about the picked file.
     *
     * @param intent
     *        An Intent, which can return result data to the caller (various 
     *        data can be attached to Intent "extras").
     */
    private void processResult(Intent intent) {
        Uri uri = intent.getData();
        if (uri == null) {
            this.failFile("null data from photo library");
            return;
        }

        String fileLocation = FileHelper.getRealPath(uri, this.cordova);

        if (fileLocation == null || fileLocation.length() == 0) {

            if (returnFileWithDetails) {
                sendOrFailFileDetails(getFileDetails(uri));
            } else {
               this.callbackContext.success(uri.toString());
            }
            
        } else {
            if (returnFileWithDetails) {
                sendOrFailFileDetails(getFileDetails(fileLocation));
            } else {
               this.callbackContext.success(fileLocation);
            }
        }
    }

    /**
     * Returns an array of the file's base 64 string representation, name, and type.
     * 
     * @param  bytesOfFile
     *         Byte representation of file
     * @param  fileName
     *         Name of file
     */
    private JSONArray formatFileDetails(byte[] bytesOfFile, String fileName) {
        if (fileName != null && fileName.length() > 0) {
            String base64EncodedString = getBase64EncodedStringFromBytes(bytesOfFile);
            String[] nameAndType = getFileNameAndType(fileName);

            try {
                return new JSONArray(new String[] { base64EncodedString, nameAndType[0], nameAndType[1] });
            } catch (JSONException e) {
                return null;
            }
        } else return null;
    }

    /**
     * Gets details of file stored externally, e.g. from Google Drive or Dropbox
     * 
     * @param  uri 
     *         Uri of picked file
     */
    private JSONArray getFileDetails(Uri uri) {
        Cursor cursor = this.cordova.getActivity().getContentResolver().query(uri, null, null, null, null);
        
        if (cursor != null && cursor.moveToFirst()) {
            String name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));

            cursor.close();

            try {
                InputStream is = this.cordova.getActivity().getContentResolver().openInputStream(uri);
                if (is != null) {
                    try {
                        byte[] bytesOfFile = IOUtils.toByteArray(is);
                        return formatFileDetails(bytesOfFile, name);
                    } catch (IOException e) {
                        return null;
                    } catch (NullPointerException e) {
                        return null;
                    }
                } else return null;
            } catch (FileNotFoundException e) {
                return null;
            }
        } else return null;
    }

    /**
     * Gets details of file stored locally.
     * 
     * @param  uri 
     *         Uri of picked file
     */
    private JSONArray getFileDetails(String path) {
        File file;

        file = new File(path);

        if (file != null) {
            byte[] bytesOfFile;

            try {
                bytesOfFile = loadFile(file);
                return formatFileDetails(bytesOfFile, file.getName());
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Converts byte array to base 64 representation of string.
     * 
     * @param  bytes
     *         Byte representation of file
     */
    private String getBase64EncodedStringFromBytes(byte[] bytes) {
        if (bytes != null && bytes.length > 0) {
            byte[] base64EncodedFile = Base64.encode(bytes, Base64.NO_WRAP);
            return new String(base64EncodedFile);
        }
        return null;
    }

    /**
     * Parses file name with extension into two separate strings
     * 
     * @param  nameWithType
     *         File name with extension (type)
     */
    private static String[] getFileNameAndType(String nameWithType) {
        String[] nameAndType = new String[2];

        int pos = nameWithType.lastIndexOf(".");

        if (pos > 0) {
            nameAndType[0] = nameWithType.substring(0, pos);
            nameAndType[1] = nameWithType.substring(pos + 1);
        } else {
            nameAndType[0] = nameWithType;
            nameAndType[1] = "";
        }

        return nameAndType;
    }

    /**
     * Calls back to JavaScript side with file details if valid,
     * or fails the file.
     * 
     * @param fileDetails
     *        Verbose details of file
     */
    private void sendOrFailFileDetails(JSONArray fileDetails) {
        if (fileDetails != null && fileDetails.length() == 3) {
            this.callbackContext.success(fileDetails);
        } else {
            this.failFile("Error parsing file from URI.");
        }
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
     * Called when the file picker view exits.
     *
     * @param requestCode
     *        The request code originally supplied to startActivityForResult(),
     *        allowing you to identify who this result came from.
     * @param resultCode
     *        The integer result code returned by the child activity through 
     *        its setResult().
     * @param intent
     *        An Intent, which can return result data to the caller (various
     *         data can be attached to Intent "extras").
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
     * Calls back to JavaScript side with error message.
     *
     * @param err
     */
    public void failFile(String err) {
        this.callbackContext.error(err);
    }
}