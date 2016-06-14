package tikal.com.myapplication.data;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by shaulr on 14/06/2016.
 */
public class MoviesContract implements BaseColumns {
    private MoviesContract() {
    }

    public static final String SCHEME = "content";

    public static final String AUTHORITY = "tikal.com.myapplication";

    public static final Uri CONTENT_URI = Uri.parse(SCHEME + "://" + AUTHORITY);


    public static final String MIME_TYPE_ROWS =
            "vnd.android.cursor.dir/vnd.tikal.com.myapplication";

    public static final String MIME_TYPE_SINGLE_ROW =
            "vnd.android.cursor.item/vnd.tikal.com.myapplication";


    public static final String ROW_ID = BaseColumns._ID;

    public static final String MOVIE_TABLE_NAME = "MovieData";


    public static final Uri MOVIE_TABLE_CONTENTURI = Uri.withAppendedPath(CONTENT_URI, MOVIE_TABLE_NAME);



    public static final String TITLE_COLUMN = "Title";
    public static final String IMAGE_THUMBURL_COLUMN = "ThumbUrl";

    public static final String DESCRIPTION_COLUMN = "Description";
    public static final String RELEASE_DATE_COLUMN = "ReleaseDate";
    public static final String SCORE_COLUMN = "Score";
    public static final String RUNNING_TIME_COLUMN = "RunningTime";
    public static final String TRAILERS_COLUMN = "Trailers";
    public static final String TIMESTAMP_COLUMN = "SyncTimestamp";
    public static final String MOVIE_ID = "MovieID";

    public static final String DATABASE_NAME = "MoviesDataDB";

    public static final int DATABASE_VERSION = 1;
}
