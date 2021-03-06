package com.freedom.lauzy.ticktockmusic.ui.fragment;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bilibili.magicasakura.utils.ThemeUtils;
import com.freedom.lauzy.model.SongType;
import com.freedom.lauzy.ticktockmusic.R;
import com.freedom.lauzy.ticktockmusic.base.BaseFragment;
import com.freedom.lauzy.ticktockmusic.contract.RecentContract;
import com.freedom.lauzy.ticktockmusic.event.ClearRecentEvent;
import com.freedom.lauzy.ticktockmusic.function.RxBus;
import com.freedom.lauzy.ticktockmusic.model.SongEntity;
import com.freedom.lauzy.ticktockmusic.presenter.RecentPresenter;
import com.freedom.lauzy.ticktockmusic.service.MusicManager;
import com.freedom.lauzy.ticktockmusic.service.MusicUtil;
import com.freedom.lauzy.ticktockmusic.ui.adapter.RecentAdapter;
import com.freedom.lauzy.ticktockmusic.utils.CheckNetwork;
import com.lauzy.freedom.librarys.widght.TickToolbar;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import io.reactivex.disposables.Disposable;

/**
 * Desc : 最近播放Fragment
 * Author : Lauzy
 * Date : 2017/9/15
 * Blog : http://www.jianshu.com/u/e76853f863a9
 * Email : freedompaladin@gmail.com
 */
public class RecentFragment extends BaseFragment<RecentPresenter> implements RecentContract.View,
        RecentAdapter.OnRecentListener {

    @BindView(R.id.toolbar_common)
    TickToolbar mToolbarCommon;
    @BindView(R.id.rv_recent)
    RecyclerView mRvRecent;
    private List<SongEntity> mSongEntities = new ArrayList<>();
    private RecentAdapter mAdapter;

    public static RecentFragment newInstance() {
        Bundle args = new Bundle();
        RecentFragment fragment = new RecentFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Disposable disposable = RxBus.INSTANCE.doDefaultSubscribe(ClearRecentEvent.class,
                clearRecentEvent -> clearRecentData());
        RxBus.INSTANCE.addDisposable(this, disposable);
    }

    private void clearRecentData() {
        if (mSongEntities != null && mSongEntities.size() != 0) {
            ColorStateList stateList = ThemeUtils.getThemeColorStateList(mActivity, R.color.color_tab);
            new MaterialDialog.Builder(mActivity)
                    .content(R.string.clear_recent_songs)
                    .positiveColor(stateList)
                    .negativeColor(stateList)
                    .positiveText(R.string.confirm)
                    .negativeText(R.string.cancel)
                    .onPositive((dialog, which) -> mPresenter.clearRecentSongs())
                    .build()
                    .show();
        }
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_recent;
    }

    @Override
    protected void initInjector() {
        getFragmentComponent().inject(this);
    }

    @Override
    protected void initViews() {
        setToolbarPadding();
        setDrawerSync();
        mToolbarCommon.setTitle(R.string.drawer_recent);
    }

    @Override
    protected void loadData() {
        setRecyclerView();
        mPresenter.loadRecentSongs();
    }

    private void setRecyclerView() {
        mRvRecent.setLayoutManager(new LinearLayoutManager(mActivity));
        mAdapter = new RecentAdapter(R.layout.layout_song_item, mSongEntities);
        mRvRecent.setAdapter(mAdapter);
        mAdapter.setOnRecentListener(this);
    }

    /**
     * 播放当前队列的音乐，并更新最近播放列表
     *
     * @param entity   当前播放音乐
     * @param position 当前位置
     */
    @Override
    public void playRecent(SongEntity entity, int position) {
        if (entity.type.equals(SongType.LOCAL)) {
            playSong(position);
            return;
        }
        CheckNetwork.checkNetwork(mActivity, () -> playSong(position));
    }

    @Override
    public void playItemSong(SongEntity entity, int position) {
        if (entity.type.equals(SongType.LOCAL)) {
            playSong(position);
            return;
        }
        if (MusicManager.getInstance().getCurrentSong() == null) {
            CheckNetwork.checkNetwork(mActivity, () -> playSong(position));
            return;
        }
        if (entity.id == MusicManager.getInstance().getCurrentSong().id) {
            playSong(position);
            return;
        }
        CheckNetwork.checkNetwork(mActivity, () -> playSong(position));
    }

    private void playSong(int position) {
        MusicManager.getInstance().playMusic(mSongEntities, MusicUtil.getSongIds(mSongEntities), position);
        MusicManager.getInstance().setRecentUpdateListener(() -> mPresenter.loadRecentSongs());
    }

    @Override
    public void deleteSong(SongEntity songEntity, int position) {
        mPresenter.deleteRecentSong(songEntity.id, position);
    }

    @Override
    public void getRecentSongs(List<SongEntity> songEntities) {
        mSongEntities.clear();
        mSongEntities.addAll(songEntities);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void clearAllRecentSongs() {
        setEmptyView();
    }

    private void setEmptyView() {
        mSongEntities.clear();
        mAdapter.notifyDataSetChanged();
        mAdapter.setEmptyView(R.layout.layout_empty, mRvRecent);
    }

    @Override
    public void emptyView() {
        setEmptyView();
    }

    @Override
    public void deleteSongSuccess(int position) {
        mSongEntities.remove(position);
        mAdapter.notifyItemRemoved(position);
        if (mSongEntities.isEmpty()) {
            mAdapter.setEmptyView(R.layout.layout_empty, mRvRecent);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RxBus.INSTANCE.dispose(this);
    }

}
