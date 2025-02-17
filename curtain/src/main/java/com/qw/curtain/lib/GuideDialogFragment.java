package com.qw.curtain.lib;

import static com.qw.curtain.lib.InnerUtils.getStatusBarHeight;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.qw.curtain.lib.dialog.NoInterceptActivityDialog;
import com.qw.curtain.lib.dialog.NoInterceptViewAlertDialog;

/**
 * @author cd5160866
 */
public class GuideDialogFragment extends DialogFragment implements IGuide, DialogInterface.OnShowListener {

    private static final int MAX_CHILD_COUNT = 2;

    private static final int GUIDE_ID = 0x3;

    private static final String EXTRA_PARAM = "extra_param";

    private FrameLayout contentView;
    private LinearLayoutCompat dialogLayout;

    private Dialog dialog;

    private int topLayoutRes = 0;

    private GuideView guideView;

    private Curtain.Param param;
    private DialogInterface.OnShowListener onShowListener;

    public static GuideDialogFragment newInstance(Curtain.Param param, Context context) {
        //build dialogFragment
        GuideDialogFragment guider = new GuideDialogFragment();
        guider.setParam(param);
        guider.setCancelable(param.cancelBackPressed);
        guider.setTopViewRes(param.topLayoutRes);

        //build contentView
        GuideView guideView = new GuideView(context);
        guideView.setCurtainColor(param.curtainColor);
        SparseArray<HollowInfo> hollows = param.hollows;
        HollowInfo[] tobeDraw = new HollowInfo[hollows.size()];
        for (int i = 0; i < hollows.size(); i++) {
            tobeDraw[i] = hollows.valueAt(i);
        }
        guideView.setHollowInfo(tobeDraw);
        guider.setGuideView(guideView);
        return guider;
    }

    public void show(FragmentActivity activity) {
        show(activity, null);
    }

    public void show(FragmentActivity activity, DialogInterface.OnShowListener onShowListener) {
        guideView.setId(GUIDE_ID);
        Context context = activity;
        this.contentView = new FrameLayout(context);

        this.onShowListener = onShowListener;

        dialogLayout = new LinearLayoutCompat(context);
        dialogLayout.setOrientation(LinearLayoutCompat.VERTICAL);

        View statusBarView = new View(context);
        statusBarView.setLayoutParams(new LinearLayoutCompat.LayoutParams(
                LinearLayoutCompat.LayoutParams.MATCH_PARENT,
                getStatusBarHeight(context)
        ));
        statusBarView.setBackgroundColor(param.curtainColor);
        dialogLayout.addView(statusBarView);
        dialogLayout.addView(contentView);

        this.contentView.addView(guideView);
        if (topLayoutRes != 0) {
            updateTopView();
        }
        show(activity.getSupportFragmentManager(), Constance.CURTAIN_FRAGMENT);
    }

    public void setParam(Curtain.Param param) {
        this.param = param;
    }

    public void setTopViewRes(int topLayoutRes) {
        this.topLayoutRes = topLayoutRes;
    }

    public void setGuideView(GuideView guideView) {
        this.guideView = guideView;
    }

    void updateContent() {
        contentView.removeAllViews();
        contentView.addView(guideView);
        updateTopView();
    }

    @Override
    public void show(@NonNull FragmentManager manager, String tag) {
        try {
            super.show(manager, tag);
        } catch (Exception e) {
            manager.beginTransaction()
                    .add(this, tag)
                    .commitAllowingStateLoss();
        }
    }

    @Override
    public void updateHollows(HollowInfo... hollows) {
        GuideView guideView = contentView.findViewById(GUIDE_ID);
        if (null != guideView) {
            guideView.setHollowInfo(hollows);
        }
    }

    @Override
    public void updateTopView(@LayoutRes int layoutId) {
        if (null == contentView || getActivity() == null) {
            return;
        }
        setTopViewRes(layoutId);
        updateTopView();
    }

    @Override
    public <T extends View> T findViewByIdInTopView(int id) {
        if (null == contentView) {
            return null;
        }
        return contentView.findViewById(id);
    }

    @Override
    public void dismissGuide() {
        dismissAllowingStateLoss();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (dialog == null) {
            if (param == null) {
                dismissGuide();
                return super.onCreateDialog(savedInstanceState);
            }

            boolean isInterceptAll = param.isInterceptTouchEvent && param.isInterceptTarget;
            if (isInterceptAll) {
                dialog = new Dialog(requireActivity(), R.style.CurtainTransparentDialog) {
                    @Override
                    protected void onCreate(Bundle savedInstanceState) {
                        super.onCreate(savedInstanceState);
                        Window window = getWindow();
                        if (window != null) {
                            window.getDecorView().setSystemUiVisibility(
                                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                        }
                    }
                };
            } else {
                dialog = !param.isInterceptTouchEvent ?
                        new NoInterceptActivityDialog(requireActivity(), R.style.CurtainTransparentDialog) :
                        new NoInterceptViewAlertDialog(requireActivity(), R.style.CurtainTransparentDialog, param.hollows);
            }
            dialog.setContentView(dialogLayout);
            setAnimation(dialog);
            dialog.setOnShowListener(this);
        }
        return dialog;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        try {
            super.onActivityCreated(savedInstanceState);
        } catch (Exception e) {
            return;
        }
        if (null == param) {
            dismissGuide();
        } else if (null != param.callBack) {
            param.callBack.onShow(this);
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (null != param && null != param.callBack) {
            param.callBack.onDismiss(this);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (dialog != null) {
            dialog = null;
        }
    }

    private void setAnimation(Dialog dialog) {
        if (dialog == null || dialog.getWindow() == null) {
            return;
        }
        if (param.animationStyle == Constance.STATE_NO_NEED_SET) {
            return;
        }
        //set the default animation if not setted
        dialog.getWindow().setWindowAnimations(
                param.animationStyle == Constance.STATE_NOT_SET ?
                        R.style.dialogWindowAnim
                        : param.animationStyle
        );
    }

    private void updateTopView() {
        if (contentView.getChildCount() == MAX_CHILD_COUNT) {
            contentView.removeViewAt(1);
        }
        guideView.setCurtainColor(param.curtainColor);
        LayoutInflater.from(contentView.getContext()).inflate(topLayoutRes, contentView, true);
        //on top view click listeners
        SparseArray<OnViewInTopClickListener> listeners = param.topViewOnClickListeners;
        int onClickListenersSize = listeners.size();
        for (int i = 0; i < onClickListenersSize; i++) {
            int idRes = listeners.keyAt(i);
            final OnViewInTopClickListener<Object> listener = listeners.valueAt(i);
            View view = contentView.findViewById(idRes);
            if (null == view) {
                throw new NullPointerException("the target view was not find in the top view, check your setTopView layout res first");
            }
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClick(v, GuideDialogFragment.this);
                }
            });
        }
    }

    @Override
    public void onShow(DialogInterface dialog) {
        if (onShowListener != null) {
            onShowListener.onShow(dialog);
        }
    }
}
