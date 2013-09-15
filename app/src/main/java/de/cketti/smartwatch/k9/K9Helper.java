/*
 * Copyright 2013 Christian Ketterer (cketti)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.cketti.smartwatch.k9;


import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;


/**
 * Helper class to interface with K-9 Mail.
 */
public class K9Helper {
    /**
     * K-9 Mail's package name.
     */
    public static final String PACKAGE_NAME = "com.fsck.k9";

    /**
     * Permission required to access K-9 Mail's public content provider.
     */
    public static final String PERMISSION = "com.fsck.k9.permission.READ_MESSAGES";

    /**
     * Authority of K-9 Mail's content provider.
     */
    public static final String AUTHORITY = "com.fsck.k9.messageprovider";

    /**
     * Base URI of K-9 Mail's content provider.
     */
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    /**
     * The content:// style URI to access K-9 Mail's accounts.
     */
    public static final Uri ACCOUNTS_URI = Uri.withAppendedPath(CONTENT_URI, "accounts");

    /**
     * The content:// style base URI to access the unread count of an account.
     */
    public static final Uri ACCOUNT_UNREAD_URI =
            Uri.withAppendedPath(CONTENT_URI, "account_unread");

    /**
     * The content:// style URI to access K-9 Mail's Inbox messages.
     */
    public static final Uri INBOX_MESSAGES_URI =
            Uri.withAppendedPath(CONTENT_URI, "inbox_messages");


    private static final Uri getAccountUnreadUri(int accountNumber) {
        return Uri.withAppendedPath(ACCOUNT_UNREAD_URI, Integer.toString(accountNumber));
    }

    /**
     * Column names for the accounts "table".
     *
     * @see K9Helper#ACCOUNTS_URI
     */
    public interface AccountColumns {
        /**
         * Account number.
         *
         * <p><strong>Note:</strong> This value will change when reordering the accounts in
         * K-9 Mail. Use {@link #UUID} when saving references to an account.</p>
         *
         * <p>Type: INTEGER</p>
         */
        public static final String NUMBER = "accountNumber";

        /**
         * Name of the account.
         *
         * <p>Type: TEXT</p>
         */
        public static final String NAME = "accountName";

        /**
         * The account's UUID.
         *
         * <p>Type: TEXT</p>
         */
        public static final String UUID = "accountUuid";

        /**
         * The RGB color code of the account's color.
         *
         * <p>Type: INTEGER</p>
         */
        public static final String COLOR = "accountColor";
    }

    /**
     * Column names for an account's unread count "table".
     *
     * <p><strong>Note:</strong> The projection supplied to the content resolver is ignored by
     * K-9 Mail. Instead {@link #NAME} is always the first column, {@link #UNREAD} the second.
     * You can use the constants {@link #ACCOUNT_NAME_INDEX} and {@link #UNREAD_INDEX} to access
     * the columns.
     * </p>
     *
     * @see K9Helper#ACCOUNT_UNREAD_URI
     */
    public interface AccountUnreadColumns {
        /**
         * Name of the account.
         *
         * <p>Type: TEXT</p>
         */
        public static final String NAME = "accountName";

        /**
         * Number of unread messages in this account.
         *
         * <p>Type: INTEGER</p>
         */
        public static final String UNREAD = "unread";

        /**
         * Index of the {@link #NAME} column.
         */
        public static final int ACCOUNT_NAME_INDEX = 0;

        /**
         * Index of the {@link #UNREAD} column.
         */
        public static final int UNREAD_INDEX = 1;
    }

    /**
     * Column names for the Inbox messages "table".
     *
     * @see K9Helper#INBOX_MESSAGES_URI
     */
    public interface MessageColumns extends BaseColumns {
        /**
         * The number of milliseconds since Jan. 1, 1970, midnight GMT.
         *
         * <P>Type: INTEGER (long)</P>
         */
        String SEND_DATE = "date";

        /**
         * <P>Type: TEXT</P>
         */
        String SENDER = "sender";

        /**
         * <P>Type: TEXT</P>
         */
        String SENDER_ADDRESS = "senderAddress";

        /**
         * <P>Type: TEXT</P>
         */
        String SUBJECT = "subject";

        /**
         * <P>Type: TEXT</P>
         */
        String PREVIEW = "preview";

        /**
         * <P>Type: BOOLEAN</P>
         */
        String UNREAD = "unread";

        /**
         * <P>Type: TEXT</P>
         */
        String ACCOUNT = "account";

        /**
         * <P>Type: INTEGER</P>
         */
        String ACCOUNT_NUMBER = "accountNumber";

        /**
         * <P>Type: BOOLEAN</P>
         */
        String HAS_ATTACHMENTS = "hasAttachments";

        /**
         * <P>Type: BOOLEAN</P>
         */
        String HAS_STAR = "hasStar";

        /**
         * <P>Type: INTEGER</P>
         */
        String ACCOUNT_COLOR = "accountColor";

        /**
         * <P>Type: TEXT</P>
         */
        String URI = "uri";

        /**
         * <P>Type: TEXT</P>
         */
        String DELETE_URI = "delUri";
    }

    /**
     * Constants related to broadcast intents sent by K-9 Mail.
     */
    public interface BroadcastIntents {
        /**
         * Broadcast Action: Sent when an email was received.
         */
        public static final String ACTION_EMAIL_RECEIVED =
                "com.fsck.k9.intent.action.EMAIL_RECEIVED";

        /**
         * Broadcast Action: Sent when an email was deleted.
         */
        public static final String ACTION_EMAIL_DELETED =
                "com.fsck.k9.intent.action.EMAIL_DELETED";

        /**
         * Broadcast Action: Sent in various situations when external applications should re-query
         * K-9 Mail's content provider(s).
         */
        public static final String ACTION_REFRESH_OBSERVER =
                "com.fsck.k9.intent.action.REFRESH_OBSERVER";
    }

    public interface IntentExtras {
        public static final String ACCOUNT = "com.fsck.k9.intent.extra.ACCOUNT";
        public static final String FOLDER = "com.fsck.k9.intent.extra.FOLDER";
        public static final String SENT_DATE = "com.fsck.k9.intent.extra.SENT_DATE";
        public static final String FROM = "com.fsck.k9.intent.extra.FROM";
        public static final String TO = "com.fsck.k9.intent.extra.TO";
        public static final String CC = "com.fsck.k9.intent.extra.CC";
        public static final String BCC = "com.fsck.k9.intent.extra.BCC";
        public static final String SUBJECT = "com.fsck.k9.intent.extra.SUBJECT";
        public static final String FROM_SELF = "com.fsck.k9.intent.extra.FROM_SELF";
    }

    /**
     * Stores information about a K-9 Mail account.
     */
    public static class Account {
        /**
         * The account number.
         */
        public final int number;

        /**
         * The name of the account.
         */
        public final String name;

        /**
         * The account's UUID.
         */
        public final String uuid;

        /**
         * RGB value of the account's color.
         */
        public final int color;

        Account(int number, String name, String uuid, int color) {
            this.number = number;
            this.name = name;
            this.uuid = uuid;
            this.color = color;
        }

        /**
         * Returns the account's name.
         */
        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Queries K-9 Mail's content provider to retrieve a list of accounts.
     *
     * <p>This should not be called from the main thread.</p>
     *
     * @param context
     *         Used to retrieve the content resolver.
     *
     * @return A (possibly empty) list of {@link Account} instances, or {@code null} in case of an
     *         error.
     */
    public static final List<Account> getAccounts(Context context) {
        try {
            ContentResolver cr = context.getContentResolver();

            String[] projection = {
                    AccountColumns.NUMBER,
                    AccountColumns.NAME,
                    AccountColumns.UUID,
                    AccountColumns.COLOR
            };

            Cursor cursor = cr.query(ACCOUNTS_URI, projection, null, null, null);
            if (cursor == null) {
                return null;
            }

            List<Account> accounts = new ArrayList<Account>();
            try {
                int unnamedCount = 0;
                while (cursor.moveToNext()) {
                    int accountNumber = cursor.getInt(0);
                    String accountName = cursor.getString(1);

                    if (accountName == null || accountName.length() == 0) {
                        accountName = "unnamed" + (++unnamedCount);
                    }

                    String accountUuid;
                    int accountColor;
                    if (cursor.getColumnCount() > 2) {
                        accountUuid = cursor.getString(2);
                        accountColor = cursor.getInt(3);
                    } else {
                        accountUuid = Integer.toString(accountNumber);
                        accountColor = 0;
                    }

                    if (accountUuid != null) {
                        accounts.add(new Account(accountNumber, accountName, accountUuid,
                                accountColor));
                    }
                }
            } finally {
                cursor.close();
            }

            return accounts;
        } catch (Exception e) {
            Log.e("K9Helper", "Something went wrong while fetching the list of accounts", e);
            return null;
        }
    }

    /**
     * Query K-9 Mail's content provider to retrieve the number of unread messages in the supplied
     * account.
     *
     * @param context
     *         Used to retrieve the content resolver.
     * @param account
     *         The account to get the unread messages for. Use {@link #getAccounts(Context)} to
     *         retrieve an {@link Account} instance. Must not be {@code null}.
     *
     * @return The number of unread messages in that account. Or {@code 0} if something went wrong.
     */
    public static final int getUnreadCount(Context context, Account account) {
        try {
            ContentResolver cr = context.getContentResolver();
            Cursor cursor = cr.query(K9Helper.getAccountUnreadUri(account.number), null, null, null,
                    null);

            if (cursor == null) {
                return 0;
            }

            int accountUnread = 0;
            try {
                if (cursor.moveToFirst()) {
                    accountUnread = cursor.getInt(AccountUnreadColumns.UNREAD_INDEX);
                }
            } finally {
                cursor.close();
            }

            return accountUnread;
        } catch (Exception e) {
            Log.e("K9Helper", "Something went wrong while fetching the unread count for " +
                    account.name + " (" + account.uuid + ")", e);
            return 0;
        }
    }

    /**
     * Returns an intent to start K-9 Mail.
     *
     * @param context
     *         Used to retrieve the package manager.
     *
     * @return An intent to start K-9 Mail's main activity, or {@code null} in case of an error.
     */
    public static final Intent getStartK9Intent(Context context) {
        try {
            PackageManager manager = context.getPackageManager();
            Intent intent = manager.getLaunchIntentForPackage(PACKAGE_NAME);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            return intent;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns an intent to display the message identified by the provided URI in K-9 Mail.
     *
     * @param context
     *         Used to retrieve the package manager.
     * @param emailUri
     *         The URI that identified the message to be displayed.
     *
     * @return An intent to start K-9 Mail's main activity, or {@code null} in case of an error.
     */
    public static final Intent getViewMessageIntent(Context context, String emailUri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setPackage(PACKAGE_NAME);
            intent.setData(Uri.parse(emailUri));
            return intent;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Find out whether or not K-9 Mail is installed.
     *
     * @param context
     *         Used to retrieve the package manager.
     *
     * @return {@code true} if K-9 Mail is installed, {@code false} otherwise.
     */
    public static final boolean isK9Installed(Context context) {
        PackageManager manager = context.getPackageManager();
        try {
            manager.getPackageInfo(PACKAGE_NAME, 0);
            return true;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Find out if K-9 Mail is enabled, i.e. an account was set up.
     *
     * @param context
     *         Used to retrieve the package manager.
     *
     * @return {@code true} if K-9 Mail is enabled, {@code false} otherwise.
     */
    public static final boolean isK9Enabled(Context context) {
        PackageManager manager = context.getPackageManager();
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
            intent.setPackage(PACKAGE_NAME);
            List<ResolveInfo> results = manager.queryIntentActivities(intent, 0);
            return (results != null && results.size() > 0);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Find out if we have the permission to access K-9 Mail's content provider.
     *
     * @param context
     *         Used to retrieve the package manager.
     *
     * @return {@code true} if we have the needed permission, {@code false} otherwise.
     */
    public static final boolean hasK9ReadPermission(Context context) {
        PackageManager manager = context.getPackageManager();
        try {
            return (manager.checkPermission(PERMISSION, context.getPackageName()) ==
                    PackageManager.PERMISSION_GRANTED);
        } catch (Exception e) {
            return false;
        }
    }
}
