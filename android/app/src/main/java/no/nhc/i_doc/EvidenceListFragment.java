package no.nhc.i_doc;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;

import com.nostra13.universalimageloader.core.ImageLoader;

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
        String title = document.getTitle();
        if (title.length() > 0) {
            titleTextView.setText(title);
        } else {
            titleTextView.setText("No title yet..");
        }

        ImageView imageView = (ImageView) convertView.findViewById(R.id.imageView);
        java.util.List<String> files = document.getFiles();
        if (files.size() > 0) {
            String fileUri = files.get(0);
            String extension = MimeTypeMap.getFileExtensionFromUrl(fileUri);
            String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (type.startsWith("image") || type.startsWith("video")) {
                ImageLoader.getInstance().displayImage(fileUri, imageView);
            } else if (type.startsWith("audio")) {
                // TODO: Display audio icon
            } else {
                // TODO: Display generic icon
            }
        } else {
            // TODO: Display generic icon
        }

        {
            TextView descriptionTextView = (TextView) convertView.findViewById(R.id.descriptionTextView);
            Date date = new Date(document.getTimestamp() * 1000L);
            DateFormat f = DateFormat.getDateTimeInstance();
            descriptionTextView.setText(f.format(date));
        }

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
