package tikal.com.myapplication;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import tikal.com.myapplication.data.MoviesContract;

/**
 * A fragment representing a single Movie detail screen.
 * This fragment is either contained in a {@link MovieListActivity}
 * in two-pane mode (on tablets) or a {@link MovieDetailActivity}
 * on handsets.
 */
public class MovieDetailFragment extends Fragment {

    private String mMovieID;
    private String mPosterURL;
    private String mDescriptionText;
    private TextView mDescription;
    private ImageView mPoster;
    private TextView mYear;
    private TextView mScore;
    private TextView mRunningTime;
    private ListView mTrailersListView;
    private String mYearText;
    private String mScoreText;
    private String mRunningTimeText;
    private String mTrailersText;

    public MovieDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMovieID = getArguments().getString(MoviesContract.MOVIE_ID);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.movie_detail, container, false);
        mLoadFromDbTask.execute();
        mDescription = (TextView) rootView.findViewById(R.id.description);
        mPoster = (ImageView) rootView.findViewById(R.id.poster);
        mYear = (TextView) rootView.findViewById(R.id.year);
        mScore = (TextView) rootView.findViewById(R.id.score);
        mRunningTime = (TextView) rootView.findViewById(R.id.runningTime);
        mTrailersListView = (ListView) rootView.findViewById(R.id.trailersListView);
        mTrailersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Trailer trailer = (Trailer)mTrailersListView.getAdapter().getItem(position);
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v=" + trailer.url)));

            }
        });
        return rootView;
    }

    AsyncTask<Void, Void, Void> mLoadFromDbTask = new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
            Cursor cursor = getContext().getContentResolver().query(
                    MoviesContract.MOVIE_TABLE_CONTENTURI,
                    null,
                    MoviesContract.MOVIE_ID + " = ? ",
                    new String[]{mMovieID},
                    null,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                int count = cursor.getCount();
                Log.d(getClass().getSimpleName(), "count " + count);
                mPosterURL = cursor.getString(cursor.getColumnIndex(MoviesContract.IMAGE_THUMBURL_COLUMN));
                mDescriptionText = cursor.getString(cursor.getColumnIndex(MoviesContract.DESCRIPTION_COLUMN));
                mYearText = cursor.getString(cursor.getColumnIndex(MoviesContract.RELEASE_DATE_COLUMN));
                mRunningTimeText = cursor.getString(cursor.getColumnIndex(MoviesContract.RUNNING_TIME_COLUMN));
                mScoreText = cursor.getString(cursor.getColumnIndex(MoviesContract.SCORE_COLUMN));
                mTrailersText = cursor.getString(cursor.getColumnIndex(MoviesContract.TRAILERS_COLUMN));
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mDescription.setText(mDescriptionText);
            Picasso.with(getContext())
                    .load("http://image.tmdb.org/t/p/w185/" + mPosterURL)
                    .into(mPoster);
            if (mScoreText != null) mScore.setText(mScoreText + "/10");
            if (mRunningTimeText != null) mRunningTime.setText(mRunningTimeText + "min");
            if (mYearText != null && mYearText.length() > 0 && mYearText.contains("-")) {
                mYear.setText(mYearText.split("-")[0]);
            }
            if(mTrailersText != null && mTrailersText.length() > 0 ) {
                String[] trailersArray = mTrailersText.split(",");
                List<Trailer> trailers = new ArrayList<Trailer>();
                for(int i = 0; i < trailersArray.length; i++) {
                    String trailerItem = trailersArray[i];
                    String[] trailerSplit = trailerItem.split("\\|");
                    trailers.add(new Trailer(trailerSplit[1], trailerSplit[0]));
                }
                mTrailersListView.setAdapter(new TrailersAdapter(getContext(), 0, trailers));
            }
        }
    };
    public class Trailer {
        public String title;
        public String url;

        public Trailer(String title, String url) {
            this.title = title;
            this.url = url;
        }
    }
    public class TrailersAdapter extends ArrayAdapter<Trailer> {


        public TrailersAdapter(Context context, int resource, List<Trailer> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            Trailer trailer = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.trailer_item, parent, false);
            }
            TextView title = (TextView) convertView.findViewById(R.id.trailerTitle);
            title.setText(trailer.title);
            return convertView;
        }

    }
}
