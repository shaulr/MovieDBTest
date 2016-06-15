package tikal.com.myapplication.data;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import tikal.com.myapplication.MovieDetailActivity;
import tikal.com.myapplication.MovieListActivity;
import tikal.com.myapplication.R;

/**
 * Created by shaulr on 14/06/2016.
 */
public class MoviesCursorAdapter extends RecyclerView.Adapter<MoviesCursorAdapter.VH> {
    private Cursor mCursor;
    private Context mContext;
    private DataSetObserver mDataSetObserver;
    private boolean mDataValid;

    private int mRowIdColumn;

    public MoviesCursorAdapter(Context context, Cursor cursor) {
        mCursor = cursor;
        mContext = context;
        mDataValid = cursor != null;
        mRowIdColumn = mDataValid ? mCursor.getColumnIndex("_id") : -1;
        mDataSetObserver = new NotifyingDataSetObserver();
        if (mCursor != null) {
            mCursor.registerDataSetObserver(mDataSetObserver);
        }
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.movie_list_content, parent, false);

        final VH vh = new VH(v);
        int expectedWidth = parent.getMeasuredWidth() / 3 ;


        double ratioHW = 185/278;
        vh.mLayout.setMinimumWidth(expectedWidth);
        vh.mLayout.setMinimumHeight((int)(expectedWidth * ratioHW));

        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = vh.getAdapterPosition();
                mCursor.moveToPosition(position);
                String movieID = mCursor.getString(mCursor.getColumnIndex(MoviesContract.MOVIE_ID));
                if(movieID != null &&movieID.length() > 0) {
                    if(isTablet(mContext) && !isPortrait()) {
                        showDetailsTablet(movieID);
                    } else {
                        Intent intent = new Intent(mContext, MovieDetailActivity.class);
                        intent.putExtra(MoviesContract.MOVIE_ID, movieID);
                        mContext.startActivity(intent);
                    }
                }
            }
        });
        return vh;
    }

    private void showDetailsTablet(String movieID) {
        if(mContext instanceof MovieListActivity) {
            ((MovieListActivity)mContext).navigateTablet(movieID);
        }
    }
    public boolean isPortrait() {
        return mContext.getResources().getDisplayMetrics().widthPixels <= mContext.getResources().getDisplayMetrics().heightPixels;
    }
    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }
    @Override
    public void onBindViewHolder(VH holder, int position) {
        mCursor.moveToPosition(position);


        String url = mCursor.getString(mCursor.getColumnIndex(MoviesContract.IMAGE_THUMBURL_COLUMN));
        Picasso.with(mContext)
                .load("http://image.tmdb.org/t/p/w185/" + url)
                .placeholder(android.R.drawable.ic_menu_compass)
                .into(holder.mPoster);
    }

    @Override
    public int getItemCount() {
        if (mDataValid && mCursor != null) {
            return mCursor.getCount();
        }
        return 0;
    }


    public void changeCursor(Cursor cursor) {
        Cursor old = swapCursor(cursor);
        if (old != null) {
            old.close();
        }
    }

    public Cursor swapCursor(Cursor newCursor) {
        if (newCursor == mCursor) {
            return null;
        }
        final Cursor oldCursor = mCursor;
        if (oldCursor != null && mDataSetObserver != null) {
            oldCursor.unregisterDataSetObserver(mDataSetObserver);
        }
        mCursor = newCursor;
        if (mCursor != null) {
            if (mDataSetObserver != null) {
                mCursor.registerDataSetObserver(mDataSetObserver);
            }
            mRowIdColumn = newCursor.getColumnIndexOrThrow("_id");
            mDataValid = true;
            notifyDataSetChanged();
        } else {
            mRowIdColumn = -1;
            mDataValid = false;
            notifyDataSetChanged();
            //There is no notifyDataSetInvalidated() method in RecyclerView.Adapter
        }
        return oldCursor;
    }

    @Override
    public long getItemId(int position) {
        if (mDataValid && mCursor != null && mCursor.moveToPosition(position)) {
            return mCursor.getLong(mRowIdColumn);
        }
        return 0;
    }

    @Override
    public void setHasStableIds(boolean hasStableIds) {
        super.setHasStableIds(true);
    }

    private class NotifyingDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            super.onChanged();
            mDataValid = true;
            notifyDataSetChanged();
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
            mDataValid = false;
            notifyDataSetChanged();
            //There is no notifyDataSetInvalidated() method in RecyclerView.Adapter
        }
    }

    public static class VH extends RecyclerView.ViewHolder {
        public ImageView mPoster;
        public ViewGroup mLayout;

        public VH(View v) {
            super(v);
            mPoster = (ImageView) v.findViewById(R.id.movie_poster);
            mLayout = (ViewGroup) v.findViewById(R.id.layout);
        }
    }

}
