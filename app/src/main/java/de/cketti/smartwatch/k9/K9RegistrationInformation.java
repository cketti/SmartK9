package de.cketti.smartwatch.k9;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;

import com.sonyericsson.extras.liveware.aef.notification.Notification;
import com.sonyericsson.extras.liveware.aef.registration.Registration;
import com.sonyericsson.extras.liveware.extension.util.ExtensionUtils;
import com.sonyericsson.extras.liveware.extension.util.registration.RegistrationInformation;

import java.util.ArrayList;
import java.util.List;

public class K9RegistrationInformation extends RegistrationInformation {

    final Context mContext;

    protected K9RegistrationInformation(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context == null");
        }
        mContext = context;
    }

    @Override
    public boolean isSourcesToBeUpdatedAtServiceCreation() {
        return true;
    }

    @Override
    public int getRequiredNotificationApiVersion() {
        return 1;
    }

    @Override
    public int getRequiredWidgetApiVersion() {
        return 0;
    }

    @Override
    public int getRequiredControlApiVersion() {
        return 1;
    }

    @Override
    public int getTargetControlApiVersion() {
        return 2;
    }

    @Override
    public int getRequiredSensorApiVersion() {
        return 0;
    }

    @Override
    public ContentValues getExtensionRegistrationConfiguration() {
        String extensionIcon = ExtensionUtils.getUriString(mContext, R.drawable.k9_36x36);
        String iconHostapp = ExtensionUtils.getUriString(mContext, R.drawable.icon);
        String extensionIcon48 = ExtensionUtils.getUriString(mContext, R.drawable.k9_48x48);

        String configurationText = mContext.getString(R.string.configuration_text);
        String extensionName = mContext.getString(R.string.extension_name);

        ContentValues values = new ContentValues();
        values.put(Registration.ExtensionColumns.CONFIGURATION_ACTIVITY,
                K9PreferenceActivity.class.getName());
        values.put(Registration.ExtensionColumns.CONFIGURATION_TEXT, configurationText);
        values.put(Registration.ExtensionColumns.EXTENSION_ICON_URI, extensionIcon);
        values.put(Registration.ExtensionColumns.EXTENSION_48PX_ICON_URI, extensionIcon48);

        values.put(Registration.ExtensionColumns.EXTENSION_KEY,
                K9ExtensionService.EXTENSION_KEY);
        values.put(Registration.ExtensionColumns.HOST_APP_ICON_URI, iconHostapp);
        values.put(Registration.ExtensionColumns.NAME, extensionName);
        values.put(Registration.ExtensionColumns.NOTIFICATION_API_VERSION,
                getRequiredNotificationApiVersion());
        values.put(Registration.ExtensionColumns.PACKAGE_NAME, mContext.getPackageName());
        values.put(Registration.ExtensionColumns.LAUNCH_MODE, Registration.LaunchMode.NOTIFICATION);

        return values;
    }

    @Override
    public ContentValues[] getSourceRegistrationConfigurations() {
        List<ContentValues> bulkValues = new ArrayList<ContentValues>();
        bulkValues
                .add(getSourceRegistrationConfiguration(K9ExtensionService.EXTENSION_SPECIFIC_ID));
        return bulkValues.toArray(new ContentValues[bulkValues.size()]);
    }

    public ContentValues getSourceRegistrationConfiguration(String extensionSpecificId) {
        String iconSource1 = ExtensionUtils.getUriString(mContext, R.drawable.k9_30x30);
        String iconSource2 = ExtensionUtils.getUriString(mContext, R.drawable.k9_18x18);
        String textToSpeech = mContext.getString(R.string.text_to_speech);

        ContentValues sourceValues = new ContentValues();
        sourceValues.put(Notification.SourceColumns.ENABLED, true);
        sourceValues.put(Notification.SourceColumns.ICON_URI_1, iconSource1);
        sourceValues.put(Notification.SourceColumns.ICON_URI_2, iconSource2);
        sourceValues.put(Notification.SourceColumns.UPDATE_TIME, System.currentTimeMillis());
        sourceValues.put(Notification.SourceColumns.NAME, mContext.getString(R.string.source_name));
        sourceValues.put(Notification.SourceColumns.EXTENSION_SPECIFIC_ID, extensionSpecificId);
        sourceValues.put(Notification.SourceColumns.PACKAGE_NAME, mContext.getPackageName());
        sourceValues.put(Notification.SourceColumns.TEXT_TO_SPEECH, textToSpeech);
        sourceValues.put(Notification.SourceColumns.COLOR, Color.argb(0, 180, 0, 0));
        sourceValues.put(Notification.SourceColumns.ACTION_1,
                mContext.getString(R.string.action_event_1));
        sourceValues.put(Notification.SourceColumns.ACTION_2,
                mContext.getString(R.string.action_event_3));
        sourceValues.put(Notification.SourceColumns.ACTION_3, (String)null);

        return sourceValues;
    }

    @Override
    public boolean isDisplaySizeSupported(int width, int height) {
        Resources resources = mContext.getResources();
        int sw2width = resources.getDimensionPixelSize(R.dimen.smart_watch_2_control_width);
        int sw2height = resources.getDimensionPixelSize(R.dimen.smart_watch_2_control_height);

        return (width >= sw2width && height >= sw2height);
    }
}
