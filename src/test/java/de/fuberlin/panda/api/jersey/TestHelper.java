package de.fuberlin.panda.api.jersey;

/*
 * #%L
 * PANDA-DEEPLINKING
 * %%
 * Copyright (C) 2014 Freie Universitaet Berlin
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * Helper class to read and write UTF8 encoded files.
 * 
 * @author Christoph Schröder
 *
 */
public class TestHelper {

    public static String getTestFile(String filePath) {
        StringBuilder testFile = new StringBuilder();
        File file = new File(filePath);

        try {
            InputStreamReader streamReader = new InputStreamReader(new FileInputStream(file), "UTF8");

            int c = 0;
            do {
                c = streamReader.read();
                if (!(c<0)){
                    testFile.append((char)c);
                }
            } while (!(c<0));

            streamReader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return testFile.toString();
    }

    public static void writeTestFile(String filePath, String content) {
        File file = new File(filePath);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            Writer out = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file), "UTF8"));
            out.write(content);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
