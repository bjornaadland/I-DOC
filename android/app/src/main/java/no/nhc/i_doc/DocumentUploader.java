package no.nhc.i_doc;

import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.Log;
import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import java.util.Random;

class DocumentUploader {
    static final String TAG = "DocumentUploader";
    static UploadListener sListener;
    private final static char[] MULTIPART_CHARS =
        "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
            .toCharArray();


    static class UploadDocumentTask extends AsyncTask<Document, Integer, Boolean> {
        class ProgressiveEntity implements HttpEntity {
            HttpEntity mOtherEntity;
            long mWrittenLength;
            long mContentLength;

            public ProgressiveEntity(HttpEntity otherEntity) {
                mOtherEntity = otherEntity;
                mContentLength = otherEntity.getContentLength();
                mWrittenLength = 0;
            }

            @Override
            public void consumeContent() throws IOException {
                mOtherEntity.consumeContent();
            }
            @Override
            public InputStream getContent() throws IOException,
                IllegalStateException {
                return mOtherEntity.getContent();
            }
            @Override
            public Header getContentEncoding() {
                return mOtherEntity.getContentEncoding();
            }
            @Override
            public long getContentLength() {
                return mOtherEntity.getContentLength();
            }
            @Override
            public Header getContentType() {
                return mOtherEntity.getContentType();
            }
            @Override
            public boolean isChunked() {
                return mOtherEntity.isChunked();
            }
            @Override
            public boolean isRepeatable() {
                return mOtherEntity.isRepeatable();
            }
            @Override
            public boolean isStreaming() {
                return mOtherEntity.isStreaming();
            }

            long mLastProgress = 0;

            private void doProgressUpdate() {
                long now = System.currentTimeMillis();
                if (now - mLastProgress > 100 || mWrittenLength == mContentLength) {
                    int progress = (int)(((float)mWrittenLength / mContentLength) * 100);
                    publishProgress(progress);
                    mLastProgress = now;
                }
            }

            @Override
            public void writeTo(final OutputStream outstream) throws IOException {
                class ProgressiveOutputStream extends FilterOutputStream {
                    public ProgressiveOutputStream(OutputStream proxy) {
                        super(proxy);
                    }
                    public void write(int idx) throws IOException {
                        outstream.write(idx);
                    }
                    public void write(byte[] bts) throws IOException {
                        outstream.write(bts);
                    }
                    public void flush() throws IOException {
                        outstream.flush();
                    }
                    public void close() throws IOException {
                        outstream.close();
                    }
                    public void write(byte[] bts, int off, int len) throws IOException {
                        outstream.write(bts, off, len);
                        mWrittenLength += len;
                        doProgressUpdate();
                    }
                }
                mOtherEntity.writeTo(new ProgressiveOutputStream(outstream));
            }
        };

        protected Boolean doInBackground(Document... documents) {
            for (Document doc : documents) {
                AndroidHttpClient client = AndroidHttpClient.newInstance("I-DOC");
                HttpPut put = new HttpPut("http://www.gjermshus.com:9000/i-doc/documents/" + doc.getUUID());
                java.nio.charset.Charset charset_utf8 = java.nio.charset.Charset.forName("UTF-8");

                MultipartEntity entity = new MultipartEntity();

                entity = new MultipartEntity(org.apache.http.entity.mime.HttpMultipartMode.STRICT);

                try {
                    entity.addPart("metadata", new StringBody(DocumentUtils.documentToJSON(doc).toString(), 
                                                              "text/json", 
                                                              charset_utf8));
                } catch (UnsupportedEncodingException e) {
                    return false;
                }

                for (String f : doc.getFiles()) {
                    try {
                        entity.addPart("jpg", new FileBody(new File(new URI(f)), "image/jpeg"));
                    } catch (URISyntaxException e) {
                        return false;
                    }
                }
                put.setEntity(new ProgressiveEntity(entity));
                try {
                    HttpResponse response = client.execute(put);
                    if (response.getStatusLine().getStatusCode() == 200) {
                        return true;
                    } else {
                        return false;
                    }
                } catch (IOException e) {
                    Log.e(TAG, "exception during put " + e.toString());
                    return false;
                } finally {
                    client.close();
                }
            }
            return true;
        }

        protected void onPostExecute(Boolean result) {
            if (sListener != null) {
                sListener.done(result);
            }
            sUploadDocumentTask = null;
        }

        protected void onProgressUpdate(Integer... progress) {
            Log.e(TAG, "onProgressUpdate " + progress[0]);
            if (sListener != null) {
                sListener.progress(progress);
            }
        }
    }

    static UploadDocumentTask sUploadDocumentTask;

    public static interface UploadListener {
        void progress(Integer... progress);
        void done(Boolean result);
    }

    public static void clearUploadListener() {
        if (sUploadDocumentTask != null) {
            sListener = null;
        }
    }

    public static void uploadDocuments(List<Document> documents, UploadListener listener) {
        sListener = listener;
        if (sUploadDocumentTask != null) {
            return;
        }
        sUploadDocumentTask = new UploadDocumentTask();
        sUploadDocumentTask.execute(documents.toArray(new Document[documents.size()]));
    }
}
