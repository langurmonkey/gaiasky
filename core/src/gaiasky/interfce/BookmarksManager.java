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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages the bookmarks in Gaia Sky
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

    public static List<String> getBookmarks() {
        initialize();
        return instance.bookmarks;
    }

    private Path bookmarksFile;
    private List<String> bookmarks;

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

    private List<String> loadBookmarks(Path file) {
        try (Stream<String> lines = Files.lines(file)) {
            return lines.filter(line -> !line.strip().startsWith("#"))
                    .filter(line -> !line.isBlank())
                    .map(line -> line.strip())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error(e);
        }
        return null;
    }

    private void persistBookmarks(Path file) {
        if (bookmarks != null) {
            String content = "# Bookmarks file for Gaia Sky, one bookmark per line";
            for(String bookmark : bookmarks)
                content += "\n" + bookmark;

            try {
                Files.write(file, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                logger.error(e);
            }
        }
    }

    public void addBookmark(String element) {
        if (bookmarks != null && !bookmarks.contains(element)) {
            bookmarks.add(0, element);
            persistBookmarks(bookmarksFile);
        }
    }

    public void removeBookmark(String element) {
        if (bookmarks != null) {
            bookmarks.remove(element);
            persistBookmarks(bookmarksFile);
        }
    }

    public boolean isBookmark(String element){
        return bookmarks != null && bookmarks.contains(element);
    }

    @Override
    public void notify(Events event, Object... data) {
        switch(event){
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
