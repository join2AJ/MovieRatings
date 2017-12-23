package com.fenchtose.movieratings.features.likespage

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.fenchtose.movieratings.MovieRatingsApplication
import com.fenchtose.movieratings.R
import com.fenchtose.movieratings.base.BaseFragment
import com.fenchtose.movieratings.base.RouterPath
import com.fenchtose.movieratings.features.searchpage.SearchPageAdapter
import com.fenchtose.movieratings.model.Movie
import com.fenchtose.movieratings.model.Sort
import com.fenchtose.movieratings.model.api.provider.DbFavoriteMovieProvider
import com.fenchtose.movieratings.model.db.like.DbLikeStore
import com.fenchtose.movieratings.model.image.GlideLoader
import com.fenchtose.movieratings.model.preferences.SettingsPreferences
import com.fenchtose.movieratings.widgets.ThemedSnackbar

class LikesPageFragment: BaseFragment(), LikesPage {

    override fun getScreenTitle() = R.string.likes_page_title

    private var root: ViewGroup? = null
    private var recyclerView: RecyclerView? = null
    private var adapter: SearchPageAdapter? = null

    private var presenter: LikesPresenter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        val dao = MovieRatingsApplication.database.movieDao()
        val favoriteProvider = DbFavoriteMovieProvider(dao)
        val likeStore = DbLikeStore(MovieRatingsApplication.database.favDao())
        val userPreferences = SettingsPreferences(context)
        presenter = LikesPresenter(favoriteProvider, likeStore, userPreferences)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        root = inflater.inflate(R.layout.likes_page_layout, container, false) as ViewGroup
        return root!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recyclerview)

        val adapter = SearchPageAdapter(context, GlideLoader(Glide.with(this)),
                object : SearchPageAdapter.AdapterCallback {
                    override fun onLiked(movie: Movie) {
                        presenter?.unlike(movie)
                    }

                    override fun onClicked(movie: Movie, sharedElement: Pair<View, String>?) {
                        // TODO check for api compatibility
                        presenter?.openMovie(movie, sharedElement)
                    }
                })

        adapter.setHasStableIds(true)

        recyclerView?.let {
            it.adapter = adapter
            it.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            it.visibility = View.GONE
        }

        this.adapter = adapter

        presenter?.attachView(this)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter?.detachView(this)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        var consumed = true
        when(item?.itemId) {
            R.id.action_sort_alphabetically -> presenter?.sort(Sort.ALPHABETICAL)
//            R.id.action_sort_genre -> presenter?.sort(Sort.GENRE)
            R.id.action_sort_year -> presenter?.sort(Sort.YEAR)
            else -> consumed = false
        }

        return if (consumed) true else super.onOptionsItemSelected(item)
    }

    override fun setData(movies: ArrayList<Movie>) {
        recyclerView?.visibility = View.VISIBLE
        adapter?.data = movies
        adapter?.notifyDataSetChanged()
    }

    override fun showRemoved(movie: Movie, index: Int) {
        adapter?.notifyItemRemoved(index)
        showMovieRemoved(movie, index)
    }

    override fun showAdded(movie: Movie, index: Int) {
        adapter?.notifyItemInserted(index)
        recyclerView?.post {
            recyclerView?.scrollToPosition(index)
        }
    }

    private fun showMovieRemoved(movie: Movie, index: Int) {
        showSnackbarWithAction(
                getString(R.string.movie_unliked_snackbar_content, movie.title),
                R.string.undo_action,
                View.OnClickListener {
                    presenter?.undoUnlike(movie, index)
                }
        )
    }

    override fun canGoBack() = true

    class LikesPath : RouterPath<LikesPageFragment>() {
        override fun createFragmentInstance(): LikesPageFragment {
            return LikesPageFragment()
        }

        override fun showMenuIcons(): IntArray {
            return intArrayOf(R.id.action_sort)
        }
    }
}