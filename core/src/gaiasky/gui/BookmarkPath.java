/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;
import java.util.Set;

/**
 * Implementation of bookmarks path.
 */
public class BookmarkPath implements Path {

    private static final String separator = "/";
    private final String[] tokens;
    private final Path parent;

    public BookmarkPath(String path) {
        if (path != null && !path.isEmpty()) {
            tokens = path.split(separator);
            if (tokens.length > 1) {
                parent = new BookmarkPath(Arrays.copyOf(tokens, tokens.length - 1));
            } else {
                parent = null;
            }
        } else {
            tokens = null;
            parent = null;
        }
    }

    public BookmarkPath(String[] tokens) {
        this.tokens = tokens;
        if (tokens != null && tokens.length > 1) {
            parent = new BookmarkPath(Arrays.copyOf(tokens, tokens.length - 1));
        } else {
            parent = null;
        }
    }

    @Override
    public FileSystem getFileSystem() {
        return new BookmarksFileSystem();
    }

    @Override
    public boolean isAbsolute() {
        return true;
    }

    @Override
    public Path getRoot() {
        if (tokens != null && tokens.length > 0) {
            return new BookmarkPath(tokens[0]);
        }
        return null;
    }

    @Override
    public Path getFileName() {
        if (tokens != null && tokens.length > 0) {
            return new BookmarkPath(tokens[tokens.length - 1]);
        }
        return null;
    }

    @Override
    public Path getParent() {
        return parent;
    }

    @Override
    public int getNameCount() {
        return 0;
    }

    @Override
    public Path getName(int i) {
        return null;
    }

    @Override
    public Path subpath(int i, int i1) {
        return null;
    }

    @Override
    public boolean startsWith(Path path) {
        return false;
    }

    @Override
    public boolean endsWith(Path path) {
        return false;
    }

    @Override
    public Path normalize() {
        return null;
    }

    @Override
    public Path resolve(Path path) {
        return null;
    }

    @Override
    public Path relativize(Path path) {
        return null;
    }

    @Override
    public URI toUri() {
        return null;
    }

    @Override
    public Path toAbsolutePath() {
        return this;
    }

    @Override
    public Path toRealPath(LinkOption... linkOptions) throws IOException {
        return null;
    }

    @Override
    public WatchKey register(WatchService watchService, WatchEvent.Kind<?>[] kinds, WatchEvent.Modifier... modifiers) throws IOException {
        return null;
    }

    @Override
    public int compareTo(Path path) {
        return 0;
    }

    @Override
    public String toString() {
        if (tokens == null || tokens.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.length; i++) {
            sb.append(tokens[i]);
            if (i < tokens.length - 1) {
                sb.append(separator);
            }
        }
        return sb.toString();
    }

    public class BookmarksFileSystem extends FileSystem {

        @Override
        public FileSystemProvider provider() {
            return null;
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public String getSeparator() {
            return "/";
        }

        @Override
        public Iterable<Path> getRootDirectories() {
            return null;
        }

        @Override
        public Iterable<FileStore> getFileStores() {
            return null;
        }

        @Override
        public Set<String> supportedFileAttributeViews() {
            return null;
        }

        @Override
        public Path getPath(String s, String... strings) {
            return null;
        }

        @Override
        public PathMatcher getPathMatcher(String s) {
            return null;
        }

        @Override
        public UserPrincipalLookupService getUserPrincipalLookupService() {
            return null;
        }

        @Override
        public WatchService newWatchService() throws IOException {
            return null;
        }
    }
}
