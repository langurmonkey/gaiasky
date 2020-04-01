package gaiasky.interfce;

import gaiasky.desktop.util.SysUtils;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.scenegraph.SceneGraphNode;
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

        public BNode(String path) {
            this.path = Path.of(path);
            this.name = this.path.getFileName().toString();
        }

        public BNode(Path path) {
            this.path = path;
            this.name = this.path.getFileName().toString();
        }

        public void insert(BNode node) {
            if (node != null) {
                initChildren();
                children.add(0, node);
                node.parent = this;
            }
        }

        private void initChildren() {
            if (children == null)
                children = new ArrayList<>(4);
        }
    }

    private Path bookmarksFile;
    private List<BNode> bookmarks;
    private Map<Path, BNode> nodes;

    private BookmarksManager() {
        initDefault();
        EventManager.instance.subscribe(this, Events.BOOKMARKS_ADD, Events.BOOKMARKS_REMOVE);
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

    private List<BNode> loadBookmarks(Path file) {
        try (Stream<String> lines = Files.lines(file)) {
            List<String> bmks = lines.filter(line -> !line.strip().startsWith("#"))
                    .filter(line -> !line.isBlank())
                    .map(line -> line.strip())
                    .collect(Collectors.toList());

            nodes = new HashMap<>();
            bookmarks = new ArrayList<>();
            for (String bookmark : bmks) {
                insertBookmark(bookmark);
            }
            return bookmarks;
        } catch (IOException e) {
            logger.error(e);
        }
        return null;
    }

    private void insertBookmark(String bookmark) {
        Path path = Path.of(bookmark);
        if (!nodes.containsKey(path)) {
            BNode bnode = new BNode(path);
            nodes.put(path, bnode);
            BNode curr = bnode;
            while (true) {
                if (curr.path.getParent() != null) {
                    if (!nodes.containsKey(curr.path.getParent())) {
                        // Add
                        BNode pnode = new BNode(curr.path.getParent());
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
        }
    }

    private void persistBookmarks(Path file) {
        if (bookmarks != null) {
            String content = "# Bookmarks file for Gaia Sky, one bookmark per line";
            content = buildContent(bookmarks, content);

            try {
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
                content += buildContent(b.children, content);
            }
        }
        return content;
    }

    public void addBookmark(String element) {
        if (bookmarks != null) {
            insertBookmark(element);
            persistBookmarks(bookmarksFile);
        }
    }


    public void removeBookmark(String element) {
        if (nodes != null && nodes.containsKey(Path.of(element))) {
            BNode n = nodes.get(Path.of(element));
            if (n.parent != null) {
                n.parent.children.remove(n);
                n.parent = null;
            } else {
                bookmarks.remove(n);
            }
            persistBookmarks(bookmarksFile);
        }
    }

    public boolean isBookmark(String element) {
        return nodes != null && nodes.containsKey(Path.of(element));
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
            case BOOKMARKS_ADD:
                SceneGraphNode elem = (SceneGraphNode) data[0];
                addBookmark(elem.getName());
                break;
            case BOOKMARKS_REMOVE:
                elem = (SceneGraphNode) data[0];
                removeBookmark(elem.getName());
                break;
            default:
                break;
        }
    }
}
