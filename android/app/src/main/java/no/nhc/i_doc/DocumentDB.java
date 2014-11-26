package no.nhc.i_doc;

import android.content.Context;

/**
 *  DocumentDB concerns the set of stored documents,
 *  and manages adding and removing of documents from the database.
 */
public abstract class DocumentDB
{
    private static volatile DocumentDB inst;

    /**
     *  Listener class for a List
     */
    public static interface Listener {
        void changed();
    }

    /**
     *  DocList - a dynamic list of documents
     */
    public static interface List {
        int getCount();
        Document getDocument(int position);
        void setListener(Listener listener);
    }

    protected DocumentDB() {}

    public static DocumentDB get(android.content.Context context) {
        if (inst == null) {
            inst = new CouchDocumentDB(context);
        }

        return inst;
    }

    /**
     *  Get the full list of stored documents.
     */
    abstract List getDocumentList();

    /**
     *  Create an empty document
     */
    abstract Document createDocument();

    /**
     *  Save a new or update an already existing Document
     */
    abstract void saveDocument(Document d);
}
