package com.example.doclens;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Rows of the library screen: the default-viewer banner, PDFs, recent files and folders. */
final class LibraryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    interface Callbacks {
        void onFile(PdfFile file);

        void onFileMenu(PdfFile file, View anchor);

        void onRecent(RecentStore.Entry entry);

        void onRecentMenu(RecentStore.Entry entry, View anchor);

        void onFolder(PdfLibrary.Folder folder);

        void onEmptyAction(int action);

        void onSetDefault();

        void onDismissDefault();
    }

    static final int EMPTY_NO_ACTION = 0;
    static final int EMPTY_GRANT_ACCESS = 1;
    static final int EMPTY_BROWSE_ALL = 2;

    private static final int TYPE_BANNER = 0;
    private static final int TYPE_SECTION = 1;
    private static final int TYPE_FILE = 2;
    private static final int TYPE_RECENT = 3;
    private static final int TYPE_FOLDER = 4;
    private static final int TYPE_EMPTY = 5;

    static final class Row {
        final int type;
        PdfFile file;
        boolean showFolder;
        RecentStore.Entry recent;
        PdfLibrary.Folder folder;
        String text;
        String message;
        String button;
        int icon;
        int action;

        private Row(int type) {
            this.type = type;
        }

        static Row banner() {
            return new Row(TYPE_BANNER);
        }

        static Row section(String label) {
            Row r = new Row(TYPE_SECTION);
            r.text = label;
            return r;
        }

        static Row file(PdfFile file, boolean showFolder) {
            Row r = new Row(TYPE_FILE);
            r.file = file;
            r.showFolder = showFolder;
            return r;
        }

        static Row recent(RecentStore.Entry entry) {
            Row r = new Row(TYPE_RECENT);
            r.recent = entry;
            return r;
        }

        static Row folder(PdfLibrary.Folder folder) {
            Row r = new Row(TYPE_FOLDER);
            r.folder = folder;
            return r;
        }

        static Row empty(int icon, String title, String message, String button, int action) {
            Row r = new Row(TYPE_EMPTY);
            r.icon = icon;
            r.text = title;
            r.message = message;
            r.button = button;
            r.action = action;
            return r;
        }
    }

    private final Callbacks callbacks;
    private List<Row> rows = new ArrayList<>();

    LibraryAdapter(Callbacks callbacks) {
        this.callbacks = callbacks;
    }

    void submit(List<Row> rows) {
        this.rows = rows;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case TYPE_BANNER:
                return new Holder(inflater.inflate(R.layout.item_default_banner, parent, false));
            case TYPE_SECTION:
                return new Holder(inflater.inflate(R.layout.item_section, parent, false));
            case TYPE_FOLDER:
                return new Holder(inflater.inflate(R.layout.item_folder, parent, false));
            case TYPE_EMPTY:
                return new Holder(inflater.inflate(R.layout.item_empty, parent, false));
            default:
                return new Holder(inflater.inflate(R.layout.item_file, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        View v = holder.itemView;
        Context context = v.getContext();
        switch (row.type) {
            case TYPE_BANNER:
                v.findViewById(R.id.bannerAction).setOnClickListener(x -> callbacks.onSetDefault());
                v.findViewById(R.id.bannerDismiss).setOnClickListener(x -> callbacks.onDismissDefault());
                break;
            case TYPE_SECTION:
                ((TextView) v.findViewById(R.id.label)).setText(row.text);
                break;
            case TYPE_FILE:
                bindFile(v, context, row);
                break;
            case TYPE_RECENT:
                bindRecent(v, context, row.recent);
                break;
            case TYPE_FOLDER:
                bindFolder(v, context, row.folder);
                break;
            case TYPE_EMPTY:
                ((ImageView) v.findViewById(R.id.emptyIcon)).setImageResource(row.icon);
                ((TextView) v.findViewById(R.id.emptyTitle)).setText(row.text);
                ((TextView) v.findViewById(R.id.emptyMessage)).setText(row.message);
                Button button = v.findViewById(R.id.emptyButton);
                button.setVisibility(row.button != null ? View.VISIBLE : View.GONE);
                button.setText(row.button);
                button.setOnClickListener(x -> callbacks.onEmptyAction(row.action));
                break;
        }
    }

    private void bindFile(View v, Context context, Row row) {
        PdfFile file = row.file;
        StringBuilder meta = new StringBuilder(PdfLibrary.formatSize(file.size));
        if (file.modified > 0) {
            meta.append(" · ").append(DateUtils.formatDateTime(context, file.modified,
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH));
        }
        if (row.showFolder) meta.append(" · ").append(PdfLibrary.folderName(file.folderPath));
        ((TextView) v.findViewById(R.id.name)).setText(file.name);
        ((TextView) v.findViewById(R.id.meta)).setText(meta);
        v.setOnClickListener(x -> callbacks.onFile(file));
        View more = v.findViewById(R.id.more);
        more.setOnClickListener(x -> callbacks.onFileMenu(file, more));
        v.setOnLongClickListener(x -> {
            callbacks.onFileMenu(file, more);
            return true;
        });
    }

    private void bindRecent(View v, Context context, RecentStore.Entry entry) {
        StringBuilder meta = new StringBuilder();
        if (entry.pageCount > 0) {
            meta.append(context.getString(R.string.last_page, entry.lastPage + 1, entry.pageCount)).append(" · ");
        }
        if (entry.size > 0) meta.append(PdfLibrary.formatSize(entry.size)).append(" · ");
        meta.append(DateUtils.getRelativeTimeSpanString(entry.openedAt, System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE));
        ((TextView) v.findViewById(R.id.name)).setText(entry.name);
        ((TextView) v.findViewById(R.id.meta)).setText(meta);
        v.setOnClickListener(x -> callbacks.onRecent(entry));
        View more = v.findViewById(R.id.more);
        more.setOnClickListener(x -> callbacks.onRecentMenu(entry, more));
        v.setOnLongClickListener(x -> {
            callbacks.onRecentMenu(entry, more);
            return true;
        });
    }

    private void bindFolder(View v, Context context, PdfLibrary.Folder folder) {
        String count = context.getResources().getQuantityString(R.plurals.pdf_count,
                folder.files.size(), folder.files.size());
        ((TextView) v.findViewById(R.id.name)).setText(folder.name);
        ((TextView) v.findViewById(R.id.meta)).setText(count + " · " + PdfLibrary.displayPath(folder.path));
        v.setOnClickListener(x -> callbacks.onFolder(folder));
    }

    /** True for a recent entry that points at a file which no longer exists. */
    static boolean isMissing(RecentStore.Entry entry) {
        return entry.path != null && !new File(entry.path).isFile();
    }

    private static final class Holder extends RecyclerView.ViewHolder {
        Holder(View itemView) {
            super(itemView);
        }
    }
}
