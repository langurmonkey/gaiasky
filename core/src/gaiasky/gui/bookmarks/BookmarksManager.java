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
import gaiasky.util.*;
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
        EventManager.instance.subscribe(this, Event.BOOKMARKS_ADD, Event.BOOKMARKS_REMOVE, Event.BOOKMARKS_REMOVE_ALL, Event.BOOKMARKS_MOVE, Event.BOOKMARKS_MOVE_UP,
                Event.BOOKMARKS_MOVE_DOWN);
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
    private void overwriteBookmarksFile(Path src,
                                        Path dst,
                                        boolean backup) {
        assert src != null && src.toFile().exists() && src.toFile().isFile() && src.toFile().canRead() : I18n.msg("error.file.exists.readable",
                src != null ? src.getFileName().toString() : "null");
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
     *
     * @return The version, or -1 if it does not exist.
     */
    private int getFileVersion(Path path) {
        var l = TextUtils.readFirstLine(path);
        if (l.isPresent()) {
            var firstLine = l.get().strip();
            if (!firstLine.isBlank()) {
                if (firstLine.startsWith("#v")) {
                    return Parser.parseIntException(firstLine.substring(2));
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

    public List<BookmarkNode> getBookmarksByType(List<BookmarkNode> bookmarks,
                                                 List<BookmarkNode> result,
                                                 boolean folder) {
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
                insertBookmark(bookmark, bookmark.endsWith("/"));
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

    public boolean containsNameRec(String name,
                                   BookmarkNode node) {
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
            if (version >= 0) {
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

    private void buildContent(StringBuilder content,
                              List<BookmarkNode> bookmarks) {
        for (BookmarkNode b : bookmarks) {
            if (b.children == null || b.children.isEmpty()) {
                // Write
                if (b.folder && !b.path.toString().endsWith("/")) {
                    content.append("\n").append(b.path.toString()).append("/");
                } else {
                    content.append("\n").append(b.path.toString());
                }
            } else {
                // Down one level
                buildContent(content, b.children);
            }
        }
    }

    /**
     * Adds a bookmark with the given path.
     *
     * @param path   The path to add.
     * @param folder Whether this bookmark is a folder.
     *
     * @return True if added.
     */
    public synchronized boolean addBookmark(String path,
                                            boolean folder) {
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
     *
     * @return Whether the bookmark was inserted.
     */
    private synchronized boolean insertBookmark(String path,
                                                boolean folder) {
        if (folder && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
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
     * Removes the settings file of the give bookmark, if it exists.
     *
     * @param bookmark The bookmark.
     *
     * @return True if the settings file was removed, false otherwise.
     */
    public synchronized boolean remove(BookmarkNode bookmark) {
        if (bookmark.uuid != null) {
            Path parent = SysUtils.getDefaultBookmarksDir().resolve("settings");
            Path settingsFile = parent.resolve(bookmark.uuid);
            try {
                Files.deleteIfExists(settingsFile);
                return true;
            } catch (IOException e) {
                logger.error(I18n.msg("error.file.delete", settingsFile.toAbsolutePath().toString()), e);
            }
        }
        return false;
    }

    /**
     * Removes a bookmark by its path.
     *
     * @param path The path to remove.
     * @param move Indicates whether this remove operation is part of a move operation, to prevent the deletion of data.
     *
     * @return True if removed.
     */
    public synchronized boolean removeBookmark(String path, boolean move) {
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
            if (!move) {
                remove(n);
            }
            return true;
        }
        return false;
    }

    /**
     * Remove all bookmarks with the given name.
     *
     * @param name The name to remove.
     *
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
                    remove(bookmark);
                    nRemoved++;
                } else {
                    nRemoved += removeBookmarksByNameRec(name, bookmark, it);
                }
            }
            return nRemoved;
        }
        return 0;
    }

    private synchronized int removeBookmarksByNameRec(String name,
                                                      BookmarkNode bookmark,
                                                      Iterator<BookmarkNode> itr) {
        int nRemoved = 0;
        if (bookmark.name.equals(name)) {
            // Remove from parent
            itr.remove();
            bookmark.parent = null;
            nodes.remove(bookmark.path);
            remove(bookmark);
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

    /**
     * Generates a unique identifier for the bookmark.
     *
     * @return A unique identifier for the bookmark, based on Java's {@link UUID} class.
     */
    private String generateId(boolean needsId) {
        return needsId ? UUID.randomUUID().toString() : null;
    }

    @Override
    public void notify(final Event event,
                       Object source,
                       final Object... data) {
        switch (event) {
            case BOOKMARKS_ADD -> {
                Object d0 = data[0];
                if (d0 instanceof String name) {
                    // Simple object bookmark.
                    boolean folder = (boolean) data[1];
                    if (addBookmark(name, folder)) {
                        persistBookmarks();
                        logger.info(I18n.msg("gui.bookmark.add.ok", name));
                    } else {
                        logger.error(I18n.msg("gui.bookmark.add.error", name));
                    }
                } else {
                    // Position bookmark.
                    Vector3b pos = data[0] != null ? (Vector3b) d0 : null;
                    Vector3d dir = data[1] != null ? (Vector3d) data[1] : null;
                    Vector3d up = data[2] != null ? (Vector3d) data[2] : null;
                    Instant t = data[3] != null ? (Instant) data[3] : null;
                    Settings s = data[4] != null ? (Settings) data[4] : null;
                    String name = (String) data[5];
                    boolean folder = (boolean) data[6];

                    // Generate ID to link settings.
                    String id = generateId(s != null);

                    // Persist settings.
                    if (s != null) {
                        Path parent = SysUtils.getDefaultBookmarksDir().resolve("settings");
                        Path settingsFile = parent.resolve(id);
                        try {
                            Files.createDirectories(parent);
                            Files.deleteIfExists(settingsFile);
                        } catch (IOException e) {
                            logger.error(I18n.msg("error.directory.create", parent.toAbsolutePath().toString()), e);
                        }
                        SettingsManager.persistSettings(s, settingsFile.toFile());
                    }
                    String text = String.format("{%s|%s|%s|%s|%s|%s}", str(pos), str(dir), str(up), str(t), name, str(id));
                    if (addBookmark(text, folder)) {
                        persistBookmarks();
                        logger.info(I18n.msg("gui.bookmark.add.ok", text));
                    } else {
                        logger.error(I18n.msg("gui.bookmark.add.error", text));
                    }
                }
            }
            case BOOKMARKS_REMOVE -> {
                String name = (String) data[0];
                if (removeBookmark(name, false)) {
                    persistBookmarks();
                    logger.info(I18n.msg("gui.bookmark.remove.ok", name));
                }
            }
            case BOOKMARKS_REMOVE_ALL -> {
                String name = (String) data[0];
                int removed = removeBookmarksByName(name);
                if (removed > 0) {
                    persistBookmarks();
                    logger.info(I18n.msg("gui.bookmark.remove.name.ok", removed, name));
                }
            }
            case BOOKMARKS_MOVE -> {
                BookmarkNode src = (BookmarkNode) data[0];
                BookmarkNode dest = (BookmarkNode) data[1];
                if (dest == null) {
                    // Move to root
                    removeBookmark(src.path.toString(), true);
                    addBookmark(src.text, false);
                    persistBookmarks();
                } else {
                    // Move to destination folder
                    if (dest.folder) {
                        removeBookmark(src.path.toString(), true);
                        addBookmark(dest.path.resolve(src.text).toString(), false);
                        persistBookmarks();
                    } else {
                        logger.error(I18n.msg("error.destination.notdir", dest));
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
                    persistBookmarks();
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
                    persistBookmarks();
                }
            }
            default -> {
            }
        }
    }

    private String str(Vector3b v) {
        return v != null ? "[" + v.x.toString() + "," + v.y.toString() + "," + v.z.toString() + "]" : "null";
    }

    private String str(Vector3d v) {
        return v != null ? "[" + v.x + "," + v.y + "," + v.z + "]" : "null";
    }

    private String str(Instant t) {
        return t != null ? t.toString() : "null";
    }

    private String str(String s) {
        return s != null ? s : "null";
    }

    public static class BookmarkNode {

        /**
         * Token to use for null values.
         */
        private static final String NULL_TOKEN = "null";

        /**
         * Regular expression for a nullable vector with three components.
         */
        private static final String VEC3_REGEX =
                "(" + NULL_TOKEN + "|\\[[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?,[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?,[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?])";
        /**
         * Regular expression that defines the format of positional bookmarks, which is:
         * <p><code>{[x,y,z]|[dx,dy,dz]|[ux,uy,uz]|instant|name|id}</code></p>
         * All terms may be null.
         */
        public static final String POS_BOOKMARK_REGEX = "\\{" + VEC3_REGEX +
                "\\|" + VEC3_REGEX +
                "\\|" + VEC3_REGEX +
                "\\|(?:" + NULL_TOKEN + "|\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z)" +
                "\\|[^|,\\\\]+" +
                "(\\|(?:" + NULL_TOKEN + "|[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}))?}";

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
         * Time, for positional bookmarks.
         */
        public Instant time;
        /**
         * The UUID, for positional bookmarks.
         */
        public String uuid;
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

        public BookmarkNode(Path path,
                            boolean folder) {
            this.path = path;
            this.text = this.path.getFileName().toString().strip();
            this.folder = folder;
            initializeText();
        }

        public void initializeText() {
            if (this.text != null) {
                if (this.text.matches(POS_BOOKMARK_REGEX)) {
                    // Location bookmark.
                    var tokens = this.text.substring(1, this.text.length() - 1).split("\\|");
                    var pos = tokens[0];
                    var dir = tokens[1];
                    var up = tokens[2];
                    var instant = tokens[3];
                    var name = tokens[4];
                    var uuid = tokens.length > 5 ? tokens[5] : null;

                    // Name can't be null.
                    this.name = name;
                    // These are nullable.
                    this.uuid = uuid != null && uuid.equals(NULL_TOKEN) ? null : uuid;
                    this.position = vectorFromString(pos);
                    this.direction = vectorFromString(dir);
                    this.up = vectorFromString(up);
                    this.time = instant.equals(NULL_TOKEN) ? null : Instant.parse(instant);
                } else {
                    // Regular bookmark, only object name.
                    this.name = this.text;
                }
            }
        }

        /**
         * This method loads the settings file for this bookmark.
         * This operation should happen every time a bookmark is made 'active'.
         */
        public Settings loadSettingsFromFile() {
            // Load settings file into Settings object.
            if (this.uuid != null) {
                Path parent = SysUtils.getDefaultBookmarksDir().resolve("settings");
                Path settingsFile = parent.resolve(this.uuid);
                try {
                    var settings = SettingsManager.instance.loadSettings(settingsFile.toFile());
                    settings.setParent(settings);
                    // Runtime.
                    settings.runtime = Settings.settings.runtime.clone();
                    settings.runtime.inputEnabled = true;
                    settings.runtime.setParent(settings);
                    return settings;
                } catch (IOException e) {
                    logger.error(String.format("Could not load settings file: %s", settingsFile.toAbsolutePath().toString()), e);
                }
            }
            return null;
        }

        private Vector3d vectorFromString(String vectorString) {
            if (vectorString.equals(NULL_TOKEN)) {
                return null;
            }
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
