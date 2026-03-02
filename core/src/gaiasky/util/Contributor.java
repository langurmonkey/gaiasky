/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** A contributor in the CONTRIBUTORS.dm file. **/
public record Contributor(String name, String contact, String contribution) {

    /**
     * Parses a CONTRIBUTORS.md file with Markdown table format.
     *
     * @param path Path to the CONTRIBUTORS.md file
     *
     * @return List of Contributor records
     */
    public static List<Contributor> parseContributorsFile(Path path) throws IOException {
        List<Contributor> contributors = new ArrayList<>();
        Pattern pipeSplit = Pattern.compile("\\s*\\|\\s*");

        List<String> lines = Files.lines(path)
                .map(String::trim)
                .filter(line -> line.startsWith("|") && line.endsWith("|"))
                .skip(2) // Skip header and separator rows
                .toList();

        for (String line : lines) {
            String[] parts = pipeSplit.split(line);
            if (parts.length >= 3) {
                contributors.add(new Contributor(
                        parts[1].trim(),  // Name
                        parts[2].trim(),  // Contact
                        parts[3].trim()   // Contribution
                ));
            }
        }
        return contributors;
    }

    // Optional: Helper to find contributors by language
    public List<Contributor> findByTranslation(String language) {
        return List.of(this).stream()
                .filter(c -> c.contribution.toLowerCase().contains(language.toLowerCase() + " translation"))
                .collect(Collectors.toList());
    }
}
