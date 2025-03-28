/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.bookmarks;

import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.util.Logger;
import gaiasky.util.Settings;
import gaiasky.util.SysUtils;
import gaiasky.util.TextUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import gaiasky.util.parse.Parser;

import java.io.IOException;
import java.nio.file.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

/**
 * Manages bookmarks for the application.
 */
public class BookmarksManager implements IObserver {
    private static final Logger.Log logger = Logger.getLogger(BookmarksManager.class);
    private Path bookmarksFile;
    private List<BookmarkNode> bookmarks;
    private Map<Path, BookmarkNode> nodes;
    private int version;

    public BookmarksManager() {
        initDefault();
        EventManager.instance.subscribe(this, Event.BOOKMARKS_ADD, Event.BOOKMARKS_REMOVE, Event.BOOKMARKS_REMOVE_ALL, Event.BOOKMARKS_MOVE, Event.BOOKMARKS_MOVE_UP, Event.BOOKMARKS_MOVE_DOWN);
    }

    private void initDefault() {
        final String bookmarksFileName = "bookmarks.txt";

        Path customBookmarks = SysUtils.getDefaultBookmarksDir().resolve(bookmarksFileName);
        Path defaultBookmarks = Paths.get(Settings.ASSETS_LOC, SysUtils.getBookmarksDirName(), bookmarksFileName);
        bookmarksFile = customBookmarks;
        if (!Files.exists(customBookmarks)) {
            // Bookmarks file does not exist, just copy it.
            overwriteBookmarksFile(defaultBookmarks, customBookmarks, false);
        } else {
            // Update, maybe.
            var customVersion = getFileVersion(customBookmarks);
            var defaultVersion = getFileVersion(defaultBookmarks);
            if (defaultVersion > customVersion) {
                overwriteBookmarksFile(defaultBookmarks, customBookmarks, true);
            }
        }
        logger.info(I18n.msg("gui.bookmark.file.use", bookmarksFile));

        bookmarks = loadBookmarks(bookmarksFile);
        version = getFileVersion(bookmarksFile);
        if (bookmarks != null) {
            logger.info(I18n.msg("gui.bookmark.loaded", bookmarks.size()));
        }

    }

    /**
     * Copies the file src to the file to, optionally making a backup.
     *
     * @param src    The source file.
     * @param dst    The destination file.
     * @param backup Whether to create a backup of dst if it exists.
     */
    private void overwriteBookmarksFile(Path src, Path dst, boolean backup) {
        assert src != null && src.toFile().exists() && src.toFile().isFile() && src.toFile().canRead() : I18n.msg("error.file.exists.readable", src != null ? src.getFileName().toString() : "null");
        assert dst != null : I18n.msg("notif.null.not", "dest");
        if (backup && dst.toFile().exists() && dst.toFile().canRead()) {
            Date date = Calendar.getInstance().getTime();
            DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-hhmmss");
            String strDate = dateFormat.format(date);

            var backupName = dst.getFileName().toString() + "." + strDate;
            Path backupFile = dst.getParent().resolve(backupName);
            // Copy.
            try {
                Files.copy(dst, backupFile, StandardCopyOption.REPLACE_EXISTING);
                logger.info(I18n.msg("notif.file.backup", backupFile));
            } catch (IOException e) {
                logger.error(e);
            }
        }
        // Actually copy file.
        try {
            Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
            logger.info(I18n.msg("notif.file.update", dst.toString()));
            if (backup) {
                EventManager.publishWaitUntilConsumer(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("notif.file.overriden.backup", dst.toString()), -1f);
            } else {
                EventManager.publishWaitUntilConsumer(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("notif.file.overriden", dst.toString()), -1f);
            }
        } catch (IOException e) {
            logger.error(e);
        }
    }


    /**
     * Gets the version from the given bookmarks file by reading the first line.
     *
     * @param path The path to the file.
     * @return The version, or -1 if it does not exist.
     */
    private int getFileVersion(Path path) {
        var l = TextUtils.readFirstLine(path);
        if (l.isPresent()) {
            var firstLine = l.get().strip();
            if (!firstLine.isBlank()) {
                if (firstLine.startsWith("#v")) {
                    int version = Parser.parseIntException(firstLine.substring(2));
                    return version;
                } else {
                    logger.warn(I18n.msg("error.file.version", path));
                }
            } else {
                logger.warn(I18n.msg("error.file.version", path));
            }
        }
        return -1;
    }

    /**
     * Returns the internal list of bookmark nodes.
     *
     * @return The list of {@link BookmarkNode} objects.
     */
    public List<BookmarkNode> getBookmarks() {
        return bookmarks;
    }

    /**
     * @return A list with all folder bookmarks
     */
    public List<BookmarkNode> getFolders() {
        return getBookmarksByType(bookmarks, new ArrayList<>(), true);
    }

    public List<BookmarkNode> getBookmarksByType(List<BookmarkNode> bookmarks, List<BookmarkNode> result, boolean folder) {
        if (bookmarks != null) {
            for (BookmarkNode bookmark : bookmarks) {
                if (bookmark.folder == folder)
                    result.add(bookmark);
                getBookmarksByType(bookmark.children, result, folder);
            }
        }
        return result;
    }

    private List<BookmarkNode> loadBookmarks(Path file) {
        try (Stream<String> lines = Files.lines(file)) {
            List<String> bookmarks = lines.filter(line -> !line.strip().startsWith("#")).filter(line -> !line.isBlank()).map(String::strip).toList();

            nodes = new HashMap<>();
            this.bookmarks = new ArrayList<>();
            for (String bookmark : bookmarks) {
                insertBookmark(bookmark, false);
            }
            return this.bookmarks;
        } catch (IOException e) {
            logger.error(e);
        }
        return null;
    }

    public boolean containsName(String name) {
        boolean contains = false;
        for (BookmarkNode bookmark : bookmarks) {
            contains = contains || containsNameRec(name, bookmark);
        }
        return contains;
    }

    public boolean containsNameRec(String name, BookmarkNode node) {
        if (node.name.equals(name)) {
            return true;
        } else if (node.children != null) {
            boolean contains = false;
            for (BookmarkNode child : node.children) {
                contains = contains | containsNameRec(name, child);
            }
            return contains;
        }
        return false;
    }

    public boolean containsPath(Path path) {
        return nodes != null && nodes.containsKey(path);
    }

    public synchronized void persistBookmarks() {
        persistBookmarks(bookmarksFile);
    }

    private synchronized void persistBookmarks(Path file) {
        if (bookmarks != null) {
            StringBuilder content = new StringBuilder();
            if(version >= 0) {
                content.append(String.format("#v%04d\n", version));
            }
            content.append("# Bookmarks file for Gaia Sky, one bookmark per line, folder separator: '/', comments: '#'");
            buildContent(content, bookmarks);

            try {
                Files.delete(file);
                Files.write(file, content.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                logger.error(e);
            }
        }
    }

    private void buildContent(StringBuilder content, List<BookmarkNode> bookmarks) {
        for (BookmarkNode b : bookmarks) {
            if (b.children == null || b.children.isEmpty()) {
                // Write
                content.append("\n").append(b.path.toString());
            } else {
                // Down one level
                buildContent(content, b.children);
            }
        }
    }

    /**
     * Adds a bookmark with the given path.
     *
     * @param path The path to add.
     * @return True if added.
     */
    public synchronized boolean addBookmark(String path, boolean folder) {
        boolean added = false;
        if (bookmarks != null) {
            added = insertBookmark(path, folder);
        }
        return added;
    }

    /**
     * Inserts a new bookmark from a line in the bookmarks.txt file.
     *
     * @param path   The line text.
     * @param folder Whether it is a folder or not.
     * @return Whether the bookmark was inserted.
     */
    private synchronized boolean insertBookmark(String path, boolean folder) {
        Path p = new BookmarkPath(path);
        if (!nodes.containsKey(p)) {
            BookmarkNode node = new BookmarkNode(p, folder);
            nodes.put(p, node);
            BookmarkNode curr = node;
            while (true) {
                if (curr.path.getParent() != null) {
                    if (!nodes.containsKey(curr.path.getParent())) {
                        // Add
                        BookmarkNode bookmarkNode = new BookmarkNode(curr.path.getParent(), true);
                        nodes.put(bookmarkNode.path, bookmarkNode);
                    }
                    // Insert
                    BookmarkNode parentNode = nodes.get(curr.path.getParent());
                    parentNode.insert(curr);
                    curr = parentNode;
                } else {
                    if (!bookmarks.contains(curr))
                        bookmarks.add(curr);
                    break;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Removes a bookmark by its path.
     *
     * @param path The path to remove
     * @return True if removed.
     */
    public synchronized boolean removeBookmark(String path) {
        Path p = new BookmarkPath(path);
        if (nodes != null && nodes.containsKey(p)) {
            BookmarkNode n = nodes.get(p);
            if (n.parent != null) {
                n.parent.children.remove(n);
                n.parent = null;
            } else {
                bookmarks.remove(n);
            }
            nodes.remove(p);
            return true;
        }
        return false;
    }

    /**
     * Remove all bookmarks with the given name.
     *
     * @param name The name to remove.
     * @return Number of removed bookmarks.
     */
    public synchronized int removeBookmarksByName(String name) {
        int nRemoved = 0;
        if (bookmarks != null && !bookmarks.isEmpty()) {
            Iterator<BookmarkNode> it = bookmarks.iterator();
            while (it.hasNext()) {
                BookmarkNode bookmark = it.next();
                if (bookmark.name.equals(name)) {
                    // Remove from root
                    it.remove();
                    bookmark.parent = null;
                    nodes.remove(bookmark.path);
                    nRemoved++;
                } else {
                    nRemoved += removeBookmarksByNameRec(name, bookmark, it);
                }
            }
            return nRemoved;
        }
        return 0;
    }

    private synchronized int removeBookmarksByNameRec(String name, BookmarkNode bookmark, Iterator<BookmarkNode> itr) {
        int nRemoved = 0;
        if (bookmark.name.equals(name)) {
            // Remove from parent
            itr.remove();
            bookmark.parent = null;
            nodes.remove(bookmark.path);
            nRemoved++;
        } else if (bookmark.children != null) {
            Iterator<BookmarkNode> it = bookmark.children.iterator();
            while (it.hasNext()) {
                BookmarkNode child = it.next();
                nRemoved += removeBookmarksByNameRec(name, child, it);
            }
        }
        return nRemoved;
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
            case BOOKMARKS_ADD -> {
                Object d0 = data[0];
                if (d0 instanceof String name) {
                    // Simple object bookmark.
                    boolean folder = (boolean) data[1];
                    if (addBookmark(name, folder)) {
                        logger.info("Bookmark added: " + name);
                    } else {
                        logger.error("Failed to add bookmark: " + name);
                    }
                } else {
                    // Position bookmark.
                    Vector3b pos = (Vector3b) d0;
                    Vector3d dir = (Vector3d) data[1];
                    Vector3d up = (Vector3d) data[2];
                    Instant t = (Instant) data[3];
                    String name = (String) data[4];
                    boolean folder = (boolean) data[5];
                    String text = "{" + str(pos) + "|" + str(dir) + "|" + str(up) + "|" + t.toString() + "|" + name + "}";
                    if (addBookmark(text, folder)) {
                        logger.info("Bookmark added: " + text);
                    } else {
                        logger.error("Failed to add bookmark: " + text);
                    }
                }
            }
            case BOOKMARKS_REMOVE -> {
                String name = (String) data[0];
                if (removeBookmark(name))
                    logger.info("Bookmark removed: " + name);
            }
            case BOOKMARKS_REMOVE_ALL -> {
                String name = (String) data[0];
                int removed = removeBookmarksByName(name);
                logger.info(removed + " bookmarks with name " + name + " removed");
            }
            case BOOKMARKS_MOVE -> {
                BookmarkNode src = (BookmarkNode) data[0];
                BookmarkNode dest = (BookmarkNode) data[1];
                if (dest == null) {
                    // Move to root
                    removeBookmark(src.path.toString());
                    addBookmark(src.text, false);
                } else {
                    // Move to dest folder
                    if (dest.folder) {
                        removeBookmark(src.path.toString());
                        addBookmark(dest.path.resolve(src.text).toString(), false);
                    } else {
                        logger.error("Destination is not a folder: " + dest);
                    }
                }
            }
            case BOOKMARKS_MOVE_UP -> {
                BookmarkNode bookmark = (BookmarkNode) data[0];
                List<BookmarkNode> list;
                if (bookmark.parent != null) {
                    list = bookmark.parent.children;
                } else {
                    list = bookmarks;
                }
                int idx0 = list.indexOf(bookmark);
                if (idx0 > 0) {
                    list.remove(idx0);
                    list.add(idx0 - 1, bookmark);
                }
            }
            case BOOKMARKS_MOVE_DOWN -> {
                BookmarkNode bookmark = (BookmarkNode) data[0];
                List<BookmarkNode> list;
                if (bookmark.parent != null) {
                    list = bookmark.parent.children;
                } else {
                    list = bookmarks;
                }
                int idx1 = list.indexOf(bookmark);
                if (idx1 < list.size() - 1) {
                    list.remove(idx1);
                    list.add(idx1 + 1, bookmark);
                }
            }
            default -> {
            }
        }
    }

    private String str(Vector3b v) {
        return "[" + v.x.toString() + "," + v.y.toString() + "," + v.z.toString() + "]";
    }

    private String str(Vector3d v) {
        return "[" + v.x + "," + v.y + "," + v.z + "]";
    }

    public static class BookmarkNode {

        private static final String VEC3_REGEX = "\\[[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?,[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?,[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?]";
        /**
         * Regular expression that defines the format of positional bookmarks, which is:
         * <code>{[x,y,z]|[dx,dy,dz]|[ux,uy,uz]|instant|name}</code>
         */
        public static final String POS_BOOKMARK_REGEX = "\\{" + VEC3_REGEX +
                "\\|" + VEC3_REGEX +
                "\\|" + VEC3_REGEX +
                "\\|\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z\\|[^|,\\\\]+}";

        /**
         * <p>The text of the bookmark in the bookmarks.txt file.
         * This coincides with the name for regular bookmarks. For
         * positional bookmarks, this has the format:</p>
         * <code>{[x,y,z]|[dx,dy,dz]|[ux,uy,uz]|instant|name}</code>
         */
        public String text;
        /**
         * The name of this node
         */
        public String name;
        /**
         * Camera position, for positional bookmarks.
         */
        public Vector3d position;
        /**
         * Camera direction, for positional bookmarks.
         */
        public Vector3d direction;
        /**
         * Camera up vector, for positional bookmarks.
         */
        public Vector3d up;
        /**
         * Time, positional bookmarks.
         */
        public Instant time;
        /**
         * Settings object, for settings bookmarks.
         */
        public Settings settings;
        /**
         * The full path.
         */
        public Path path;
        /**
         * The parent of this node, null if root.
         */
        public BookmarkNode parent;
        /**
         * Children, if any.
         */
        public List<BookmarkNode> children;
        /**
         * Is it a folder?
         */
        public boolean folder;

        public BookmarkNode(Path path, boolean folder) {
            this.path = path;
            this.text = this.path.getFileName().toString().strip();
            this.folder = folder;
            initializeText();
        }

        public void initializeText() {
            if (this.text != null) {
                if (this.text.matches(POS_BOOKMARK_REGEX)) {
                    // Position bookmark.
                    var tokens = this.text.substring(1, this.text.length() - 1).split("\\|");
                    var pos = tokens[0];
                    var dir = tokens[1];
                    var up = tokens[2];
                    var instant = tokens[3];
                    this.name = tokens[4];

                    this.position = vectorFromString(pos);
                    this.direction = vectorFromString(dir);
                    this.up = vectorFromString(up);
                    this.time = Instant.parse(instant);
                } else {
                    // Regular bookmark.
                    this.name = this.text;
                }
            }
        }

        private Vector3d vectorFromString(String vectorString) {
            var tokens = vectorString.substring(1, vectorString.length() - 1).split(",");
            return new Vector3d(Parser.parseDouble(tokens[0]), Parser.parseDouble(tokens[1]), Parser.parseDouble(tokens[2]));
        }

        public void insert(BookmarkNode node) {
            if (node != null) {
                initChildren();
                if (!children.contains(node)) {
                    children.add(node);
                    node.parent = this;
                }
            }
        }

        private void initChildren() {
            if (children == null)
                children = new ArrayList<>(4);
        }

        @Override
        public String toString() {
            return path.toString();
        }

        public BookmarkNode getFirstFolderAncestor() {
            if (folder)
                return this;
            else if (parent != null)
                return parent.getFirstFolderAncestor();
            else
                return null;
        }

        public boolean isDescendantOf(BookmarkNode other) {
            BookmarkNode current = this;
            while (current != null) {
                if (current == other) {
                    return true;
                }
                current = current.parent;
            }
            return false;
        }
    }
}
