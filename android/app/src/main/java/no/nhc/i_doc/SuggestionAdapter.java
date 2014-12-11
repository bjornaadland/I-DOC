package no.nhc.i_doc;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SuggestionAdapter extends BaseAdapter implements Filterable {
    public static final String TAG = "SuggestionAdapter";

    public static class Suggestion {
        public String mDisplay;
        public Object mId;
    }

    private List<Suggestion> mSuggestions;
    private final DocumentDB mDatabase;
    private final Enum mKeyProperty;

    public SuggestionAdapter(DocumentDB db, String type) {
        mDatabase = db;
        mKeyProperty = Metadata.Person.GivenName;
    }

    @Override
    public int getCount() { return mSuggestions.size(); }

    @Override
    public Object getItem(int i) { return mSuggestions.get(i); }

    @Override
    public long getItemId(int i) { return i; }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater)parent.getContext().
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(android.R.layout.simple_spinner_item, parent, false);
        } else {
            view = convertView;
        }

        TextView text = (TextView)view.findViewById(android.R.id.text1);

        if (text != null) {
            text.setText(((Suggestion)getItem(position)).mDisplay);
        }

        return view;
    }

    public Filter getFilter() {
        return new Filter() {
            protected Filter.FilterResults performFiltering(CharSequence constraint) {
                Filter.FilterResults res = new Filter.FilterResults();
                ArrayList<Suggestion> suggestions = new ArrayList<Suggestion>();

                DocumentDB.List<Metadata.PropertyMap> lst = mDatabase.mapMetadata(mKeyProperty);

                if (constraint != null) {
                    String search = constraint.toString();
                    Log.d(TAG, "FilterResults: " + Integer.toString(lst.getCount()) + " results for " + search);

                    for (int i = 0; i < lst.getCount(); ++i) {
                        Metadata.PropertyMap map = lst.getObject(i);

                        Log.d(TAG, "map suggestion: " + (String)map.propertyValue);

                        if (((String)map.propertyValue).startsWith(search)) {
                            Suggestion sug = new Suggestion();
                            sug.mDisplay = (String)map.propertyValue;
                            sug.mId = map.id;
                            suggestions.add(sug);
                        }
                    }
                }

                res.values = suggestions;
                res.count = suggestions.size();

                return res;
            }

            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results.count == 0) {
                    notifyDataSetInvalidated();
                } else {
                    mSuggestions = (List<Suggestion>)results.values;
                    notifyDataSetChanged();
                }
            }
        };
    }
}
