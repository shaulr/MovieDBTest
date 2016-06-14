package tikal.com.myapplication.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;
import android.util.SparseArray;

/**
 * Created by shaulr on 14/06/2016.
 */

public class MoviesContentProvider extends ContentProvider {
    public static final int MOVIE_URL_QUERY = 1;


    public static final int INVALID_URI = -1;

    private static final String TEXT_TYPE = "TEXT";
    private static final String REAL_TYPE = "REAL";
    private static final String PRIMARY_KEY_TYPE = "INTEGER PRIMARY KEY";
    private static final String INTEGER_TYPE = "INTEGER";

    private static final String CREATE_MOVIE_TABLE_SQL = "CREATE TABLE" + " " +
            MoviesContract.MOVIE_TABLE_NAME + " " +
            "(" + " " +
            MoviesContract.ROW_ID + " " + PRIMARY_KEY_TYPE + " AUTOINCREMENT NOT NULL ," +
            MoviesContract.TITLE_COLUMN + " " + TEXT_TYPE + " ," +
            MoviesContract.IMAGE_THUMBURL_COLUMN + " " + TEXT_TYPE + " ," +
            MoviesContract.DESCRIPTION_COLUMN + " " + TEXT_TYPE + " ," +
            MoviesContract.SCORE_COLUMN + " " + REAL_TYPE + " ," +
            MoviesContract.RUNNING_TIME_COLUMN + " " + INTEGER_TYPE + " ," +
            MoviesContract.TRAILERS_COLUMN + " " + TEXT_TYPE + " ," +
            MoviesContract.TIMESTAMP_COLUMN + " " + INTEGER_TYPE + " ," +
            MoviesContract.MOVIE_ID + " " + TEXT_TYPE + " ," +
            MoviesContract.RELEASE_DATE_COLUMN + " " + TEXT_TYPE +
            ")";


    public static final String LOG_TAG = "MoviesContentProvider";

    private SQLiteOpenHelper mHelper;

    private static final UriMatcher sUriMatcher;

    private static final SparseArray<String> sMimeTypes;

    static {

        sUriMatcher = new UriMatcher(0);

        sMimeTypes = new SparseArray<String>();

        sUriMatcher.addURI(
                MoviesContract.AUTHORITY,
                MoviesContract.MOVIE_TABLE_NAME,
                MOVIE_URL_QUERY);


        sMimeTypes.put(
                MOVIE_URL_QUERY,
                "vnd.android.cursor.dir/vnd." +
                        MoviesContract.AUTHORITY + "." +
                        MoviesContract.MOVIE_TABLE_NAME);

        sMimeTypes.put(
                MOVIE_URL_QUERY,
                "vnd.android.cursor.item/vnd." +
                        MoviesContract.AUTHORITY + "." +
                        MoviesContract.MOVIE_TABLE_NAME);
    }

    public void close() {
        mHelper.close();
    }

    private class MovieProviderHelper extends SQLiteOpenHelper {

        MovieProviderHelper(Context context) {
            super(context,
                    MoviesContract.DATABASE_NAME,
                    null,
                    MoviesContract.DATABASE_VERSION);
        }


        private void dropTables(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS " + MoviesContract.MOVIE_TABLE_NAME);
        }


        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_MOVIE_TABLE_SQL);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int version1, int version2) {
            Log.w(MovieProviderHelper.class.getSimpleName(),
                    "Upgrading database from version " + version1 + " to "
                            + version2 + ", which will destroy all the existing data");

            dropTables(db);

            onCreate(db);
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int version1, int version2) {
            Log.w(MovieProviderHelper.class.getSimpleName(),
                    "Downgrading database from version " + version1 + " to "
                            + version2 + ", which will destroy all the existing data");

            dropTables(db);
            onCreate(db);
        }
    }

    @Override
    public boolean onCreate() {
        mHelper = new MovieProviderHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder) {

        SQLiteDatabase db = mHelper.getReadableDatabase();
        switch (sUriMatcher.match(uri)) {

            case MOVIE_URL_QUERY:
                Cursor returnCursor = db.query(
                        MoviesContract.MOVIE_TABLE_NAME,
                        projection,
                        selection, selectionArgs, null, null, null);

                returnCursor.setNotificationUri(getContext().getContentResolver(), uri);
                return returnCursor;


            case INVALID_URI:
                throw new IllegalArgumentException("Query -- Invalid URI:" + uri);
        }

        return null;
    }


    @Override
    public String getType(Uri uri) {
        return sMimeTypes.get(sUriMatcher.match(uri));
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        switch (sUriMatcher.match(uri)) {
            case MOVIE_URL_QUERY:

                SQLiteDatabase localSQLiteDatabase = mHelper.getWritableDatabase();
                long id = localSQLiteDatabase.insert(
                        MoviesContract.MOVIE_TABLE_NAME,
                        MoviesContract.TITLE_COLUMN,
                        values
                );

                if (-1 != id) {
                    getContext().getContentResolver().notifyChange(uri, null);
                    return Uri.withAppendedPath(uri, Long.toString(id));
                } else {

                    throw new SQLiteException("Insert error:" + uri);
                }

        }

        return null;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] insertValuesArray) {

        switch (sUriMatcher.match(uri)) {
            case MOVIE_URL_QUERY:
                SQLiteDatabase localSQLiteDatabase = mHelper.getWritableDatabase();

                localSQLiteDatabase.beginTransaction();
                localSQLiteDatabase.delete(MoviesContract.MOVIE_TABLE_NAME, null, null);
                int numImages = insertValuesArray.length;
                for (int i = 0; i < numImages; i++) {
                    localSQLiteDatabase.insert(MoviesContract.MOVIE_TABLE_NAME,
                            MoviesContract.TITLE_COLUMN, insertValuesArray[i]);
                }

                localSQLiteDatabase.setTransactionSuccessful();

                localSQLiteDatabase.endTransaction();
                localSQLiteDatabase.close();


                getContext().getContentResolver().notifyChange(uri, null);

                return numImages;


        }

        return -1;

    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Delete -- unsupported operation " + uri);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {

        switch (sUriMatcher.match(uri)) {

            case MOVIE_URL_QUERY:

                SQLiteDatabase localSQLiteDatabase = mHelper.getWritableDatabase();

                int rows = localSQLiteDatabase.update(
                        MoviesContract.MOVIE_TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);

                if (0 != rows) {
                    getContext().getContentResolver().notifyChange(uri, null);
                    return rows;
                } else {

                    throw new SQLiteException("Update error:" + uri);
                }
        }

        return -1;
    }
}
