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
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.server.UID;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;

import de.fuberlin.panda.data.configuration.resourcemap.ResourceMapEntryType.ResourceInfo;
import de.fuberlin.panda.enums.DataResourceType;

/**
 * Custom dialog to add resources with advanced requirements.
 * 
 * @author Christoph Schröder
 */
public class AddResourceDialog extends JDialog implements ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = -3131827731113007813L;

    // Buttons
    JButton                   addButton        = new JButton("Add Resources");
    JButton                   cancelButton     = new JButton("Cancel");

    // Panels
    JPanel                    buttonPanel      = new JPanel(new GridLayout());
    JPanel                    configPanel      = new JPanel(new GridLayout());
    JPanel                    tablePanel       = new JPanel(new BorderLayout());

    // Input components
    String[]                  resourceTypes    = { "HTML", "XML" };
    JComboBox<String>         typeChooser      = new JComboBox<String>(resourceTypes);
    JFormattedTextField       urlTextField     = new JFormattedTextField();

    // Table
    ResourceTableModel        tableModel       = new ResourceTableModel();
    JTable                    resourceTable    = new JTable(tableModel);
    // TableRowSorter<TableModel> rowSorter = new TableRowSorter<TableModel>(
    // tableModel );
    JScrollPane               tableScrollPane  = new JScrollPane(resourceTable);

    public AddResourceDialog(JFrame mainWindow) {
        // set Dialog Layout
        this.setLayout(new BorderLayout());
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.setSize(480, 360);
        this.setLocationRelativeTo(mainWindow);

        // Buttons
        buttonPanel.add(addButton);
        buttonPanel.add(cancelButton);

        // Input components
        typeChooser.setSelectedIndex(0);
        urlTextField.setColumns(20);
        urlTextField.setDocument(new TextFieldLimiter(256));
        configPanel.add(typeChooser);
        configPanel.add(urlTextField);

        // Add Listener
        addButton.addActionListener(this);
        cancelButton.addActionListener(this);
        typeChooser.addActionListener(this);
        urlTextField.addActionListener(this);

        // Configure and add Table
        resourceTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        resourceTable.setFillsViewportHeight(true);
        resourceTable.setRowSelectionAllowed(true);
        tablePanel.add(tableScrollPane, BorderLayout.CENTER);
        // resourceTable.setRowSorter( rowSorter );

        // Add panels and menu to dialog
        this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        this.getContentPane().add(tablePanel, BorderLayout.CENTER);
        this.getContentPane().add(configPanel, BorderLayout.NORTH);
    }

    /**
     * Show modal Dialog and return resourceMap
     * 
     * @return map of resource information
     */
    public Map<String, ResourceInfo> showDialog() {
        this.setModal(true);
        setVisible(true);
        return tableModel.getResourceMap();
    }

    /**
     * Checks format and online status of resource.
     * 
     * @param url
     * @return - URL or null if URL malformated or resource offline
     */
    private URL checkOnlineResource(String url) {
        URL resURL;
        // check URL format
        try {
            resURL = new URL(url);
        } catch (MalformedURLException e) {
            JOptionPane.showOptionDialog(this, "URL does not have a valid form.", "Malformed URL",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,
                    UIManager.getIcon("OptionPane.errorIcon"), null, null);
            return null;
        }

        // check online status
        try {
            URLConnection resCon = resURL.openConnection();
            resCon.setReadTimeout(500);
            resCon.setConnectTimeout(500);
            resCon.connect();
        } catch (Exception e) {
            JOptionPane.showOptionDialog(this, "Resource could not be reached via input URL.",
                    "Resource Offline", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,
                    UIManager.getIcon("OptionPane.errorIcon"), null, null);
            return null;
        }
        return resURL;
    }

    /**
     * Adds resource with given URL to the resourceMap
     * 
     * @param resURL
     */
    private void addOnlineResource(URL resURL) {
        if (resURL != null) {
            ResourceInfo resInfo = new ResourceInfo();
            resInfo.setURL(resURL);
            String resType = (String) typeChooser.getSelectedItem();
            try {
                switch (resType) {
                case "XML":
                    resInfo.setType(DataResourceType.XML);
                    break;
                case "HTML":
                    resInfo.setType(DataResourceType.HTML);
                    break;
                default:
                    throw new IOException();
                }

                // add resource to table
                UID resID = new UID();
                tableModel.addResource(resID.toString(), resInfo);
            } catch (IOException e) {
                // Unknown Type
                JOptionPane.showOptionDialog(this, "Resource type unknown! Resource type\""
                        + resType + "\" not implemented!", "Unknown Type",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,
                        UIManager.getIcon("OptionPane.errorIcon"), null, null);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == this.addButton) {
            this.setVisible(false);
            this.dispose();
        } else if (event.getSource() == this.cancelButton) {
            this.dispose();
        } else if (event.getSource() == this.urlTextField) {
            URL resURL = checkOnlineResource(urlTextField.getText());
            if (resURL != null) {
                this.addOnlineResource(checkOnlineResource(urlTextField.getText()));
                this.urlTextField.setValue("");
            }
        }
    }

}
