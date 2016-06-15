package tikal.com.myapplication;



import android.os.Bundle;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.GridLayoutManager;

import tikal.com.myapplication.data.MoviesContract;
import tikal.com.myapplication.data.MoviesCursorAdapter;


public class MovieListActivity extends AppCompatActivity {


    private boolean mTwoPane;
    private RecyclerView mRecyclerView;
    private MoviesCursorAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        MoviesManager.getInstance().setContext(this);
        MoviesManager.getInstance().setRecyclerView(mRecyclerView);

        mRecyclerView = (RecyclerView)findViewById(R.id.movie_list);
        mRecyclerView.setLayoutManager(new GridLayoutManager(MovieListActivity.this, 3));

        assert mRecyclerView != null;
        MoviesManager.getInstance().setRecyclerView(mRecyclerView);
        if (findViewById(R.id.movie_detail_container) != null) {
            mTwoPane = true;
        }
        getSupportLoaderManager().initLoader(0, null, MoviesManager.getInstance());

    }

    public void navigateTablet(String movieID) {
        MovieDetailFragment fragment = new MovieDetailFragment();
        Bundle args = new Bundle();
        args.putString(MoviesContract.MOVIE_ID, movieID);
        fragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.movie_detail_container, fragment)
                .commit();
    }
}
