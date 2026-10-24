package com.example.doclens;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Recently opened PDFs and the page each one was left on, kept in
 * SharedPreferences as a small JSON list (newest first).
 */
final class RecentStore {

    static final class Entry {
        /** Identity of the document: file:// or content:// URI string. */
        final String key;
        final String name;
        /** Where to open it from: a file path, or null to open {@link #key} itself. */
        final String path;
        final long size;
        final long openedAt;
        final int lastPage;
        final int pageCount;

        Entry(String key, String name, String path, long size, long openedAt, int lastPage, int pageCount) {
            this.key = key;
            this.name = name;
            this.path = path;
            this.size = size;
            this.openedAt = openedAt;
            this.lastPage = lastPage;
            this.pageCount = pageCount;
        }
    }

    private static final String PREFS = "recent";
    private static final String KEY_LIST = "list";
    private static final int MAX_ENTRIES = 30;

    private final SharedPreferences prefs;
    private final File privateCopies;

    RecentStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        privateCopies = privateCopyDir(context);
    }

    /** Copies of PDFs received from other apps, so they stay openable from Recent. */
    static File privateCopyDir(Context context) {
        return new File(context.getFilesDir(), "received");
    }

    List<Entry> all() {
        List<Entry> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs.getString(KEY_LIST, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                result.add(new Entry(
                        o.getString("key"),
                        o.optString("name", ""),
                        o.has("path") ? o.getString("path") : null,
                        o.optLong("size"),
                        o.optLong("openedAt"),
                        o.optInt("lastPage"),
                        o.optInt("pageCount")));
            }
        } catch (JSONException e) {
            // Corrupt list: start again
        }
        return result;
    }

    Entry get(String key) {
        for (Entry e : all()) {
            if (e.key.equals(key)) return e;
        }
        return null;
    }

    /** Moves the document to the top of the list, keeping its last page. */
    void opened(String key, String name, String path, long size, int pageCount) {
        Entry old = get(key);
        String keepPath = path != null ? path : old != null ? old.path : null;
        int lastPage = old != null ? old.lastPage : 0;
        put(new Entry(key, name, keepPath, size, System.currentTimeMillis(), lastPage, pageCount));
    }

    void setLastPage(String key, int page) {
        Entry old = get(key);
        if (old == null || old.lastPage == page) return;
        replace(new Entry(old.key, old.name, old.path, old.size, old.openedAt, page, old.pageCount));
    }

    void setPath(String key, String path) {
        Entry old = get(key);
        if (old == null) return;
        replace(new Entry(old.key, old.name, path, old.size, old.openedAt, old.lastPage, old.pageCount));
    }

    void remove(String key) {
        List<Entry> list = all();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).key.equals(key)) {
                deletePrivateCopy(list.remove(i));
                break;
            }
        }
        save(list);
    }

    private void put(Entry entry) {
        List<Entry> list = all();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).key.equals(entry.key)) {
                list.remove(i);
                break;
            }
        }
        list.add(0, entry);
        while (list.size() > MAX_ENTRIES) deletePrivateCopy(list.remove(list.size() - 1));
        save(list);
    }

    private void replace(Entry entry) {
        List<Entry> list = all();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).key.equals(entry.key)) {
                list.set(i, entry);
                save(list);
                return;
            }
        }
    }

    private void deletePrivateCopy(Entry entry) {
        if (entry.path == null) return;
        File file = new File(entry.path);
        if (privateCopies.equals(file.getParentFile())) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    private void save(List<Entry> list) {
        JSONArray array = new JSONArray();
        try {
            for (Entry e : list) {
                JSONObject o = new JSONObject();
                o.put("key", e.key);
                o.put("name", e.name);
                if (e.path != null) o.put("path", e.path);
                o.put("size", e.size);
                o.put("openedAt", e.openedAt);
                o.put("lastPage", e.lastPage);
                o.put("pageCount", e.pageCount);
                array.put(o);
            }
        } catch (JSONException e) {
            return;
        }
        prefs.edit().putString(KEY_LIST, array.toString()).apply();
    }
}
