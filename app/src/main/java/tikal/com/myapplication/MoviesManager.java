package tikal.com.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.StringBuilderPrinter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import tikal.com.myapplication.data.MoviesContract;
import tikal.com.myapplication.data.MoviesCursorAdapter;

/**
 * Created by shaulr on 14/06/2016.
 */
public class MoviesManager implements LoaderManager.LoaderCallbacks<Cursor> {
    private static MoviesManager ourInstance = new MoviesManager();
    private final OkHttpClient client = new OkHttpClient();
    private final static String BASE_URL = "http://api.themoviedb.org/3/";
    private final static String TAG = "MoviesManager";
    private Context mContext;
    private MoviesCursorAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private Cursor mMoviesLoopCursor;
    private boolean mIsSyncing = false;
    public static MoviesManager getInstance() {
        return ourInstance;
    }

    private MoviesManager() {
    }

    public void setContext(Context context) {
        mContext = context;
    }

    public void sync() {
        if(!isConnected()) return;
        if(!mIsSyncing) {
            mIsSyncing = true;
            getDiscover();
        }
    }

    public void getDiscover() {
        request("discover/movie", new IResponseHandler() {
            @Override
            public void onResponse(ResponseBody response) {
                try {
                    JSONObject jsonObject = new JSONObject(response.string());
                    JSONArray results = jsonObject.getJSONArray("results");
                    ContentValues[] bulkToInsert;
                    List<ContentValues> mValueList = new ArrayList<ContentValues>();

                    for (int i = 0; i < results.length(); i++) {
                        JSONObject result = (JSONObject) results.get(i);
                        ContentValues mNewValues = new ContentValues();
                        mNewValues.put(MoviesContract.TITLE_COLUMN, result.getString("title"));
                        mNewValues.put(MoviesContract.IMAGE_THUMBURL_COLUMN, result.getString("poster_path"));
                        mNewValues.put(MoviesContract.DESCRIPTION_COLUMN, result.getString("overview"));
                        mNewValues.put(MoviesContract.SCORE_COLUMN, result.getString("vote_average"));
                        mNewValues.put(MoviesContract.MOVIE_ID, result.getString("id"));

                        mValueList.add(mNewValues);
                    }

                    bulkToInsert = new ContentValues[mValueList.size()];
                    mValueList.toArray(bulkToInsert);
                    mContext.getContentResolver().bulkInsert(MoviesContract.MOVIE_TABLE_CONTENTURI, bulkToInsert);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {

                        @Override
                        public void run() {
                            mAdapter.notifyDataSetChanged();
                        }
                    });
                    //basic info done, get extra info
                    getExtraInfo();

                } catch (JSONException e) {
                    Log.e(TAG, "error parsing JSON");
                } catch (IOException e) {
                    Log.e(TAG, "error reading response");
                }

            }

            @Override
            public void onError() {
                Log.e(TAG, "error network response");
            }
        });
    }

    public void getExtraInfo() {
        mMoviesLoopCursor = mContext.getContentResolver().query(
                MoviesContract.MOVIE_TABLE_CONTENTURI,
                new String[] {MoviesContract.MOVIE_ID},
                null,
                null,
                null);
        if(mMoviesLoopCursor != null && mMoviesLoopCursor.moveToFirst()) {
            String movieID = mMoviesLoopCursor.getString(mMoviesLoopCursor.getColumnIndex(MoviesContract.MOVIE_ID));
            getMovieDetails(movieID);

        }
    }

    private void nextMovieExtraInfo() {
        if(mMoviesLoopCursor.moveToNext()) {
            String movieID = mMoviesLoopCursor.getString(mMoviesLoopCursor.getColumnIndex(MoviesContract.MOVIE_ID));
            getMovieDetails(movieID);
        } else {
            mIsSyncing = false;
            mMoviesLoopCursor.close();
        }
    }

    public void getMovieDetails(final String movieID) {
        request("movie/" + movieID, new IResponseHandler() {
            @Override
            public void onResponse(ResponseBody response) {
                try {
                    JSONObject result = new JSONObject(response.string());

                    ContentValues newValues = new ContentValues();

                    //not needed but could have updated info
                    newValues.put(MoviesContract.TITLE_COLUMN, result.getString("title"));
                    newValues.put(MoviesContract.IMAGE_THUMBURL_COLUMN, result.getString("poster_path"));
                    newValues.put(MoviesContract.DESCRIPTION_COLUMN, result.getString("overview"));
                    newValues.put(MoviesContract.SCORE_COLUMN, result.getString("vote_average"));
                    newValues.put(MoviesContract.MOVIE_ID, result.getString("id"));
                    //this is what we don't have
                    newValues.put(MoviesContract.RUNNING_TIME_COLUMN, result.getInt("runtime"));
                    newValues.put(MoviesContract.RELEASE_DATE_COLUMN, result.getString("release_date"));


                    String[] whereParams = new String[1];
                    whereParams[0] = movieID;
                    mContext.getContentResolver().update(
                            MoviesContract.MOVIE_TABLE_CONTENTURI,
                            newValues,
                            MoviesContract.MOVIE_ID + " = ?",
                            whereParams
                    );
                    //get trailers
                    getMovieTrailers(movieID);
                } catch (JSONException e) {
                    Log.e(TAG, "error parsing JSON");
                } catch (IOException e) {
                    Log.e(TAG, "error reading response");
                }

            }

            @Override
            public void onError() {
                Log.e(TAG, "error network response");
            }
        });
    }

    public void getMovieTrailers(final String movieID) {
        request("movie/" + movieID + "/videos" , new IResponseHandler() {
                    @Override
                    public void onResponse(ResponseBody response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response.string());
                            JSONArray results = jsonObject.getJSONArray("results");
                            StringBuilder sb = new StringBuilder();

                            for (int i = 0; i < results.length(); i++) {
                                JSONObject result = (JSONObject) results.get(i);
                                sb.append(result.getString("key") + "|" + result.getString("name"));
                                if (i < results.length() - 1 ) {
                                    sb.append(",");
                                }
                            }
                            ContentValues newValues = new ContentValues();
                            newValues.put(MoviesContract.TRAILERS_COLUMN, sb.toString());
                            String[] whereParams = new String[1];
                            whereParams[0] = movieID;
                            mContext.getContentResolver().update(
                                    MoviesContract.MOVIE_TABLE_CONTENTURI,
                                    newValues,
                                    MoviesContract.MOVIE_ID + " = ?",
                                    whereParams
                            );
                            //next movie
                            nextMovieExtraInfo();
                        } catch (JSONException e) {
                            Log.e(TAG, "error parsing JSON");
                        } catch (IOException e) {
                            Log.e(TAG, "error reading response");
                        }

                    }

                    @Override
                    public void onError() {
                        Log.e(TAG, "error network response");
                    }
                }

        );
    }

    private void request(String api, @NonNull final IResponseHandler responseHandler) {
        Request request = new Request.Builder()
                .url(BASE_URL + api + "?api_key=822f63a6cb98ddcd0aed976231869515")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    responseHandler.onResponse(response.body());
                } else {
                    responseHandler.onError();
                }
            }
        });
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader = new CursorLoader(
                mContext,
                MoviesContract.MOVIE_TABLE_CONTENTURI,
                null,
                null,
                null,
                null
        );
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (mAdapter == null) {
            mAdapter = new MoviesCursorAdapter(mContext, data);
            mRecyclerView.setAdapter(mAdapter);
        } else {
            mAdapter.changeCursor(data);
        }
        if (data.getCount() == 0) {
            sync();
        } else {
            new Handler(Looper.getMainLooper()).post(new Runnable() {

                @Override
                public void run() {
                    mAdapter.notifyDataSetChanged();
                }
            });
        }
        ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);
        exec.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                sync();
            }
        }, 0, 10, TimeUnit.MINUTES);
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    public void setRecyclerView(RecyclerView recyclerView) {
        this.mRecyclerView = recyclerView;
    }

    public boolean isConnected() {
        ConnectivityManager cm =
                (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

    }

    public interface IResponseHandler {
        public void onResponse(ResponseBody response);

        public void onError();
    }
}
