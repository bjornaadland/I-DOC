package no.nhc.i_doc;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

class EvidenceAdapter extends BaseAdapter {
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
        TextView titleTextView = (TextView) convertView.findViewById(R.id.titleTextView);
        titleTextView.setText(evidenceList.getDocument(position).getTitle());
        return convertView;
    }
}

public class EvidenceListFragment extends ListFragment {

    public EvidenceListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        EvidenceAdapter adapter = new EvidenceAdapter(DocumentDB.get(getActivity()).getDocumentList());
        setListAdapter(adapter);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // do something with the data
    }
}