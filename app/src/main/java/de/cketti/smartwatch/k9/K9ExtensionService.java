package de.cketti.smartwatch.k9;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.sonyericsson.extras.liveware.aef.control.Control;
import com.sonyericsson.extras.liveware.aef.notification.Notification;
import com.sonyericsson.extras.liveware.aef.registration.Registration;
import com.sonyericsson.extras.liveware.extension.util.ExtensionService;
import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;
import com.sonyericsson.extras.liveware.extension.util.notification.NotificationUtil;
import com.sonyericsson.extras.liveware.extension.util.registration.RegistrationInformation;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class K9ExtensionService extends ExtensionService {

    /**
     * Extensions specific id for the source
     */
    public static final String EXTENSION_SPECIFIC_ID = "K9_NEW_MESSAGE";

    /**
     * Extension key
     */
    public static final String EXTENSION_KEY = "de.cketti.smartwatch.k9";

    /**
     * Log tag
     */
    public static final String LOG_TAG = "SmartK9";

    private static final int INVALID_EVENT_ID = -1;

    private static final String[] MESSAGE_PROJECTION = {
            K9Helper.MessageColumns.URI,
            K9Helper.MessageColumns.PREVIEW,
            K9Helper.MessageColumns.SENDER
    };

    private static final String[] READ_STATE_PROJECTION = {
            K9Helper.MessageColumns.URI,
            K9Helper.MessageColumns.UNREAD
    };

    private static final int URI_COLUMN = 0;
    private static final int PREVIEW_COLUMN = 1;
    private static final int UNREAD_COLUMN = 1;
    private static final int SENDER_COLUMN = 2;

    private static final String[] NOTIFICATION_PROJECTION = {
            Notification.EventColumns._ID
    };

    private static final String[] FRIEND_KEY_PROJECTION = {
            Notification.EventColumns._ID,
            Notification.EventColumns.FRIEND_KEY
    };

    private static final int EVENT_ID_COLUMN = 0;
    private static final int FRIEND_KEY_COLUMN = 1;


    private int mEventId = INVALID_EVENT_ID;


    public K9ExtensionService() {
        super(EXTENSION_KEY);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int retVal = super.onStartCommand(intent, flags, startId);
        if (intent != null) {
            if (K9Helper.BroadcastIntents.ACTION_EMAIL_RECEIVED.equals(intent.getAction())) {
                onEmailReceived(intent);
                stopSelfCheck();
            } else if (K9Helper.BroadcastIntents.ACTION_EMAIL_DELETED.equals(intent.getAction())) {
                onEmailRemoved(intent);
                stopSelfCheck();
            } else if (K9Helper.BroadcastIntents.ACTION_REFRESH_OBSERVER.equals(intent.getAction())) {
                onChangeEvent();
                stopSelfCheck();
            } else if (Control.Intents.CONTROL_RESUME_INTENT.equals(intent.getAction())) {
                if (mEventId != INVALID_EVENT_ID) {
                    sendEventIdToControlExtension();
                }
            }
        }

        return retVal;
    }

    private void sendEventIdToControlExtension() {
        Bundle bundle = new Bundle();
        bundle.putInt(K9ControlExtension.KEY_EVENT_ID, mEventId);
        doActionOnAllControls(K9ControlExtension.REQUEST_CODE_SET_EVENT_ID, bundle);
        mEventId = INVALID_EVENT_ID;
    }

    private void onEmailReceived(Intent intent) {
        long sourceId = NotificationUtil.getSourceId(this, EXTENSION_SPECIFIC_ID);
        if (sourceId == NotificationUtil.INVALID_ID) {
            Log.e(LOG_TAG, "Failed to insert data");
            return;
        }

//        String profileImage = ExtensionUtils.getUriString(this,
//                R.drawable.widget_default_userpic_bg);

        String from = intent.getStringExtra(K9Helper.IntentExtras.FROM);
        String subject = intent.getStringExtra(K9Helper.IntentExtras.SUBJECT);
        Date time = (Date) intent.getSerializableExtra(K9Helper.IntentExtras.SENT_DATE);
        Uri emailUri = intent.getData();

        String preview = "";
        String sender = from;
        try {
            Cursor cursor = getContentResolver().query(
                    K9Helper.INBOX_MESSAGES_URI, MESSAGE_PROJECTION, null, null, null);

            if (cursor != null) {
                try {
                    while (cursor.moveToNext()) {
                        String uri = cursor.getString(URI_COLUMN);
                        if (emailUri.toString().equals(uri)) {
                            preview = cursor.getString(PREVIEW_COLUMN);
                            sender = cursor.getString(SENDER_COLUMN);
                            break;
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Couldn't read data from K-9 Mail", e);
        }

        ContentValues eventValues = new ContentValues();
        eventValues.put(Notification.EventColumns.EVENT_READ_STATUS, false);
        eventValues.put(Notification.EventColumns.DISPLAY_NAME, sender);
        eventValues.put(Notification.EventColumns.TITLE, subject);
        eventValues.put(Notification.EventColumns.MESSAGE, preview);
        eventValues.put(Notification.EventColumns.PERSONAL, 1);
//        eventValues.put(Notification.EventColumns.PROFILE_IMAGE_URI, profileImage);
        eventValues.put(Notification.EventColumns.PUBLISHED_TIME, time.getTime());
        eventValues.put(Notification.EventColumns.SOURCE_ID, sourceId);
        eventValues.put(Notification.EventColumns.FRIEND_KEY, emailUri.toString());

        try {
            getContentResolver().insert(Notification.Event.URI, eventValues);
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "Failed to insert event", e);
        } catch (SecurityException e) {
            Log.e(LOG_TAG, "Failed to insert event, is Live Ware Manager installed?", e);
        } catch (SQLException e) {
            Log.e(LOG_TAG, "Failed to insert event", e);
        }
    }

    private void onEmailRemoved(Intent intent) {
        long sourceId = NotificationUtil.getSourceId(this, EXTENSION_SPECIFIC_ID);
        if (sourceId == NotificationUtil.INVALID_ID) {
            Log.e(LOG_TAG, "Failed to insert data");
            return;
        }

        Uri emailUri = intent.getData();

        try {
            String selection = Notification.EventColumns.SOURCE_ID + "=? AND " +
                    Notification.EventColumns.FRIEND_KEY + "=?";
            String[] selectionArgs = new String[] {
                    Long.toString(sourceId),
                    emailUri.toString()
            };

            Cursor cursor = getContentResolver().query( Notification.Event.URI,
                    NOTIFICATION_PROJECTION, selection, selectionArgs, null);

            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        deleteEvent(cursor.getLong(EVENT_ID_COLUMN));
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error", e);
        }
    }

    private void deleteEvent(long eventId) {
        NotificationUtil.deleteEvents(this, Notification.EventColumns._ID + "=?",
                new String[]{Long.toString(eventId)});
    }

    private void onChangeEvent() {
        long sourceId = NotificationUtil.getSourceId(this, EXTENSION_SPECIFIC_ID);
        if (sourceId == NotificationUtil.INVALID_ID) {
            Log.e(LOG_TAG, "Failed to insert data");
            return;
        }

        try {
            String selection = Notification.EventColumns.SOURCE_ID + "=? AND " +
                    Notification.EventColumns.EVENT_READ_STATUS + "=0";
            String[] selectionArgs = { Long.toString(sourceId) };

            Cursor cursor = getContentResolver().query(Notification.Event.URI,
                    FRIEND_KEY_PROJECTION, selection, selectionArgs, null);

            if (cursor != null) {
                Map<String, Long> mapping = new HashMap<String, Long>();
                try {
                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(EVENT_ID_COLUMN);
                        String friendKey = cursor.getString(FRIEND_KEY_COLUMN);
                        mapping.put(friendKey, id);
                    }
                } finally {
                    cursor.close();
                }

                if (!mapping.isEmpty()) {
                    cursor = getContentResolver().query(K9Helper.INBOX_MESSAGES_URI,
                            READ_STATE_PROJECTION, null, null, null);

                    if (cursor != null) {
                        try {
                            while (cursor.moveToNext()) {
                                String uri = cursor.getString(URI_COLUMN);
                                boolean unread = Boolean.TRUE.toString().equals(
                                        cursor.getString(UNREAD_COLUMN));

                                if (!unread && mapping.containsKey(uri.toString())) {
                                    Long eventId = mapping.get(uri.toString());

                                    ContentValues values = new ContentValues();
                                    values.put(Notification.EventColumns.EVENT_READ_STATUS, 1);

                                    NotificationUtil.updateEvents(this,
                                            values,
                                            Notification.EventColumns._ID + "=?",
                                            new String[]{eventId.toString()});
                                }
                            }
                        } finally {
                            cursor.close();
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Something went wrong", e);
        }
    }

    @Override
    protected void onViewEvent(Intent intent) {
        String action = intent.getStringExtra(Notification.Intents.EXTRA_ACTION);
        String hostAppPackageName =
                intent.getStringExtra(Registration.Intents.EXTRA_AHA_PACKAGE_NAME);
        int eventId = intent.getIntExtra(Notification.Intents.EXTRA_EVENT_ID, INVALID_EVENT_ID);

        if (Notification.SourceColumns.ACTION_1.equals(action)) {
            String friendKey = NotificationUtil.getFriendKey(this, eventId);
            displayMessageOnPhone(friendKey);
        } else if (Notification.SourceColumns.ACTION_2.equals(action)) {
            mEventId = eventId;
            controlStartRequest(hostAppPackageName);
        }
    }

    private void displayMessageOnPhone(String friendKey) {
        Intent startIntent = K9Helper.getViewMessageIntent(this, friendKey);
        startActivity(startIntent);
    }

    @Override
    protected RegistrationInformation getRegistrationInformation() {
        return new K9RegistrationInformation(this);
    }

    @Override
    protected boolean keepRunningWhenConnected() {
        /*
         * Keep the service running when mEventId contains a value so it can later be passed to
         * the control extension.
         */
        return (mEventId != INVALID_EVENT_ID);
    }

    @Override
    public void startActivity(Intent intent) {
        // We're a service. So we need to start all activities with FLAG_ACTIVITY_NEW_TASK.
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        super.startActivity(intent);
    }

    @Override
    public ControlExtension createControlExtension(String hostAppPackageName) {
        return new K9ControlExtension(hostAppPackageName, this);
    }
}
