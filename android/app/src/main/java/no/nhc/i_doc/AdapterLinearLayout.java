package no.nhc.i_doc;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.View;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.Adapter;

/**
 *  A class that behaves a lot like ListView, using adapter and all,
 *  but still lays out its children like a normal LinearLayout.
 */
public class AdapterLinearLayout extends LinearLayout {
    public interface OnItemClickListener {
        void onItemClick(AdapterLinearLayout parent, View view, int position);
    }

    private Adapter mAdapter = null;
    private boolean mInvalid = false;
    private OnItemClickListener mOnItemClickListener = null;

    private View.OnClickListener mChildClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            onChildClicked(v);
        }
    };

    public AdapterLinearLayout(Context context, AttributeSet set) {
        super(context, set);
    }

    public void setAdapter(Adapter adapter) {
        mAdapter = adapter;
        mAdapter.registerDataSetObserver(new DataSetObserver() {
            public void onChanged() {
                rebuild();
            }

            public void onInvalidated() {
                mInvalid = true;
            }
        });
        rebuild();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    private void onChildClicked(View child) {
        if (mOnItemClickListener != null) {
            mOnItemClickListener.onItemClick(
                this, child,
                ((Integer)child.getTag(R.id.TAG_INDEX)).intValue());
        }
    }

    private View configureView(View view, final int index) {
        view.setTag(R.id.TAG_INDEX, new Integer(index));

        if (!view.hasOnClickListeners()) {
            view.setOnClickListener(mChildClickListener);
        }

        return view;
    }

    private void rebuild() {
        if (mAdapter == null) return;

        View view = null;

        final int viewCount = getChildCount();
        final int adapterCount = mAdapter.getCount();
        int i = 0;

        for (; i < Math.min(viewCount, adapterCount); ++i) {
            // reuse views
            View existing = getChildAt(i);

            if ((view = mAdapter.getView(i, getChildAt(i), this)) != null) {
                removeView(existing);
                addView(configureView(view, i), i);
            }
        }

        if (viewCount > adapterCount) {
            // remove views
            for (; i < viewCount; ++i) {
                removeViewAt(i);
            }
        } else {
            // add views
            for (; i < adapterCount; ++i) {
                if ((view = mAdapter.getView(i, null, this)) != null) {
                    addView(configureView(view, i));
                }
            }
        }
    }
}
