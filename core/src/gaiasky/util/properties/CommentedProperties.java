/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.properties;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class CommentedProperties extends java.util.Properties {

    private static final char[] hexDigit = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
    /**
     * Use a Vector to keep a copy of lines that are a comment or 'blank'
     */
    public Vector<String> lineData = new Vector<>(0, 1);
    /**
     * Use a Vector to keep a copy of lines containing a key, i.e. they are a property.
     */
    public Vector<String> keyData = new Vector<>(0, 1);

    private static char toHex(int var0) {
        return hexDigit[var0 & 15];
    }

    /**
     * Load properties from the specified InputStreamReader.
     * Overload the load method in Properties so we can keep comment and blank lines.
     *
     * @param isr The InputStreamReader to read.
     */
    public void load(InputStreamReader isr) throws IOException {
        BufferedReader reader =
                new BufferedReader(isr);
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            char c = 0;
            int pos = 0;

            // Skip BOM character
            if (!line.isEmpty() && line.charAt(0) == '\uFEFF') {
                pos++;
            }

            // If empty line or begins with a comment character, save this line
            // in lineData and save a "" in keyData.
            if ((line.length() - pos) == 0
                    || line.charAt(pos) == '#' || line.charAt(pos) == '!') {
                lineData.add(line);
                keyData.add("");
                continue;
            }

            // The characters up to the next Whitespace, ':', or '='
            // describe the key.  But look for escape sequences.
            // Try to short-circuit when there is no escape char.
            int start = pos;
            boolean needsEscape = line.indexOf('\\', pos) != -1;
            StringBuffer key = needsEscape ? new StringBuffer() : null;

            while (pos < line.length()
                    && !Character.isWhitespace(c = line.charAt(pos++))
                    && c != '=' && c != ':') {
                if (needsEscape && c == '\\') {
                    if (pos == line.length()) {
                        // The line continues on the next line.  If there
                        // is no next line, just treat it as a key with an
                        // empty value.
                        line = reader.readLine();
                        if (line == null)
                            line = "";
                        pos = 0;
                        while (pos < line.length()
                                && Character.isWhitespace(c = line.charAt(pos)))
                            pos++;
                    } else {
                        c = line.charAt(pos++);
                        switch (c) {
                        case 'n':
                            key.append('\n');
                            break;
                        case 't':
                            key.append('\t');
                            break;
                        case 'r':
                            key.append('\r');
                            break;
                        case 'u':
                            if (pos + 4 <= line.length()) {
                                char uni = (char) Integer.parseInt
                                        (line.substring(pos, pos + 4), 16);
                                key.append(uni);
                                pos += 4;
                            } // else throw exception?
                            break;
                        default:
                            key.append(c);
                            break;
                        }
                    }
                } else if (needsEscape)
                    key.append(c);
            }

            boolean isDelimiter = (c == ':' || c == '=');

            String keyString;
            if (needsEscape)
                keyString = key.toString();
            else if (isDelimiter || Character.isWhitespace(c))
                keyString = line.substring(start, pos - 1);
            else
                keyString = line.substring(start, pos);

            while (pos < line.length()
                    && Character.isWhitespace(c = line.charAt(pos)))
                pos++;

            if (!isDelimiter && (c == ':' || c == '=')) {
                pos++;
                while (pos < line.length()
                        && Character.isWhitespace(c = line.charAt(pos)))
                    pos++;
            }

            // Short-circuit if no escape chars found.
            if (!needsEscape) {
                put(keyString, line.substring(pos));
                // Save a "" in lineData and save this
                // keyString in keyData.
                lineData.add("");
                keyData.add(keyString);
                continue;
            }

            // Escape char found so iterate through the rest of the line.
            StringBuilder element = new StringBuilder(line.length() - pos);
            while (pos < line.length()) {
                c = line.charAt(pos++);
                if (c == '\\') {
                    if (pos == line.length()) {
                        // The line continues on the next line.
                        line = reader.readLine();

                        // We might have seen a backslash at the end of
                        // the file.  The JDK ignores the backslash in
                        // this case, so we follow for compatibility.
                        if (line == null)
                            break;

                        pos = 0;
                        while (pos < line.length()
                                && Character.isWhitespace(line.charAt(pos)))
                            pos++;
                        element.ensureCapacity(line.length() - pos +
                                element.length());
                    } else {
                        c = line.charAt(pos++);
                        switch (c) {
                        case 'n':
                            element.append('\n');
                            break;
                        case 't':
                            element.append('\t');
                            break;
                        case 'r':
                            element.append('\r');
                            break;
                        case 'u':
                            if (pos + 4 <= line.length()) {
                                char uni = (char) Integer.parseInt
                                        (line.substring(pos, pos + 4), 16);
                                element.append(uni);
                                pos += 4;
                            } // else throw exception?
                            break;
                        default:
                            element.append(c);
                            break;
                        }
                    }
                } else
                    element.append(c);
            }
            put(keyString, element.toString());
            // Save a "" in lineData and save this
            // keyString in keyData.
            lineData.add("");
            keyData.add(keyString);
        }
    }

    /**
     * Load properties from the specified InputStream.
     * Overload the load method in Properties so we can keep comment and blank lines.
     *
     * @param inStream The InputStream to read.
     */
    public void load(InputStream inStream) throws IOException {
        load(new InputStreamReader(inStream, StandardCharsets.UTF_8));
    }

    public void store(OutputStream out, String header) throws IOException {
        // The spec says that the file must be encoded using UTF-8.
        store(out, header, "UTF-8");
    }

    public void store(OutputStream out, String header, String encoding, Map<String, String> missing) throws IOException {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, encoding));

        // We ignore the header, because if we prepend a commented header
        // then read it back in it is now a comment, which will be saved
        // and then when we write again we would prepend Another header...

        String line;

        Set<String> keys = new HashSet<>(size());
        for (Object k : keySet()) {
            keys.add((String) k);
        }

        for (int i = 0; i < lineData.size(); i++) {
            line = lineData.get(i);
            String key = keyData.get(i);
            if (key.length() > 0) { // This is a 'property' line, so rebuild it
                String k = saveConvert(key, true, false);
                if (get(key) != null && (missing == null || !missing.containsKey(key))) {
                    String val = saveConvert((String) get(key), false, false);
                    writer.println(k + '=' + val);
                } else {
                    if (missing == null || missing.isEmpty()) {
                        writer.println('#' + k + '=');
                    } else {
                        if (missing.containsKey(key)) {
                            writer.println('#' + k + '=' + missing.get(key));
                        } else {
                            writer.println('#' + k + '=');
                        }
                    }
                }
            } else { // was a blank or comment line, so just restore it
                writer.println(line);
            }
            // Remove from set
            keys.remove(key);
        }

        // Print rest of props at the end
        if (!keys.isEmpty()) {
            writer.println("# Remaining new properties");
            for (String key : keys) {
                if (key.length() > 0) {
                    String k = saveConvert(key, true, false);
                    if (get(key) != null && (missing == null || !missing.containsKey(key))) {
                        String val = saveConvert((String) get(key), false, false);
                        writer.println(k + '=' + val);
                    } else {
                        if (missing == null || missing.isEmpty()) {
                            writer.println('#' + k + '=');
                        } else {
                            if (missing.containsKey(key)) {
                                writer.println('#' + k + '=' + missing.get(key));
                            } else {
                                writer.println('#' + k + '=');
                            }
                        }
                    }
                }
            }
        }
        writer.flush();
    }

    /**
     * Write the properties to the specified OutputStream.
     * <p>
     * Overloads the store method in Properties so we can put back comment
     * and blank lines.
     *
     * @param out      The OutputStream to write to.
     * @param header   Ignored, here for compatibility w/ Properties.
     * @param encoding The encoding of the file
     *
     * @throws IOException If the creation of the writer fails.
     */
    public void store(OutputStream out, String header, String encoding) throws IOException {
        store(out, header, encoding, null);
    }

    private String saveConvert(String var1, boolean var2, boolean var3) {
        int var4 = var1.length();
        int var5 = var4 * 2;
        if (var5 < 0) {
            var5 = 2147483647;
        }

        StringBuilder var6 = new StringBuilder(var5);

        for (int var7 = 0; var7 < var4; ++var7) {
            char var8 = var1.charAt(var7);
            if (var8 > '=' && var8 < 127) {
                if (var8 == '\\') {
                    var6.append('\\');
                    var6.append('\\');
                } else {
                    var6.append(var8);
                }
            } else {
                switch (var8) {
                case '\t' -> {
                    var6.append('\\');
                    var6.append('t');
                    continue;
                }
                case '\n' -> {
                    var6.append('\\');
                    var6.append('n');
                    continue;
                }
                case '\f' -> {
                    var6.append('\\');
                    var6.append('f');
                    continue;
                }
                case '\r' -> {
                    var6.append('\\');
                    var6.append('r');
                    continue;
                }
                case ' ' -> {
                    if (var7 == 0 || var2) {
                        var6.append('\\');
                    }
                    var6.append(' ');
                    continue;
                }
                case '=' -> {
                    var6.append('\\');
                    var6.append(var8);
                    continue;
                }
                }

                if ((var8 < ' ' || var8 > '~') & var3) {
                    var6.append('\\');
                    var6.append('u');
                    var6.append(toHex(var8 >> 12 & 15));
                    var6.append(toHex(var8 >> 8 & 15));
                    var6.append(toHex(var8 >> 4 & 15));
                    var6.append(toHex(var8 & 15));
                } else {
                    var6.append(var8);
                }
            }
        }

        return var6.toString();
    }

    /**
     * Need this method from Properties because original code has StringBuilder,
     * which is an element of Java 1.5, used StringBuffer instead (because
     * this code was written for Java 1.4)
     *
     * @param str    - the string to format
     * @param buffer - buffer to hold the string
     * @param key    - true if str the key is formatted, false if the value is formatted
     */
    private void formatForOutput(String str, StringBuffer buffer, boolean key) {
        if (key) {
            buffer.setLength(0);
            buffer.ensureCapacity(str.length());
        } else
            buffer.ensureCapacity(buffer.length() + str.length());
        boolean head = true;
        int size = str.length();
        for (int i = 0; i < size; i++) {
            char c = str.charAt(i);
            switch (c) {
            case '\n':
                buffer.append("\\n");
                break;
            case '\r':
                buffer.append("\\r");
                break;
            case '\t':
                buffer.append("\\t");
                break;
            case ' ':
                buffer.append(head ? "\\ " : " ");
                break;
            case '\\':
            case '!':
            case '#':
            case '=':
            case ':':
                buffer.append('\\').append(c);
                break;
            default:
                if (c < ' ' || c > '~') {
                    String hex = Integer.toHexString(c);
                    buffer.append("\\u0000", 0, 6 - hex.length());
                    buffer.append(hex);
                } else
                    buffer.append(c);
            }
            if (c != ' ')
                head = key;
        }
    }

    /**
     * Add a Property to the end of the CommentedProperties.
     *
     * @param keyString The Property key.
     * @param value     The value of this Property.
     */
    public void add(String keyString, String value) {
        put(keyString, value);
        lineData.add("");
        keyData.add(keyString);
    }

    /**
     * Add a comment or blank line or comment to the end of the CommentedProperties.
     *
     * @param line The string to add to the end, make sure this is a comment
     *             or a 'whitespace' line.
     */
    public void addLine(String line) {
        lineData.add(line);
        keyData.add("");
    }

    public CommentedProperties clone() {
        CommentedProperties cl = new CommentedProperties();

        // Copy props
        Set<Object> keys = this.keySet();
        for (Object key : keys) {
            cl.setProperty((String) key, this.getProperty((String) key));
        }

        cl.lineData = lineData;
        cl.keyData = keyData;

        return cl;

    }
}
