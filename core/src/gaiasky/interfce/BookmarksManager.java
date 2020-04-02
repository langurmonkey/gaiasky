package gaiasky.interfce;

import gaiasky.desktop.util.SysUtils;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.util.GlobalConf;
import gaiasky.util.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages the bookmarks in Gaia Sky. Supports folders.
 */
public class BookmarksManager implements IObserver {
    private static Logger.Log logger = Logger.getLogger(BookmarksManager.class);

    public static BookmarksManager instance;

    public static void initialize() {
        if (instance == null) {
            instance = new BookmarksManager();
        }
    }

    public static BookmarksManager instance() {
        initialize();
        return instance;
    }

    public static List<BNode> getBookmarks() {
        initialize();
        return instance.bookmarks;
    }

    public class BNode {
        // The name of this node
        public String name;
        // The full path
        public Path path;
        // The parent of this node, null if root
        public BNode parent;
        // Children, if any
        public List<BNode> children;
        // Is it a folder?
        public boolean folder;

        public BNode(String path, boolean folder) {
            this.path = Path.of(path);
            this.name = this.path.getFileName().toString();
            this.folder = folder;
        }

        public BNode(Path path, boolean folder) {
            this.path = path;
            this.name = this.path.getFileName().toString();
            this.folder = folder;
        }

        public void insert(BNode node) {
            if (node != null) {
                initChildren();
                if(!children.contains(node)) {
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

        public BNode getFirstFolderAncestor(){
            if(folder)
                return this;
            else if (parent != null)
                return parent.getFirstFolderAncestor();
            else
                return null;
        }

        public boolean isDescendantOf(BNode other) {
            BNode current = this;
            while (current != null) {
                if (current == other) {
                    return true;
                }
                current = current.parent;
            }
            return false;
        }
    }

    private Path bookmarksFile;
    private List<BNode> bookmarks;
    private Map<Path, BNode> nodes;

    private BookmarksManager() {
        initDefault();
        EventManager.instance.subscribe(this, Events.BOOKMARKS_ADD, Events.BOOKMARKS_REMOVE, Events.BOOKMARKS_MOVE);
    }

    private void initDefault() {
        final String bookmarksFileName = "bookmarks.txt";

        Path customBookmarks = SysUtils.getDefaultBookmarksDir().resolve(bookmarksFileName);
        Path defaultBookmarks = Paths.get(GlobalConf.ASSETS_LOC, SysUtils.getBookmarksDirName(), bookmarksFileName);
        bookmarksFile = customBookmarks;
        if (!Files.exists(customBookmarks)) {
            try {
                Files.copy(defaultBookmarks, customBookmarks, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                logger.error(e);
            }
        }
        logger.info("Using bookmarks file: " + bookmarksFile);

        bookmarks = loadBookmarks(bookmarksFile);
        if (bookmarks != null) {
            logger.info(bookmarks.size() + " bookmarks loaded");
        }

    }

    /**
     * @return A list with all folder bookmarks
     */
    public List<BNode> getFolders() {
        return getBookmarksByType(bookmarks, new ArrayList<>(), true);
    }

    /**
     * @return A list with all non-folder bookmarks
     */
    public List<BNode> getLeafBookmarks() {
        return getBookmarksByType(bookmarks, new ArrayList<>(), false);
    }

    public List<BNode> getBookmarksByType(List<BNode> bookmarks, List<BNode> result, boolean folder) {
        if (bookmarks != null) {
            for (BNode bookmark : bookmarks) {
                if (bookmark.folder == folder)
                    result.add(bookmark);
                getBookmarksByType(bookmark.children, result, folder);
            }
        }
        return result;
    }

    private List<BNode> loadBookmarks(Path file) {
        try (Stream<String> lines = Files.lines(file)) {
            List<String> bmks = lines.filter(line -> !line.strip().startsWith("#"))
                    .filter(line -> !line.isBlank())
                    .map(line -> line.strip())
                    .collect(Collectors.toList());

            nodes = new HashMap<>();
            bookmarks = new ArrayList<>();
            for (String bookmark : bmks) {
                insertBookmark(bookmark, false);
            }
            return bookmarks;
        } catch (IOException e) {
            logger.error(e);
        }
        return null;
    }

    public boolean containsName(String name) {
        boolean contains = false;
        for (BNode bookmark : bookmarks) {
            contains = contains || containsNameRec(name, bookmark);
        }
        return contains;
    }

    public boolean containsNameRec(String name, BNode node) {
        if (node.name.equals(name)) {
            return true;
        } else if (node.children != null) {
            boolean contains = false;
            for (BNode child : node.children) {
                contains = contains | containsNameRec(name, child);
            }
            return contains;
        }
        return false;
    }

    public boolean containsPath(String path) {
        return containsPath(Path.of(path));
    }

    public boolean containsPath(Path path) {
        return nodes != null && nodes.containsKey(path);
    }


    public synchronized void persistBookmarks() {
        persistBookmarks(bookmarksFile);
    }

    private synchronized void persistBookmarks(Path file) {
        if (bookmarks != null) {
            String content = "# Bookmarks file for Gaia Sky, one bookmark per line, folder separator: '/', comments: '#'";
            content += buildContent(bookmarks, "");

            try {
                Files.delete(file);
                Files.write(file, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                logger.error(e);
            }
        }
    }

    private String buildContent(List<BNode> bmks, String content) {
        for (BNode b : bmks) {
            if (b.children == null || b.children.isEmpty()) {
                // Write
                content += "\n" + b.path.toString();
            } else {
                // Down one level
                content += buildContent(b.children, "");
            }
        }
        return content;
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

    private synchronized boolean insertBookmark(String path, boolean folder) {
        Path p = Path.of(path);
        if (!nodes.containsKey(p)) {
            BNode bnode = new BNode(p, folder);
            nodes.put(p, bnode);
            BNode curr = bnode;
            while (true) {
                if (curr.path.getParent() != null) {
                    if (!nodes.containsKey(curr.path.getParent())) {
                        // Add
                        BNode pnode = new BNode(curr.path.getParent(), true);
                        nodes.put(pnode.path, pnode);
                    }
                    // Insert
                    BNode parentNode = nodes.get(curr.path.getParent());
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
        Path p = Path.of(path);
        if (nodes != null && nodes.containsKey(p)) {
            BNode n = nodes.get(p);
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
     * @return True if removed one or more bookmarks.
     */
    public synchronized boolean removeBookmarksByName(String name) {
        int nRemoved = 0;
        if (bookmarks != null && !bookmarks.isEmpty()) {
            Iterator<BNode> it = bookmarks.iterator();
            while (it.hasNext()) {
                BNode bookmark = it.next();
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
            return nRemoved > 0;
        }
        return false;
    }

    private synchronized int removeBookmarksByNameRec(String name, BNode bookmark, Iterator<BNode> itr) {
        int nRemoved = 0;
        if (bookmark.name.equals(name)) {
            // Remove from parent
            itr.remove();
            bookmark.parent = null;
            nodes.remove(bookmark.path);
            nRemoved++;
        } else if (bookmark.children != null) {
            Iterator<BNode> it = bookmark.children.iterator();
            while (it.hasNext()) {
                BNode child = it.next();
                nRemoved += removeBookmarksByNameRec(name, child, it);
            }
        }
        return nRemoved;
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
            case BOOKMARKS_ADD:
                String name = (String) data[0];
                boolean folder = (boolean) data[1];
                if (addBookmark(name, folder))
                    logger.info("Bookmark added: " + name);
                break;
            case BOOKMARKS_REMOVE:
                name = (String) data[0];
                if (removeBookmark(name))
                    logger.info("Bookmark removed: " + name);
                break;
            case BOOKMARKS_MOVE:
                BNode src = (BNode) data[0];
                BNode dest = (BNode) data[1];
                if(dest == null){
                    // Move to root
                    removeBookmark(src.path.toString());
                    addBookmark(src.name, false);
                } else {
                    // Move to dest folder
                    if (dest.folder) {
                        removeBookmark(src.path.toString());
                        addBookmark(dest.path.resolve(src.name).toString(), false);
                    } else {
                        logger.error("Destination is not a folder: " + dest);
                    }
                }
                break;
            default:
                break;
        }
    }
}
