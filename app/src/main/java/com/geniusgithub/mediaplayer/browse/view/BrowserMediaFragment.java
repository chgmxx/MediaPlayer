package com.geniusgithub.mediaplayer.browse.view;


import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.geniusgithub.mediaplayer.DialogFactory;
import com.geniusgithub.mediaplayer.R;
import com.geniusgithub.mediaplayer.base.BaseFragment;
import com.geniusgithub.mediaplayer.base.IToolBar;
import com.geniusgithub.mediaplayer.base.ToolEntry;
import com.geniusgithub.mediaplayer.browse.BrowseContract;
import com.geniusgithub.mediaplayer.browse.BrowsePresenter;
import com.geniusgithub.mediaplayer.dlna.model.MediaItem;

import org.cybergarage.upnp.Device;

import java.util.ArrayList;
import java.util.List;


public class BrowserMediaFragment extends BaseFragment{

    public static final String TAG = BrowserMediaFragment.class.getSimpleName();

    private View mRootView;
    private BrowsePresenter mBrowsePresenter;
    private BrowseContract.IView mBrowseView;


    private IToolBar mExternToolbar;
    public void bindToolbar(IToolBar toolbar){
        mExternToolbar = toolbar;
    }



    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dms_layout, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        onUIReady(view);
    }

    @Override
    public void onPause() {
        super.onPause();
        DialogFactory.closeAllDialog();
    }

    @Override
    public void onDestroy(){
        mBrowsePresenter.onUiDestroy();
        super.onDestroy();

    }


    public boolean onBackPressed(){
        return mBrowsePresenter.onBackPressed();
    }

    private void onUIReady(View view){
        mRootView = view.findViewById(R.id.ll_root);

        mBrowsePresenter = new BrowsePresenter();
        mBrowseView = new BrowserView(getActivity());
        mBrowseView.setupView(mRootView);
        mBrowsePresenter.bindView(mBrowseView);
        mBrowsePresenter.onUiCreate(getActivity());
    }


    public void updateToolTitle(String title){
        if (mExternToolbar != null){
            ToolEntry entry = new ToolEntry();
            entry.title = title;
            mExternToolbar.updateToolTitle(entry);
        }
    }









    public class BrowserView implements BrowseContract.IView{

        private Context mContext;

        private RecyclerView mDevListView;
        private DeviceAdapter mDevAdapter;
        private  OnDeviceItemClick mOnDeviceItemClick;

        private RecyclerView mContentListView;
        private ContentAdapter mContentAdapter;
        private OnContentItemClick mOnContentItemClick;


        private ProgressDialog mProgressDialog;

        private BrowseContract.IPresenter mIBrowsePresenter;

        public BrowserView(Context context){
            mContext = context;
        }


        @Override
        public void bindPresenter(BrowseContract.IPresenter presenter) {
            mIBrowsePresenter = presenter;
        }

        @Override
        public void setupView(View rootView) {
            initView(rootView);
        }


        @Override
        public void showProgress(boolean bShow)
        {
            mProgressDialog.dismiss();
            if (bShow){
                mProgressDialog.show();
            }

        }

        @Override
        public void showDeviceList(boolean bShow){
            mDevListView.setVisibility(bShow ? View.VISIBLE : View.GONE);
        }


        @Override
        public void showItemList(boolean bShow){
            mContentListView.setVisibility(bShow ? View.VISIBLE : View.GONE);
        }

        @Override
        public void showDeviceDetail(Device device) {
            DialogFactory.popupDeviceDetailDialog(getActivity(), device);
        }

        @Override
        public void updateDeviceList(List<Device> devices)
        {
            mDevAdapter.refreshDevices(devices);
        }

        @Override
        public void updateItemList(List<MediaItem>  contentItem)
        {
            mContentAdapter.refreshData(contentItem);
        }

        @Override
        public void updateToolTitle(String title) {
            BrowserMediaFragment.this.updateToolTitle(title);
        }

        private class OnDeviceItemClick  implements DeviceItemViewHolder.onItemClickListener{
            @Override
            public void onItemClick(Device device) {
                mIBrowsePresenter.enterDevice(device);
            }

            @Override
            public void onDetailClick(Device device) {
                showDeviceDetail(device);
            }
        }


        private class OnContentItemClick implements ContentItemViewHolder.onItemClickListener{

            @Override
            public void onItemClick(int pos, MediaItem item){
                mIBrowsePresenter.browseItem(pos, item);

            }
        }

        private void initView(View view){



            mDevListView = (RecyclerView) view.findViewById(R.id.device_list);
            mDevListView.setHasFixedSize(true);
            mDevListView.setNestedScrollingEnabled(false);
            LinearLayoutManager mLayoutManager1 = new LinearLayoutManager(mContext);
            mDevListView.setLayoutManager(mLayoutManager1);

            mDevAdapter = new DeviceAdapter(mContext, new ArrayList<Device>());
            mOnDeviceItemClick = new OnDeviceItemClick();
            mDevAdapter.setOnItemClickListener(mOnDeviceItemClick);
            mDevListView.setAdapter(mDevAdapter);

            mContentListView = (RecyclerView) view.findViewById(R.id.content_list);
            mContentListView.setHasFixedSize(true);
            mContentListView.setNestedScrollingEnabled(false);
            LinearLayoutManager  mLayoutManager2 = new LinearLayoutManager(mContext);
            mContentListView.setLayoutManager(mLayoutManager2);
            mOnContentItemClick = new OnContentItemClick();
            mContentAdapter = new ContentAdapter(mContext,  new ArrayList<MediaItem>());
            mContentAdapter.setOnItemClickListener(mOnContentItemClick);
            mContentListView.setAdapter(mContentAdapter);
            mContentListView.addOnScrollListener(mScrollListener);

            mProgressDialog = new ProgressDialog(mContext);
            mProgressDialog.setMessage("Loading...");


        }

        private RecyclerView.OnScrollListener mScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    switch (newState) {
                        case RecyclerView.SCROLL_STATE_SETTLING:
                        //    AlwaysLog.d(TAG, "newState = SCROLL_STATE_SETTLING");
                            mContentAdapter.setFlagBusy(true);
                            break;
                        case RecyclerView.SCROLL_STATE_IDLE:
                        //    AlwaysLog.d(TAG, "newState = SCROLL_STATE_IDLE");
                            mContentAdapter.setFlagBusy(false);
                            mContentAdapter.notifyDataSetChanged();
                            break;

                        case RecyclerView.SCROLL_STATE_DRAGGING:
                        //    AlwaysLog.d(TAG, "newState = SCROLL_STATE_DRAGGING");
                            mContentAdapter.setFlagBusy(false);
                            break;
                        default:
                            break;
                    }
            }
        };

    }

}

