package no.nhc.i_doc;

import android.net.Uri;

import java.util.List;
import java.util.ArrayList;


/**
 *  Document is a more or less abstract definition that
 *  represents an instance of "evidence".
 *  The Document has a title e.g. for display in a list of documents.
 *  Other details about documents are handled by other classes.
 */
public abstract interface Document
{
    abstract Uri getUri();

    /**
     *  Listener class for changes.
     */
    public static interface Listener {
        void changed();
    }

    /**
     * Add listener for changes.
     */
    abstract void addChangeListener(Listener newListener);

    /**
     * Remove listener for changes.
     */
    abstract void removeChangeListener(Listener newListener);

    /**
     *  Retrieve Document title
     */
    abstract String getTitle();

    /**
     *  Set or update title
     */
    abstract void setTitle(String title);

    /**
     *  Get seconds (from 1970)
     */
    abstract int getTimestamp();

    /**
     *  Retrieve the metadata associated with the document
     */
    abstract List<Metadata> getMetadata();

    /**
     *  Overwrite the list of metadata objects
     */
    abstract void setMetadata(List<Metadata> metadata);

    /**
     *  Set associated Metadata with the Document
     */
    abstract void addMetadata(Metadata data);

    /**
     * Get the files held by this document.
     */
    abstract List<String> getFiles();

    /**
     * Add a file to this document.
     */
    abstract void addFile(String file);
}
