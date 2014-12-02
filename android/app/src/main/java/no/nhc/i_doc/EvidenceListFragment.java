package no.nhc.i_doc;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;

import java.text.DateFormat;
import java.util.Date;

class EvidenceAdapter extends BaseAdapter {
    static final String TAG = "EvidenceAdapter";
    private final DocumentDB.List evidenceList;

    public EvidenceAdapter(DocumentDB.List evidenceList) {
        this.evidenceList = evidenceList;
        this.evidenceList.setListener(new DocumentDB.Listener() {
            @Override
            public void changed() {
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getCount() {
        return evidenceList.getCount();
    }

    @Override
    public Object getItem(int i) {
        return evidenceList.getDocument(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) parent.getContext().
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.item_evidence, parent, false);
        }

        Document document = evidenceList.getDocument(position);

        TextView titleTextView = (TextView) convertView.findViewById(R.id.titleTextView);
        ImageView imageView = (ImageView) convertView.findViewById(R.id.imageView);
        TextView descriptionTextView = (TextView) convertView.findViewById(R.id.descriptionTextView);

        if (document == null) {
            titleTextView.setText("");
            imageView.setImageDrawable(null);
            descriptionTextView.setText("");
        } else {
            String title = document.getTitle();
            if (title.length() > 0) {
                titleTextView.setText(title);
            } else {
                titleTextView.setText("No title yet..");
            }

            imageView.setImageDrawable(null);

            DocumentUtils.DisplayImage(document, imageView);
            {
                Date date = new Date(document.getTimestamp() * 1000L);
                DateFormat f = DateFormat.getDateTimeInstance();
                descriptionTextView.setText(f.format(date));
            }
        }

        return convertView;
    }
}

public class EvidenceListFragment extends ListFragment {

    public EvidenceListFragment() {
        // Required empty public constructor
    }

    private void deleteSelectedItems() {
        ListView listView = getListView();
        DocumentDB db = DocumentDB.get(getActivity());
        ListAdapter adapter = getListAdapter();
        SparseBooleanArray positions = listView.getCheckedItemPositions();
        for (int i = 0; i < positions.size(); i++) { 
            db.deleteDocument((Document)adapter.getItem(positions.keyAt(i)));
        }
    }

    private void setupMultiChoice() {
        ListView listView = getListView();
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(new MultiChoiceModeListener() {
                @Override
                public void onItemCheckedStateChanged(ActionMode mode, int position,
                                                      long id, boolean checked) {
                    // Here you can do something when items are selected/de-selected,
                    // such as update the title in the CAB
                }
                
                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    // Respond to clicks on the actions in the CAB
                    switch (item.getItemId()) {
                    case R.id.menu_evidence_delete:
                        deleteSelectedItems();
                        mode.finish(); // Action picked, so close the CAB
                        return true;
                    default:
                        return false;
                    }
                }

                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    // Inflate the menu for the CAB
                    MenuInflater inflater = mode.getMenuInflater();
                    inflater.inflate(R.menu.menu_evidence_context, menu);
                    return true;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {
                    // Here you can make any necessary updates to the activity when
                    // the CAB is removed. By default, selected items are deselected/unchecked.
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    // Here you can perform updates to the CAB due to
                    // an invalidate() request
                    return false;
                }
            });
    }

    @Override
    public void onStart() {
        setupMultiChoice();
        super.onStart();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        EvidenceAdapter adapter = new EvidenceAdapter(DocumentDB.get(getActivity()).getDocumentList());
        setListAdapter(adapter);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent(getActivity(), ShowEvidenceActivity.class);
        Document doc = (Document)getListAdapter().getItem(position);
        intent.setData(doc.getUri());
        startActivity(intent);
    }
}
