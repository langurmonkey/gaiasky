/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.svt;

import gaiasky.util.Logger;
import gaiasky.util.comp.FilenameComparator;
import gaiasky.util.i18n.I18n;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class SVTQuadtreeBuilder {
    private static final Logger.Log logger = Logger.getLogger(SVTQuadtreeBuilder.class);

    public SVTQuadtreeBuilder() {

    }

    /**
     * Creates a new SVT quadtree and initializes it with the given file system location and
     * the given tile size.
     *
     * @param name     The name of the tree.
     * @param location The location where the levels are. A directory for each level is expected within this
     *                 location, with the name "level[num]". Usually, "level0" is mandatory.
     * @param tileSize The size (width and height) of each tile in the SVT.
     *
     * @return The SVT quadtree object.
     */
    public SVTQuadtree<Path> build(final String name, final Path location, final int tileSize) {
        var comp = new FilenameComparator();
        var tree = new SVTQuadtree<Path>(name, tileSize, 2);

        var level0 = location.resolve("level0");
        if (!Files.exists(level0)) {
            level0 = location.resolve("level00");
            if (!Files.exists(level0)) {
                logger.error("Can't initialize SVT without 'level0' or 'level00' directory: " + location);
                return null;
            }
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
            tree.depth = depth.get();
        } catch (Exception e) {
            logger.error(e, "Error building SVT quadtree: " + location);
            return null;
        }
        return tree;
    }
}
