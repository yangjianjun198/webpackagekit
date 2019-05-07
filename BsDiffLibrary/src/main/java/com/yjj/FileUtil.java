package com.yjj;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

/**
 * created by yangjianjun on 2019/5/2
 */
public class FileUtil {

  private static final String TAG = FileUtil.class.getSimpleName();

  public static final void writeIntoFile(File file, String content) {
    if (file.exists()) {
      Log.d(TAG, "Delete File: " + file.getPath());
      file.delete();
    }

    FileWriter fileWriter = null;
    try {
      fileWriter = new FileWriter(file);
      fileWriter.write(content);
      Log.d(TAG, "Successfully copied to file: " + file.getPath());
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        fileWriter.flush();
        fileWriter.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public static String readContentFromFile(File file) {
    String content = null;
    try {
      FileInputStream is = new FileInputStream(file);
      int size = is.available();
      byte[] buffer = new byte[size];
      is.read(buffer);
      is.close();
      content = new String(buffer, "UTF-8");
    } catch (FileNotFoundException exception) {
      Log.d(TAG, exception.toString());
    } catch (IOException ex) {
      Log.d(TAG, ex.toString());
    }
    return content;
  }
}
