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
//import android.view.ViewGroup.LayoutParams;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class EvidenceAdapter extends BaseAdapter {
    static final String TAG = "EvidenceAdapter";
    private final DocumentDB.List evidenceList;
    private DocumentDB.Listener mListener;

    public EvidenceAdapter(DocumentDB.List evidenceList) {
        this.evidenceList = evidenceList;
        this.evidenceList.addListener(mListener = new DocumentDB.Listener() {
            @Override
            public void changed() {
                notifyDataSetChanged();
            }
        });
    }

    public DocumentDB.List getEvidenceList() { return evidenceList; }

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

    void populateView(Document document, int position, View view) {
        TextView titleTextView = (TextView) view.findViewById(R.id.titleTextView);
        ImageView imageView = (ImageView) view.findViewById(R.id.imageView);
        TextView descriptionTextView = (TextView) view.findViewById(R.id.descriptionTextView);

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

    }

    private View inflateView(View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) parent.getContext().
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.item_evidence, parent, false);
        }
        return convertView;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final View returnView = inflateView(convertView, parent);
        final Document document = evidenceList.getDocument(position);
        if (document == null) {
            return returnView;
        }

        Document.Listener documentListener = new Document.Listener() {
                public void changed() {
                    Log.d("CouchDocumentDB", "changed");
                    populateView(document, position, returnView);
                }
            };

        Document.Listener oldListener = (Document.Listener)returnView.getTag(R.id.TAG_DOCUMENT_LISTENER);
        Document oldDocument = (Document)returnView.getTag(R.id.TAG_DOCUMENT);
        if (oldListener != null && oldDocument != null) {
            oldDocument.removeChangeListener(oldListener);
        }
        
        document.addChangeListener(documentListener);
        returnView.setTag(R.id.TAG_DOCUMENT, document);
        returnView.setTag(R.id.TAG_DOCUMENT_LISTENER, documentListener);

        populateView(document, position, returnView);
        return returnView;
    }
}

public class EvidenceListFragment extends ListFragment {

    public EvidenceListFragment() {
        // Required empty public constructor
    }

    private List<Document> getSelectedDocuments(DocumentDB db) {
        List<Document> ret = new ArrayList<Document>();
        ListView listView = getListView();
        ListAdapter adapter = getListAdapter();
        SparseBooleanArray positions = listView.getCheckedItemPositions();
        for (int i = 0; i < positions.size(); i++) { 
            ret.add((Document)adapter.getItem(positions.keyAt(i)));
        }
        return ret;
    }


    private void deleteSelectedItems() {
        DocumentDB db = DocumentDB.get(getActivity());
        for (Document d  : getSelectedDocuments(db)) {
            db.deleteDocument(d);
        }
    }

    private void uploadSelectedItems() {
        DocumentDB db = DocumentDB.get(getActivity());
        List<Document> docList = new ArrayList<Document>();
        for (Document d  : getSelectedDocuments(db)) {
            docList.add(d);
        }
        DocumentUtils.uploadDocuments(docList);
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
                    case R.id.menu_evidence_upload:
                        uploadSelectedItems();
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
//        Intent intent = new Intent(getActivity(), ShowEvidenceActivity.class);
//        Document doc = (Document)getListAdapter().getItem(position);
//        intent.setData(doc.getUri());

//        Intent intent = new Intent(getActivity(), EditEvidenceActivity.class);
//        Document doc = (Document)getListAdapter().getItem(position);
//        intent.setData(doc.getUri());
//        startActivity(intent);

        Intent intent = new Intent(getActivity(), ViewEvidenceActivity.class);
        intent.putExtra("position", position);
        startActivity(intent);
    }
}
