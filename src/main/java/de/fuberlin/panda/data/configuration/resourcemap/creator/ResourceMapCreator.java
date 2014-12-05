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


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import de.fuberlin.panda.data.configuration.resourcemap.ResourceMap;
import de.fuberlin.panda.data.configuration.resourcemap.ResourceMapEntryType.ResourceInfo;
import de.fuberlin.panda.enums.DataResourceType;

/**
 * Main class of resource map configuration creator GUI tool.
 * 
 * @author Christoph Schröder
 */
public class ResourceMapCreator implements ActionListener {

    // Main Window of ConfigFileCreator
    private MainWindow         mainWindow;
    // Path of loaded or last saved Resource Map
    private String             configFilePath;

    // Table
    private ResourceTableModel tableModel;
    private JTable             resourceTable;

    public ResourceMapCreator() {
        mainWindow = new MainWindow(this);
        tableModel = mainWindow.tableModel;
        resourceTable = mainWindow.resourceTable;
        mainWindow.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == mainWindow.addLocalFileButton) {
            this.openFileSelector();
        } else if (event.getSource() == mainWindow.addOnlineResourceButton) {
            openAddResourceDialog();
        } else if (event.getSource() == mainWindow.openButton
                || event.getSource() == mainWindow.openItem) {
            this.openOpenDialog();

        } else if (event.getSource() == mainWindow.saveButton
                || event.getSource() == mainWindow.saveItem) {
            if (configFilePath == null) {
                this.openSaveDialog();
            } else {
                saveConfigFile(new File(configFilePath));
            }
        } else if (event.getSource() == mainWindow.saveAsButton
                || event.getSource() == mainWindow.saveAsItem) {
            this.openSaveDialog();

        } else if (event.getSource() == mainWindow.deleteSelectedButton) {
            this.deleteSelected();
        } else if (event.getSource() == mainWindow.closeItem) {
            System.exit(0);
        }

    }

    /**
     * Delete one or more selected resources from table and resource map.
     */
    private void deleteSelected() {
        int[] delRows = resourceTable.getSelectedRows();
        int i = 0;
        for (int row : delRows) {
            delRows[i] = resourceTable.convertRowIndexToModel(row);
            i++;
        }

        if (delRows.length == 0) {
            JOptionPane.showOptionDialog(mainWindow, "No rows selected!", "Delte Resources Error",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,
                    UIManager.getIcon("OptionPane.errorIcon"), null, null);
        } else {
            tableModel.removeRows(delRows);
        }
    }

    /**
     * Opens a customized JFileChooser Dialog to add local files to the resource
     * map.
     */
    private void openFileSelector() {
        AddLocalFileDialog fileSelectorDialog = new AddLocalFileDialog();
        int ret = fileSelectorDialog.showOpenDialog(mainWindow);
        if (ret == JFileChooser.APPROVE_OPTION) {

            for (File f : fileSelectorDialog.getSelectedFiles()) {
                ResourceInfo resInfo = new ResourceInfo();
                resInfo.setFilePath(f.getAbsolutePath());
                String fileExtension = getFileExtensionName(f);

                try {
                    switch (fileExtension) {
                    case "doc":
                        resInfo.setType(DataResourceType.DOC);
                        break;
                    case "docx":
                        resInfo.setType(DataResourceType.DOCX);
                        break;
                    case "xls":
                        resInfo.setType(DataResourceType.XLS);
                        break;
                    case "xlsx":
                        resInfo.setType(DataResourceType.XLSX);
                        break;
                    case "xml":
                        resInfo.setType(DataResourceType.XML);
                        break;
                    case "html":
                    case "htm":
                        resInfo.setType(DataResourceType.HTML);
                        break;
                    case "pdf":
                        resInfo.setType(DataResourceType.PDF);
                        break;
                    case "csv":
                        resInfo.setType(DataResourceType.CSV);
                        resInfo.setSeparator(fileSelectorDialog.getSeparator());
                        break;
                    default:
                        throw new IOException();
                    }
                    //UID resID = new UID();
                    UUID resID = UUID.randomUUID();
                    tableModel.addResource(resID.toString(), resInfo);
                } catch (IOException e) {
                    // Unknown Type
                    JOptionPane.showOptionDialog(mainWindow,
                            "Resource type unknown! Resource type\"" + fileExtension
                                    + "\" not implemented!", "Unknown Type",
                            JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,
                            UIManager.getIcon("OptionPane.errorIcon"), null, null);
                    break;
                }
            }
        }
    }

    /**
     * Opens dialog to add other resources than local files (e.g. online
     * resources) which need additional user input
     */
    private void openAddResourceDialog() {
        AddResourceDialog addResDialog = new AddResourceDialog(this.mainWindow);
        Map<String, ResourceInfo> newResources = addResDialog.showDialog();
        for (String resID : newResources.keySet()) {
            tableModel.addResource(resID, newResources.get(resID));
        }
    }

    /**
     * Opens preconfigured dialog to save resource map to configuration file.
     */
    private void openSaveDialog() {
        SaveDialog saveDialog = new SaveDialog();
        saveDialog.setDialogTitle("Save Resource Configuration File");
        int ret = saveDialog.showSaveDialog(mainWindow);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File f = saveDialog.getSelectedFile();
            saveConfigFile(f);
        }
    }

    /**
     * Opens preconfigured dialog to open configuration and unmarshal it into
     * local resource map.
     */
    private void openOpenDialog() {
        SaveDialog openDialog = new SaveDialog();
        openDialog.setDialogTitle("Open Resource Configuration File");
        int ret = openDialog.showOpenDialog(mainWindow);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File configFile = openDialog.getSelectedFile();
            this.openConfigFile(configFile);
        }
    }

    /**
     * Helper method to get file extension.
     * 
     * @param file - file
     * @return
     */
    private String getFileExtensionName(File file) {
        int extIndex = file.getName().lastIndexOf(".");

        if (extIndex == -1) {
            return "";
        } else {
            return file.getName().substring(extIndex + 1);
        }
    }

    /**
     * Save local resource map.
     * 
     * @param file - file to save resource map.
     */
    private void saveConfigFile(File file) {
        try {

            String fileExtension = getFileExtensionName(file);
            if (fileExtension == "") {
                file = new File(file.getAbsolutePath() + ".xml");
            }
            if (!file.exists()) {
                file.createNewFile();
            }

            JAXBContext jaxbContext = JAXBContext.newInstance(ResourceMap.class);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.marshal(new ResourceMap(tableModel.getResourceMap()), file);
        } catch (JAXBException jaxbE) {
            // handle JAXB Exception
            JOptionPane.showOptionDialog(mainWindow, "Internal JAXB marshalling error.",
                    "JAXB Error", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,
                    UIManager.getIcon("OptionPane.errorIcon"), null, null);
        } catch (IOException ioE) {
            // handle IOException
            JOptionPane.showOptionDialog(mainWindow, "Error while saving configuration file!",
                    "Save File Error", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,
                    UIManager.getIcon("OptionPane.errorIcon"), null, null);
        }
    }

    /**
     * Loads an existing configuration file.
     * 
     * @param configFile - configuration tile to open
     */
    private void openConfigFile(File configFile) {

        try {
            // unmarshal file and set local resourceMap + filePath
            JAXBContext jaxbContext = JAXBContext.newInstance(ResourceMap.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            ResourceMap resourceMap = (ResourceMap) unmarshaller.unmarshal(configFile);
            this.configFilePath = configFile.getAbsolutePath();
            tableModel.setResourceMap(resourceMap.getMap());

        } catch (JAXBException e) {
            // handle JAXB Exception
            JOptionPane.showOptionDialog(mainWindow,
                    "JAXB error, file does not exist or does not conform the expected structure.",
                    "JAXB Error", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,
                    UIManager.getIcon("OptionPane.errorIcon"), null, null);
        }
    }

    public static void main(String[] args) {
        new ResourceMapCreator();
    }

}
