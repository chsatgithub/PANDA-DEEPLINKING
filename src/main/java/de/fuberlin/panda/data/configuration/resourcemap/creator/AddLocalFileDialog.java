package de.fuberlin.panda.data.configuration.resourcemap.creator;

/*
 * #%L
 * PANDA-DEEPLINKING
 * %%
 * Copyright (C) 2014 Freie Universität Berlin
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


import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Standard dialog to add new resources from local files.
 * 
 * @author Christoph Schröder
 */
public class AddLocalFileDialog extends JFileChooser {
    /**
     * 
     */
    private static final long       serialVersionUID = -8334839839564577273L;

    // Separator chars for CSV files
    private HashMap<String, String> separators       = new LinkedHashMap<String, String>();

    // Input components
    private String[]                sepNames         = { "Semicolon", "Tabulator", "Colon",
            "Comma", "Space"                        };
    private JPanel                  accessoryPanel   = new JPanel(new BorderLayout());
    private JPanel                  sepPanel         = new JPanel(new BorderLayout());
    private JComboBox<String>       sepChooser       = new JComboBox<String>(sepNames);
    private JLabel                  sepLabel         = new JLabel("Sepator");

    AddLocalFileDialog() {
        initSeparatorMap();
        FileFilter resourceFilter = new FileNameExtensionFilter("PANDA Resources", "xls", "xlsx",
                "xml", "doc", "docx", "html", "htm", "pdf", "csv");
        this.setMultiSelectionEnabled(true);
        this.setFileSelectionMode(JFileChooser.FILES_ONLY);
        this.setFileFilter(resourceFilter);
        this.setAcceptAllFileFilterUsed(false);
        this.setApproveButtonText("Add to Resources");

        // Configure and add accessories
        sepPanel.add(sepLabel, BorderLayout.NORTH);
        sepPanel.add(sepChooser, BorderLayout.SOUTH);
        accessoryPanel.add(sepPanel, BorderLayout.SOUTH);
        this.setAccessory(accessoryPanel);
        this.setSize(300, 200);
    }

    /**
     * Initialization of map used to get selected separator char
     */
    private void initSeparatorMap() {
        separators.put("Comma", "\u002C");
        separators.put("Tabulator", "\u0009");
        separators.put("Colon", "\u003A");
        separators.put("Semicolon", "\u003B");
        separators.put("Space", "\u0020");
    }

    /**
     * Returns the selected separator for CSV files.
     * 
     * @return configured delimiter of CSV file
     */
    public String getSeparator() {
        String separator = separators.get(sepChooser.getSelectedItem());
        return separator;
    }

}
