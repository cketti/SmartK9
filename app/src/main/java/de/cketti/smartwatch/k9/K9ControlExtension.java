package de.cketti.smartwatch.k9;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.sonyericsson.extras.liveware.aef.control.Control;
import com.sonyericsson.extras.liveware.aef.notification.Notification;
import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;
import com.sonyericsson.extras.liveware.extension.util.control.ControlObjectClickEvent;
import com.sonyericsson.extras.liveware.extension.util.control.ControlTouchEvent;
import com.sonyericsson.extras.liveware.extension.util.control.ControlView;
import com.sonyericsson.extras.liveware.extension.util.control.ControlViewGroup;
import com.sonyericsson.extras.liveware.extension.util.notification.NotificationUtil;


class K9ControlExtension extends ControlExtension {
    public static final String LOG_TAG = "SmartK9";

    public static final int REQUEST_CODE_SET_EVENT_ID = 1;

    public static final String KEY_EVENT_ID = "eventId";

    private static final int INVALID_EVENT_ID = -1;

    private static final String[] MESSAGE_PROJECTION = {
            K9Helper.MessageColumns.URI,
            K9Helper.MessageColumns.DELETE_URI
    };

    private static final int URI_COLUMN = 0;
    private static final int DELETE_URI_COLUMN = 1;


    private ControlViewGroup mLayout = null;
    private int mEventId = INVALID_EVENT_ID;


    K9ControlExtension(final String hostAppPackageName, final Context context) {
        super(context, hostAppPackageName);
        setupClickables(context);
    }

    @Override
    public void onResume() {
        showLayout(R.layout.confirmation_screen, null);
    }

    @Override
    public void onObjectClick(final ControlObjectClickEvent event) {
        if (event.getLayoutReference() != -1) {
            mLayout.onClick(event.getLayoutReference());
        }
    }

    private void setupClickables(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.confirmation_screen, null);
        mLayout = parseLayout(layout);
        if (mLayout != null) {
            ControlView positiveButton = mLayout.findViewById(R.id.button2);
            positiveButton.setOnClickListener(new ControlView.OnClickListener() {
                @Override
                public void onClick() {
                    deleteMessage();
                    stopRequest();
                }
            });
            ControlView negativeButton = mLayout.findViewById(R.id.button);
            negativeButton.setOnClickListener(new ControlView.OnClickListener() {
                @Override
                public void onClick() {
                    stopRequest();
                }
            });
        }
    }

    @Override
    public void onDoAction(int requestCode, Bundle bundle) {
        switch (requestCode) {
            case REQUEST_CODE_SET_EVENT_ID: {
                mEventId = bundle.getInt(KEY_EVENT_ID);
                break;
            }
        }
    }

    private void deleteMessage() {
        if (mEventId == INVALID_EVENT_ID) {
            return;
        }

        long sourceId = NotificationUtil.getSourceId(mContext,
                K9ExtensionService.EXTENSION_SPECIFIC_ID);
        if (sourceId == NotificationUtil.INVALID_ID) {
            return;
        }

        String friendKey = NotificationUtil.getFriendKey(mContext, mEventId);
        if (friendKey == null) {
            Log.w(LOG_TAG, "Friend key not found");
            return;
        }

        NotificationUtil.deleteEvents(mContext,
                Notification.EventColumns._ID + "=?",
                new String[] { Integer.toString(mEventId) });

        ContentResolver contentResolver = mContext.getContentResolver();

        try {
            String deleteUri = null;
            Cursor cursor = contentResolver.query(K9Helper.INBOX_MESSAGES_URI,
                    MESSAGE_PROJECTION, null, null, null);
            if (cursor != null) {
                try {
                    while (cursor.moveToNext()) {
                        String uri = cursor.getString(URI_COLUMN);
                        if (friendKey.equals(uri)) {
                            deleteUri = cursor.getString(DELETE_URI_COLUMN);
                            break;
                        }
                    }
                } finally {
                    cursor.close();
                }
            }

            if (deleteUri != null) {
                contentResolver.delete(Uri.parse(deleteUri), null, null);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Something went wrong while trying to delete message", e);
        }
    }
}
