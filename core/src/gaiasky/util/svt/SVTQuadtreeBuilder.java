package gaiasky.util.svt;

import gaiasky.util.Logger;
import gaiasky.util.comp.FilenameComparator;
import gaiasky.util.i18n.I18n;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * <p>
 * Builds a quadtree from a file system path.
 * </p>
 * <p>
 * The SVT is laid out in a quadtree where each tile is subdivided into four at each level. In the file system,
 * tiles are grouped by level. Each level is in its own directory with the format:
 * </p>
 * <ud>
 * <li>level\d+ -> (level3)</li>
 * </ud>
 * <p>The files use the format:</p>
 * <ud>
 * <li>tx[_|-| ]\d+[_|-| ]\d+\.\w+ -> (tx_0_2.jpg)</li>
 * </ud>
 * <p>The tiles can be in any of the supported image formats in Gaia Sky.</p>
 */
public class SVTQuadtreeBuilder {
    private static final Logger.Log logger = Logger.getLogger(SVTQuadtreeBuilder.class);

    public SVTQuadtreeBuilder() {

    }

    /**
     * Creates a new SVT quadtree and initializes it with the given file system location and
     * the given tile size.
     *
     * @param location The location where the levels are. A directory for each level is expected within this
     *                 location, with the name "level[num]". Usually, "level0" is mandatory.
     * @param tileSize The size (width and height) of each tile in the SVT.
     *
     * @return The SVT quadtree object.
     */
    public SVTQuadtree<Path> build(final Path location, final int tileSize) {
        var comp = new FilenameComparator();
        var tree = new SVTQuadtree<Path>(tileSize, 2);

        var level0 = location.resolve("level0");
        if (!Files.exists(level0)) {
            logger.error("Can't initialize SVT without 'level0' directory: " + location);
            return null;
        }
        logger.info(I18n.msg("notif.loading", "SVT quadtree: " + location));
        try (Stream<Path> stream = Files.list(location)) {
            final AtomicInteger depth = new AtomicInteger(0);
            stream.sorted(comp).forEach(directory -> {
                var dirName = directory.getFileName().toString();
                if (dirName.matches("level\\d+")) {
                    var level = Integer.parseInt(dirName.substring(5));
                    logger.debug("visit: " + dirName + " (l" + level + ")");
                    try (Stream<Path> files = Files.list(directory)) {
                        files.sorted(comp).forEach(file -> {
                            var fileName = file.getFileName().toString();
                            // Accepted file names: tx[_|-| ]COLNUM[_|-| ]ROWNUM.ext
                            if (fileName.matches("tx[_\\-\\s.]\\d+[_\\-\\s.]\\d+\\.\\w+")) {
                                String[] tokens = fileName.split("[_\\-\\s.]");
                                int col = Integer.parseInt(tokens[1].trim());
                                int row = Integer.parseInt(tokens[2].trim());
                                logger.debug("l" + level + " -> col: " + col + " row: " + row);
                                tree.insert(level, col, row, file);
                                if (level > depth.get()) {
                                    depth.set(level);
                                }
                            } else {
                                logger.error("Wrong tile name format: " + fileName);
                            }
                        });
                    } catch (IOException e) {
                        logger.error(e, "Error building SVT quadtree: " + location);
                    }
                } else {
                    logger.warn("Wrong directory name format, skipping: " + dirName);
                }
            });
            logger.debug("Initialized SVT quadtree with " + depth + " levels and " + tree.numTiles + " tiles.");
        } catch (Exception e) {
            logger.error(e, "Error building SVT quadtree: " + location);
            return null;
        }
        return tree;
    }
}
