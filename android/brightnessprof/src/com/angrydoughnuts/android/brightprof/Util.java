/****************************************************************************
 * Copyright 2009 kraigs.android@gmail.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 ****************************************************************************/

package com.angrydoughnuts.android.brightprof;

import java.math.BigDecimal;

import android.app.Activity;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.view.WindowManager;

public class Util {

  /**
   * Calculates the current application-defined brightness value of the phone.
   * This is done by taking the actual system brightness (a value from 0 to 255)
   * and normalizing it to a scale of 0 to 100. The actual brightness percentage
   * is calculated on the scale of minimumBrightness to 255, though.
   * 
   * @param activity
   *          The current activity.
   * @param db
   *          A database accessor pointing to a DB with the minimum minimum
   *          brightness setting in it.
   * @return A value between 0 and 100.
   */
  static int getPhoneBrighness(Activity activity, DbAccessor db) {
    int systemBrightness = getSystemBrightness(activity);
    int minValue = db.getMinimumBrightness();

    // The system brightness can range from 0 to 255. To normalize this
    // to the application's 0 to 100 brightness values, we lookup the
    // configured minimum value and then normalize for the range
    // minValue to 255.
    BigDecimal d = new BigDecimal((systemBrightness - minValue)
        / (255.0 - minValue) * 100.0);
    d = d.setScale(0, BigDecimal.ROUND_HALF_EVEN);
    int normalizedBrightness = d.intValue();

    if (normalizedBrightness < 0) {
      // This can happen if another application sets the phone's brightness
      // to a value lower than our configured minimum.
      return 0;
    } else {
      return normalizedBrightness;
    }
  }

  /**
   * Finds the phone's system brightness setting. Returns 0 if there is an error
   * getting this setting.
   * 
   * @param activity
   *          The current activity.
   * @return A value between 0 and 255.
   */
  static int getSystemBrightness(Activity activity) {
    // Lookup the initial system brightness.
    int systemBrightness = 0;
    try {
      systemBrightness = Settings.System.getInt(activity.getContentResolver(),
          Settings.System.SCREEN_BRIGHTNESS);
    } catch (SettingNotFoundException e) {
      // TODO Log an error message.
    }
    return systemBrightness;
  }

  /**
   * Sets the brightness for the activity supplied as well as the system
   * brightness level. The brightness value passed in should be an integer
   * between 0 and 100. This method will translate that number into a normalized
   * value using the devices actual maximum brightness level and the minimum
   * brightness level calibrated via the CalibrateActivity activity.
   * 
   * @param activity
   *          The active activity.
   * @param brightnessPercentage
   *          An integer between 0 and 100.
   */
  static void setPhoneBrightness(Activity activity, DbAccessor db,
      int brightnessPercentage) {
    // Lookup the minimum acceptable brightness set by the CalibrationActivity.
    int min_value = db.getMinimumBrightness();

    // Convert the normalized application brightness to a system value (between
    // min_value and 255).
    BigDecimal d = new BigDecimal((brightnessPercentage / 100.0)
        * (255 - min_value) + min_value);
    d = d.setScale(0, BigDecimal.ROUND_HALF_EVEN);
    int brightnessUnits = d.intValue();

    if (brightnessUnits < min_value) {
      brightnessUnits = min_value;
    } else if (brightnessUnits > 255) {
      brightnessUnits = 255;
    }
    setSystemBrightness(activity, brightnessUnits);
    setActivityBrightness(activity, brightnessUnits);
  }

  /**
   * Sets the phone's global brightness level. This does not change the screen's
   * brightness immediately. Valid brightnesses range from 0 to 255.
   * 
   * @param activity
   *          The active activity.
   * @param brightnessUnits
   *          An integer between 0 and 255.
   */
  static void setSystemBrightness(Activity activity, int brightnessUnits) {
    // Change the system brightness setting. This doesn't change the
    // screen brightness immediately. (Scale 0 - 255).
    Settings.System.putInt(activity.getContentResolver(),
        Settings.System.SCREEN_BRIGHTNESS, brightnessUnits);
  }

  /**
   * Sets the screen brightness for this activity. The screen brightness will
   * change immediately. As soon as the activity terminates, the brightness will
   * return to the system brightness. Valid brightnesses range from 0 to 255.
   * 
   * @param activity
   *          The active activity.
   * @param brightnessUnits
   *          An integer between 0 and 255.
   */
  static void setActivityBrightness(Activity activity, int brightnessUnits) {
    // Set the brightness of the current window. This takes effect immediately.
    // When the window is closed, the new system brightness is used.
    // (Scale 0.0 - 1.0).
    WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
    lp.screenBrightness = brightnessUnits / 255.0f;
    activity.getWindow().setAttributes(lp);
  }

  static boolean supportsAutoBrightness() {
    // TODO(cgallek): Implement this.
    return false;
  }

  // TODO(cgallek) remove this boolean.
  static private boolean autoBrightnessEnabled = true;
  static boolean getAutoBrightnessEnabled() {
    // TODO(cgallek): Implement this.
    return autoBrightnessEnabled;
  }

  static void setAutoBrightnessEnabled(boolean enabled) {
    // TODO(cgallek): Implement this.
    autoBrightnessEnabled = enabled;
  }
}
